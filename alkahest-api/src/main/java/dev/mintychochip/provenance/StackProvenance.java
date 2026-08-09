package dev.mintychochip.provenance;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Stamp payload carried on an item stack (and mirrored in the lineage store).
 */
public final class StackProvenance {

    private final UUID id;
    private final ProvenanceSource source;
    private final List<UUID> parents;
    private final long bornEpochMs;

    public StackProvenance(
        final @NotNull UUID id,
        final @NotNull ProvenanceSource source,
        final @NotNull List<UUID> parents,
        final long bornEpochMs
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.source = Objects.requireNonNull(source, "source");
        this.parents = List.copyOf(Objects.requireNonNull(parents, "parents"));
        this.bornEpochMs = bornEpochMs;
    }

    public @NotNull UUID id() {
        return this.id;
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

    @Override
    public String toString() {
        return "StackProvenance{id=" + id + ", source=" + source + ", parents=" + parents + '}';
    }
}
