package dev.mintychochip.ecology;

import dev.mintychochip.season.Season;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Built-in passive land-animal catalog (azoth-season defaults). */
public final class AnimalCatalog {
    private final Map<String, AnimalProfile> byId;

    public AnimalCatalog(final Map<String, AnimalProfile> profiles) {
        final Map<String, AnimalProfile> id = new LinkedHashMap<>();
        for (final AnimalProfile p : profiles.values()) {
            if (id.put(p.target(), p) != null) {
                throw new IllegalArgumentException("duplicate animal id " + p.target());
            }
        }
        this.byId = Collections.unmodifiableMap(id);
    }

    public static AnimalCatalog builtin() {
        final Map<String, AnimalProfile> m = new LinkedHashMap<>();
        m.put("minecraft:rabbit", animal("minecraft:rabbit",
            Set.of("plains", "forest", "taiga", "snowy", "desert", "badlands", "savanna", "mountain"),
            Set.of(), 0.0, 0.8, neutral()));
        m.put("minecraft:chicken", animal("minecraft:chicken",
            Set.of("plains", "forest", "taiga", "savanna", "jungle", "swamp"),
            Set.of(), 0.2, 0.9, neutral()));
        m.put("minecraft:pig", animal("minecraft:pig",
            Set.of("plains", "forest", "taiga"), Set.of("swamp"), 0.2, 0.9, neutral()));
        m.put("minecraft:sheep", animal("minecraft:sheep",
            Set.of("plains", "savanna", "mountain"), Set.of("taiga"), 0.1, 0.8, neutral()));
        m.put("minecraft:cow", animal("minecraft:cow",
            Set.of("plains", "forest", "taiga", "savanna"), Set.of("mountain"), 0.2, 0.9, neutral()));
        m.put("minecraft:horse", animal("minecraft:horse",
            Set.of("plains", "savanna"), Set.of(), 0.05, 0.8, neutral()));
        m.put("minecraft:donkey", animal("minecraft:donkey",
            Set.of("plains"), Set.of("savanna"), 0.05, 0.8, neutral()));
        m.put("minecraft:llama", animal("minecraft:llama",
            Set.of("savanna", "mountain"), Set.of(), 0.0, 0.5, neutral()));
        m.put("minecraft:armadillo", animal("minecraft:armadillo",
            Set.of("desert", "badlands", "savanna"), Set.of(), 0.0, 0.35, neutral()));
        m.put("minecraft:cat", animal("minecraft:cat",
            Set.of("plains", "forest"), Set.of(), 0.2, 0.9, neutral()));
        m.put("minecraft:wolf", animal("minecraft:wolf",
            Set.of("forest", "taiga"), Set.of(), 0.2, 0.9, seasonal(1.0, 0.8, 1.0, 0.7)));
        m.put("minecraft:fox", animal("minecraft:fox",
            Set.of("forest", "taiga", "snowy"), Set.of(), 0.2, 0.9, seasonal(1.0, 0.9, 1.0, 1.0)));
        m.put("minecraft:polar_bear", animal("minecraft:polar_bear",
            Set.of("snowy"), Set.of(), 0.0, 0.5, seasonal(0.2, 0.1, 0.2, 1.0)));
        m.put("minecraft:parrot", animal("minecraft:parrot",
            Set.of("jungle"), Set.of(), 0.55, 1.0, neutral()));
        m.put("minecraft:ocelot", animal("minecraft:ocelot",
            Set.of("jungle"), Set.of(), 0.55, 1.0, neutral()));
        m.put("minecraft:camel", animal("minecraft:camel",
            Set.of("desert"), Set.of(), 0.0, 0.3, neutral()));
        m.put("minecraft:frog", animal("minecraft:frog",
            Set.of("swamp"), Set.of("jungle"), 0.6, 1.0, neutral()));
        m.put("minecraft:goat", animal("minecraft:goat",
            Set.of("mountain"), Set.of(), 0.0, 0.5, neutral()));
        m.put("minecraft:turtle", animal("minecraft:turtle",
            Set.of("beach"), Set.of(), 0.4, 0.9, neutral()));
        return new AnimalCatalog(m);
    }

    public AnimalCatalog withOverrides(final Map<String, AnimalProfile> overrides) {
        final Map<String, AnimalProfile> merged = new LinkedHashMap<>(this.byId);
        merged.putAll(overrides);
        return new AnimalCatalog(merged);
    }

    public AnimalProfile forEntity(final String entityKey) {
        return this.byId.get(entityKey);
    }

    public Map<String, AnimalProfile> all() {
        return this.byId;
    }

    private static AnimalProfile animal(
        final String target,
        final Set<String> natives,
        final Set<String> tolerant,
        final double minHum,
        final double maxHum,
        final Map<Season, Double> weights
    ) {
        return AnimalProfile.of(target, natives, tolerant, minHum, maxHum, weights);
    }

    private static Map<Season, Double> neutral() {
        return seasonal(1.0, 1.0, 1.0, 1.0);
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
}
