package dev.mintychochip.season;

/**
 * Pure seasonal temperature adjustments for native precipitation decisions.
 *
 * <p>Vanilla treats temperatures below {@link #SNOW_THRESHOLD} as cold enough
 * for snow ({@code Biome.warmEnoughToRain} / {@code coldEnoughToSnow}).
 */
public final class SeasonClimate {
    /** Vanilla snow/rain threshold used by {@code Biome.warmEnoughToRain}. */
    public static final float SNOW_THRESHOLD = 0.15F;

    private SeasonClimate() {
    }

    /**
     * Fixed seasonal swing applied on top of biome height-adjusted temperature.
     * Winter is colder; summer is warmer — matching the ecology reference model.
     */
    public static float temperatureSwing(final Season season) {
        return switch (season) {
            case WINTER -> -0.3F;
            case AUTUMN -> -0.1F;
            case SPRING -> 0.1F;
            case SUMMER -> 0.3F;
        };
    }

    /** Base temperature plus seasonal swing. */
    public static float adjustTemperature(final float baseTemperature, final Season season) {
        return baseTemperature + temperatureSwing(season);
    }

    /**
     * Whether the adjusted temperature is cold enough for snow (strict less than
     * the vanilla 0.15 rain threshold, i.e. not warm enough to rain).
     */
    public static boolean coldEnoughToSnow(final float baseTemperature, final Season season) {
        return adjustTemperature(baseTemperature, season) < SNOW_THRESHOLD;
    }

    /**
     * Whether seasonal temperature is warm enough that placed snow should thaw.
     * Inverse of {@link #coldEnoughToSnow}; permanently frozen biomes stay snowy
     * even in summer, while winter-only snow (e.g. temperate taiga) melts after spring.
     */
    public static boolean warmEnoughToMeltSnow(final float baseTemperature, final Season season) {
        return !coldEnoughToSnow(baseTemperature, season);
    }

    /** Winter forces world rain so {@code tickPrecipitation} can place snow. */
    public static boolean shouldForcePrecipitation(final Season season) {
        return season == Season.WINTER;
    }
}
