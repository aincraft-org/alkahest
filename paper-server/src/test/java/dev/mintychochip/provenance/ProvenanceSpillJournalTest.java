package dev.mintychochip.provenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
import org.bukkit.support.environment.Normal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Append-only spill journal: JSONL round-trip for critical (and audit) records.
 */
@Normal
public class ProvenanceSpillJournalTest {

    private static final UUID PLAYER = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @TempDir
    Path tempDir;

    @Test
    public void appendAndReplayRoundTrip() throws Exception {
        final Path path = tempDir.resolve("provenance-spill.log");
        final ProvenanceSpillJournal journal = new ProvenanceSpillJournal(path);
        final UUID id = UUID.randomUUID();
        final LineageNode node = new LineageNode(
            id, "minecraft:stone", ProvenanceSource.BLOCK_DROP, List.of(), 100L, "hand"
        );
        journal.appendLineage(node);
        journal.appendLive(new LiveRecord(id, "minecraft:stone", "player:x:0", 1, 100L, false));

        final List<ProvenanceSpillJournal.SpillRecord> records = journal.readAll();
        assertEquals(2, records.size());
        assertTrue(records.get(0) instanceof ProvenanceSpillJournal.SpillRecord.Lineage);
        assertTrue(records.get(1) instanceof ProvenanceSpillJournal.SpillRecord.Live);

        final ProvenanceSpillJournal.SpillRecord.Lineage lineage =
            (ProvenanceSpillJournal.SpillRecord.Lineage) records.get(0);
        assertEquals(id, lineage.node().id());
        assertEquals("minecraft:stone", lineage.node().itemId());
        assertEquals(ProvenanceSource.BLOCK_DROP, lineage.node().source());
        assertEquals(100L, lineage.node().bornEpochMs());
        assertEquals("hand", lineage.node().bornHolder());

        final ProvenanceSpillJournal.SpillRecord.Live live =
            (ProvenanceSpillJournal.SpillRecord.Live) records.get(1);
        assertEquals(id, live.record().id());
        assertEquals("player:x:0", live.record().locationDisplay());
        assertEquals(1, live.record().count());
        assertEquals(false, live.record().dead());

        journal.truncate();
        assertTrue(Files.notExists(path) || Files.size(path) == 0);
        assertEquals(0L, journal.sizeBytes());
    }

    @Test
    public void appendCollisionAndAuditRoundTrip() throws Exception {
        final Path path = tempDir.resolve("provenance-spill.log");
        final ProvenanceSpillJournal journal = new ProvenanceSpillJournal(path);
        final UUID id = UUID.randomUUID();
        final UUID parent = UUID.randomUUID();
        final StackLocation existing = StackLocation.playerSlot(PLAYER, 0);
        final StackLocation observed = StackLocation.playerSlot(PLAYER, 1);

        journal.appendCollision(new CollisionRecord(
            id, ProvenanceCollisionKind.DUPLICATE_LOCATION, existing, observed, 200L
        ));
        journal.appendAudit(new ProvenanceEvent(
            200L,
            ProvenanceEventType.COLLISION,
            id,
            "minecraft:diamond",
            ProvenanceSource.LOOT,
            null,
            List.of(parent),
            existing.display(),
            "dupe"
        ));

        final List<ProvenanceSpillJournal.SpillRecord> records = journal.readAll();
        assertEquals(2, records.size());

        final ProvenanceSpillJournal.SpillRecord.Collision collision =
            assertInstanceOf(ProvenanceSpillJournal.SpillRecord.Collision.class, records.get(0));
        assertEquals(id, collision.record().id());
        assertEquals(ProvenanceCollisionKind.DUPLICATE_LOCATION, collision.record().kind());
        assertEquals(existing, collision.record().existingLocation());
        assertEquals(observed, collision.record().observedLocation());
        assertEquals(200L, collision.record().epochMs());

        final ProvenanceSpillJournal.SpillRecord.Audit audit =
            assertInstanceOf(ProvenanceSpillJournal.SpillRecord.Audit.class, records.get(1));
        assertEquals(ProvenanceEventType.COLLISION, audit.event().type());
        assertEquals(id, audit.event().id());
        assertEquals("minecraft:diamond", audit.event().itemId());
        assertEquals(ProvenanceSource.LOOT, audit.event().source());
        assertEquals(List.of(parent), audit.event().related());
        assertEquals(existing.display(), audit.event().holder());
        assertEquals("dupe", audit.event().detail());
    }

