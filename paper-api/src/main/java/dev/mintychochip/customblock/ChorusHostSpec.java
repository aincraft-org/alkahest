package dev.mintychochip.customblock;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Baked host: chorus plant block states remapped by a resource pack.
 *
 * @param stateIndex optional pack-allocated state slot; {@code null} until assigned
 */
public record ChorusHostSpec(@Nullable Integer stateIndex) implements HostSpec {

    public ChorusHostSpec {
        if (stateIndex != null && stateIndex < 0) {
            throw new IllegalArgumentException("stateIndex must be >= 0, got " + stateIndex);
        }
    }

    /** Unassigned chorus host (state chosen later by pack / runtime allocator). */
    public static ChorusHostSpec unassigned() {
        return new ChorusHostSpec(null);
    }

    public static ChorusHostSpec ofState(final int stateIndex) {
        return new ChorusHostSpec(stateIndex);
    }

    @Override
    public @NotNull BlockHostType type() {
        return BlockHostType.CHORUS;
    }
}
