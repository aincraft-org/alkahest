package dev.mintychochip.customentity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * In-memory registry of {@link CustomEntityDefinition}s keyed by {@link NamespacedKey}.
 *
 * <p>Registrations publish immutable snapshots atomically. Readers therefore observe either
 * the previous complete catalog or the newly published complete catalog.
 */
public final class CustomEntityCatalog {

    private static final Comparator<CustomEntityDefinition> KEY_ORDER =
        Comparator.comparing(definition -> definition.namespacedKey().toString());

    private volatile Snapshot snapshot = Snapshot.empty();

    public synchronized void register(final CustomEntityDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        final NamespacedKey key = Objects.requireNonNull(definition.namespacedKey(), "definition key");
        if (NamespacedKey.MINECRAFT.equals(key.getNamespace())) {
            throw new IllegalArgumentException("custom entity key must not use the minecraft namespace: " + key);
        }
        final Snapshot current = this.snapshot;
        if (current.byKey().containsKey(key)) {
            throw new IllegalStateException("custom entity already registered: " + key);
        }
        for (final CustomEntityDefinition existing : current.values()) {
            if (existing == definition) {
                throw new IllegalStateException("custom entity object already registered: " + key);
            }
        }

        final List<CustomEntityDefinition> next = new ArrayList<>(current.values());
        next.add(definition);
        next.sort(KEY_ORDER);
        this.snapshot = Snapshot.of(next);
    }

    public boolean contains(final NamespacedKey key) {
        return this.snapshot.byKey().containsKey(Objects.requireNonNull(key, "key"));
    }

    public boolean contains(final Key key) {
        return get(key).isPresent();
    }

    public boolean contains(final String namespacedKey) {
        final NamespacedKey key = NamespacedKey.fromString(Objects.requireNonNull(namespacedKey, "namespacedKey"));
        return key != null && this.snapshot.byKey().containsKey(key);
    }

    public Optional<CustomEntityDefinition> get(final NamespacedKey key) {
        return Optional.ofNullable(this.snapshot.byKey().get(Objects.requireNonNull(key, "key")));
    }

    public Optional<CustomEntityDefinition> get(final Key key) {
        Objects.requireNonNull(key, "key");
        if (key instanceof NamespacedKey nk) {
            return get(nk);
        }
        final NamespacedKey nk = new NamespacedKey(key.namespace(), key.value());
        return get(nk);
    }

    public Optional<CustomEntityDefinition> get(final String namespacedKey) {
        final NamespacedKey key = NamespacedKey.fromString(Objects.requireNonNull(namespacedKey, "namespacedKey"));
        if (key == null) {
            return Optional.empty();
        }
        return get(key);
    }

    public @Nullable CustomEntityDefinition getOrNull(final NamespacedKey key) {
        return this.snapshot.byKey().get(Objects.requireNonNull(key, "key"));
    }

    public @UnmodifiableView Collection<CustomEntityDefinition> all() {
        return this.snapshot.values();
    }

    public @UnmodifiableView Map<NamespacedKey, CustomEntityDefinition> asMap() {
        return this.snapshot.byKey();
    }

    public int size() {
        return this.snapshot.values().size();
    }

    public boolean isEmpty() {
        return this.snapshot.values().isEmpty();
    }

    public synchronized void clear() {
        this.snapshot = Snapshot.empty();
    }

    /** Fresh empty catalog. */
    public static @NotNull CustomEntityCatalog create() {
        return new CustomEntityCatalog();
    }

    private record Snapshot(
        List<CustomEntityDefinition> values,
        Map<NamespacedKey, CustomEntityDefinition> byKey
    ) {
        private static Snapshot empty() {
            return new Snapshot(List.of(), Map.of());
        }

        private static Snapshot of(final List<CustomEntityDefinition> values) {
            final LinkedHashMap<NamespacedKey, CustomEntityDefinition> byKey = new LinkedHashMap<>();
            for (final CustomEntityDefinition definition : values) {
                byKey.put(definition.namespacedKey(), definition);
            }
            return new Snapshot(
                List.copyOf(values),
                Collections.unmodifiableMap(byKey)
            );
        }
    }
}