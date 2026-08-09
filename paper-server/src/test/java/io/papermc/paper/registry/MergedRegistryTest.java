package io.papermc.paper.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.customentity.CustomEntities;
import dev.mintychochip.customentity.CustomEntityDefinition;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.VanillaEntityType;
import org.bukkit.support.environment.AllFeatures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@AllFeatures
class MergedRegistryTest {

    @BeforeEach
    void clearCatalog() {
        CustomEntities.reset();
    }

    @AfterEach
    void resetCatalog() {
        CustomEntities.reset();
    }

    @Test
    void interfaceConstantsFollowAlreadyInitializedVanillaEnum() {
        assertNotNull(VanillaEntityType.PIG);
        assertNotNull(EntityType.PIG);
        assertSame(VanillaEntityType.PIG, EntityType.PIG);
    }

    @Test
    void serverRegistryMergesCustomEntitiesAfterNativeValues() {
        final CustomEntityDefinition zebra = CustomEntityDefinition.builder("mintychochip:zebra")
            .blockModel(Material.STONE)
            .build();
        final CustomEntityDefinition apple = CustomEntityDefinition.builder("mintychochip:apple")
            .blockModel(Material.STONE)
            .build();
        CustomEntities.register(zebra);
        CustomEntities.register(apple);

        final Registry<EntityType> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENTITY_TYPE);
        final List<EntityType> values = registry.stream().toList();

        assertSame(apple, registry.get(apple.getKey()));
        assertSame(zebra, registry.get(zebra.getKey()));
        assertEquals(apple, values.get(values.size() - 2));
        assertEquals(zebra, values.get(values.size() - 1));
        assertTrue(registry instanceof PaperCatalogRegistry<?>);
        final PaperCatalogRegistry<EntityType> merged = (PaperCatalogRegistry<EntityType>) registry;
        assertTrue(merged.isNative(EntityType.PIG));
        assertTrue(merged.isCatalog(apple));
    }
}
