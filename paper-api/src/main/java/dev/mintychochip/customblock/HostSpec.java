package dev.mintychochip.customblock;

import org.jetbrains.annotations.NotNull;

/**
 * Host-specific parameters for a {@link CustomBlockDefinition}.
 *
 * <p>Sealed so each {@link BlockHostType} has exactly one matching spec shape.
 */
public sealed interface HostSpec
    permits ChorusHostSpec, MushroomHostSpec, TripwireHostSpec, PacketHostSpec {

    @NotNull
    BlockHostType type();
}
