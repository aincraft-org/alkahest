package org.bukkit;

import static org.junit.jupiter.api.Assertions.*;

import dev.mintychochip.customentity.CustomEntities;
import dev.mintychochip.customentity.CustomEntityDefinition;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.VanillaEntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Registry#ENTITY_TYPE} / {@link EntityTypeRegistry}:
 * vanilla non-UNKNOWN constants plus registered custom entity types.
 */
public class EntityTypeRegistryTest {

    @BeforeEach
    public void setUp() {
        CustomEntities.reset();
    }

    @AfterEach
    public void tearDown() {
        CustomEntities.reset();
    }

    @Test
    public void getResolvesVanillaNonUnknown() {
        final NamespacedKey pigKey = NamespacedKey.minecraft("pig");
        assertSame(EntityType.PIG, Registry.ENTITY_TYPE.get(pigKey));
        assertTrue(Registry.ENTITY_TYPE.stream().anyMatch(t -> t == EntityType.PIG));
        assertTrue(Registry.ENTITY_TYPE.size() > 0);
        assertInstanceOf(EntityTypeRegistry.class, Registry.ENTITY_TYPE);
        assertNull(Registry.ENTITY_TYPE.get(NamespacedKey.minecraft("unknown")));
    }

    @Test
    public void getResolvesCustomAfterRegister() {
        final CustomEntityDefinition def = CustomEntityDefinition.builder("mintychochip:registry_cube")
            .blockModel(Material.GLOWSTONE)
            .build();
        CustomEntities.register(def);

        assertSame(def, Registry.ENTITY_TYPE.get(def.getKey()));
        assertSame(def, Registry.ENTITY_TYPE.getOrThrow(def.getKey()));
        assertTrue(Registry.ENTITY_TYPE.stream().anyMatch(t -> t == def));
        assertTrue(Registry.ENTITY_TYPE.keyStream().anyMatch(k -> k.equals(def.getKey())));

        final int sizeWithCustom = Registry.ENTITY_TYPE.size();
        CustomEntities.reset();
        assertNull(Registry.ENTITY_TYPE.get(def.getKey()));
        assertEquals(sizeWithCustom - 1, Registry.ENTITY_TYPE.size());
        assertSame(EntityType.PIG, Registry.ENTITY_TYPE.get(NamespacedKey.minecraft("pig")));
    }

    @Test
    public void doesNotIncludeUnknown() {
        for (final EntityType t : Registry.ENTITY_TYPE) {
            assertNotEquals(VanillaEntityType.UNKNOWN, t, "UNKNOWN leaked into Registry.ENTITY_TYPE");
        }
    }

    @Test
    public void valuesStayVanillaOnlyWhileRegistryIncludesCustom() {
        final CustomEntityDefinition def = CustomEntityDefinition.builder("mintychochip:values_check")
            .blockModel(Material.STONE)
            .build();
        CustomEntities.register(def);

        assertTrue(Registry.ENTITY_TYPE.stream().anyMatch(t -> t == def));
        for (final EntityType t : EntityType.values()) {
            assertTrue(t.isVanilla() || t == EntityType.UNKNOWN);
            assertNotEquals(def, t);
        }
    }
}
