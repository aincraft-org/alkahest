package dev.mintychochip.customblock.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.support.environment.Normal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Normal
public class CustomBlockPackArchiveTest {

    @TempDir
    Path temp;

    @Test
    public void zipFromDirectoryHasStableSha1() throws Exception {
        final Path pack = this.temp.resolve("pack");
        Files.createDirectories(pack.resolve("assets/mintychochip/textures/block"));
        Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{}}");
        Files.write(pack.resolve("assets/mintychochip/textures/block/electrum_ore.png"), new byte[] {1, 2, 3, 4});

        final CustomBlockPackArchive a = CustomBlockPackArchive.fromDirectory(pack);
        final CustomBlockPackArchive b = CustomBlockPackArchive.fromDirectory(pack);

        assertEquals(40, a.sha1Hex().length());
        assertEquals(a.sha1Hex(), b.sha1Hex());
        assertTrue(a.size() > 0);
        assertEquals(a.size(), a.zipBytes().length);
    }

    @Test
    public void classpathElectrumOreUsesBlockModelNotFlatItemGenerated() throws Exception {
        final CustomBlockPackArchive archive = CustomBlockPackArchive.fromClasspath("mintychochip-pack/");
        final String zip = new String(archive.zipBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);

        // Entry paths are plain text in the central directory / local headers for our simple assets.
        assertTrue(zip.contains("assets/mintychochip/models/block/electrum_ore.json"),
            "pack must ship block model");
        assertTrue(zip.contains("assets/mintychochip/textures/block/electrum_ore.png"),
            "pack must ship block texture");
        assertTrue(zip.contains("assets/mintychochip/items/electrum_ore.json"),
            "pack must ship item definition");

        final byte[] bytes = archive.zipBytes();
        // Parse entries properly and assert JSON content of the models.
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                new java.io.ByteArrayInputStream(bytes))) {
            java.util.zip.ZipEntry entry;
            String itemDef = null;
            String blockModel = null;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("assets/mintychochip/items/electrum_ore.json")) {
                    itemDef = new String(zis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                } else if (entry.getName().equals("assets/mintychochip/models/block/electrum_ore.json")) {
                    blockModel = new String(zis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                }
            }
            assertTrue(itemDef != null && itemDef.contains("mintychochip:block/electrum_ore"),
                "item definition must point at block model, got: " + itemDef);
            assertTrue(itemDef != null && !itemDef.contains("mintychochip:item/electrum_ore"),
                "item definition must not use item model path");
            assertTrue(blockModel != null && blockModel.contains("cube_all"),
                "block model must use cube_all parent, got: " + blockModel);
            assertTrue(blockModel != null && blockModel.contains("mintychochip:block/electrum_ore"),
                "block model texture must be block path");
            assertTrue(blockModel != null && !blockModel.contains("item/generated"),
                "must not be flat generated item model");
        }
    }
}