    @Test
    public void deadLineageRoundTrips() throws Exception {
        final Path path = tempDir.resolve("provenance-spill.log");
        final ProvenanceSpillJournal journal = new ProvenanceSpillJournal(path);
        final UUID id = UUID.randomUUID();
        final LineageNode node = new LineageNode(
            id, "minecraft:cobblestone", ProvenanceSource.CRAFT, List.of(UUID.randomUUID()), 50L, "crafter"
        );
        node.markDead(ProvenanceReason.CONSUMED, 75L);
        journal.appendLineage(node);

        final List<ProvenanceSpillJournal.SpillRecord> records = journal.readAll();
        assertEquals(1, records.size());
        final LineageNode loaded =
            ((ProvenanceSpillJournal.SpillRecord.Lineage) records.get(0)).node();
        assertTrue(loaded.dead());
        assertEquals(ProvenanceReason.CONSUMED, loaded.deathReason());
        assertEquals(75L, loaded.deathEpochMs());
        assertEquals(1, loaded.parents().size());
    }

    @Test
    public void readAllEmptyWhenMissing() throws Exception {
        final Path path = tempDir.resolve("missing-spill.log");
        final ProvenanceSpillJournal journal = new ProvenanceSpillJournal(path);
        assertTrue(journal.readAll().isEmpty());
        assertEquals(0L, journal.sizeBytes());
    }

    @Test
    public void readAllSkipsTruncatedTrailingLine() throws Exception {
        final Path path = tempDir.resolve("provenance-spill.log");
        final ProvenanceSpillJournal journal = new ProvenanceSpillJournal(path);
        final UUID id = UUID.randomUUID();
        final LineageNode node = new LineageNode(
            id, "minecraft:stone", ProvenanceSource.BLOCK_DROP, List.of(), 100L, null
        );
        journal.appendLineage(node);

        // Simulate crash mid-write: incomplete JSON on the last line.
        Files.writeString(
            path,
            "{\"k\":\"live\",\"id\":\"" + UUID.randomUUID() + "\",\"item\":\"minecraft:dirt",
            StandardCharsets.UTF_8,
            StandardOpenOption.APPEND
        );

        final List<ProvenanceSpillJournal.SpillRecord> records = journal.readAll();
        assertEquals(1, records.size());
        final ProvenanceSpillJournal.SpillRecord.Lineage lineage =
            assertInstanceOf(ProvenanceSpillJournal.SpillRecord.Lineage.class, records.get(0));
        assertEquals(id, lineage.node().id());
    }

    @Test
    public void readAllThrowsOnCorruptMiddleLine() throws Exception {
        final Path path = tempDir.resolve("provenance-spill.log");
        final ProvenanceSpillJournal journal = new ProvenanceSpillJournal(path);
        final UUID first = UUID.randomUUID();
        final UUID last = UUID.randomUUID();
        journal.appendLineage(new LineageNode(
            first, "minecraft:stone", ProvenanceSource.BLOCK_DROP, List.of(), 100L, null
        ));
        Files.writeString(
            path,
            "not-json-at-all\n",
            StandardCharsets.UTF_8,
            StandardOpenOption.APPEND
        );
        journal.appendLineage(new LineageNode(
            last, "minecraft:dirt", ProvenanceSource.BLOCK_DROP, List.of(), 200L, null
        ));

        assertThrows(IOException.class, journal::readAll);
    }

