package dev.mintychochip.customentity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Structural proof: custom entity code lives as normal sources (not Minecraft patch tree).
 * API-side so it runs without a full paper-server compile.
 */
public class CustomEntitySourcesPlacementTest {

    @Test
    public void apiAndServerSourcesExistOutsidePatchTree() throws Exception {
        final Path root = projectRoot();
        assertTrue(Files.isRegularFile(root.resolve(
            "alkahest-api/src/main/java/dev/mintychochip/customentity/CustomEntities.java")));
        assertTrue(Files.isRegularFile(root.resolve(
            "alkahest-api/src/main/java/dev/mintychochip/customentity/CustomEntityDefinition.java")));
        assertTrue(Files.isRegularFile(root.resolve(
            "alkahest-api/src/main/java/dev/mintychochip/customentity/BlockModelHostSpec.java")));
        assertTrue(Files.isRegularFile(root.resolve(
            "alkahest-api/src/main/java/dev/mintychochip/customentity/CustomEntityLifecycle.java")));
        assertTrue(Files.isRegularFile(root.resolve(
            "paper-server/src/main/java/dev/mintychochip/customentity/CustomEntityBootstrap.java")));
        assertTrue(Files.isRegularFile(root.resolve(
            "paper-server/src/main/java/dev/mintychochip/customentity/DefaultCustomEntities.java")));

        assertFalse(Files.exists(root.resolve("paper-server/patches/sources/dev/mintychochip/customentity")));
        assertFalse(Files.exists(root.resolve("paper-server/src/minecraft/java/dev/mintychochip/customentity")));
    }

    @Test
    public void craftServerWiresBootstrap() throws Exception {
        final Path root = projectRoot();
        final String src = Files.readString(root.resolve(
            "paper-server/src/main/java/org/bukkit/craftbukkit/CraftServer.java"));
        assertTrue(src.contains("dev.mintychochip.customentity.CustomEntityBootstrap.ensureInstalled"),
            "CraftServer must install CustomEntityBootstrap");
        assertTrue(src.contains("// mintychochip"), "bootstrap hook must be marked mintychochip");
    }

    @Test
    public void entityInterfaceHasAdditiveIdentityDefaults() throws Exception {
        final Path root = projectRoot();
        final String src = Files.readString(root.resolve(
            "alkahest-api/src/main/java/org/bukkit/entity/Entity.java")));
        assertTrue(src.contains("getCustomKey()"), "Entity must expose getCustomKey");
        assertTrue(src.contains("getCustomEntity()"), "Entity must expose getCustomEntity");
        assertTrue(src.contains("isCustomEntity()"), "Entity must expose isCustomEntity");
        assertTrue(src.contains("dev.mintychochip.customentity.CustomEntities"),
            "Entity defaults must delegate to CustomEntities");
    }

    @Test
    public void defaultSampleDefinitionIsGlowCube() throws Exception {
        final Path root = projectRoot();
        final String src = Files.readString(root.resolve(
            "paper-server/src/main/java/dev/mintychochip/customentity/DefaultCustomEntities.java"));
        assertTrue(src.contains("glow_cube"), "sample entity key");
        assertTrue(src.contains("GLOWSTONE") || src.contains("Material.GLOWSTONE"),
            "sample uses glowstone block model");
        assertTrue(src.contains("BlockModelHostSpec"), "sample is block-model host");
    }

    private static Path projectRoot() throws Exception {
        Path cwd = Path.of("").toAbsolutePath();
        for (int i = 0; i < 6; i++) {
            if (Files.isRegularFile(cwd.resolve("settings.gradle.kts"))
                || Files.isRegularFile(cwd.resolve("settings.gradle"))) {
                return cwd;
            }
            // alkahest-api test cwd is often alkahest-api/
            if (Files.isDirectory(cwd.resolve("alkahest-api")) && Files.isDirectory(cwd.resolve("paper-server"))) {
                return cwd;
            }
            final Path parent = cwd.getParent();
            if (parent == null) {
                break;
            }
            cwd = parent;
        }
        final Path fromModule = Path.of("").toAbsolutePath().getParent();
        if (fromModule != null && Files.isDirectory(fromModule.resolve("alkahest-api"))) {
            return fromModule;
        }
        throw new java.nio.file.NoSuchFileException("could not locate project root from " + Path.of("").toAbsolutePath());
    }
}
