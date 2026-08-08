package dev.mintychochip.genetics;

import dev.mintychochip.genetics.catalog.DefaultGeneticsCatalog;
import dev.mintychochip.genetics.dna.MutationSettings;
import dev.mintychochip.genetics.dto.BreedGenetics;
import dev.mintychochip.genetics.dto.GenotypeSnapshot;
import dev.mintychochip.genetics.dto.PhenotypeDecoder;
import dev.mintychochip.genetics.dto.PhenotypeSnapshot;
import dev.mintychochip.genetics.dto.PhenotypeVariantResolver;
import dev.mintychochip.genetics.engine.BreedingEngine;
import dev.mintychochip.genetics.engine.BreedingResult;
import dev.mintychochip.genetics.engine.GeneticMatePolicy;
import dev.mintychochip.genetics.engine.RecombinationSettings;
import dev.mintychochip.genetics.io.GenomeCodec;
import dev.mintychochip.genetics.model.Genome;
import dev.mintychochip.genetics.model.GenomeGenerator;
import dev.mintychochip.genetics.model.LocusCatalog;
import dev.mintychochip.genetics.model.Sex;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.entity.CraftEntityType;
import org.bukkit.entity.EntityType;
import org.jspecify.annotations.Nullable;

/**
 * Server façade: genome attach/persist on animals, opposite-sex mate gate,
 * and child genomes from the pure {@link BreedingEngine}.
 *
 * <p>Logic lives in paper-api; this class only bridges NMS entities.
 */
public final class AnimalGenetics {

    public static final String NBT_KEY = "MintyGenome";

    private static final Map<UUID, Genome> CACHE = new ConcurrentHashMap<>();
    private static final LocusCatalog CATALOG = DefaultGeneticsCatalog.get();
    private static final PhenotypeDecoder PHENOTYPE = new PhenotypeDecoder(CATALOG);
    private static volatile boolean enabled = true;

    private AnimalGenetics() {
    }

    /**
     * Outcome of a genetic cross ready for {@link org.bukkit.event.entity.EntityBreedEvent}.
     * Mother/father are chromosomal dam/sire entities.
     */
    public record BreedPrep(
        Animal mother,
        Animal father,
        Genome childGenome,
        BreedGenetics genetics
    ) {
    }

