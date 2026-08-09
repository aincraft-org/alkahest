package org.bukkit.entity.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MemoryKeyRegistryTest {

    @AfterEach
    void clearCatalog() {
        MemoryKeyRegistry.clear();
    }

    @Test
    void createsCustomKeyAndPublishesSortedSnapshot() {
        final MemoryKey<Integer> zebra = MemoryKeyRegistry.create(
            new NamespacedKey("mintychochip", "zebra"), Integer.class);
        final MemoryKey<String> apple = MemoryKeyRegistry.create(
            new NamespacedKey("mintychochip", "apple"), String.class);

        assertTrue(zebra.isCustom());
        assertEquals(Integer.class, zebra.getMemoryClass());
        assertEquals(zebra, MemoryKey.getByKey(zebra.getKey()));
        assertEquals(
            List.of("mintychochip:apple", "mintychochip:zebra"),
            MemoryKeyRegistry.values().stream().map(key -> key.getKey().toString()).toList()
        );
        assertEquals(
            List.of("minecraft:admiring_disabled", "minecraft:admiring_item"),
            new ArrayList<>(MemoryKey.values()).stream().map(key -> key.getKey().toString()).limit(2).toList()
        );
        assertEquals(apple, MemoryKeyRegistry.get(apple.getKey()));
    }

    @Test
    void rejectsMinecraftAndDuplicateKeysWithoutChangingSnapshot() {
        final MemoryKey<Integer> value = MemoryKeyRegistry.create(
            new NamespacedKey("mintychochip", "one"), Integer.class);

        assertThrows(IllegalArgumentException.class,
            () -> MemoryKeyRegistry.create(new NamespacedKey("minecraft", "nope"), Integer.class));
        assertThrows(IllegalStateException.class,
            () -> MemoryKeyRegistry.create(value.getKey(), Integer.class));
        assertEquals(List.of(value), new ArrayList<>(MemoryKeyRegistry.values()));
    }

    @Test
    void nativeKeysRemainVanilla() {
        assertTrue(MemoryKey.ADMIRING_ITEM.isVanilla());
        assertEquals(MemoryKey.ADMIRING_ITEM, MemoryKey.getByKey(MemoryKey.ADMIRING_ITEM.getKey()));
        assertTrue(MemoryKeyRegistry.values().isEmpty());
    }
}
