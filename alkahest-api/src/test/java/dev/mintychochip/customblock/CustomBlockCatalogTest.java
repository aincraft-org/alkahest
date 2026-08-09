package dev.mintychochip.customblock;

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
 * Invariants for the atomic immutable {@link CustomBlockCatalog} snapshot.
 */
public class CustomBlockCatalogTest {

    private static CustomBlockDefinition block(final String key) {
        return CustomBlockDefinition.builder(key)
            .host(PacketHostSpec.defaults())
            .itemMaterial(Material.GLASS)
            .build();
    }

    @Test
    public void duplicateKeyFailsWithoutChangingSnapshot() {
        final CustomBlockCatalog catalog = CustomBlockCatalog.create();
        final CustomBlockDefinition first = block("mintychochip:a");
        catalog.register(first);

        assertThrows(IllegalStateException.class, () -> catalog.register(block("mintychochip:a")));

        assertEquals(1, catalog.size());
        assertEquals(List.of(first), new ArrayList<>(catalog.all()));
        assertTrue(catalog.get(new NamespacedKey("mintychochip", "a")).isPresent());
    }

    @Test
    public void minecraftNamespaceKeyFailsBeforePublication() {
        final CustomBlockCatalog catalog = CustomBlockCatalog.create();
        assertThrows(IllegalArgumentException.class,
            () -> catalog.register(block("minecraft:not_allowed")));
        assertTrue(catalog.isEmpty());
    }

    @Test
    public void duplicateObjectIdentityFails() {
        final CustomBlockCatalog catalog = CustomBlockCatalog.create();
        final CustomBlockDefinition def = block("mintychochip:a");
        catalog.register(def);
        assertThrows(IllegalStateException.class, () -> catalog.register(def));
        assertEquals(1, catalog.size());
    }

    @Test
    public void iterationIsSortedByFullNamespacedKey() {
        final CustomBlockCatalog catalog = CustomBlockCatalog.create();
        catalog.register(block("mintychochip:zebra"));
        catalog.register(block("mintychochip:apple"));
        catalog.register(block("aaa:banana"));

        final List<String> keys = new ArrayList<>();
        for (final CustomBlockDefinition def : catalog.all()) {
            keys.add(def.namespacedKey().toString());
        }
        // "aaa:banana" < "mintychochip:apple" < "mintychochip:zebra" by full string
        assertEquals(List.of("aaa:banana", "mintychochip:apple", "mintychochip:zebra"), keys);
    }

    @Test
    public void failedDefinitionKeepsPriorSnapshot() {
        final CustomBlockCatalog catalog = CustomBlockCatalog.create();
        final CustomBlockDefinition first = block("mintychochip:a");
        catalog.register(first);

        // A failed registration (duplicate key) must not mutate the published snapshot.
        assertThrows(IllegalStateException.class, () -> catalog.register(block("mintychochip:a")));
        assertEquals(List.of(first), new ArrayList<>(catalog.all()));
        assertEquals(1, catalog.size());
        assertFalse(catalog.all().isEmpty());
    }

    @Test
    public void exposedViewsAreImmutable() {
        final CustomBlockCatalog catalog = CustomBlockCatalog.create();
        catalog.register(block("mintychochip:a"));
        assertThrows(UnsupportedOperationException.class, () -> catalog.all().clear());
        assertThrows(UnsupportedOperationException.class,
            () -> catalog.asMap().put(new NamespacedKey("mintychochip", "b"), block("mintychochip:b")));
        assertEquals(1, catalog.size());
    }
}