    public static void setEnabled(final boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static LocusCatalog catalog() {
        return CATALOG;
    }

    public static void clearCache() {
        CACHE.clear();
    }

    // ------------------------------------------------------------------
    // Persistence (entity NBT + in-memory cache)
    // ------------------------------------------------------------------

    public static void save(final Animal animal, final ValueOutput output) {
        final Genome genome = CACHE.get(animal.getUUID());
        if (genome != null) {
            output.putString(NBT_KEY, GenomeCodec.encode(genome));
        }
    }

    public static void load(final Animal animal, final ValueInput input) {
        input.getString(NBT_KEY).ifPresent(encoded -> {
            try {
                final Genome genome = GenomeCodec.decode(encoded);
                CACHE.put(animal.getUUID(), genome);
                // Re-apply looks so NBT genome wins over any vanilla default variant.
                PhenotypeApplier.apply(animal, genome);
            } catch (final RuntimeException ignored) {
                // Corrupt data: regenerate on next access.
            }
        });
    }

    public static void remove(final Entity entity) {
        CACHE.remove(entity.getUUID());
    }

    /**
     * Direct attach for tests and tools (no entity required beyond UUID keying).
     */
    public static void setGenome(final UUID entityId, final Genome genome) {
        CACHE.put(entityId, genome);
    }

    public static @Nullable Genome getGenome(final UUID entityId) {
        return CACHE.get(entityId);
    }

    public static void setGenome(final Animal animal, final Genome genome) {
        CACHE.put(animal.getUUID(), genome);
        PhenotypeApplier.apply(animal, genome);
    }

    public static @Nullable Genome getGenome(final Animal animal) {
        return CACHE.get(animal.getUUID());
    }

    /**
     * Round-trip helper for tests: encode → decode without touching entities.
     */
    public static Genome roundTrip(final Genome genome) {
        return GenomeCodec.decode(GenomeCodec.encode(genome));
    }

    public static PhenotypeSnapshot phenotypeOf(final Genome genome) {
        return PHENOTYPE.decode(genome);
    }

    public static Genome getOrCreate(final Animal animal, final RandomSource random) {
        final Genome existing = CACHE.get(animal.getUUID());
        if (existing != null) {
            return existing;
        }
        final Sex sex = random.nextBoolean() ? Sex.MALE : Sex.FEMALE;
        final Genome created = new GenomeGenerator(CATALOG, asGenerator(random)).generate(sex);
        CACHE.put(animal.getUUID(), created);
        // First attach: push coat (etc.) onto registry variants so looks match genome.
        PhenotypeApplier.apply(animal, created);
        return created;
    }

    /**
     * After a successful (non-cancelled) breed, paint the child from its genome.
     */
    public static void applyChildAppearance(final @Nullable AgeableMob offspring) {
        if (offspring == null || !(offspring instanceof Animal animal)) {
            return;
        }
        final Genome genome = CACHE.get(animal.getUUID());
        if (genome != null) {
            PhenotypeApplier.apply(animal, genome);
        }
    }

    // ------------------------------------------------------------------
    // Mate / breed bridge (invoked from thin vanilla hooks)
    // ------------------------------------------------------------------

    /**
     * Additional mate gate after vanilla same-class / in-love checks.
     * Opposite sex only when both have (or receive) genomes.
     */
    public static boolean allowsMate(final Animal self, final Animal partner) {
        if (!enabled) {
            return true;
        }
        final Genome a = getOrCreate(self, self.getRandom());
        final Genome b = getOrCreate(partner, partner.getRandom());
        return GeneticMatePolicy.allowsMate(a, b);
    }

    /**
     * After vanilla creates the baby entity, assign a recombinant genome.
     * No-op if disabled or same-sex (should not reach here if canMate enforced).
     *
     * @deprecated prefer {@link #prepareBreed} so {@code EntityBreedEvent} gets metadata
     *     and cancel can discard the child genome
     */
    @Deprecated
    public static void onBreed(final Animal parentA, final Animal parentB, final @Nullable AgeableMob offspring) {
        prepareBreed(parentA, parentB, offspring);
    }

    /**
     * Run the genetic cross, attach the child genome, and build event metadata.
     *
     * <p>Call before {@code EntityBreedEvent}. If the event is cancelled, call
     * {@link #discardBreed(AgeableMob)} so the orphan genome is not left in cache.
     *
     * @return prep with genetic mother/father + {@link BreedGenetics}, or null if
     *     genetics disabled / no offspring / same-sex (should not happen after canMate)
     */
    public static @Nullable BreedPrep prepareBreed(
        final Animal parentA,
        final Animal parentB,
        final @Nullable AgeableMob offspring
    ) {
        if (!enabled || offspring == null) {
            return null;
        }
        final Genome ga = getOrCreate(parentA, parentA.getRandom());
        final Genome gb = getOrCreate(parentB, parentB.getRandom());
        final Optional<BreedingResult> result = cross(ga, gb, parentA.getRandom());
        if (result.isEmpty()) {
            return null;
        }
        final Genome child = result.get().child();
        CACHE.put(offspring.getUUID(), child);

        final Animal mother = ga.sex() == Sex.FEMALE ? parentA : parentB;
        final Animal father = ga.sex() == Sex.MALE ? parentA : parentB;
        final Genome motherGenome = mother == parentA ? ga : gb;
        final Genome fatherGenome = father == parentA ? ga : gb;

        final EntityType childType = CraftEntityType.minecraftToBukkit(offspring.getType());
        return new BreedPrep(mother, father, child, snapshotsOf(motherGenome, fatherGenome, child, childType));
    }

    /**
     * Drop a child genome after a cancelled breed (entity never enters the world).
     */
    public static void discardBreed(final @Nullable AgeableMob offspring) {
        if (offspring != null) {
            discardGenome(offspring.getUUID());
        }
    }

    /**
     * Drop a cached genome by entity id (cancel path / tests).
     */
    public static void discardGenome(final UUID entityId) {
        CACHE.remove(entityId);
    }

    /**
     * Build plugin-facing breed metadata from three genomes (tests + event payload).
     */
    public static BreedGenetics snapshotsOf(final Genome mother, final Genome father, final Genome child) {
        return snapshotsOf(mother, father, child, null);
    }

    /**
     * Build breed metadata and resolve the child's registry variant for {@code childType}.
     */
    public static BreedGenetics snapshotsOf(
        final Genome mother,
        final Genome father,
        final Genome child,
        final @Nullable EntityType childType
    ) {
        final PhenotypeSnapshot childPhenotype = PHENOTYPE.decode(child);
        final NamespacedKey childVariant = childType == null
            ? null
            : PhenotypeVariantResolver.resolve(childType, childPhenotype).orElse(null);
        return new BreedGenetics(
            GenotypeSnapshot.from(mother, CATALOG),
            GenotypeSnapshot.from(father, CATALOG),
            GenotypeSnapshot.from(child, CATALOG),
            PHENOTYPE.decode(mother),
            PHENOTYPE.decode(father),
            childPhenotype,
            childVariant
        );
    }

    /**
     * Pure cross entry used by tests and {@link #onBreed}.
     */
    public static Optional<BreedingResult> cross(
        final Genome parentA,
        final Genome parentB,
        final RandomSource random
    ) {
        if (!GeneticMatePolicy.allowsMate(parentA, parentB)) {
            return Optional.empty();
        }
        final BreedingEngine engine = new BreedingEngine(
            CATALOG,
            RecombinationSettings.DEFAULT,
            MutationSettings.DEFAULT,
            asGenerator(random)
        );
        return engine.cross(parentA, parentB);
    }

    public static Optional<BreedingResult> cross(
        final Genome parentA,
        final Genome parentB,
        final RandomGenerator random,
        final MutationSettings mutation,
        final RecombinationSettings recombination
    ) {
        if (!GeneticMatePolicy.allowsMate(parentA, parentB)) {
            return Optional.empty();
        }
        return new BreedingEngine(CATALOG, recombination, mutation, random).cross(parentA, parentB);
    }

    private static RandomGenerator asGenerator(final RandomSource random) {
        // Adapt Minecraft RandomSource to RandomGenerator without sharing state oddly:
        // wrap each call. RandomSource is already the entity's RNG.
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return random.nextLong();
            }

            @Override
            public double nextDouble() {
                return random.nextDouble();
            }

            @Override
            public int nextInt() {
                return random.nextInt();
            }

            @Override
            public int nextInt(final int bound) {
                return random.nextInt(bound);
            }

            @Override
            public boolean nextBoolean() {
                return random.nextBoolean();
            }

            @Override
            public float nextFloat() {
                return random.nextFloat();
            }
        };
    }
}
