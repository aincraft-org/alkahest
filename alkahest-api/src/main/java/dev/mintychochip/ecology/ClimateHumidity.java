package dev.mintychochip.ecology;

/**
 * Composes continuous humidity for ecology sampling.
 *
 * <p>Base biome downfall is pulled toward 1 by nearby water, then again by
 * active rain (not snow). Both bonuses use the same asymptotic blend so arid
 * cells gain more from moisture and wet cells are not pushed past 1.
 */
public final class ClimateHumidity {
    private ClimateHumidity() {
    }

    /**
     * @param baseHumidity biome downfall clamped to {@code [0, 1]}
     * @param waterBonus   water-proximity contribution already capped (e.g. {@code [0, waterBonusCap]})
     * @param raining      true when liquid rain is falling on the sample (not snow)
     * @param rainBonus    rain pull strength in {@code [0, 1]} (0 disables)
     * @return humidity in {@code [0, 1]}
     */
    public static double compose(
        final double baseHumidity,
        final double waterBonus,
        final boolean raining,
        final double rainBonus
    ) {
        double h = clamp01(baseHumidity);
        final double water = Math.max(0.0, waterBonus);
        h = h + water * (1.0 - h);
        if (raining) {
            final double rain = clamp01(rainBonus);
            h = h + rain * (1.0 - h);
        }
        return clamp01(h);
    }

    private static double clamp01(final double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
