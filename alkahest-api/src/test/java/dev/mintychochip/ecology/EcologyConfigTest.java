package dev.mintychochip.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class EcologyConfigTest {

    @TempDir
    Path tempDir;

    @Test
    public void defaultJsonRoundTripKeepsWheatNativePlains() {
        final JsonObject json = EcologyDefaults.toJson();
        final EcologySettings settings = EcologyConfig.fromJson(json);
        final CropProfile wheat = settings.catalog().forBlock("minecraft:wheat");
        assertNotNull(wheat);
        assertTrue(wheat.nativeCategories().contains("plains"));
        assertEquals(0.20, wheat.minHumidity(), 1e-9);
        assertEquals(0.85, wheat.maxHumidity(), 1e-9);
        assertEquals("plains", settings.biomeCategories().get("minecraft:plains"));
        assertEquals(EcologyDefaults.DEFAULT_RAIN_HUMIDITY_BONUS, settings.rainHumidityBonus(), 1e-9);
        assertTrue(json.has("rainHumidityBonus"));
    }

    @Test
    public void rainHumidityBonusFromJson() {
        final JsonObject root = EcologyDefaults.toJson();
        root.addProperty("rainHumidityBonus", 0.35);
        final EcologySettings settings = EcologyConfig.fromJson(root);
        assertEquals(0.35, settings.rainHumidityBonus(), 1e-9);
    }

    @Test
    public void missingRainHumidityBonusUsesDefault() {
        final JsonObject root = EcologyDefaults.toJson();
        root.remove("rainHumidityBonus");
        final EcologySettings settings = EcologyConfig.fromJson(root);
        assertEquals(EcologyDefaults.DEFAULT_RAIN_HUMIDITY_BONUS, settings.rainHumidityBonus(), 1e-9);
    }

    @Test
    public void ensureLoadedWritesAndReadsFile() throws Exception {
        final EcologySettings loaded = EcologyConfig.ensureLoaded(tempDir);
        final Path path = tempDir.resolve(EcologyConfig.relativePath());
        assertTrue(Files.exists(path), "default ecology.json should be created");
        assertTrue(loaded.catalog().forBlock("minecraft:carrots") != null);
        assertTrue(Files.size(path) > 100);
    }
    @Test
    public void ensureLoadedReusesSettingsForSameServerDirectory() throws Exception {
        final EcologySettings first = EcologyConfig.ensureLoaded(this.tempDir);
        writeWheatHumidity(this.tempDir, 0.5, 0.6);

        final EcologySettings cached = EcologyConfig.ensureLoaded(this.tempDir.resolve(".").toAbsolutePath().normalize());

        assertSame(first, cached);
        assertEquals(0.20, cached.catalog().forBlock("minecraft:wheat").minHumidity(), 1e-9);
    }

    @Test
    public void publicLoadForcesReloadForSameServerDirectory() throws Exception {
        EcologyConfig.ensureLoaded(this.tempDir);
        writeWheatHumidity(this.tempDir, 0.5, 0.6);

        final EcologySettings reloaded = Ecology.load(this.tempDir);

        assertEquals(0.5, reloaded.catalog().forBlock("minecraft:wheat").minHumidity(), 1e-9);
    }

    @Test
    public void failedLoadIsCachedUntilExplicitReload() throws Exception {
        final Path path = this.tempDir.resolve(EcologyConfig.relativePath());
        Files.createDirectories(path.getParent());
        Files.writeString(path, "{");
        final EcologySettings fallback = EcologyConfig.ensureLoaded(this.tempDir);
        writeWheatHumidity(this.tempDir, 0.5, 0.6);

        final EcologySettings cached = EcologyConfig.ensureLoaded(this.tempDir);

        assertSame(fallback, cached);
        assertEquals(0.20, cached.catalog().forBlock("minecraft:wheat").minHumidity(), 1e-9);
        assertEquals(0.5, Ecology.load(this.tempDir).catalog().forBlock("minecraft:wheat").minHumidity(), 1e-9);
    }

    @Test
    public void ensureLoadedChangesConfigurationForDifferentServerDirectory() throws Exception {
        final Path firstDirectory = this.tempDir.resolve("first");
        final Path secondDirectory = this.tempDir.resolve("second");
        writeWheatHumidity(firstDirectory, 0.4, 0.5);
        writeWheatHumidity(secondDirectory, 0.6, 0.7);

        assertEquals(0.4, EcologyConfig.ensureLoaded(firstDirectory).catalog().forBlock("minecraft:wheat").minHumidity(), 1e-9);
        assertEquals(0.6, EcologyConfig.ensureLoaded(secondDirectory).catalog().forBlock("minecraft:wheat").minHumidity(), 1e-9);
    }

    @Test
    public void customCropJsonOverridesCatalog() {
        final JsonObject root = EcologyDefaults.toJson();
        final JsonObject crops = root.getAsJsonObject("crops");
        final JsonObject wheat = crops.getAsJsonObject("minecraft:wheat");
        wheat.addProperty("minHumidity", 0.5);
        wheat.addProperty("maxHumidity", 0.6);
        final EcologySettings settings = EcologyConfig.fromJson(root);
        final CropProfile wheatProfile = settings.catalog().forBlock("minecraft:wheat");
        assertEquals(0.5, wheatProfile.minHumidity(), 1e-9);
        assertEquals(0.6, wheatProfile.maxHumidity(), 1e-9);
    }

    private static void writeWheatHumidity(
        final Path serverDirectory,
        final double minHumidity,
        final double maxHumidity
    ) throws Exception {
        final Path path = serverDirectory.resolve(EcologyConfig.relativePath());
        Files.createDirectories(path.getParent());
        final JsonObject root = EcologyDefaults.toJson();
        final JsonObject wheat = root.getAsJsonObject("crops").getAsJsonObject("minecraft:wheat");
        wheat.addProperty("minHumidity", minHumidity);
        wheat.addProperty("maxHumidity", maxHumidity);
        Files.writeString(path, root.toString());
    }
}
