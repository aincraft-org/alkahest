package dev.mintychochip.ecology;

import java.util.Collections;
import java.util.Map;

/** Loaded ecology knobs: water, rain humidity, suitability, biomes, crops, animals. */
public final class EcologySettings {
    private final int waterRadius;
    private final double waterBonusCap;
    /** Pull toward 1.0 applied while liquid rain falls at the sample (not snow). */
    private final double rainHumidityBonus;
    private final double minSuitability;
    private final double rangeShoulder;
    private final double tolerantTier;
    private final Map<String, String> biomeCategories;
    private final CropCatalog catalog;
    private final boolean animalsEnabled;
    private final AnimalCatalog animalCatalog;

    public EcologySettings(
        final int waterRadius,
        final double waterBonusCap,
        final double rainHumidityBonus,
        final double minSuitability,
        final double rangeShoulder,
        final double tolerantTier,
        final Map<String, String> biomeCategories,
        final CropCatalog catalog,
        final boolean animalsEnabled,
        final AnimalCatalog animalCatalog
    ) {
        this.waterRadius = Math.max(0, waterRadius);
        this.waterBonusCap = Math.max(0.0, Math.min(1.0, waterBonusCap));
        this.rainHumidityBonus = Math.max(0.0, Math.min(1.0, rainHumidityBonus));
        this.minSuitability = Math.max(0.0, Math.min(1.0, minSuitability));
        this.rangeShoulder = Math.max(0.0, Math.min(0.5, rangeShoulder));
        this.tolerantTier = Math.max(0.0, Math.min(1.0, tolerantTier));
        this.biomeCategories = Collections.unmodifiableMap(biomeCategories);
        this.catalog = catalog;
        this.animalsEnabled = animalsEnabled;
        this.animalCatalog = animalCatalog;
    }

    public int waterRadius() {
        return this.waterRadius;
    }

    public double waterBonusCap() {
        return this.waterBonusCap;
    }

    public double rainHumidityBonus() {
        return this.rainHumidityBonus;
    }

    public double minSuitability() {
        return this.minSuitability;
    }

    public double rangeShoulder() {
        return this.rangeShoulder;
    }

    public double tolerantTier() {
        return this.tolerantTier;
    }

    public Map<String, String> biomeCategories() {
        return this.biomeCategories;
    }

    public CropCatalog catalog() {
        return this.catalog;
    }

    public boolean animalsEnabled() {
        return this.animalsEnabled;
    }

    public AnimalCatalog animalCatalog() {
        return this.animalCatalog;
    }
}
