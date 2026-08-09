package dev.mintychochip.customblock;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Baked host: tripwire string block states remapped by a resource pack.
 *
 * @param stateIndex optional pack-allocated state slot; {@code null} until assigned
 */
public record TripwireHostSpec(@Nullable Integer stateIndex) implements HostSpec {

    public TripwireHostSpec {
        if (stateIndex != null && stateIndex < 0) {
            throw new IllegalArgumentException("stateIndex must be >= 0, got " + stateIndex);
        }
    }

    /** Unassigned tripwire host (state chosen later by pack / runtime allocator). */
    public static TripwireHostSpec unassigned() {
        return new TripwireHostSpec(null);
    }

    public static TripwireHostSpec ofState(final int stateIndex) {
        return new TripwireHostSpec(stateIndex);
    }

    @Override
    public @NotNull BlockHostType type() {
        return BlockHostType.TRIPWIRE;
    }
}
