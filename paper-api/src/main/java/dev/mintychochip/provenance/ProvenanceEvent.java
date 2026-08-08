package dev.mintychochip.provenance;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * One append-only audit entry.
 */
public final class ProvenanceEvent {

    private final long epochMs;
    private final ProvenanceEventType type;
    private final UUID id;
    private final @Nullable String itemId;
    private final @Nullable ProvenanceSource source;
    private final @Nullable ProvenanceReason reason;
    private final List<UUID> related;
    private final @Nullable String holder;
    private final @Nullable String detail;

    public ProvenanceEvent(
        final long epochMs,
        final @NotNull ProvenanceEventType type,
        final @NotNull UUID id,
        final @Nullable String itemId,
        final @Nullable ProvenanceSource source,
        final @Nullable ProvenanceReason reason,
        final @NotNull List<UUID> related,
        final @Nullable String holder,
        final @Nullable String detail
    ) {
        this.epochMs = epochMs;
        this.type = Objects.requireNonNull(type, "type");
        this.id = Objects.requireNonNull(id, "id");
        this.itemId = itemId;
        this.source = source;
        this.reason = reason;
        this.related = List.copyOf(Objects.requireNonNull(related, "related"));
        this.holder = holder;
        this.detail = detail;
    }

    public long epochMs() {
        return this.epochMs;
    }

    public @NotNull ProvenanceEventType type() {
        return this.type;
    }

    public @NotNull UUID id() {
        return this.id;
    }

    public @Nullable String itemId() {
        return this.itemId;
    }

    public @Nullable ProvenanceSource source() {
        return this.source;
    }

    public @Nullable ProvenanceReason reason() {
        return this.reason;
    }

    public @NotNull @Unmodifiable List<UUID> related() {
        return this.related;
    }

    public @Nullable String holder() {
        return this.holder;
    }

    public @Nullable String detail() {
        return this.detail;
    }

    @Override
    public String toString() {
        return type + " id=" + id
            + (itemId != null ? " item=" + itemId : "")
            + (source != null ? " source=" + source : "")
            + (reason != null ? " reason=" + reason : "")
            + (related.isEmpty() ? "" : " related=" + related)
            + (holder != null ? " holder=" + holder : "")
            + (detail != null ? " (" + detail + ")" : "");
    }
}
