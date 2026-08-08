package dev.mintychochip.provenance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Process-lifetime map of every UUID ever stamped (including dead ones) for history
 * walks. Backed by a bounded in-memory cache plus the durable
 * {@link ProvenanceRepository} so ancestry survives restarts.
 */
public final class LineageStore {

    private static final int CACHE_CAPACITY = 65_536;

    private final java.util.LinkedHashMap<UUID, LineageNode> cache =
        new java.util.LinkedHashMap<>(1_024, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(final java.util.Map.Entry<UUID, LineageNode> eldest) {
                return this.size() > CACHE_CAPACITY;
            }
        };

    private volatile @Nullable ProvenanceRepository repository;

    /** Attach the durable store (installed at server startup). */
    public void attachRepository(final @Nullable ProvenanceRepository repository) {
        this.repository = repository;
    }

    public @Nullable ProvenanceRepository repository() {
        return this.repository;
    }

    public void put(final @NotNull LineageNode node) {
        synchronized (this.cache) {
            this.cache.put(node.id(), node);
        }
        ProvenanceWriter.enqueueLineage(node);
    }

    public @NotNull Optional<LineageNode> get(final @NotNull UUID id) {
        final LineageNode cached;
        synchronized (this.cache) {
            cached = this.cache.get(id);
        }
        if (cached != null) {
            return Optional.of(cached);
        }
        final ProvenanceRepository repo = this.repository;
        if (repo == null) {
            return Optional.empty();
        }
        final Optional<LineageNode> loaded = repo.loadLineage(id);
        loaded.ifPresent(node -> {
            synchronized (this.cache) {
                this.cache.put(node.id(), node);
            }
        });
        return loaded;
    }

    public int size() {
        synchronized (this.cache) {
            return this.cache.size();
        }
    }

    /** Drop the in-memory cache only; durable records stay in the repository. */
    public void clearCache() {
        synchronized (this.cache) {
            this.cache.clear();
        }
    }

    /**
     * Depth-first ancestry list: root first, then each parent chain.
     * Cycles are guarded.
     */
    public @NotNull List<LineageNode> walkAncestors(final @NotNull UUID id) {
        final List<LineageNode> out = new ArrayList<>();
        final java.util.HashSet<UUID> seen = new java.util.HashSet<>();
        this.walk(id, out, seen, 0, 64);
        return List.copyOf(out);
    }

    private void walk(
        final UUID id,
        final List<LineageNode> out,
        final java.util.Set<UUID> seen,
        final int depth,
        final int maxDepth
    ) {
        if (depth > maxDepth || !seen.add(id)) {
            return;
        }
        final LineageNode node = this.get(id).orElse(null);
        if (node == null) {
            return;
        }
        out.add(node);
        for (final UUID parent : node.parents()) {
            this.walk(parent, out, seen, depth + 1, maxDepth);
        }
    }

    @Override
    public String toString() {
        return "LineageStore{cache=" + this.size() + ", repo=" + (this.repository != null ? "attached" : "none") + '}';
    }
}
