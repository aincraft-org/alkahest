package dev.mintychochip;

import static org.junit.jupiter.api.Assertions.*;

import dev.mintychochip.customblock.BlockFeel;
import dev.mintychochip.customblock.CustomBlockDefinition;
import dev.mintychochip.customblock.CustomBlocks;
import dev.mintychochip.customblock.PacketHostSpec;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.VanillaMaterial;
import org.bukkit.support.environment.Normal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@Normal
public class MaterialBootstrapTest {

    @AfterEach
    public void tearDown() {
        CustomBlocks.reset();
    }

    @Test
    public void vanillaMaterialEnumConstantsNonNull() {
        // Prefer VanillaMaterial for bootstrap assertions: Material interface constants can
        // still be null under partial class-init order in the Normal suite.
        assertNotNull(VanillaMaterial.GLASS);
        assertNotNull(VanillaMaterial.GLOWSTONE);
        assertNotNull(VanillaMaterial.IRON_ORE);
        assertTrue(VanillaMaterial.GLASS instanceof Material);
        assertTrue(VanillaMaterial.GLASS.isVanilla());
        assertFalse(VanillaMaterial.GLASS.isCustom());
    }

    @Test
    public void registryMaterialIncludesVanillaAndCustom() {
        assertSame(
            VanillaMaterial.STONE,
            Registry.MATERIAL.get(NamespacedKey.minecraft("stone"))
        );

        final CustomBlockDefinition def = CustomBlockDefinition.builder("mintychochip:server_registry_ore")
            .host(PacketHostSpec.defaults())
            .feel(BlockFeel.of(3.0F, 3.0F, true, VanillaMaterial.IRON_ORE))
            .build();
        CustomBlocks.register(def);

        assertSame(def, Registry.MATERIAL.get(def.getKey()));
        assertTrue(Registry.MATERIAL.stream().anyMatch(m -> m == def));
        assertEquals(def, Material.getByKey(def.getKey()).orElseThrow());
        assertTrue(def.isCustom());
        assertFalse(def.isVanilla());
    }
}
