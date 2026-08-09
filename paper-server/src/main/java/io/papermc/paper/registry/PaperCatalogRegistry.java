package io.papermc.paper.registry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

/**
 * Reusable server-side {@link Registry} view that merges a native (NMS-backed) registry with an
 * atomically published custom catalog snapshot.
 *
 * <p>Native values are iterated first. Custom values are sorted by their complete namespaced key
 * for every operation, and each operation captures one complete custom snapshot. Tags delegate
 * only to the native registry.
 *
 * @param <V> registry value type
 */
@NullMarked
public class PaperCatalogRegistry<V extends Keyed> extends Registry.NotARegistry<V> {

    private static final Comparator<Keyed> KEY_ORDER =
        Comparator.comparing(value -> value.getKey().toString());

    private final Supplier<? extends Registry<? extends V>> nativeRegistry;
    private final Supplier<? extends Map<NamespacedKey, ? extends V>> catalogSnapshot;

    public PaperCatalogRegistry(
        final Supplier<? extends Registry<? extends V>> nativeRegistry,
        final Supplier<? extends Map<NamespacedKey, ? extends V>> catalogSnapshot
    ) {
        this.nativeRegistry = Objects.requireNonNull(nativeRegistry, "nativeRegistry");
        this.catalogSnapshot = Objects.requireNonNull(catalogSnapshot, "catalogSnapshot");
    }

    private Registry<? extends V> nativeRegistry() {
        return Objects.requireNonNull(this.nativeRegistry.get(), "native registry");
    }

    private Snapshot<V> customSnapshot() {
        final Map<NamespacedKey, ? extends V> source =
            Objects.requireNonNull(this.catalogSnapshot.get(), "catalog snapshot");
        final List<V> values = new ArrayList<>(source.values());
        values.sort((left, right) -> KEY_ORDER.compare(left, right));
        final LinkedHashMap<NamespacedKey, V> byKey = new LinkedHashMap<>(source.size());
        for (final Map.Entry<NamespacedKey, ? extends V> entry : source.entrySet()) {
            byKey.put(entry.getKey(), entry.getValue());
        }
        return new Snapshot<>(List.copyOf(values), Map.copyOf(byKey));
    }

    @Override
    public @Nullable V get(final NamespacedKey key) {
        Objects.requireNonNull(key, "key");
        final Registry<? extends V> nativeReg = nativeRegistry();
        final V nativeValue = nativeReg.get(key);
        if (nativeValue != null) {
            return nativeValue;
        }
        return customSnapshot().byKey().get(key);
    }

    @Override
    public @NotNull Iterator<V> iterator() {
        final Iterator<? extends V> nativeIterator = nativeRegistry().iterator();
        final Iterator<V> customIterator = customSnapshot().values().iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return nativeIterator.hasNext() || customIterator.hasNext();
            }

            @Override
            public V next() {
                return nativeIterator.hasNext() ? nativeIterator.next() : customIterator.next();
            }
        };
    }

    @Override
    public int size() {
        return nativeRegistry().size() + customSnapshot().values().size();
    }

    @Override
    public Stream<NamespacedKey> keyStream() {
        final Registry<? extends V> nativeReg = nativeRegistry();
        final Snapshot<V> custom = customSnapshot();
        return Stream.concat(nativeReg.keyStream(), custom.values().stream().map(Keyed::getKey));
    }

    /**
     * Returns whether {@code value} is the exact native object resolved by this view's native
     * registry. Implementing the same API interface is not sufficient.
     */
    public boolean isNative(final V value) {
        Objects.requireNonNull(value, "value");
        return nativeRegistry().get(value.getKey()) == value;
    }

    /** Returns whether {@code value} is the exact object in the current catalog snapshot. */
    public boolean isCatalog(final V value) {
        Objects.requireNonNull(value, "value");
        return customSnapshot().byKey().get(value.getKey()) == value;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Registry<V> nativeTags() {
        return (Registry) nativeRegistry();
    }

    @Override
    public boolean hasTag(final io.papermc.paper.registry.tag.TagKey<V> key) {
        return nativeTags().hasTag(key);
    }

    @Override
    public io.papermc.paper.registry.tag.Tag<V> getTag(final io.papermc.paper.registry.tag.TagKey<V> key) {
        return nativeTags().getTag(key);
    }

    @Override
    public java.util.Collection<io.papermc.paper.registry.tag.Tag<V>> getTags() {
        return nativeTags().getTags();
    }

    private record Snapshot<V>(List<V> values, Map<NamespacedKey, V> byKey) {
    }
}
