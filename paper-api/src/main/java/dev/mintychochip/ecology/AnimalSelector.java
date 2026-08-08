package dev.mintychochip.ecology;

import dev.mintychochip.season.Season;
import java.util.Optional;
import java.util.Random;

/**
 * Animal × climate × season selection: tier × humidity trapezoid × season weight,
 * then weighted random among positive scores (azoth-season algorithm).
 */
public final class AnimalSelector {
    private AnimalSelector() {
    }

    public static double tier(final AnimalProfile profile, final String region) {
        if (profile.unrestricted()) {
            return 1.0;
        }
        if (profile.nativeCategories().contains(region)) {
            return 1.0;
        }
        if (profile.tolerantCategories().contains(region)) {
            return EcologyConfig.get().tolerantTier();
        }
        return 0.0;
    }

    public static double score(final AnimalProfile profile, final ClimateSample cell, final Season season) {
        return tier(profile, cell.region())
            * trapezoid(cell.humidityValue(), profile.minHumidity(), profile.maxHumidity())
            * profile.seasonWeight(season);
    }

    /**
     * Weighted random pick among catalog animals with score &gt; 0.
     * Empty when no candidate fits (caller should skip inventing spawns).
     */
    public static Optional<AnimalProfile> select(
        final AnimalCatalog catalog,
        final ClimateSample cell,
        final Season season,
        final Random rng
    ) {
        double total = 0;
        for (final AnimalProfile p : catalog.all().values()) {
            total += score(p, cell, season);
        }
        if (total <= 0) {
            return Optional.empty();
        }
        double r = rng.nextDouble() * total;
        for (final AnimalProfile p : catalog.all().values()) {
            final double s = score(p, cell, season);
            if (s <= 0) {
                continue;
            }
            if (r < s) {
                return Optional.of(p);
            }
            r -= s;
        }
        for (final AnimalProfile p : catalog.all().values()) {
            if (score(p, cell, season) > 0) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
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
