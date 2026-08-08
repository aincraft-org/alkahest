package dev.mintychochip.ecology;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

/**
 * Public entry point for mintychochip crop ecology (config, catalog, suitability).
 *
 * <p>Pure API — no NMS. Server code uses {@code CropEcology} for world hooks.
 * Call {@link #load(Path)} once with the server root so {@link #settings()}
 * reflects {@code config/mintychochip/ecology.json}.
 */
public final class Ecology {
    private Ecology() {
    }

    /** Load or create {@code config/mintychochip/ecology.json} under {@code serverDirectory}. */
    public static EcologySettings load(final Path serverDirectory) {
        return EcologyConfig.ensureLoaded(Objects.requireNonNull(serverDirectory, "serverDirectory"));
    }

    /** Active settings (defaults until {@link #load(Path)} runs). */
    public static EcologySettings settings() {
        return EcologyConfig.get();
    }

    public static CropCatalog catalog() {
        return settings().catalog();
    }

    public static CropProfile crop(final String blockKey) {
        return catalog().forBlock(blockKey);
    }

    public static Collection<CropProfile> crops() {
        return catalog().all().values();
    }

    public static String regionForBiome(final String biomeKey) {
        return BiomeCategories.category(biomeKey);
    }

    public static double suitability(final CropProfile crop, final ClimateSample climate) {
        return SuitabilityEngine.suitability(crop, climate);
    }

    public static boolean isSuitable(final CropProfile crop, final ClimateSample climate) {
        return SuitabilityEngine.isSuitable(crop, climate);
    }

    public static double acceptProbability(final CropProfile crop, final ClimateSample climate) {
        return SuitabilityEngine.acceptProbability(crop, climate);
    }

    public static boolean animalsEnabled() {
        return settings().animalsEnabled();
    }

    public static AnimalCatalog animalCatalog() {
        return settings().animalCatalog();
    }

    public static java.util.Optional<AnimalProfile> selectAnimal(
        final ClimateSample climate,
        final dev.mintychochip.season.Season season,
        final java.util.Random rng
    ) {
        return AnimalSelector.select(animalCatalog(), climate, season, rng);
    }
}
