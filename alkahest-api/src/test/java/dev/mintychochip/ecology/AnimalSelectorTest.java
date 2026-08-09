package dev.mintychochip.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.season.Season;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class AnimalSelectorTest {
    private static final ClimateSample TAIGA_WET = new ClimateSample(0.8, "taiga");
    private static final ClimateSample DESERT_ARID = new ClimateSample(0.1, "desert");

    private static AnimalProfile profile(
        final String target,
        final Set<String> natives,
        final double minHum,
        final double maxHum,
        final Map<Season, Double> weights
    ) {
        return AnimalProfile.of(target, natives, Set.of(), minHum, maxHum, weights);
    }

    private static Map<Season, Double> w(final double v) {
        final Map<Season, Double> m = new EnumMap<>(Season.class);
        for (final Season s : Season.values()) {
            m.put(s, v);
        }
        return m;
    }

    private static Map<Season, Double> seasonal(
        final double spring,
        final double summer,
        final double autumn,
        final double winter
    ) {
        final Map<Season, Double> w = new EnumMap<>(Season.class);
        w.put(Season.SPRING, spring);
        w.put(Season.SUMMER, summer);
        w.put(Season.AUTUMN, autumn);
        w.put(Season.WINTER, winter);
        return w;
    }

    @Test
    public void tierByCategory() {
        final AnimalProfile fox = profile("minecraft:fox", Set.of("taiga", "forest"), 0.2, 0.9, w(1.0));
        assertEquals(1.0, AnimalSelector.tier(fox, "taiga"), 1e-12);
        assertEquals(0.0, AnimalSelector.tier(fox, "plains"), 1e-12);
        assertEquals(0.0, AnimalSelector.tier(fox, "desert"), 1e-12);
    }

    @Test
    public void tolerantCategoryYieldsConfiguredTier() {
        final AnimalProfile fox = AnimalProfile.of(
            "minecraft:fox", Set.of("taiga"), Set.of("forest"), 0.2, 0.9, w(1.0)
        );
        assertEquals(EcologyConfig.get().tolerantTier(), AnimalSelector.tier(fox, "forest"), 1e-12);
    }

    @Test
    public void unrestrictedAnimalsGetFullTier() {
        final AnimalProfile rabbit = profile("minecraft:rabbit", Set.of(), 0.0, 1.0, w(1.0));
        assertEquals(1.0, AnimalSelector.tier(rabbit, "snowy"), 1e-12);
    }

    @Test
    public void scoreIsTierTimesHumidityTimesSeason() {
        final AnimalProfile fox = profile("minecraft:fox", Set.of("taiga"), 0.2, 0.9, w(1.0));
        assertEquals(1.0, AnimalSelector.score(fox, TAIGA_WET, Season.SUMMER), 1e-12);
        assertEquals(0.0, AnimalSelector.score(fox, DESERT_ARID, Season.SUMMER), 1e-12);
        final AnimalProfile winterFox = profile(
            "minecraft:fox", Set.of("taiga"), 0.2, 0.9, seasonal(1.0, 1.0, 1.0, 0.3)
        );
        assertEquals(0.3, AnimalSelector.score(winterFox, TAIGA_WET, Season.WINTER), 1e-12);
    }

    @Test
    public void seasonWeightZeroGates() {
        final AnimalProfile noWinter = profile(
            "minecraft:turtle", Set.of("beach"), 0.4, 0.9, seasonal(1.0, 1.0, 1.0, 0.0)
        );
        assertEquals(
            0.0,
            AnimalSelector.score(noWinter, new ClimateSample(0.6, "beach"), Season.WINTER),
            1e-12
        );
        final Optional<AnimalProfile> pick = AnimalSelector.select(
            new AnimalCatalog(Map.of("minecraft:turtle", noWinter)),
            new ClimateSample(0.6, "beach"),
            Season.WINTER,
            new Random(1)
        );
        assertTrue(pick.isEmpty(), "zero-weight animal must not be selectable in winter");
    }

    @Test
    public void weightedPickPrefersHigherScore() {
        final AnimalProfile common = profile("minecraft:rabbit", Set.of("taiga"), 0.0, 1.0, w(1.0));
        final AnimalProfile rare = profile("minecraft:wolf", Set.of("taiga"), 0.0, 1.0, w(0.05));
        final AnimalCatalog cat = new AnimalCatalog(
            Map.of("minecraft:rabbit", common, "minecraft:wolf", rare)
        );
        final Random rng = new Random(42);
        int wolf = 0;
        int rabbit = 0;
        for (int i = 0; i < 4000; i++) {
            final AnimalProfile p = AnimalSelector.select(cat, TAIGA_WET, Season.SUMMER, rng).orElseThrow();
            if (p.target().equals("minecraft:wolf")) {
                wolf++;
            } else {
                rabbit++;
            }
        }
        assertTrue(
            wolf > 0 && rabbit > wolf,
            "weighted pick must favor rabbit over wolf, got wolf=" + wolf + " rabbit=" + rabbit
        );
    }

    @Test
    public void noCandidateYieldsEmpty() {
        final AnimalCatalog empty = new AnimalCatalog(Map.of());
        assertTrue(AnimalSelector.select(empty, DESERT_ARID, Season.SUMMER, new Random(1)).isEmpty());
    }

    @Test
    public void builtinCatalogHasLandAnimals() {
        final AnimalCatalog catalog = AnimalCatalog.builtin();
        assertTrue(catalog.forEntity("minecraft:cow") != null);
        assertTrue(catalog.forEntity("minecraft:pig") != null);
        assertTrue(catalog.forEntity("minecraft:sheep") != null);
        assertTrue(catalog.all().size() >= 10);
    }
}
