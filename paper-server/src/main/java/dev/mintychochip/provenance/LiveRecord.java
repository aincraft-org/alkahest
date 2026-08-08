package dev.mintychochip.provenance;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Durable live-census row: last-seen identity, location, and count for a stack UUID.
 */
public record LiveRecord(
    @NotNull UUID id,
    @NotNull String itemId,
    @NotNull String locationDisplay,
    int count,
    long epochMs,
    boolean dead
) {
}
