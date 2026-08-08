package dev.mintychochip.provenance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Bounded in-memory ring of provenance events (forensics / tests / admin inspect).
 */
public final class AuditLog {

    private final int capacity;
    private final ArrayList<ProvenanceEvent> events;
    private int writeIndex;
    private int size;

    public AuditLog(final int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
        this.capacity = capacity;
        this.events = new ArrayList<>(Collections.nCopies(capacity, null));
        this.writeIndex = 0;
        this.size = 0;
    }

    public synchronized void append(final @NotNull ProvenanceEvent event) {
        this.events.set(this.writeIndex, event);
        this.writeIndex = (this.writeIndex + 1) % this.capacity;
        if (this.size < this.capacity) {
            this.size++;
        }
        // Durable trail: batched writer (no-op until ProvenanceWriter.install)
        ProvenanceWriter.enqueueAudit(event);
    }

    public synchronized @NotNull List<ProvenanceEvent> snapshot() {
        if (this.size == 0) {
            return List.of();
        }
        final List<ProvenanceEvent> out = new ArrayList<>(this.size);
        final int start = this.size < this.capacity ? 0 : this.writeIndex;
        for (int i = 0; i < this.size; i++) {
            final ProvenanceEvent event = this.events.get((start + i) % this.capacity);
            if (event != null) {
                out.add(event);
            }
        }
        return List.copyOf(out);
    }

    public synchronized @NotNull List<ProvenanceEvent> latest(final int n) {
        final List<ProvenanceEvent> all = this.snapshot();
        if (n >= all.size()) {
            return all;
        }
        return List.copyOf(all.subList(all.size() - n, all.size()));
    }

    public synchronized void clear() {
        Collections.fill(this.events, null);
        this.writeIndex = 0;
        this.size = 0;
    }

    public synchronized int size() {
        return this.size;
    }
}
