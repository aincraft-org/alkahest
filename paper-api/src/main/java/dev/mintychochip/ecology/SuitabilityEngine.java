package dev.mintychochip.ecology;

/** Crop × climate suitability; thresholds from {@link EcologyConfig}. */
public final class SuitabilityEngine {
    private SuitabilityEngine() {
    }

    public static double tier(final CropProfile crop, final String region) {
        if (crop.unrestricted()) {
            return 1.0;
        }
        if (crop.nativeCategories().contains(region)) {
            return 1.0;
        }
        if (crop.tolerantCategories().contains(region)) {
            return EcologyConfig.get().tolerantTier();
        }
        return 0.0;
    }

    public static double suitability(final CropProfile crop, final ClimateSample cell) {
        return tier(crop, cell.region())
            * trapezoid(cell.humidityValue(), crop.minHumidity(), crop.maxHumidity());
    }

    public static boolean isSuitable(final CropProfile crop, final ClimateSample cell) {
        return suitability(crop, cell) >= EcologyConfig.get().minSuitability();
    }

    public static double acceptProbability(final CropProfile crop, final ClimateSample cell) {
        final double s = suitability(crop, cell);
        if (s < EcologyConfig.get().minSuitability()) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, s));
    }

    static double trapezoid(final double v, final double min, final double max) {
        final double shoulder = EcologyConfig.get().rangeShoulder();
        if (shoulder <= 0.0) {
            return (v >= min && v <= max) ? 1.0 : 0.0;
        }
        if (v < min - shoulder || v > max + shoulder) {
            return 0.0;
        }
        if (v < min) {
            return (v - (min - shoulder)) / shoulder;
        }
        if (v > max) {
            return ((max + shoulder) - v) / shoulder;
        }
        return 1.0;
    }
}