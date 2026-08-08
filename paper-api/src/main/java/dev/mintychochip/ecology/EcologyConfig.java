package dev.mintychochip.ecology;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import dev.mintychochip.season.Season;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;

/**
 * Loads {@code config/mintychochip/ecology.json} from the server root.
 * Writes a full default file on first run if missing.
 */
public final class EcologyConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(EcologyConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String RELATIVE_PATH = "config/mintychochip/ecology.json";

    private static volatile EcologySettings settings = EcologyDefaults.create();
    private static volatile boolean loadedFromDisk;

    private EcologyConfig() {
    }

    public static EcologySettings get() {
        return settings;
    }

    public static synchronized EcologySettings ensureLoaded(final Path serverDirectory) {
        if (serverDirectory == null) {
            return settings;
        }
        final Path path = serverDirectory.resolve(RELATIVE_PATH);
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                writeDefaults(path);
                LOGGER.info("[mintychochip] Wrote default ecology config to {}", path.toAbsolutePath());
            }
            settings = load(path);
            loadedFromDisk = true;
            LOGGER.info(
                "[mintychochip] Loaded ecology config from {} ({} crops, {} animals, animalsEnabled={})",
                path.toAbsolutePath(),
                settings.catalog().all().size(),
                settings.animalCatalog().all().size(),
                settings.animalsEnabled()
            );
        } catch (final Exception ex) {
            LOGGER.error("[mintychochip] Failed to load ecology config {}; using built-in defaults", path, ex);
            settings = EcologyDefaults.create();
        }
        return settings;
    }

    public static boolean isLoadedFromDisk() {
        return loadedFromDisk;
    }

    public static String relativePath() {
        return RELATIVE_PATH;
    }

    private static void writeDefaults(final Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(EcologyDefaults.toJson(), writer);
            writer.write('\n');
        }
    }

    static EcologySettings load(final Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return fromJson(JsonParser.parseReader(reader).getAsJsonObject());
        }
    }

    static EcologySettings fromJson(final JsonObject root) {
        final int waterRadius = getInt(root, "waterRadius", 24);
        final double waterBonusCap = getDouble(root, "waterBonusCap", 0.4);
        final double rainHumidityBonus = getDouble(
            root,
            "rainHumidityBonus",
            EcologyDefaults.DEFAULT_RAIN_HUMIDITY_BONUS
        );
        final double minSuitability = getDouble(root, "minSuitability", 0.05);
        final double rangeShoulder = getDouble(root, "rangeShoulder", 0.05);
        final double tolerantTier = getDouble(root, "tolerantTier", 0.6);

        final Map<String, String> biomes = new LinkedHashMap<>(EcologyDefaults.defaultBiomeCategories());
        if (root.has("biomeCategories") && root.get("biomeCategories").isJsonObject()) {
            biomes.clear();
            for (final Map.Entry<String, JsonElement> e : root.getAsJsonObject("biomeCategories").entrySet()) {
                biomes.put(e.getKey(), e.getValue().getAsString());
            }
        }

        final Map<String, CropProfile> crops = new LinkedHashMap<>();
        if (root.has("crops") && root.get("crops").isJsonObject()) {
            for (final Map.Entry<String, JsonElement> e : root.getAsJsonObject("crops").entrySet()) {
                final String id = e.getKey();
                final JsonObject o = e.getValue().getAsJsonObject();
                crops.put(
                    id,
                    new CropProfile(
                        id,
                        readStringSet(o, "native"),
                        readStringSet(o, "tolerant"),
                        getDouble(o, "minHumidity", 0.0),
                        getDouble(o, "maxHumidity", 1.0)
                    )
                );
            }
        } else {
            for (final CropProfile p : EcologyDefaults.create().catalog().all().values()) {
                crops.put(p.id(), p);
            }
        }

        boolean animalsEnabled = true;
        Map<String, AnimalProfile> animals = new LinkedHashMap<>();
        for (final AnimalProfile p : AnimalCatalog.builtin().all().values()) {
            animals.put(p.target(), p);
        }
        if (root.has("animals") && root.get("animals").isJsonObject()) {
            final JsonObject animalsRoot = root.getAsJsonObject("animals");
            if (animalsRoot.has("enabled")) {
                animalsEnabled = animalsRoot.get("enabled").getAsBoolean();
            }
            if (animalsRoot.has("profiles") && animalsRoot.get("profiles").isJsonObject()) {
                animals = new LinkedHashMap<>();
                for (final Map.Entry<String, JsonElement> e : animalsRoot.getAsJsonObject("profiles").entrySet()) {
                    final String id = e.getKey();
                    final JsonObject o = e.getValue().getAsJsonObject();
                    animals.put(
                        id,
                        AnimalProfile.of(
                            id,
                            readStringSet(o, "native"),
                            readStringSet(o, "tolerant"),
                            getDouble(o, "minHumidity", 0.0),
                            getDouble(o, "maxHumidity", 1.0),
                            readSeasonWeights(o)
                        )
                    );
                }
            }
        }

        return new EcologySettings(
            waterRadius,
            waterBonusCap,
            rainHumidityBonus,
            minSuitability,
            rangeShoulder,
            tolerantTier,
            biomes,
            new CropCatalog(crops),
            animalsEnabled,
            new AnimalCatalog(animals)
        );
    }

    private static Map<Season, Double> readSeasonWeights(final JsonObject o) {
        final Map<Season, Double> w = new EnumMap<>(Season.class);
        if (o.has("seasonWeights") && o.get("seasonWeights").isJsonObject()) {
            for (final Map.Entry<String, JsonElement> e : o.getAsJsonObject("seasonWeights").entrySet()) {
                final Season season = Season.valueOf(e.getKey().toUpperCase(Locale.ROOT));
                w.put(season, e.getValue().getAsDouble());
            }
        }
        for (final Season s : Season.values()) {
            w.putIfAbsent(s, 1.0);
        }
        return w;
    }

    private static Set<String> readStringSet(final JsonObject o, final String key) {
        final Set<String> out = new LinkedHashSet<>();
        if (!o.has(key) || !o.get(key).isJsonArray()) {
            return out;
        }
        for (final JsonElement el : o.getAsJsonArray(key)) {
            out.add(el.getAsString());
        }
        return out;
    }

    private static int getInt(final JsonObject o, final String key, final int def) {
        return o.has(key) ? o.get(key).getAsInt() : def;
    }

    private static double getDouble(final JsonObject o, final String key, final double def) {
        return o.has(key) ? o.get(key).getAsDouble() : def;
    }
}