package dev.mintychochip.customentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.VanillaEntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Proves custom definitions are real {@link EntityType}s usable like vanilla constants.
 *
 * <p>Registry.ENTITY_TYPE merge is covered by {@code org.bukkit.EntityTypeRegistryTest}.
 */
public class CustomEntityTypeParityTest {

    @AfterEach
    public void tearDown() {
        CustomEntities.reset();
    }

    @Test
    public void vanillaConstantsAreEntityTypeAndSameInstanceAsEnum() {
        assertTrue(EntityType.PIG instanceof VanillaEntityType);
        assertSame(VanillaEntityType.PIG, EntityType.PIG);
        assertEquals("PIG", EntityType.PIG.name());
        assertTrue(EntityType.PIG.isVanilla());
        assertFalse(EntityType.PIG.isCustom());
        assertTrue(EntityType.BLOCK_DISPLAY.isSpawnable());
    }

    @Test
    public void customDefinitionIsEntityType() {
        final CustomEntityDefinition def = CustomEntityDefinition.builder("mintychochip:glow_cube")
            .blockModel(Material.GLOWSTONE)
            .build();
        CustomEntities.register(def);

        final EntityType type = def;
        assertTrue(type.isCustom());
        assertFalse(type.isVanilla());
        assertEquals("mintychochip:glow_cube", type.getKey().toString());
        assertEquals(BlockDisplay.class, type.getEntityClass());
        assertTrue(type.isSpawnable());
        assertSame(def, CustomEntities.get("mintychochip:glow_cube").orElseThrow());

        // Lookup by key (custom catalog)
        assertEquals(def, EntityType.getByKey(def.getKey()).orElseThrow());
    }

    @Test
    public void valueOfAndValuesStillWorkForVanilla() {
        assertSame(EntityType.COW, EntityType.valueOf("COW"));
        boolean foundPig = false;
        for (final EntityType t : EntityType.values()) {
            if (t == EntityType.PIG) {
                foundPig = true;
                break;
            }
            // values() is vanilla-only — customs never appear there
            assertTrue(t.isVanilla() || t == EntityType.UNKNOWN);
        }
        assertTrue(foundPig);
    }
}
