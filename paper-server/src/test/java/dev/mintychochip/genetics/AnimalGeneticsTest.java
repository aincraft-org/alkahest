package dev.mintychochip.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.genetics.catalog.DefaultGeneticsCatalog;
import dev.mintychochip.genetics.dna.MutationSettings;
import dev.mintychochip.genetics.engine.BreedingResult;
import dev.mintychochip.genetics.engine.GeneticMatePolicy;
import dev.mintychochip.genetics.engine.RecombinationSettings;
import dev.mintychochip.genetics.io.GenomeCodec;
import dev.mintychochip.genetics.model.Allele;
import dev.mintychochip.genetics.model.GeneCopy;
import dev.mintychochip.genetics.model.Genome;
import dev.mintychochip.genetics.model.Sex;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.bukkit.support.environment.Normal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Server façade tests — real {@link AnimalGenetics} + {@link GenomeCodec}, no live world.
 */
@Normal
public class AnimalGeneticsTest {

    @AfterEach
    public void cleanup() {
        AnimalGenetics.clearCache();
        AnimalGenetics.setEnabled(true);
    }

    @Test
    public void setGenomeThenGetReturnsSameState() {
        final UUID id = UUID.randomUUID();
        final Genome genome = sampleFemale();
        AnimalGenetics.setGenome(id, genome);
        final Genome loaded = AnimalGenetics.getGenome(id);
        assertNotNull(loaded);
        assertTrue(GenomeCodec.deepEquals(genome, loaded));
    }

    @Test
    public void codecRoundTripViaServerHelper() {
        final Genome genome = sampleMale();
        final Genome back = AnimalGenetics.roundTrip(genome);
        assertTrue(GenomeCodec.deepEquals(genome, back));
        assertTrue(back.getOrNull(DefaultGeneticsCatalog.COAT.id()).isHemizygous());
    }

    @Test
    public void sameSexCrossRejected() {
        final Genome maleA = sampleMale();
        final Genome maleB = sampleMale();
        assertFalse(GeneticMatePolicy.allowsMate(maleA, maleB));
        final Optional<BreedingResult> result = AnimalGenetics.cross(
            maleA,
            maleB,
            new Random(1L),
            MutationSettings.NONE,
            RecombinationSettings.NONE
        );
        assertTrue(result.isEmpty(), "same-sex must not produce a breeding result");
    }

    @Test
    public void oppositeSexCrossProducesChildGenomeFromEngine() {
        final Genome father = sampleMale();
        final Genome mother = sampleFemale();
        assertTrue(GeneticMatePolicy.allowsMate(father, mother));

        final Optional<BreedingResult> result = AnimalGenetics.cross(
            father,
            mother,
            new Random(42L),
            MutationSettings.NONE,
            RecombinationSettings.NONE
        );
        assertTrue(result.isPresent());
        final Genome child = result.get().child();
        assertNotNull(child);
        assertTrue(child.get(DefaultGeneticsCatalog.COAT.id()).isPresent());
        assertTrue(child.get(DefaultGeneticsCatalog.VITALITY.id()).isPresent());
        assertTrue(child.get(DefaultGeneticsCatalog.MT_VIGOR.id()).isPresent());

        // mt-vigor from mother only (same sequence with NONE mutation)
        final Allele motherMt = mother.getOrNull(DefaultGeneticsCatalog.MT_VIGOR.id()).alleleA();
        final Allele childMt = child.getOrNull(DefaultGeneticsCatalog.MT_VIGOR.id()).alleleA();
        assertEquals(motherMt.sequence(), childMt.sequence());

        if (child.sex() == Sex.MALE) {
            assertTrue(child.getOrNull(DefaultGeneticsCatalog.COAT.id()).isHemizygous());
            // son X from dam (black)
            assertEquals(
                mother.getOrNull(DefaultGeneticsCatalog.COAT.id()).alleleA().sequence(),
                child.getOrNull(DefaultGeneticsCatalog.COAT.id()).alleleA().sequence()
            );
        } else {
            assertTrue(child.getOrNull(DefaultGeneticsCatalog.COAT.id()).isDiploid());
        }
    }

