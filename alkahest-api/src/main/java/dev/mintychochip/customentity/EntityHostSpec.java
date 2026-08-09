package dev.mintychochip.customentity;

import org.jetbrains.annotations.NotNull;

/**
 * Host-specific parameters for a {@link CustomEntityDefinition}.
 *
 * <p>Sealed so each {@link EntityHostType} has exactly one matching spec shape.
 */
public sealed interface EntityHostSpec permits BlockModelHostSpec {

    @NotNull
    EntityHostType type();
}
