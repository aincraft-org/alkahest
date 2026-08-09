package dev.mintychochip.ecology;

import dev.mintychochip.season.Season;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Regional animal definition: entity key, biome categories, humidity range,
 * and per-season spawn weights (azoth-season model).
 */
public final class AnimalProfile {
    private final String target;
    private final Set<String> nativeCategories;
    private final Set<String> tolerantCategories;
    private final double minHumidity;
    private final double maxHumidity;
    private final Map<Season, Double> seasonWeights;

    private AnimalProfile(
        final String target,
        final Set<String> nativeCategories,
        final Set<String> tolerantCategories,
        final double minHumidity,
        final double maxHumidity,
        final Map<Season, Double> seasonWeights
    ) {
        this.target = target;
        this.nativeCategories = nativeCategories;
        this.tolerantCategories = tolerantCategories;
        this.minHumidity = minHumidity;
        this.maxHumidity = maxHumidity;
        this.seasonWeights = seasonWeights;
    }

    public static AnimalProfile of(
        final String target,
        final Set<String> nativeCategories,
        final Set<String> tolerantCategories,
        final Map<Season, Double> seasonWeights
    ) {
        return of(target, nativeCategories, tolerantCategories, 0.0, 1.0, seasonWeights);
    }

    public static AnimalProfile of(
        final String target,
        final Set<String> nativeCategories,
        final Set<String> tolerantCategories,
        final double minHumidity,
        final double maxHumidity,
        final Map<Season, Double> seasonWeights
    ) {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("target required");
        }
        if (!(minHumidity <= maxHumidity && minHumidity >= 0 && maxHumidity <= 1)) {
            throw new IllegalArgumentException("invalid humidity range [" + minHumidity + ", " + maxHumidity + "]");
        }
        if (seasonWeights == null || seasonWeights.isEmpty()) {
            throw new IllegalArgumentException("seasonWeights required");
        }
        validateCategories(nativeCategories, "nativeCategories");
        validateCategories(tolerantCategories, "tolerantCategories");
        final Map<Season, Double> w = new EnumMap<>(Season.class);
        for (final Season s : Season.values()) {
            final Double v = seasonWeights.get(s);
            if (v == null) {
                throw new IllegalArgumentException("missing season weight for " + s);
            }
            if (v < 0 || v > 1) {
                throw new IllegalArgumentException("season weight out of [0,1] for " + s + ": " + v);
            }
            w.put(s, v);
        }
        return new AnimalProfile(
            target,
            nativeCategories == null ? Set.of() : Set.copyOf(nativeCategories),
            tolerantCategories == null ? Set.of() : Set.copyOf(tolerantCategories),
            minHumidity,
            maxHumidity,
            Collections.unmodifiableMap(w)
        );
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

    public String target() {
        return this.target;
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

    public double seasonWeight(final Season season) {
        return this.seasonWeights.get(season);
    }

    public Map<Season, Double> seasonWeights() {
        return this.seasonWeights;
    }

    public boolean unrestricted() {
        return this.nativeCategories.isEmpty() && this.tolerantCategories.isEmpty();
    }
}
