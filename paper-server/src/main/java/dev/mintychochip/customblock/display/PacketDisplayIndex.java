package dev.mintychochip.customblock.display;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two-way index between display location keys and the chunk they belong to.
 *
 * <p>Keeps {@link PacketDisplayService} chunk show/hide O(displays in that chunk)
 * instead of scanning every display in every world. All mutations go through this
 * class so the location map and the chunk map can never diverge.
 *
 * <p>Generic over the display value type so the index logic is unit-testable
 * without packet/entity machinery; the service uses {@code PacketDisplayIndex<PacketItemDisplay>}.
 */
final class PacketDisplayIndex<V> {

    private final Map<String, V> byLocation = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> byChunk = new ConcurrentHashMap<>();

    /**
     * Registers (or replaces) {@code display} at {@code locKey} in chunk {@code chunkKey}.
     *
     * @return the previous display at {@code locKey}, or {@code null} if none
     */
    V put(final String locKey, final String chunkKey, final V display) {
        final V old = this.byLocation.put(locKey, display);
        this.byChunk.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet()).add(locKey);
        return old;
    }

    /**
     * Removes the display at {@code locKey} and drops it from the chunk index.
     *
     * @return the removed display, or {@code null} if it was not present
     */
    V remove(final String locKey, final String chunkKey) {
        final V old = this.byLocation.remove(locKey);
        if (old != null) {
            this.byChunk.computeIfPresent(chunkKey, (k, set) -> {
                set.remove(locKey);
                return set.isEmpty() ? null : set;
            });
        }
        return old;
    }

    V get(final String locKey) {
        return this.byLocation.get(locKey);
    }

    /** All displays currently held (values of the location map). */
    Collection<V> all() {
        return this.byLocation.values();
    }

    /**
     * Displays registered in {@code chunkKey} as a {@code locKey -> display} map,
     * in an arbitrary order. Never returns {@code null}.
     */
    Map<String, V> entriesInChunk(final String chunkKey) {
        final Set<String> keys = this.byChunk.get(chunkKey);
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, V> out = new java.util.LinkedHashMap<>(keys.size());
        for (final String locKey : keys) {
            final V display = this.byLocation.get(locKey);
            if (display != null) {
                out.put(locKey, display);
            }
        }
        return out;
    }

    /**
     * Displays registered in {@code chunkKey}, in an arbitrary order. Never returns
     * {@code null}.
     */
    Collection<V> displaysInChunk(final String chunkKey) {
        return entriesInChunk(chunkKey).values();
    }

    void clear() {
        this.byLocation.clear();
        this.byChunk.clear();
    }
}