    @Test
    public void nbtKeyConstantStableForPersistence() {
        assertEquals("MintyGenome", AnimalGenetics.NBT_KEY);
    }

    @Test
    public void snapshotsOfExposesMotherFatherChildSexAndPhenotypes() {
        final Genome mother = sampleFemale();
        final Genome father = sampleMale();
        final Optional<BreedingResult> result = AnimalGenetics.cross(
            father,
            mother,
            new Random(7L),
            MutationSettings.NONE,
            RecombinationSettings.NONE
        );
        assertTrue(result.isPresent());
        final Genome child = result.get().child();

        final var genetics = AnimalGenetics.snapshotsOf(mother, father, child);
        assertEquals(Sex.FEMALE, genetics.motherSex());
        assertEquals(Sex.MALE, genetics.fatherSex());
        assertEquals(child.sex(), genetics.childSex());
        assertNotNull(genetics.motherPhenotype());
        assertNotNull(genetics.fatherPhenotype());
        assertNotNull(genetics.childPhenotype());
        assertFalse(genetics.child().loci().isEmpty());
    }

    @Test
    public void snapshotsOfResolvesCatChildVariantFromCoatPhenotype() {
        final Genome mother = Genome.builder(Sex.FEMALE)
            .put(DefaultGeneticsCatalog.COAT, GeneCopy.diploid(
                Allele.of("ATGAAACCC", "O"),
                Allele.of("ATGCCCGGG", "o")
            ))
            .put(DefaultGeneticsCatalog.VITALITY, GeneCopy.diploid(Allele.of("ATGGGGGGG"), Allele.of("ATGGGGGGG")))
            .put(DefaultGeneticsCatalog.MT_VIGOR, GeneCopy.diploid(Allele.of("ATGTTTTTT"), Allele.of("ATGTTTTTT")))
            .build();
        final Genome father = sampleMale();
        // Force female child path isn't controlled here; resolve against mother phenotype as stand-in for calico female.
        final var genetics = AnimalGenetics.snapshotsOf(mother, father, mother, org.bukkit.entity.EntityType.CAT);
        assertEquals(Optional.of(org.bukkit.NamespacedKey.minecraft("calico")), genetics.childVariant());
    }

    @Test
    public void discardGenomeRemovesCachedChildAfterCancelledBreed() {
        final UUID childId = UUID.randomUUID();
        AnimalGenetics.setGenome(childId, sampleFemale());
        assertNotNull(AnimalGenetics.getGenome(childId));
        AnimalGenetics.discardGenome(childId);
        assertEquals(null, AnimalGenetics.getGenome(childId));
    }

    private static Genome sampleMale() {
        return Genome.builder(Sex.MALE)
            .put(DefaultGeneticsCatalog.COAT, GeneCopy.hemizygous(Allele.of("ATGAAACCC", "O")))
            .put(DefaultGeneticsCatalog.VITALITY, GeneCopy.diploid(Allele.of("ATGAAAAAA"), Allele.of("ATGAAAAAA")))
            .put(DefaultGeneticsCatalog.MT_VIGOR, GeneCopy.diploid(Allele.of("ATGCCCCCC"), Allele.of("ATGCCCCCC")))
            .build();
    }

    private static Genome sampleFemale() {
        return Genome.builder(Sex.FEMALE)
            .put(DefaultGeneticsCatalog.COAT, GeneCopy.diploid(Allele.of("ATGCCCGGG", "o"), Allele.of("ATGCCCGGG", "o")))
            .put(DefaultGeneticsCatalog.VITALITY, GeneCopy.diploid(Allele.of("ATGGGGGGG"), Allele.of("ATGGGGGGG")))
            .put(DefaultGeneticsCatalog.MT_VIGOR, GeneCopy.diploid(Allele.of("ATGTTTTTT"), Allele.of("ATGTTTTTT")))
            .build();
    }
}
