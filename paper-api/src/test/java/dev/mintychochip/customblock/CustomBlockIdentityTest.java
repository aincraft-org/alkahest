package dev.mintychochip.customblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Identity / façade tests that do not require a live Craft item stack.
 */
public class CustomBlockIdentityTest {

    @AfterEach
    public void tearDown() {
        CustomBlocks.reset();
    }

    @Test
    public void parseItemTag() {
        assertTrue(CustomBlockItemTags.parse(null).isEmpty());
        assertTrue(CustomBlockItemTags.parse("").isEmpty());
        assertTrue(CustomBlockItemTags.parse("not a key").isEmpty());

        final Optional<NamespacedKey> key = CustomBlockItemTags.parse("mintychochip:electrum_ore");
        assertTrue(key.isPresent());
        assertEquals("mintychochip", key.get().getNamespace());
        assertEquals("electrum_ore", key.get().getKey());
    }

    @Test
    public void ofBlockUsesLookupThenCatalog() {
        final NamespacedKey id = NamespacedKey.fromString("mintychochip:electrum_ore");
        final CustomBlockDefinition def = CustomBlockDefinition.builder(id)
            .host(PacketHostSpec.defaults())
            .itemMaterial(Material.PAPER)
            .build();
        CustomBlocks.register(def);

        final Block fake = org.mockito.Mockito.mock(Block.class);
        CustomBlocks.setLookup(block -> Optional.of(id));

        assertTrue(CustomBlocks.isCustomBlock(fake));
        assertEquals(id, CustomBlocks.keyOf(fake).orElseThrow());
        assertEquals(def, CustomBlocks.of(fake).orElseThrow());
        // Block#getCustomKey / getCustomBlock / isCustomBlock are default methods that
        // delegate here; Mockito mocks do not invoke interface defaults (they return empty
        // Optional), so we assert the façade path which is what those methods call.
    }

    @Test
    public void ofBlockEmptyWhenNotPlaced() {
        final Block fake = org.mockito.Mockito.mock(Block.class);
        assertTrue(CustomBlocks.keyOf(fake).isEmpty());
        assertFalse(CustomBlocks.isCustomBlock(fake));
        assertFalse(fake.isCustomBlock());
    }

    @Test
    public void ofBlockKeyWithoutCatalogEntryStillReportsKey() {
        final NamespacedKey id = NamespacedKey.fromString("mintychochip:unknown_ore");
        final Block fake = org.mockito.Mockito.mock(Block.class);
        CustomBlocks.setLookup(block -> Optional.of(id));

        assertEquals(id, CustomBlocks.keyOf(fake).orElseThrow());
        // key present but not registered → of() empty, isCustomBlock still true (identity present)
        assertTrue(CustomBlocks.of(fake).isEmpty());
        assertTrue(CustomBlocks.isCustomBlock(fake));
    }

    @Test
    public void getTypeRemainsSeparateConcept() {
        // Documentation guard: catalog identity is NamespacedKey, not Material.
        final CustomBlockDefinition def = CustomBlockDefinition.builder("mintychochip:electrum_ore")
            .host(ChorusHostSpec.unassigned())
            .itemMaterial(Material.PAPER)
            .build();
        assertEquals(Material.PAPER, def.itemMaterial());
        assertEquals("mintychochip:electrum_ore", def.namespacedKey().toString());
    }
}
