package dev.mintychochip.ecology;

import java.util.Locale;
import java.util.Set;

/** Crop climate requirements: biome categories and humidity range. */
public final class CropProfile {
    private final String id;
    private final Set<String> nativeCategories;
    private final Set<String> tolerantCategories;
    private final double minHumidity;
    private final double maxHumidity;

    public CropProfile(
        final String id,
        final Set<String> nativeCategories,
        final Set<String> tolerantCategories,
        final double minHumidity,
        final double maxHumidity
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id required");
        }
        if (!(minHumidity <= maxHumidity && minHumidity >= 0 && maxHumidity <= 1)) {
            throw new IllegalArgumentException("invalid humidity range [" + minHumidity + ", " + maxHumidity + "]");
        }
        validateCategories(nativeCategories, "nativeCategories");
        validateCategories(tolerantCategories, "tolerantCategories");
        this.id = id;
        this.nativeCategories = nativeCategories == null ? Set.of() : Set.copyOf(nativeCategories);
        this.tolerantCategories = tolerantCategories == null ? Set.of() : Set.copyOf(tolerantCategories);
        this.minHumidity = minHumidity;
        this.maxHumidity = maxHumidity;
    }

    private static void validateCategories(final Set<String> categories, final String name) {
        if (categories == null) {
            return;
        }
        for (final String c : categories) {
            if (c == null || c.isBlank() || !c.equals(c.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException(name + " must be lowercase keys, got '" + c + "'");
            }
        }
    }

    public String id() {
        return this.id;
    }

    public Set<String> nativeCategories() {
        return this.nativeCategories;
    }

    public Set<String> tolerantCategories() {
        return this.tolerantCategories;
    }

    public double minHumidity() {
        return this.minHumidity;
    }

    public double maxHumidity() {
        return this.maxHumidity;
    }

    public boolean unrestricted() {
        return this.nativeCategories.isEmpty() && this.tolerantCategories.isEmpty();
    }
}