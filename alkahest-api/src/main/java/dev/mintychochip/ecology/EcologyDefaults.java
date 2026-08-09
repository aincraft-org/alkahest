package dev.mintychochip.ecology;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Built-in defaults used before config loads and when writing ecology.json. */
public final class EcologyDefaults {
    private EcologyDefaults() {
    }

    /** Default rain humidity pull (0.2 → ~14% absolute bump at base 0.3). */
    public static final double DEFAULT_RAIN_HUMIDITY_BONUS = 0.2;

    public static EcologySettings create() {
        return new EcologySettings(
            24,
            0.4,
            DEFAULT_RAIN_HUMIDITY_BONUS,
            0.05,
            0.05,
            0.6,
            defaultBiomeCategories(),
            CropCatalog.builtin(),
            true,
            AnimalCatalog.builtin()
        );
    }

    public static Map<String, String> defaultBiomeCategories() {
        final Map<String, String> m = new LinkedHashMap<>();
        m.put("minecraft:plains", "plains");
        m.put("minecraft:sunflower_plains", "plains");
        m.put("minecraft:meadow", "plains");
        m.put("minecraft:forest", "forest");
        m.put("minecraft:birch_forest", "forest");
        m.put("minecraft:old_growth_birch_forest", "forest");
        m.put("minecraft:dark_forest", "forest");
        m.put("minecraft:flower_forest", "forest");
        m.put("minecraft:cherry_grove", "forest");
        m.put("minecraft:jungle", "jungle");
        m.put("minecraft:bamboo_jungle", "jungle");
        m.put("minecraft:sparse_jungle", "jungle");
        m.put("minecraft:desert", "desert");
        m.put("minecraft:taiga", "taiga");
        m.put("minecraft:snowy_taiga", "snowy");
        m.put("minecraft:old_growth_pine_taiga", "taiga");
        m.put("minecraft:old_growth_spruce_taiga", "taiga");
        m.put("minecraft:swamp", "swamp");
        m.put("minecraft:mangrove_swamp", "swamp");
        m.put("minecraft:savanna", "savanna");
        m.put("minecraft:savanna_plateau", "savanna");
        m.put("minecraft:windswept_savanna", "savanna");
        m.put("minecraft:badlands", "badlands");
        m.put("minecraft:wooded_badlands", "badlands");
        m.put("minecraft:eroded_badlands", "badlands");
        m.put("minecraft:snowy_plains", "snowy");
        m.put("minecraft:snowy_beach", "snowy");
        m.put("minecraft:ice_spikes", "snowy");
        m.put("minecraft:frozen_river", "snowy");
        m.put("minecraft:snowy_slopes", "snowy");
        m.put("minecraft:grove", "snowy");
        m.put("minecraft:windswept_hills", "mountain");
        m.put("minecraft:windswept_forest", "mountain");
        m.put("minecraft:windswept_gravelly_hills", "mountain");
        m.put("minecraft:stony_peaks", "mountain");
        m.put("minecraft:jagged_peaks", "mountain");
        m.put("minecraft:frozen_peaks", "mountain");
        m.put("minecraft:stony_shore", "beach");
        m.put("minecraft:beach", "beach");
        return m;
    }

    public static JsonObject toJson() {
        final EcologySettings s = create();
        final JsonObject root = new JsonObject();
        root.addProperty("waterRadius", s.waterRadius());
        root.addProperty("waterBonusCap", s.waterBonusCap());
        root.addProperty("rainHumidityBonus", s.rainHumidityBonus());
        root.addProperty("minSuitability", s.minSuitability());
        root.addProperty("rangeShoulder", s.rangeShoulder());
        root.addProperty("tolerantTier", s.tolerantTier());

        final JsonObject biomes = new JsonObject();
        for (final Map.Entry<String, String> e : s.biomeCategories().entrySet()) {
            biomes.addProperty(e.getKey(), e.getValue());
        }
        root.add("biomeCategories", biomes);

        final JsonObject crops = new JsonObject();
        for (final CropProfile p : s.catalog().all().values()) {
            final JsonObject o = new JsonObject();
            o.add("native", stringArray(p.nativeCategories()));
            o.add("tolerant", stringArray(p.tolerantCategories()));
            o.addProperty("minHumidity", p.minHumidity());
            o.addProperty("maxHumidity", p.maxHumidity());
            crops.add(p.id(), o);
        }
        root.add("crops", crops);

        final JsonObject animals = new JsonObject();
        animals.addProperty("enabled", s.animalsEnabled());
        final JsonObject animalProfiles = new JsonObject();
        for (final AnimalProfile p : s.animalCatalog().all().values()) {
            final JsonObject o = new JsonObject();
            o.add("native", stringArray(p.nativeCategories()));
            o.add("tolerant", stringArray(p.tolerantCategories()));
            o.addProperty("minHumidity", p.minHumidity());
            o.addProperty("maxHumidity", p.maxHumidity());
            final JsonObject weights = new JsonObject();
            for (final Map.Entry<dev.mintychochip.season.Season, Double> we : p.seasonWeights().entrySet()) {
                weights.addProperty(we.getKey().name().toLowerCase(Locale.ROOT), we.getValue());
            }
            o.add("seasonWeights", weights);
            animalProfiles.add(p.target(), o);
        }
        animals.add("profiles", animalProfiles);
        root.add("animals", animals);
        return root;
    }

    private static JsonArray stringArray(final Set<String> values) {
        final JsonArray arr = new JsonArray();
        for (final String v : values) {
            arr.add(v);
        }
        return arr;
    }
}