package dev.mintychochip.provenance;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Durable provenance store (SQLite).
 *
 * <p>Holds the permanent lineage graph, collision trail, live census, and audit
 * event log. The live census is durable and seeds runtime {@link LiveIndex} on
 * restart; audit is the permanent event ledger.
 */
public final class ProvenanceRepository implements AutoCloseable {

    private final Connection connection;
    private volatile boolean failed;

    public ProvenanceRepository(final @NotNull java.nio.file.Path dbPath) throws SQLException {
        Objects.requireNonNull(dbPath, "dbPath");
        dbPath.toFile().getParentFile().mkdirs();
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        try (Statement st = this.connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("""
                CREATE TABLE IF NOT EXISTS lineage (
                    id TEXT PRIMARY KEY,
                    item TEXT NOT NULL,
                    source TEXT NOT NULL,
                    parents TEXT NOT NULL,
                    born INTEGER NOT NULL,
                    holder TEXT,
                    dead INTEGER NOT NULL DEFAULT 0,
                    death_reason TEXT,
                    death_epoch INTEGER
                )
                """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS collisions (
                    id TEXT NOT NULL,
                    kind TEXT NOT NULL,
                    existing TEXT NOT NULL,
                    observed TEXT NOT NULL,
                    epoch INTEGER NOT NULL
                )
                """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_collisions_id ON collisions(id)");
            st.execute("""
                CREATE TABLE IF NOT EXISTS live (
                    id TEXT PRIMARY KEY,
                    item TEXT NOT NULL,
                    location TEXT NOT NULL,
                    count INTEGER NOT NULL,
                    epoch INTEGER NOT NULL,
                    dead INTEGER NOT NULL DEFAULT 0
                )
                """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS audit (
                    seq INTEGER PRIMARY KEY AUTOINCREMENT,
                    epoch INTEGER NOT NULL,
                    kind TEXT NOT NULL,
                    id TEXT NOT NULL,
                    item TEXT,
                    source TEXT,
                    reason TEXT,
                    related TEXT,
                    holder TEXT,
                    detail TEXT
                )
                """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_audit_epoch ON audit(epoch)");
        }
    }

    public boolean isFailed() {
        return this.failed;
    }

    public synchronized void upsertLineage(final @NotNull LineageNode node) {
        if (this.failed) {
            return;
        }
        final String sql = """
            INSERT INTO lineage (id, item, source, parents, born, holder, dead, death_reason, death_epoch)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                item = excluded.item,
                source = excluded.source,
                parents = excluded.parents,
                born = excluded.born,
                holder = excluded.holder,
                dead = excluded.dead,
                death_reason = excluded.death_reason,
                death_epoch = excluded.death_epoch
            """;
        try (PreparedStatement ps = this.connection.prepareStatement(sql)) {
            ps.setString(1, node.id().toString());
            ps.setString(2, node.itemId());
            ps.setString(3, node.source().name());
            ps.setString(4, node.parents().stream().map(UUID::toString).collect(Collectors.joining(",")));
            ps.setLong(5, node.bornEpochMs());
            ps.setString(6, node.bornHolder());
            ps.setInt(7, node.dead() ? 1 : 0);
            ps.setString(8, node.dead() ? node.deathReason().name() : null);
            ps.setLong(9, node.dead() ? node.deathEpochMs() : 0L);
            ps.executeUpdate();
        } catch (final SQLException ex) {
            this.failed = true;
            ProvenanceWriter.reportStorageError("lineage upsert", ex);
        }
    }

    public synchronized @NotNull Optional<LineageNode> loadLineage(final @NotNull UUID id) {
        if (this.failed) {
            return Optional.empty();
        }
        try (PreparedStatement ps = this.connection.prepareStatement(
            "SELECT item, source, parents, born, holder, dead, death_reason, death_epoch FROM lineage WHERE id = ?"
        )) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(toNode(id, rs));
            }
        } catch (final SQLException ex) {
            this.failed = true;
            ProvenanceWriter.reportStorageError("lineage load", ex);
            return Optional.empty();
        }
    }

    public synchronized void insertCollision(final @NotNull CollisionRecord record) {
        if (this.failed) {
            return;
        }
        try (PreparedStatement ps = this.connection.prepareStatement(
            "INSERT INTO collisions (id, kind, existing, observed, epoch) VALUES (?, ?, ?, ?, ?)"
        )) {
            ps.setString(1, record.id().toString());
            ps.setString(2, record.kind().name());
            ps.setString(3, record.existingLocation().display());
            ps.setString(4, record.observedLocation().display());
            ps.setLong(5, record.epochMs());
            ps.executeUpdate();
        } catch (final SQLException ex) {
            this.failed = true;
            ProvenanceWriter.reportStorageError("collision insert", ex);
        }
    }

    public synchronized @NotNull List<CollisionRecord> loadRecentCollisions(final int limit) {
        if (this.failed) {
            return List.of();
        }
        final List<CollisionRecord> out = new ArrayList<>();
        try (PreparedStatement ps = this.connection.prepareStatement(
            "SELECT id, kind, existing, observed, epoch FROM collisions ORDER BY epoch DESC LIMIT ?"
        )) {
            ps.setInt(1, Math.max(1, Math.min(limit, 10_000)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new CollisionRecord(
                        UUID.fromString(rs.getString("id")),
                        ProvenanceCollisionKind.valueOf(rs.getString("kind")),
                        parseLocationDisplay(rs.getString("existing")),
                        parseLocationDisplay(rs.getString("observed")),
                        rs.getLong("epoch")
                    ));
                }
            }
        } catch (final SQLException | IllegalArgumentException ex) {
            this.failed = true;
            ProvenanceWriter.reportStorageError("collision load", ex);
        }
        return out;
    }

    public synchronized void upsertLive(final @NotNull LiveRecord record) {
        if (this.failed) {
            return;
        }
        final String sql = """
            INSERT INTO live (id, item, location, count, epoch, dead)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                item = excluded.item,
                location = excluded.location,
                count = excluded.count,
                epoch = excluded.epoch,
                dead = excluded.dead
            """;
        try (PreparedStatement ps = this.connection.prepareStatement(sql)) {
            ps.setString(1, record.id().toString());
            ps.setString(2, record.itemId());
            ps.setString(3, record.locationDisplay());
            ps.setInt(4, record.count());
            ps.setLong(5, record.epochMs());
            ps.setInt(6, record.dead() ? 1 : 0);
            ps.executeUpdate();
        } catch (final SQLException ex) {
            this.failed = true;
            ProvenanceWriter.reportStorageError("live upsert", ex);
        }
    }

    public synchronized @NotNull List<LiveRecord> loadAliveLive() {
        if (this.failed) {
            return List.of();
        }
        final List<LiveRecord> out = new ArrayList<>();
        try (PreparedStatement ps = this.connection.prepareStatement(
            "SELECT id, item, location, count, epoch, dead FROM live WHERE dead = 0"
        )) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new LiveRecord(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("item"),
                        rs.getString("location"),
                        rs.getInt("count"),
                        rs.getLong("epoch"),
                        rs.getInt("dead") != 0
                    ));
                }
            }
        } catch (final SQLException | IllegalArgumentException ex) {
            this.failed = true;
            ProvenanceWriter.reportStorageError("live load", ex);
        }
        return out;
    }

    public synchronized void insertAudit(final @NotNull ProvenanceEvent event) {
        if (this.failed) {
            return;
        }
        try (PreparedStatement ps = this.connection.prepareStatement(
            "INSERT INTO audit (epoch, kind, id, item, source, reason, related, holder, detail) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
        )) {
            ps.setLong(1, event.epochMs());
            ps.setString(2, event.type().name());
            ps.setString(3, event.id().toString());
            ps.setString(4, event.itemId());
            ps.setString(5, event.source() != null ? event.source().name() : null);
            ps.setString(6, event.reason() != null ? event.reason().name() : null);
            ps.setString(7, event.related().isEmpty()
                ? null
                : event.related().stream().map(UUID::toString).collect(Collectors.joining(",")));
            ps.setString(8, event.holder());
            ps.setString(9, event.detail());
            ps.executeUpdate();
        } catch (final SQLException ex) {
            this.failed = true;
            ProvenanceWriter.reportStorageError("audit insert", ex);
        }
    }

    public synchronized @NotNull List<ProvenanceEvent> loadRecentAudit(final int limit) {
        if (this.failed) {
            return List.of();
        }
        final List<ProvenanceEvent> out = new ArrayList<>();
        try (PreparedStatement ps = this.connection.prepareStatement(
            "SELECT epoch, kind, id, item, source, reason, related, holder, detail FROM audit ORDER BY seq DESC LIMIT ?"
        )) {
            ps.setInt(1, Math.max(1, Math.min(limit, 10_000)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(toEvent(rs));
                }
            }
        } catch (final SQLException | IllegalArgumentException ex) {
            this.failed = true;
            ProvenanceWriter.reportStorageError("audit load", ex);
        }
        return out;
    }

    /** Row count for tests / ops (`SELECT COUNT(*) FROM lineage`). */
    public synchronized long countLineage() {
        if (this.failed) {
            return 0L;
        }
        try (PreparedStatement ps = this.connection.prepareStatement("SELECT COUNT(*) FROM lineage");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (final SQLException ex) {
            this.failed = true;
            ProvenanceWriter.reportStorageError("lineage count", ex);
            return 0L;
        }
    }

    /**
     * Run work inside a single SQLite transaction (writer-thread batching).
     * Nested calls reuse the outer transaction.
     */
    public synchronized void runInTransaction(final @NotNull Runnable work) {
        Objects.requireNonNull(work, "work");
        if (this.failed) {
            work.run();
            return;
        }
        try {
            final boolean wasAuto = this.connection.getAutoCommit();
            if (!wasAuto) {
                work.run();
                return;
            }
            this.connection.setAutoCommit(false);
            try {
                work.run();
                this.connection.commit();
            } catch (final RuntimeException ex) {
                try {
                    this.connection.rollback();
                } catch (final SQLException ignored) {
                    // already failing
                }
                throw ex;
            } finally {
                try {
                    this.connection.setAutoCommit(true);
                } catch (final SQLException ex) {
                    this.failed = true;
                    ProvenanceWriter.reportStorageError("transaction restore autocommit", ex);
                }
            }
        } catch (final SQLException ex) {
            this.failed = true;
            ProvenanceWriter.reportStorageError("transaction begin", ex);
            work.run();
        }
    }

    private static @NotNull ProvenanceEvent toEvent(final ResultSet rs) throws SQLException {
        final String relatedRaw = rs.getString("related");
        final List<UUID> related = new ArrayList<>();
        if (relatedRaw != null && !relatedRaw.isEmpty()) {
            for (final String part : relatedRaw.split(",")) {
                try {
                    related.add(UUID.fromString(part));
                } catch (final IllegalArgumentException ignored) {
                    // skip bad related id
                }
            }
        }
        final String sourceRaw = rs.getString("source");
        final String reasonRaw = rs.getString("reason");
        return new ProvenanceEvent(
            rs.getLong("epoch"),
            ProvenanceEventType.valueOf(rs.getString("kind")),
            UUID.fromString(rs.getString("id")),
            rs.getString("item"),
            sourceRaw != null ? ProvenanceSource.valueOf(sourceRaw) : null,
            reasonRaw != null ? ProvenanceReason.valueOf(reasonRaw) : null,
            List.copyOf(related),
            rs.getString("holder"),
            rs.getString("detail")
        );
    }

    private static @NotNull LineageNode toNode(final UUID id, final ResultSet rs) throws SQLException {
        final String parentsRaw = rs.getString("parents");
        final List<UUID> parents = new ArrayList<>();
        if (parentsRaw != null && !parentsRaw.isEmpty()) {
            for (final String part : parentsRaw.split(",")) {
                try {
                    parents.add(UUID.fromString(part));
                } catch (final IllegalArgumentException ignored) {
                    // skip bad parent
                }
            }
        }
        final boolean dead = rs.getInt("dead") != 0;
        final LineageNode node = new LineageNode(
            id,
            rs.getString("item"),
            ProvenanceSource.valueOf(rs.getString("source")),
            List.copyOf(parents),
            rs.getLong("born"),
            rs.getString("holder")
        );
        if (dead) {
            node.markDead(ProvenanceReason.valueOf(rs.getString("death_reason")), rs.getLong("death_epoch"));
        }
        return node;
    }

    /** Parse a stored location display string back into a {@link StackLocation}. */
    static @NotNull StackLocation parseLocationDisplay(final String raw) {
        if (raw == null) {
            return StackLocation.unknown();
        }
        if (raw.startsWith("player:") && raw.indexOf(':', 7) > 7) {
            final int sep = raw.indexOf(':', 7);
            try {
                return StackLocation.playerSlot(UUID.fromString(raw.substring(7, sep)), Integer.parseInt(raw.substring(sep + 1)));
            } catch (final IllegalArgumentException ignored) {
                // fall through
            }
        }
        if (raw.startsWith("item_entity:")) {
            try {
                return StackLocation.itemEntity(UUID.fromString(raw.substring("item_entity:".length())));
            } catch (final IllegalArgumentException ignored) {
                // fall through
            }
        }
        if (raw.equals("unknown")) {
            return StackLocation.unknown();
        }
        return StackLocation.labeled(raw);
    }

    @Override
    public synchronized void close() {
        try {
            this.connection.close();
        } catch (final SQLException ignored) {
            // already closing
        }
    }
}
