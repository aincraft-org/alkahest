package org.bukkit.entity.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Atomic catalog for logical, non-native {@link MemoryKey} values.
 *
 * <p>Catalog keys never enter Minecraft's memory-module registry. They are suitable for plugin
 * state and registry lookup, but native brain and memory-module paths must reject them.
 */
public final class MemoryKeyRegistry {

    private static final Comparator<MemoryKey<?>> KEY_ORDER =
        Comparator.comparing(value -> value.getKey().toString());
    private static volatile Snapshot snapshot = Snapshot.empty();

    private MemoryKeyRegistry() {
    }

    /** Creates and atomically publishes a custom memory key. */
    public static synchronized <T> @NotNull MemoryKey<T> create(
        @NotNull final NamespacedKey key,
        @NotNull final Class<T> memoryClass
    ) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(memoryClass, "memoryClass");
        if (NamespacedKey.MINECRAFT.equals(key.getNamespace())) {
            throw new IllegalArgumentException("custom memory key must not use the minecraft namespace: " + key);
        }
        if (MemoryKey.nativeValue(key) != null) {
            throw new IllegalStateException("memory key collides with a native key: " + key);
        }

        final Snapshot current = snapshot;
        if (current.byKey().containsKey(key)) {
            throw new IllegalStateException("memory key already registered: " + key);
        }
        final MemoryKey<T> value = MemoryKey.custom(key, memoryClass);
        final List<MemoryKey<?>> values = new ArrayList<>(current.values());
        values.add(value);
        values.sort(KEY_ORDER);
        snapshot = Snapshot.of(values);
        return value;
    }

    public static @Nullable MemoryKey<?> get(@NotNull final NamespacedKey key) {
        return snapshot.byKey().get(Objects.requireNonNull(key, "key"));
    }

    /** Returns an immutable, deterministically sorted snapshot of custom memory keys. */
    public static @NotNull Set<MemoryKey<?>> values() {
        return snapshot.values();
    }
    /** Clears the custom catalog for an isolated bootstrap or test. */
    public static synchronized void clear() {
        snapshot = Snapshot.empty();
    }

    private record Snapshot(Set<MemoryKey<?>> values, Map<NamespacedKey, MemoryKey<?>> byKey) {
        private static Snapshot empty() {
            return new Snapshot(Set.of(), Map.of());
        }

        private static Snapshot of(final List<MemoryKey<?>> values) {
            final LinkedHashMap<NamespacedKey, MemoryKey<?>> byKey = new LinkedHashMap<>();
            for (final MemoryKey<?> value : values) {
                byKey.put(value.getKey(), value);
            }
            final LinkedHashSet<MemoryKey<?>> ordered = new LinkedHashSet<>(values);
            return new Snapshot(
                Collections.unmodifiableSet(ordered),
                Collections.unmodifiableMap(byKey)
            );
        }
    }
}
