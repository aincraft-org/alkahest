package org.bukkit;

import static org.junit.jupiter.api.Assertions.*;

import dev.mintychochip.customblock.BlockFeel;
import dev.mintychochip.customblock.CustomBlockDefinition;
import dev.mintychochip.customblock.CustomBlocks;
import dev.mintychochip.customblock.PacketHostSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Registry#MATERIAL} / {@link MaterialRegistry}:
 * vanilla non-legacy constants plus registered custom materials.
 */
public class MaterialRegistryTest {

    @BeforeEach
    public void setUp() {
        CustomBlocks.reset();
    }

    @AfterEach
    public void tearDown() {
        CustomBlocks.reset();
    }

    @Test
    public void getResolvesVanillaNonLegacy() {
        final NamespacedKey stoneKey = NamespacedKey.minecraft("stone");
        assertSame(Material.STONE, Registry.MATERIAL.get(stoneKey));
        assertTrue(Registry.MATERIAL.stream().anyMatch(m -> m == Material.STONE));
        assertTrue(Registry.MATERIAL.size() > 0);
        assertInstanceOf(MaterialRegistry.class, Registry.MATERIAL);
    }

    @Test
    public void getResolvesCustomAfterRegister() {
        final CustomBlockDefinition def = CustomBlockDefinition.builder("mintychochip:registry_ore")
            .host(PacketHostSpec.defaults())
            .feel(BlockFeel.of(3.0F, 3.0F, true, Material.IRON_ORE))
            .build();
        CustomBlocks.register(def);

        assertSame(def, Registry.MATERIAL.get(def.getKey()));
        assertSame(def, Registry.MATERIAL.getOrThrow(def.getKey()));
        assertTrue(Registry.MATERIAL.stream().anyMatch(m -> m == def));
        assertTrue(Registry.MATERIAL.keyStream().anyMatch(k -> k.equals(def.getKey())));

        final int sizeWithCustom = Registry.MATERIAL.size();
        CustomBlocks.reset();
        assertNull(Registry.MATERIAL.get(def.getKey()));
        assertEquals(sizeWithCustom - 1, Registry.MATERIAL.size());
        assertSame(Material.STONE, Registry.MATERIAL.get(NamespacedKey.minecraft("stone")));
    }

    @Test
    public void doesNotIncludeLegacyMaterials() {
        assertNull(Registry.MATERIAL.get(NamespacedKey.minecraft("legacy_air")));
        for (final Material m : Registry.MATERIAL) {
            assertFalse(m.isLegacy(), () -> "legacy leaked into Registry.MATERIAL: " + m);
        }
    }

    @Test
    public void valuesStayVanillaOnlyWhileRegistryIncludesCustom() {
        final CustomBlockDefinition def = CustomBlockDefinition.builder("mintychochip:values_check")
            .host(PacketHostSpec.defaults())
            .build();
        CustomBlocks.register(def);

        assertTrue(Registry.MATERIAL.stream().anyMatch(m -> m == def));
        for (final Material m : Material.values()) {
            assertTrue(m.isVanilla());
            assertNotEquals(def, m);
        }
    }
}
