package dev.mintychochip.provenance;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * Census row: a stack UUID that currently exists, where its instances were last
 * observed, and the total count.
 *
 * <p>A legitimate identity has exactly one location. A second concrete location
 * is recorded as a collision; colliding observations are not added so the
 * accepted location stays intact.
 */
public final class LiveEntry {

    private final UUID id;
    private final String itemId;
    /** Tracked instances of this identity (normally one). */
    private final ConcurrentHashMap<StackLocation, Boolean> locations = new ConcurrentHashMap<>();
    private volatile int count;
    private final long bornEpochMs;

    public LiveEntry(
        final @NotNull UUID id,
        final @NotNull String itemId,
        final @NotNull StackLocation location,
        final int count,
        final long bornEpochMs
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.itemId = Objects.requireNonNull(itemId, "itemId");
        this.count = count;
        this.bornEpochMs = bornEpochMs;
        if (location.isConcrete()) {
            this.locations.put(location, Boolean.TRUE);
        }
    }

    public @NotNull UUID id() {
        return this.id;
    }

    public @NotNull String itemId() {
        return this.itemId;
    }

    public @NotNull Collection<StackLocation> locations() {
        return this.locations.keySet();
    }

    /** Primary tracked location, if any. */
    public @NotNull StackLocation location() {
        return this.locations.keySet().stream().findFirst().orElse(StackLocation.unknown());
    }

    public void addLocation(final @NotNull StackLocation location) {
        if (location.isConcrete()) {
            this.locations.put(location, Boolean.TRUE);
        }
    }

    public void removeLocation(final @NotNull StackLocation location) {
        this.locations.remove(location);
    }

    public int count() {
        return this.count;
    }

    public void setCount(final int count) {
        this.count = count;
    }

    public long bornEpochMs() {
        return this.bornEpochMs;
    }
}
