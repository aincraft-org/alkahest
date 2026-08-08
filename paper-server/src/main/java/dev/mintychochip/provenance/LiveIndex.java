package dev.mintychochip.provenance;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * In-memory census of stack UUIDs that should currently exist.
 */
public final class LiveIndex {

    private final ConcurrentHashMap<UUID, LiveEntry> entries = new ConcurrentHashMap<>();

    public @NotNull Optional<LiveEntry> get(final @NotNull UUID id) {
        return Optional.ofNullable(this.entries.get(Objects.requireNonNull(id, "id")));
    }

    public boolean contains(final @NotNull UUID id) {
        return this.entries.containsKey(id);
    }

    public void put(final @NotNull LiveEntry entry) {
        this.entries.put(entry.id(), entry);
    }

    public @Nullable LiveEntry remove(final @NotNull UUID id) {
        return this.entries.remove(id);
    }

    public int size() {
        return this.entries.size();
    }

    public @NotNull Collection<LiveEntry> values() {
        return this.entries.values();
    }

    public void clear() {
        this.entries.clear();
    }
}
