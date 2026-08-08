package dev.mintychochip.provenance;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Single-writer durable sink for provenance: SQLite repository writes plus a
 * rotating JSONL audit export, drained off the server thread.
 *
 * <p>Queue pressure is bounded; when full, events are dropped and counted
 * (never blocking the main thread). Storage failures are surfaced through
 * {@link #status()} and rate-limited logs instead of being silently ignored.
 */
public final class ProvenanceWriter {

    private static final int QUEUE_CAPACITY = 8_192;
    private static final long MAX_AUDIT_BYTES = 64L * 1024 * 1024;
    private static final int MAX_ROTATIONS = 3;
    private static final long ERROR_LOG_INTERVAL_MS = 30_000L;

    private static volatile @Nullable ProvenanceWriter instance;

    private final @NotNull Path auditPath;
    private final @Nullable ProvenanceRepository repository;
    private final @NotNull BlockingQueue<WriteItem> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final @NotNull AtomicBoolean running = new AtomicBoolean(true);
    private final @NotNull Thread thread;
    private final @NotNull AtomicLong dropped = new AtomicLong();
    private final @NotNull AtomicLong written = new AtomicLong();
    private volatile @Nullable String lastError;
    private volatile long lastErrorLogMs;
    private @Nullable BufferedWriter auditWriter;
    private long auditBytes;
    private int itemsSinceFlush;
    private final @NotNull Consumer<String> logger;

    private ProvenanceWriter(final @NotNull Path worldFolder, final @NotNull Consumer<String> logger) {
        this.logger = logger;
        final Path dir = worldFolder.resolve("mintychochip");
        try {
            Files.createDirectories(dir);
        } catch (final IOException ex) {
            this.lastError = "cannot create " + dir + ": " + ex.getMessage();
        }
        this.auditPath = dir.resolve("provenance-audit.jsonl");

        ProvenanceRepository repo = null;
        try {
            repo = new ProvenanceRepository(dir.resolve("provenance.db"));
        } catch (final Exception ex) {
            this.lastError = "provenance store failed to open: " + ex.getMessage();
            logger.accept("[mintychochip] WARN provenance store unavailable, running in-memory only: " + ex.getMessage());
        }
        this.repository = repo;
        LineageStore lineage = ItemProvenance.lineage();
        lineage.attachRepository(repo);

        this.thread = new Thread(this::drain, "mintychochip-provenance-writer");
        this.thread.setDaemon(true);
        this.thread.start();
    }

    public static synchronized void install(final @NotNull Path worldFolder, final @NotNull Consumer<String> logger) {
        if (instance != null) {
            return;
        }
        instance = new ProvenanceWriter(worldFolder, logger);
        final Path audit = instance.auditPath;
        final String store = instance.repository != null ? "sqlite" : "in-memory";
        logger.accept("[mintychochip] provenance audit → " + audit + " (store: " + store + ")");
    }

    /** Test hook: stop the writer and detach. */
    public static synchronized void clearInstall() {
        final ProvenanceWriter writer = instance;
        instance = null;
        if (writer != null) {
            writer.shutdown();
        }
        ItemProvenance.lineage().attachRepository(null);
    }

    /** Flush and close the installed writer (server shutdown). Safe to call repeatedly. */
    public static void flushAndClose() {
        final ProvenanceWriter w = instance;
        if (w != null) {
            w.shutdown();
        }
    }

    public static boolean isInstalled() {
        return instance != null;
    }

    public static void enqueueAudit(final @NotNull ProvenanceEvent event) {
        final ProvenanceWriter w = instance;
        if (w == null) {
            return;
        }
        w.offer(new WriteItem.Audit(event));
    }

    public static void enqueueLineage(final @NotNull LineageNode node) {
        final ProvenanceWriter w = instance;
        if (w == null) {
            return;
        }
        w.offer(new WriteItem.Lineage(node));
    }

    public static void enqueueCollision(final @NotNull CollisionRecord record) {
        final ProvenanceWriter w = instance;
        if (w == null) {
            return;
        }
        w.offer(new WriteItem.Collision(record));
    }

    public static void reportStorageError(final @NotNull String context, final @NotNull Exception ex) {
        final ProvenanceWriter w = instance;
        if (w == null) {
            return;
        }
        w.recordError(context + ": " + ex.getMessage());
    }

    private void offer(final WriteItem item) {
        if (!this.running.get() || this.queue.offer(item)) {
            return;
        }
        this.dropped.incrementAndGet();
        this.recordError("write queue full, event dropped");
    }

    private void drain() {
        while (this.running.get()) {
            try {
                final WriteItem item = this.queue.poll(500, TimeUnit.MILLISECONDS);
                if (item != null) {
                    this.process(item);
                } else {
                    this.flushAudit();
                }
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (final Throwable t) {
                this.recordError("writer failure: " + t);
            }
        }
        // Final drain on close.
        WriteItem item;
        while ((item = this.queue.poll()) != null) {
            try {
                this.process(item);
            } catch (final Throwable t) {
                this.recordError("final drain failure: " + t);
            }
        }
        this.flushAudit();
        this.closeAudit();
        final ProvenanceRepository repo = this.repository;
        if (repo != null) {
            repo.close();
        }
    }

    private void process(final WriteItem item) {
        this.written.incrementAndGet();
        switch (item) {
            case WriteItem.Audit audit -> this.appendAudit(audit.event());
            case WriteItem.Lineage lineage -> {
                final ProvenanceRepository repo = this.repository;
                if (repo != null) {
                    repo.upsertLineage(lineage.node());
                }
            }
            case WriteItem.Collision collision -> {
                final ProvenanceRepository repo = this.repository;
                if (repo != null) {
                    repo.insertCollision(collision.record());
                }
            }
        }
    }

    private void appendAudit(final @NotNull ProvenanceEvent event) {
        if (this.auditWriter == null) {
            try {
                this.auditWriter = Files.newBufferedWriter(
                    this.auditPath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
                );
                this.auditBytes = Files.size(this.auditPath);
            } catch (final IOException ex) {
                this.recordError("cannot open audit file: " + ex.getMessage());
                this.auditWriter = null;
                this.dropped.incrementAndGet();
                return;
            }
        }
        final String line = toJsonLine(event);
        try {
            this.auditWriter.write(line);
            this.auditWriter.newLine();
            this.auditBytes += line.length() + 1L;
            if (++this.itemsSinceFlush >= 100) {
                this.flushAudit();
            }
            if (this.auditBytes >= MAX_AUDIT_BYTES) {
                this.rotateAudit();
            }
        } catch (final IOException ex) {
            this.recordError("audit write failed: " + ex.getMessage());
            this.closeAudit();
            this.dropped.incrementAndGet();
        }
    }

    private void rotateAudit() {
        this.flushAudit();
        this.closeAudit();
        for (int i = MAX_ROTATIONS - 1; i >= 1; i--) {
            final Path from = Path.of(this.auditPath + "." + i);
            final Path to = Path.of(this.auditPath + "." + (i + 1));
            try {
                Files.deleteIfExists(to);
                if (Files.exists(from)) {
                    Files.move(from, to);
                }
            } catch (final IOException ex) {
                this.recordError("audit rotation failed: " + ex.getMessage());
            }
        }
        final Path first = Path.of(this.auditPath + ".1");
        try {
            Files.deleteIfExists(first);
            Files.move(this.auditPath, first);
        } catch (final IOException ex) {
            this.recordError("audit rotation failed: " + ex.getMessage());
        }
        this.auditBytes = 0;
    }

    private void flushAudit() {
        final BufferedWriter writer = this.auditWriter;
        if (writer == null) {
            return;
        }
        try {
            writer.flush();
            this.itemsSinceFlush = 0;
        } catch (final IOException ex) {
            this.recordError("audit flush failed: " + ex.getMessage());
            this.closeAudit();
        }
    }

    private void closeAudit() {
        final BufferedWriter writer = this.auditWriter;
        this.auditWriter = null;
        if (writer != null) {
            try {
                writer.close();
            } catch (final IOException ignored) {
                // already broken
            }
        }
    }

    private void recordError(final @NotNull String message) {
        this.lastError = message;
        final long now = System.currentTimeMillis();
        if (now - this.lastErrorLogMs > ERROR_LOG_INTERVAL_MS) {
            this.lastErrorLogMs = now;
            this.logger.accept("[mintychochip] WARN provenance storage: " + message);
        }
    }

    public void shutdown() {
        this.running.set(false);
        try {
            this.thread.join(3_000L);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public static @NotNull String status() {
        final ProvenanceWriter w = instance;
        if (w == null) {
            return "not installed";
        }
        final String error = w.lastError == null ? "none" : w.lastError;
        return "queue-dropped=" + w.dropped.get()
            + " written=" + w.written.get()
            + " store=" + (w.repository != null && !w.repository.isFailed() ? "sqlite" : "in-memory")
            + " last-error=" + error;
    }

    // -------------------------------------------------------------------------
    // Queue payloads
    // -------------------------------------------------------------------------

    private sealed interface WriteItem {
        record Audit(@NotNull ProvenanceEvent event) implements WriteItem {
        }

        record Lineage(@NotNull LineageNode node) implements WriteItem {
        }

        record Collision(@NotNull CollisionRecord record) implements WriteItem {
        }
    }

    // -------------------------------------------------------------------------
    // JSONL export
    // -------------------------------------------------------------------------

    private static @NotNull String toJsonLine(final ProvenanceEvent event) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("{\"t\":").append(event.epochMs());
        sb.append(",\"type\":\"").append(event.type().name()).append('"');
        sb.append(",\"id\":\"").append(event.id()).append('"');
        if (event.itemId() != null) {
            sb.append(",\"item\":\"").append(escape(event.itemId())).append('"');
        }
        if (event.source() != null) {
            sb.append(",\"source\":\"").append(event.source().name()).append('"');
        }
        if (event.reason() != null) {
            sb.append(",\"reason\":\"").append(event.reason().name()).append('"');
        }
        if (!event.related().isEmpty()) {
            sb.append(",\"related\":[");
            boolean first = true;
            for (final UUID u : event.related()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append('"').append(u).append('"');
            }
            sb.append(']');
        }
        if (event.holder() != null) {
            sb.append(",\"location\":\"").append(escape(event.holder())).append('"');
        }
        if (event.detail() != null) {
            sb.append(",\"detail\":\"").append(escape(event.detail())).append('"');
        }
        sb.append('}');
        return sb.toString();
    }

    private static @NotNull String escape(final String s) {
        final StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    /** Keep the list import used by future tooling (repository queries). */
    static @NotNull List<String> auditFields() {
        return List.of("t", "type", "id", "item", "source", "reason", "related", "location", "detail");
    }
}
