package dev.mintychochip.provenance;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Permanent (for the server process) record of one stack UUID's origin.
 *
 * <p>Parent UUIDs point at prior nodes so callers can walk
 * {@code pickaxe ← cobble + sticks} style histories.
 */
public final class LineageNode {

    private final UUID id;
    private final String itemId;
    private final ProvenanceSource source;
    private final List<UUID> parents;
    private final long bornEpochMs;
    private final @Nullable String bornHolder;
    private volatile boolean dead;
    private volatile @Nullable ProvenanceReason deathReason;
    private volatile long deathEpochMs;

    public LineageNode(
        final @NotNull UUID id,
        final @NotNull String itemId,
        final @NotNull ProvenanceSource source,
        final @NotNull List<UUID> parents,
        final long bornEpochMs,
        final @Nullable String bornHolder
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.itemId = Objects.requireNonNull(itemId, "itemId");
        this.source = Objects.requireNonNull(source, "source");
        this.parents = List.copyOf(Objects.requireNonNull(parents, "parents"));
        this.bornEpochMs = bornEpochMs;
        this.bornHolder = bornHolder;
    }

    public @NotNull UUID id() {
        return this.id;
    }

    public @NotNull String itemId() {
        return this.itemId;
    }

    public @NotNull ProvenanceSource source() {
        return this.source;
    }

    public @NotNull @Unmodifiable List<UUID> parents() {
        return this.parents;
    }

    public long bornEpochMs() {
        return this.bornEpochMs;
    }

    public @Nullable String bornHolder() {
        return this.bornHolder;
    }

    public boolean dead() {
        return this.dead;
    }

    public @Nullable ProvenanceReason deathReason() {
        return this.deathReason;
    }

    public long deathEpochMs() {
        return this.deathEpochMs;
    }

    public void markDead(final @NotNull ProvenanceReason reason, final long epochMs) {
        this.dead = true;
        this.deathReason = Objects.requireNonNull(reason, "reason");
        this.deathEpochMs = epochMs;
    }

    @Override
    public String toString() {
        return "LineageNode{id=" + this.id
            + ", item=" + this.itemId
            + ", source=" + this.source
            + ", parents=" + this.parents
            + ", dead=" + this.dead
            + '}';
    }
}
