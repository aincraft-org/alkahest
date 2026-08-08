package dev.mintychochip;

import static org.junit.jupiter.api.Assertions.*;

import dev.mintychochip.customentity.CustomEntities;
import dev.mintychochip.customentity.CustomEntityDefinition;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.VanillaMaterial;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.VanillaEntityType;
import org.bukkit.support.environment.Normal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@Normal
public class EntityTypeBootstrapTest {

    @AfterEach
    public void tearDown() {
        CustomEntities.reset();
    }

    @Test
    public void vanillaEntityTypeEnumConstantsNonNull() {
        assertNotNull(VanillaEntityType.PIG);
        assertNotNull(VanillaEntityType.BLOCK_DISPLAY);
        assertTrue(VanillaEntityType.PIG instanceof EntityType);
        assertTrue(VanillaEntityType.PIG.isVanilla());
        assertFalse(VanillaEntityType.PIG.isCustom());
    }

    @Test
    public void registryEntityTypeIncludesVanillaAndCustom() {
        assertSame(
            VanillaEntityType.PIG,
            Registry.ENTITY_TYPE.get(NamespacedKey.minecraft("pig"))
        );

        // Use VanillaMaterial — Material.* interface constants can be null under Normal suite init order
        final CustomEntityDefinition def = CustomEntityDefinition.builder("mintychochip:server_registry_cube")
            .blockModel(VanillaMaterial.GLOWSTONE)
            .build();
        CustomEntities.register(def);

        assertSame(def, Registry.ENTITY_TYPE.get(def.getKey()));
        assertTrue(Registry.ENTITY_TYPE.stream().anyMatch(t -> t == def));
        assertEquals(def, EntityType.getByKey(def.getKey()).orElseThrow());
        assertTrue(def.isCustom());
        assertFalse(def.isVanilla());
    }
}
