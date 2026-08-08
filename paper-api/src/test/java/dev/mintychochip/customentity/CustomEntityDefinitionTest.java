package dev.mintychochip.customentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class CustomEntityDefinitionTest {

    @AfterEach
    public void tearDown() {
        CustomEntities.reset();
    }

    @Test
    public void blockModelDefinitionDefaults() {
        final CustomEntityDefinition def = CustomEntityDefinition.builder("mintychochip:glow_cube")
            .host(BlockModelHostSpec.of(Material.GLOWSTONE))
            .displayName(Component.text("Glow Cube"))
            .build();

        assertEquals(EntityHostType.BLOCK_MODEL, def.hostType());
        assertTrue(def.isBlockModel());
        assertEquals(Material.GLOWSTONE, def.blockMaterial());
        assertNotNull(def.displayName());
        assertEquals("mintychochip:glow_cube", def.namespacedKey().toString());

        final BlockModelHostSpec host = (BlockModelHostSpec) def.host();
        assertEquals(Material.GLOWSTONE, host.blockMaterial());
        assertEquals(new Vector3f(0.0f, 0.0f, 0.0f), host.translation());
        assertEquals(new Vector3f(1.0f, 1.0f, 1.0f), host.scale());

        final Transformation t = host.toTransformation();
        assertEquals(new Vector3f(1.0f, 1.0f, 1.0f), t.getScale());
    }

    @Test
    public void blockModelBuilderShorthand() {
        final CustomEntityDefinition def = CustomEntityDefinition.builder("mintychochip:stone_orb")
            .blockModel(Material.STONE)
            .build();
        assertEquals(Material.STONE, def.blockMaterial());
        assertEquals(EntityHostType.BLOCK_MODEL, def.hostType());
    }

    @Test
    public void customTransform() {
        final BlockModelHostSpec host = BlockModelHostSpec.builder(Material.DIAMOND_BLOCK)
            .translation(new Vector3f(0.5f, 0.0f, 0.5f))
            .scale(new Vector3f(0.5f, 0.5f, 0.5f))
            .build();
        assertEquals(new Vector3f(0.5f, 0.0f, 0.5f), host.translation());
        assertEquals(new Vector3f(0.5f, 0.5f, 0.5f), host.scale());
    }

    @Test
    public void builderRequiresHost() {
        assertThrows(IllegalStateException.class, () ->
            CustomEntityDefinition.builder("mintychochip:no_host")
                .displayName(Component.text("x"))
                .build()
        );
    }

    @Test
    public void rejectsAirBlockMaterial() {
        assertThrows(IllegalArgumentException.class, () ->
            BlockModelHostSpec.of(Material.AIR)
        );
        assertThrows(IllegalArgumentException.class, () ->
            BlockModelHostSpec.of(Material.CAVE_AIR)
        );
    }

    @Test
    public void catalogRegisterAndLookup() {
        final CustomEntityDefinition cube = CustomEntityDefinition.builder(
                NamespacedKey.fromString("mintychochip:glow_cube"))
            .host(BlockModelHostSpec.of(Material.GLOWSTONE))
            .build();

        CustomEntities.register(cube);

        assertTrue(CustomEntities.contains(cube.namespacedKey()));
        assertEquals(cube, CustomEntities.get("mintychochip:glow_cube").orElseThrow());
        assertEquals(1, CustomEntities.all().size());
        assertEquals(cube, CustomEntities.catalog().get(cube.namespacedKey()).orElseThrow());

        assertThrows(IllegalStateException.class, () -> CustomEntities.register(cube));
    }

    @Test
    public void hostTypeHelpers() {
        assertTrue(EntityHostType.BLOCK_MODEL.isBlockModel());
    }

    @Test
    public void invalidNamespacedKeyRejected() {
        assertThrows(IllegalArgumentException.class, () ->
            CustomEntityDefinition.builder("not a key")
        );
    }

    @Test
    public void getTypeRemainsSeparateConcept() {
        // Documentation guard: catalog identity is NamespacedKey, not EntityType.
        final CustomEntityDefinition def = CustomEntityDefinition.builder("mintychochip:glow_cube")
            .host(BlockModelHostSpec.of(Material.GLOWSTONE))
            .build();
        assertEquals(Material.GLOWSTONE, def.blockMaterial());
        assertEquals("mintychochip:glow_cube", def.namespacedKey().toString());
    }
}
