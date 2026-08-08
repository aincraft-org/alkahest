package dev.mintychochip.customentity;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
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
 * <p>Not thread-safe for concurrent mutation; intended for bootstrap registration then
 * mostly-read use (same pattern as custom blocks / ecology catalogs).
 */
public final class CustomEntityCatalog {

    private final Map<NamespacedKey, CustomEntityDefinition> byKey = new LinkedHashMap<>();

    public void register(final CustomEntityDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        final NamespacedKey key = definition.namespacedKey();
        if (this.byKey.containsKey(key)) {
            throw new IllegalStateException("custom entity already registered: " + key);
        }
        this.byKey.put(key, definition);
    }

    public boolean contains(final NamespacedKey key) {
        return this.byKey.containsKey(Objects.requireNonNull(key, "key"));
    }

    public boolean contains(final Key key) {
        return get(key).isPresent();
    }

    public boolean contains(final String namespacedKey) {
        final NamespacedKey key = NamespacedKey.fromString(namespacedKey);
        return key != null && this.byKey.containsKey(key);
    }

    public Optional<CustomEntityDefinition> get(final NamespacedKey key) {
        return Optional.ofNullable(this.byKey.get(Objects.requireNonNull(key, "key")));
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
        final NamespacedKey key = NamespacedKey.fromString(namespacedKey);
        if (key == null) {
            return Optional.empty();
        }
        return get(key);
    }

    public @Nullable CustomEntityDefinition getOrNull(final NamespacedKey key) {
        return this.byKey.get(Objects.requireNonNull(key, "key"));
    }

    public @UnmodifiableView Collection<CustomEntityDefinition> all() {
        return Collections.unmodifiableCollection(this.byKey.values());
    }

    public @UnmodifiableView Map<NamespacedKey, CustomEntityDefinition> asMap() {
        return Collections.unmodifiableMap(this.byKey);
    }

    public int size() {
        return this.byKey.size();
    }

    public boolean isEmpty() {
        return this.byKey.isEmpty();
    }

    public void clear() {
        this.byKey.clear();
    }

    /** Fresh empty catalog. */
    public static @NotNull CustomEntityCatalog create() {
        return new CustomEntityCatalog();
    }
}
