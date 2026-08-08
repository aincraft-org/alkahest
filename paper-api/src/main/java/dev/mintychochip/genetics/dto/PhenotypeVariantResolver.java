package dev.mintychochip.genetics.dto;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.jspecify.annotations.Nullable;

/**
 * Maps abstract {@link PhenotypeSnapshot} traits onto Minecraft registry variant keys.
 *
 * <p>Pure / NMS-free: returns {@link NamespacedKey}s that the server applier
 * looks up in {@code CAT_VARIANT}, {@code COW_VARIANT}, etc. Genotype and
 * phenotype DTOs stay the genetics source of truth; registry variants are the
 * client-visible apply target.
 */
public final class PhenotypeVariantResolver {

    public static final String TRAIT_COAT = "coat";

    private PhenotypeVariantResolver() {
    }

    /**
     * Resolve the primary visual variant key for a species from decoded traits.
     *
     * @return empty when no mapping applies (leave vanilla appearance alone)
     */
    public static Optional<NamespacedKey> resolve(
        final EntityType type,
        final PhenotypeSnapshot phenotype
    ) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(phenotype, "phenotype");
        if (type == EntityType.CAT) {
            return coatToCatVariant(phenotype.getOrNull(TRAIT_COAT));
        }
        if (type == EntityType.WOLF) {
            return coatToWolfVariant(phenotype.getOrNull(TRAIT_COAT));
        }
        if (type == EntityType.COW) {
            return coatToCowVariant(phenotype.getOrNull(TRAIT_COAT));
        }
        return Optional.empty();
    }

    /**
     * Classic orange-system coat labels → {@code minecraft:cat_variant/*}.
     *
     * <ul>
     *   <li>{@code calico} → calico
     *   <li>{@code O} / {@code ORANGE} → red
     *   <li>{@code o} / {@code BLACK} → black
     *   <li>other known labels → best-effort; unknown → empty
     * </ul>
     */
    public static Optional<NamespacedKey> coatToCatVariant(final @Nullable String coat) {
        if (coat == null || coat.isEmpty()) {
            return Optional.empty();
        }
        // PhenotypeDecoder keeps classic O/o case-sensitive.
        if (coat.equals("O")) {
            return Optional.of(minecraft("red"));
        }
        if (coat.equals("o")) {
            return Optional.of(minecraft("black"));
        }
        final String n = normalize(coat);
        return switch (n) {
            case "calico" -> Optional.of(minecraft("calico"));
            case "orange", "red" -> Optional.of(minecraft("red"));
            case "black", "b", "non_orange", "all_black" -> Optional.of(minecraft("all_black"));
            case "tabby" -> Optional.of(minecraft("tabby"));
            case "white" -> Optional.of(minecraft("white"));
            case "jellie" -> Optional.of(minecraft("jellie"));
            case "siamese" -> Optional.of(minecraft("siamese"));
            case "persian" -> Optional.of(minecraft("persian"));
            case "ragdoll" -> Optional.of(minecraft("ragdoll"));
            case "british_shorthair" -> Optional.of(minecraft("british_shorthair"));
            default -> Optional.empty();
        };
    }

    /**
     * Best-effort wolf coat → {@code minecraft:wolf_variant/*}.
     * Vanilla wolf variants are biome-themed; map orange-system loosely.
     */
    public static Optional<NamespacedKey> coatToWolfVariant(final @Nullable String coat) {
        if (coat == null || coat.isEmpty()) {
            return Optional.empty();
        }
        if (coat.equals("O") || equalsIgnore(coat, "orange") || equalsIgnore(coat, "red")) {
            return Optional.of(minecraft("rusty"));
        }
        if (coat.equals("o") || equalsIgnore(coat, "black") || equalsIgnore(coat, "b")) {
            return Optional.of(minecraft("black"));
        }
        if (equalsIgnore(coat, "calico") || coat.contains("+")) {
            // No true calico wolf — pale is a distinctive mixed look.
            return Optional.of(minecraft("pale"));
        }
        return Optional.empty();
    }

    /**
     * Cow climate variants are not coat genetics; only map explicit labels if present.
     * Default orange-system labels leave the cow's existing variant unchanged.
     */
    public static Optional<NamespacedKey> coatToCowVariant(final @Nullable String coat) {
        if (coat == null || coat.isEmpty()) {
            return Optional.empty();
        }
        return switch (normalize(coat)) {
            case "cold" -> Optional.of(minecraft("cold"));
            case "warm" -> Optional.of(minecraft("warm"));
            case "temperate" -> Optional.of(minecraft("temperate"));
            default -> Optional.empty();
        };
    }

    private static NamespacedKey minecraft(final String path) {
        return NamespacedKey.minecraft(path);
    }

    private static String normalize(final String coat) {
        return coat.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static boolean equalsIgnore(final String a, final String b) {
        return a.equalsIgnoreCase(b);
    }
}
