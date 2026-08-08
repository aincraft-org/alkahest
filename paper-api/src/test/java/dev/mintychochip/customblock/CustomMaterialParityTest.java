package dev.mintychochip.customblock;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Material;
import org.bukkit.VanillaMaterial;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Proves custom definitions are real {@link Material}s usable like vanilla constants.
 *
 * <p>Registry.MATERIAL merge is covered by {@code org.bukkit.MaterialRegistryTest}
 * (avoids full {@link org.bukkit.Registry} clinit in pure API unit tests).
 */
public class CustomMaterialParityTest {

    @AfterEach
    public void tearDown() {
        CustomBlocks.reset();
    }

    @Test
    public void vanillaConstantsAreMaterialAndSameInstanceAsEnum() {
        assertTrue(Material.STONE instanceof VanillaMaterial);
        assertSame(VanillaMaterial.STONE, Material.STONE);
        assertEquals("STONE", Material.STONE.name());
        assertTrue(Material.STONE.isVanilla());
        assertFalse(Material.STONE.isCustom());
    }

    @Test
    public void valueOfAndValuesStillWorkForVanilla() {
        assertSame(Material.COBBLESTONE, Material.valueOf("COBBLESTONE"));
        boolean found = false;
        for (final Material m : Material.values()) {
            if (m == Material.STONE) {
                found = true;
                break;
            }
        }
        assertTrue(found);
        // values() is vanilla-only — customs never appear there
        for (final Material m : Material.values()) {
            assertTrue(m.isVanilla());
            assertFalse(m.isCustom());
        }
    }

    @Test
    public void getByKeyResolvesCustomAfterRegister() {
        final CustomBlockDefinition def = CustomBlockDefinition.builder("mintychochip:test_ore")
            .host(PacketHostSpec.defaults())
            .emulate(Material.IRON_ORE)
            .build();
        CustomBlocks.register(def);

        assertEquals(def, Material.getByKey(def.getKey()).orElseThrow());
        assertEquals(def, Material.matchMaterial("mintychochip:test_ore"));
        assertNull(Material.getMaterial("mintychochip:test_ore")); // vanilla name only
    }

    @Test
    public void customDefinitionIsMaterial() {
        // Explicit feel so hardness is testable without full BlockType registry bootstrap.
        final CustomBlockDefinition def = CustomBlockDefinition.builder("mintychochip:test_ore")
            .host(PacketHostSpec.defaults())
            .feel(BlockFeel.of(3.0F, 3.0F, true, Material.IRON_ORE))
            .build();
        CustomBlocks.register(def);

        final Material type = def;
        assertTrue(type.isCustom());
        assertFalse(type.isVanilla());
        assertEquals("mintychochip:test_ore", type.getKey().toString());
        assertTrue(type.isBlock());
        assertTrue(type.isItem());
        assertFalse(type.isAir());
        assertEquals(3.0F, type.getHardness(), 0.001f);
        assertEquals(3.0F, type.getBlastResistance(), 0.001f);
        assertEquals("mintychochip:test_ore", type.name());
        assertEquals(Material.GLASS, def.carrierMaterial()); // packet default collision
    }

    @Test
    public void carrierCreateBlockDataUsesHost() {
        final CustomBlockDefinition def = CustomBlockDefinition.builder("mintychochip:chorus_ore")
            .host(ChorusHostSpec.unassigned())
            .build();
        assertEquals(Material.CHORUS_PLANT, def.carrierMaterial());
    }

    @Test
    public void emulateRejectsCustomMaterial() {
        final CustomBlockDefinition other = CustomBlockDefinition.builder("mintychochip:other")
            .host(PacketHostSpec.defaults())
            .build();
        assertThrows(IllegalArgumentException.class, () -> BlockFeel.emulate(other));
        assertThrows(IllegalArgumentException.class, () ->
            CustomBlockDefinition.builder("mintychochip:bad")
                .host(PacketHostSpec.defaults())
                .emulate(other)
                .build()
        );
    }

    @Test
    public void mushroomAndTripwireCarriers() {
        final CustomBlockDefinition red = CustomBlockDefinition.builder("mintychochip:red_crate")
            .host(MushroomHostSpec.red())
            .build();
        assertEquals(Material.RED_MUSHROOM_BLOCK, red.carrierMaterial());

        final CustomBlockDefinition brown = CustomBlockDefinition.builder("mintychochip:brown_crate")
            .host(MushroomHostSpec.brown())
            .build();
        assertEquals(Material.BROWN_MUSHROOM_BLOCK, brown.carrierMaterial());

        final CustomBlockDefinition wire = CustomBlockDefinition.builder("mintychochip:wire")
            .host(TripwireHostSpec.unassigned())
            .build();
        assertEquals(Material.TRIPWIRE, wire.carrierMaterial());
    }

    @Test
    public void packetBarrierCollision() {
        final CustomBlockDefinition def = CustomBlockDefinition.builder("mintychochip:barrier_ore")
            .host(PacketHostSpec.builder().collisionMaterialKey("minecraft:barrier").build())
            .build();
        assertEquals(Material.BARRIER, def.carrierMaterial());
    }
}
