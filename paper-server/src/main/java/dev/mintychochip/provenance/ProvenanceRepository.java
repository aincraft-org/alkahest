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
 * <p>Holds the permanent lineage graph and collision trail. The live census is
 * intentionally transient: it is rebuilt by observing loaded players, entities,
 * and containers.
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
                        parseLocation(rs.getString("existing")),
                        parseLocation(rs.getString("observed")),
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

    private static @NotNull StackLocation parseLocation(final String raw) {
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
