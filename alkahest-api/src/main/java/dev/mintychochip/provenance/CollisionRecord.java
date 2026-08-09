package dev.mintychochip.provenance;

import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * One recorded duplicate: the same live stack UUID observed in a second
 * independent place (or merged from a second independent stack).
 */
public final class CollisionRecord {

    private final UUID id;
    private final @NotNull ProvenanceCollisionKind kind;
    private final @NotNull StackLocation existingLocation;
    private final @NotNull StackLocation observedLocation;
    private final long epochMs;

    public CollisionRecord(
        final @NotNull UUID id,
        final @NotNull ProvenanceCollisionKind kind,
        final @NotNull StackLocation existingLocation,
        final @NotNull StackLocation observedLocation,
        final long epochMs
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.existingLocation = Objects.requireNonNull(existingLocation, "existingLocation");
        this.observedLocation = Objects.requireNonNull(observedLocation, "observedLocation");
        this.epochMs = epochMs;
    }

    public @NotNull UUID id() {
        return this.id;
    }

    public @NotNull ProvenanceCollisionKind kind() {
        return this.kind;
    }

    public @NotNull StackLocation existingLocation() {
        return this.existingLocation;
    }

    public @NotNull StackLocation observedLocation() {
        return this.observedLocation;
    }

    public long epochMs() {
        return this.epochMs;
    }

    @Override
    public String toString() {
        return "COLLISION id=" + this.id
            + " kind=" + this.kind
            + " [" + this.existingLocation.display() + " | " + this.observedLocation.display() + "]";
    }
}