    @Test
    public void deadLineageMissingReasonStaysDeadWithDestroyed() throws Exception {
        final Path path = tempDir.resolve("provenance-spill.log");
        // Hand-written dead record without death_reason must not revive as alive.
        final UUID id = UUID.randomUUID();
        Files.writeString(
            path,
            "{\"k\":\"lineage\",\"id\":\"" + id + "\",\"item\":\"minecraft:apple\","
                + "\"source\":\"CRAFT\",\"parents\":[],\"born\":10,\"dead\":true,\"death_epoch\":20}\n",
            StandardCharsets.UTF_8
        );

        final ProvenanceSpillJournal journal = new ProvenanceSpillJournal(path);
        final List<ProvenanceSpillJournal.SpillRecord> records = journal.readAll();
        assertEquals(1, records.size());
        final LineageNode loaded =
            ((ProvenanceSpillJournal.SpillRecord.Lineage) records.get(0)).node();
        assertTrue(loaded.dead());
        assertEquals(ProvenanceReason.DESTROYED, loaded.deathReason());
        assertEquals(20L, loaded.deathEpochMs());
    }

    @Test
    public void seizePendingLeavesReplayUntilAck() throws Exception {
        final Path path = tempDir.resolve("provenance-spill.log");
        final Path replay = path.resolveSibling(path.getFileName().toString() + ".replay");
        final ProvenanceSpillJournal journal = new ProvenanceSpillJournal(path);
        final UUID id = UUID.randomUUID();
        journal.appendLineage(new LineageNode(
            id, "minecraft:stone", ProvenanceSource.BLOCK_DROP, List.of(), 100L, null
        ));

        final List<ProvenanceSpillJournal.SpillRecord> seized = journal.seizePending();
        assertEquals(1, seized.size());
        assertTrue(Files.isRegularFile(replay), "seize must leave .replay until ack");
        assertTrue(Files.notExists(path) || !Files.isRegularFile(path));

        // Concurrent appends open a new active spill while .replay is outstanding.
        final UUID later = UUID.randomUUID();
        journal.appendLineage(new LineageNode(
            later, "minecraft:dirt", ProvenanceSource.BLOCK_DROP, List.of(), 200L, null
        ));
        assertTrue(Files.isRegularFile(path));

        // Existing .replay is preferred; must not delete it or steal the new spill yet.
        final List<ProvenanceSpillJournal.SpillRecord> again = journal.seizePending();
        assertEquals(1, again.size());
        assertEquals(id, ((ProvenanceSpillJournal.SpillRecord.Lineage) again.get(0)).node().id());
        assertTrue(Files.isRegularFile(replay));

        journal.ackSeized();
        assertTrue(Files.notExists(replay));

        // After ack, the spill written during replay is available.
        final List<ProvenanceSpillJournal.SpillRecord> next = journal.seizePending();
        assertEquals(1, next.size());
        assertEquals(later, ((ProvenanceSpillJournal.SpillRecord.Lineage) next.get(0)).node().id());
        journal.ackSeized();
        assertTrue(Files.notExists(replay));
    }

    @Test
    public void seizePendingPrefersExistingReplayWithoutDeleting() throws Exception {
        final Path path = tempDir.resolve("provenance-spill.log");
        final Path replay = path.resolveSibling(path.getFileName().toString() + ".replay");
        final UUID replayId = UUID.randomUUID();
        final UUID spillId = UUID.randomUUID();

        final ProvenanceSpillJournal journal = new ProvenanceSpillJournal(path);
        journal.appendLineage(new LineageNode(
            replayId, "minecraft:stone", ProvenanceSource.BLOCK_DROP, List.of(), 1L, null
        ));
        Files.move(path, replay);
        journal.appendLineage(new LineageNode(
            spillId, "minecraft:dirt", ProvenanceSource.BLOCK_DROP, List.of(), 2L, null
        ));

        final List<ProvenanceSpillJournal.SpillRecord> seized = journal.seizePending();
        assertEquals(1, seized.size());
        assertEquals(replayId, ((ProvenanceSpillJournal.SpillRecord.Lineage) seized.get(0)).node().id());
        assertTrue(Files.isRegularFile(replay), "must not wipe pre-existing .replay");
        assertTrue(Files.isRegularFile(path), "active spill must wait until .replay is acked");
    }
}
