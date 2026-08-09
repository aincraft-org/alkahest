package dev.mintychochip.customentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

/**
 * Invariants for the atomic immutable {@link CustomEntityCatalog} snapshot.
 */
public class CustomEntityCatalogTest {

    private static CustomEntityDefinition entity(final String key) {
        return CustomEntityDefinition.builder(key)
            .host(BlockModelHostSpec.of(Material.STONE))
            .build();
    }

    @Test
    public void duplicateKeyFailsWithoutChangingSnapshot() {
        final CustomEntityCatalog catalog = CustomEntityCatalog.create();
        final CustomEntityDefinition first = entity("mintychochip:a");
        catalog.register(first);

        assertThrows(IllegalStateException.class, () -> catalog.register(entity("mintychochip:a")));

        assertEquals(1, catalog.size());
        assertEquals(List.of(first), new ArrayList<>(catalog.all()));
        assertTrue(catalog.get(new NamespacedKey("mintychochip", "a")).isPresent());
    }

    @Test
    public void minecraftNamespaceKeyFailsBeforePublication() {
        final CustomEntityCatalog catalog = CustomEntityCatalog.create();
        assertThrows(IllegalArgumentException.class,
            () -> catalog.register(entity("minecraft:not_allowed")));
        assertTrue(catalog.isEmpty());
    }

    @Test
    public void duplicateObjectIdentityFails() {
        final CustomEntityCatalog catalog = CustomEntityCatalog.create();
        final CustomEntityDefinition def = entity("mintychochip:a");
        catalog.register(def);
        assertThrows(IllegalStateException.class, () -> catalog.register(def));
        assertEquals(1, catalog.size());
    }

    @Test
    public void iterationIsSortedByFullNamespacedKey() {
        final CustomEntityCatalog catalog = CustomEntityCatalog.create();
        catalog.register(entity("mintychochip:zebra"));
        catalog.register(entity("mintychochip:apple"));
        catalog.register(entity("aaa:banana"));

        final List<String> keys = new ArrayList<>();
        for (final CustomEntityDefinition def : catalog.all()) {
            keys.add(def.namespacedKey().toString());
        }
        assertEquals(List.of("aaa:banana", "mintychochip:apple", "mintychochip:zebra"), keys);
    }

    @Test
    public void failedDefinitionKeepsPriorSnapshot() {
        final CustomEntityCatalog catalog = CustomEntityCatalog.create();
        final CustomEntityDefinition first = entity("mintychochip:a");
        catalog.register(first);

        assertThrows(IllegalStateException.class, () -> catalog.register(entity("mintychochip:a")));
        assertEquals(List.of(first), new ArrayList<>(catalog.all()));
        assertEquals(1, catalog.size());
        assertFalse(catalog.all().isEmpty());
    }

    @Test
    public void exposedViewsAreImmutable() {
        final CustomEntityCatalog catalog = CustomEntityCatalog.create();
        catalog.register(entity("mintychochip:a"));
        assertThrows(UnsupportedOperationException.class, () -> catalog.all().clear());
        assertThrows(UnsupportedOperationException.class,
            () -> catalog.asMap().put(new NamespacedKey("mintychochip", "b"), entity("mintychochip:b")));
        assertEquals(1, catalog.size());
    }
}