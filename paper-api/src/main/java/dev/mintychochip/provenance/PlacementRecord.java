package dev.mintychochip.provenance;

import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provenance stamped onto a world block position when a player places a stack.
 *
 * <p>On break, drops are born as {@link ProvenanceSource#BLOCK_RECOVER} with
 * {@link #parentStackId()} as parent so place→break cannot wash identity.
 */
public final class PlacementRecord {

    private final UUID parentStackId;
    private final String blockItemId;
    private final @Nullable String placer;
    private final long placedEpochMs;

    public PlacementRecord(
        final @NotNull UUID parentStackId,
        final @NotNull String blockItemId,
        final @Nullable String placer,
        final long placedEpochMs
    ) {
        this.parentStackId = Objects.requireNonNull(parentStackId, "parentStackId");
        this.blockItemId = Objects.requireNonNull(blockItemId, "blockItemId");
        this.placer = placer;
        this.placedEpochMs = placedEpochMs;
    }

    public @NotNull UUID parentStackId() {
        return this.parentStackId;
    }

    public @NotNull String blockItemId() {
        return this.blockItemId;
    }

    public @Nullable String placer() {
        return this.placer;
    }

    public long placedEpochMs() {
        return this.placedEpochMs;
    }

    @Override
    public String toString() {
        return "PlacementRecord{parent=" + parentStackId
            + ", item=" + blockItemId
            + ", placer=" + placer
            + '}';
    }
}
