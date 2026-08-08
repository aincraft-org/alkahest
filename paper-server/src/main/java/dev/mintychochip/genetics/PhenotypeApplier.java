package dev.mintychochip.genetics;

import dev.mintychochip.genetics.dto.PhenotypeSnapshot;
import dev.mintychochip.genetics.dto.PhenotypeVariantResolver;
import dev.mintychochip.genetics.model.Genome;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.Optional;
import net.minecraft.world.entity.animal.Animal;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.craftbukkit.entity.CraftEntityType;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Wolf;
import org.jspecify.annotations.Nullable;

/**
 * Pushes decoded genetics phenotypes onto Minecraft registry variants on live entities.
 *
 * <p>Genotype / {@link PhenotypeSnapshot} remain source of truth; this only sets
 * client-visible {@code Cat.Type}, {@code Cow.Variant}, {@code Wolf.Variant}, etc.
 */
public final class PhenotypeApplier {

    private PhenotypeApplier() {
    }

    /**
     * Decode genome with the server catalog and apply matching registry variants.
     *
     * @return true if at least one visual property was changed
     */
    public static boolean apply(final Animal animal, final Genome genome) {
        final PhenotypeSnapshot phenotype = AnimalGenetics.phenotypeOf(genome);
        return apply(animal, phenotype);
    }

    /**
     * Apply a pre-decoded phenotype snapshot to the animal's Bukkit facade.
     */
    public static boolean apply(final Animal animal, final PhenotypeSnapshot phenotype) {
        final Entity bukkit = animal.getBukkitEntity();
        final EntityType type = CraftEntityType.minecraftToBukkit(animal.getType());
        final Optional<NamespacedKey> variantKey = PhenotypeVariantResolver.resolve(type, phenotype);
        if (variantKey.isEmpty()) {
            return false;
        }
        return applyVariantKey(bukkit, type, variantKey.get());
    }

    /**
     * Apply an explicit registry key (e.g. from {@link dev.mintychochip.genetics.dto.BreedGenetics#childVariant()}).
     */
    public static boolean applyVariantKey(
        final Entity bukkit,
        final EntityType type,
        final @Nullable NamespacedKey key
    ) {
        if (key == null) {
            return false;
        }
        // EntityType is an interface; compare against vanilla constants by identity
        if (type == EntityType.CAT) {
            return applyCat(bukkit, key);
        }
        if (type == EntityType.WOLF) {
            return applyWolf(bukkit, key);
        }
        if (type == EntityType.COW) {
            return applyCow(bukkit, key);
        }
        return false;
    }

    private static boolean applyCat(final Entity bukkit, final NamespacedKey key) {
        if (!(bukkit instanceof Cat cat)) {
            return false;
        }
        final Cat.Type type = registry(RegistryKey.CAT_VARIANT).get(key);
        if (type == null) {
            return false;
        }
        cat.setCatType(type);
        return true;
    }

    private static boolean applyWolf(final Entity bukkit, final NamespacedKey key) {
        if (!(bukkit instanceof Wolf wolf)) {
            return false;
        }
        final Wolf.Variant variant = registry(RegistryKey.WOLF_VARIANT).get(key);
        if (variant == null) {
            return false;
        }
        wolf.setVariant(variant);
        return true;
    }

    private static boolean applyCow(final Entity bukkit, final NamespacedKey key) {
        if (!(bukkit instanceof Cow cow)) {
            return false;
        }
        final Cow.Variant variant = registry(RegistryKey.COW_VARIANT).get(key);
        if (variant == null) {
            return false;
        }
        cow.setVariant(variant);
        return true;
    }

    private static <T extends org.bukkit.Keyed> Registry<T> registry(final RegistryKey<T> key) {
        return RegistryAccess.registryAccess().getRegistry(key);
    }
}
