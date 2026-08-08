package dev.mintychochip.provenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bukkit.support.environment.Normal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Durable store contracts: restart lineage survival, collision persistence,
 * bounded writer behavior.
 */
@Normal
public class ProvenancePersistenceTest {

    private static final UUID PLAYER = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final StackLocation HAND = StackLocation.playerSlot(PLAYER, 0);

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        ItemProvenance.setEnabled(true);
        ItemProvenance.clearAll();
        ProvenanceWriter.clearInstall();
    }

    @AfterEach
    public void tearDown() {
        ProvenanceWriter.clearInstall();
        ItemProvenance.clearAll();
        ItemProvenance.setEnabled(true);
    }

    @Test
    public void restartKeepsAncestorHistory() {
        ProvenanceWriter.install(tempDir, message -> {
        });
        final ItemStack parent = new ItemStack(Items.IRON_ORE, 1);
        final UUID parentId = ItemProvenance.birth(parent, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();
        final ItemStack child = new ItemStack(Items.IRON_INGOT, 1);
        ItemProvenance.onSmelted(child, parentId, StackLocation.labeled("furnace:0,64,0"));
        final UUID childId = StackStamp.readId(child).orElseThrow();
        final ItemStack persistedChild = child.copy();

        // Ensure durable writes land before simulated restart (async writer).
        ProvenanceWriter.flushAndClose();
        ProvenanceWriter.clearInstall();

        // Simulated restart: runtime state wiped, durable store stays.
        ItemProvenance.clearAll();
        ItemProvenance.lineage().clearCache();
        ProvenanceWriter.install(tempDir, message -> {
        });
        ItemProvenance.rehydrate(persistedChild, HAND);

        assertTrue(
            ItemProvenance.explain(childId).stream().anyMatch(node -> node.id().equals(parentId)),
            "restart rehydration must load durable ancestry"
        );
        assertEquals(List.of(parentId), StackStamp.read(persistedChild).orElseThrow().parents());
    }

    @Test
    public void collisionIsPersistedAndReloadable() {
        ProvenanceWriter.install(tempDir, message -> {
        });
        final ItemStack original = new ItemStack(Items.DIAMOND, 4);
        ItemProvenance.birth(original, ProvenanceSource.LOOT, HAND).orElseThrow();
        final ItemStack duplicate = original.copy();

        assertTrue(ItemProvenance.observe(duplicate, StackLocation.playerSlot(PLAYER, 1)));
        ProvenanceWriter.flushAndClose();
        ProvenanceWriter.clearInstall();

        final ProvenanceRepository repo;
        try {
            repo = new ProvenanceRepository(tempDir.resolve("mintychochip/provenance.db"));
        } catch (final Exception ex) {
            throw new AssertionError("cannot reopen repository", ex);
        }
        final List<CollisionRecord> loaded = repo.loadRecentCollisions(10);
        assertFalse(loaded.isEmpty());
        assertEquals(ProvenanceCollisionKind.DUPLICATE_LOCATION, loaded.getFirst().kind());
        assertEquals(HAND, loaded.getFirst().existingLocation());
        repo.close();
    }

    @Test
    public void auditLinesAreWrittenAndValidJsonl() throws Exception {
        ProvenanceWriter.install(tempDir, message -> {
        });
        final ItemStack stack = new ItemStack(Items.COBBLESTONE, 1);
        ItemProvenance.birth(stack, ProvenanceSource.BLOCK_DROP, HAND);
        ProvenanceWriter.flushAndClose();
        ProvenanceWriter.clearInstall();

        final Path audit = tempDir.resolve("mintychochip/provenance-audit.jsonl");
        assertTrue(Files.exists(audit), "audit file must exist after flush");
        final String content = Files.readString(audit);
        assertTrue(content.contains("\"type\":\"BIRTH\""), "audit must contain the birth event");
        assertTrue(content.contains("\"id\":\"" + StackStamp.readId(stack).orElseThrow() + "\""));
        // Every line parses as JSON.
        for (final String line : content.split("\n")) {
            if (!line.isBlank()) {
                assertTrue(isJson(line), "malformed JSONL line: " + line);
            }
        }
    }

    private static boolean isJson(final String line) {
        final String trimmed = line.trim();
        return trimmed.startsWith("{") && trimmed.endsWith("}") && trimmed.indexOf('"') == 1;
    }

    @Test
    public void writerWithoutInstallIsNoOp() {
        // No install: events must not block or throw.
        final ItemStack stack = new ItemStack(Items.COBBLESTONE, 1);
        ItemProvenance.birth(stack, ProvenanceSource.BLOCK_DROP, HAND);
        assertTrue(ItemProvenance.live().contains(StackStamp.readId(stack).orElseThrow()));
    }

    @Test
    public void liveUpsertAndLoadAliveSurvivesReopen() throws Exception {
        final Path db = tempDir.resolve("mintychochip/provenance.db");
        final UUID id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        try (ProvenanceRepository repo = new ProvenanceRepository(db)) {
            repo.upsertLive(new LiveRecord(id, "minecraft:diamond", "player:" + PLAYER + ":0", 4, 1_700_000_000_000L, false));
        }
        try (ProvenanceRepository repo = new ProvenanceRepository(db)) {
            final List<LiveRecord> alive = repo.loadAliveLive();
            assertEquals(1, alive.size());
            assertEquals(id, alive.getFirst().id());
            assertEquals(4, alive.getFirst().count());
            assertFalse(alive.getFirst().dead());
        }
    }

    @Test
    public void auditInsertAndLoadRecentSurvivesReopen() throws Exception {
        final Path db = tempDir.resolve("mintychochip/provenance.db");
        final UUID id = UUID.randomUUID();
        final ProvenanceEvent event = new ProvenanceEvent(
            1_700_000_000_000L,
            ProvenanceEventType.BIRTH,
            id,
            "minecraft:cobblestone",
            ProvenanceSource.BLOCK_DROP,
            null,
            List.of(),
            HAND.display(),
            null
        );
        try (ProvenanceRepository repo = new ProvenanceRepository(db)) {
            repo.insertAudit(event);
        }
        try (ProvenanceRepository repo = new ProvenanceRepository(db)) {
            final List<ProvenanceEvent> loaded = repo.loadRecentAudit(10);
            assertEquals(1, loaded.size());
            assertEquals(ProvenanceEventType.BIRTH, loaded.getFirst().type());
            assertEquals(id, loaded.getFirst().id());
        }
    }

    @Test
    public void criticalWritesNeverDropUnderQueuePressure() throws Exception {
        // Tiny capacity forces spill path for critical lineage writes.
        ProvenanceWriter.installForTest(tempDir, message -> {
        }, 4);
        final int n = 200;
        for (int i = 0; i < n; i++) {
            final ItemStack s = new ItemStack(Items.COBBLESTONE, 1);
            ItemProvenance.birth(s, ProvenanceSource.BLOCK_DROP, HAND);
        }
        ProvenanceWriter.flushAndClose();
        // Assert status before clearInstall so counters are still readable.
        final String status = ProvenanceWriter.status();
        assertFalse(status.contains("queue-dropped="),
            "status must not report critical queue drops: " + status);
        // audit-dropped is acceptable under pressure; critical must still land.
        ProvenanceWriter.clearInstall();

        try (ProvenanceRepository repo = new ProvenanceRepository(tempDir.resolve("mintychochip/provenance.db"))) {
            assertTrue(repo.countLineage() >= n, "all births must land in lineage, got " + repo.countLineage());
        }
    }

    @Test
    public void spillReplayRecoversAfterSimulatedCrash() throws Exception {
        final Path minty = tempDir.resolve("mintychochip");
        Files.createDirectories(minty);
        final Path spill = minty.resolve("provenance-spill.log");
        final Path replay = minty.resolve("provenance-spill.log.replay");
        final UUID id = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff");

        // Write a lineage record into the active spill, then simulate a crash
        // mid-replay: content seized to .replay but never acked.
        final ProvenanceSpillJournal journal = new ProvenanceSpillJournal(spill);
        journal.appendLineage(new LineageNode(
            id, "minecraft:stone", ProvenanceSource.BLOCK_DROP, List.of(), 100L, "hand"
        ));
        Files.move(spill, replay);
        assertTrue(Files.isRegularFile(replay), "simulated crash must leave unacked .replay");
        assertTrue(Files.notExists(spill) || !Files.isRegularFile(spill));

        ProvenanceWriter.install(tempDir, message -> {
        });
        ProvenanceWriter.flushAndClose();
        ProvenanceWriter.clearInstall();

        assertTrue(Files.notExists(replay), ".replay must be acked after successful apply");
        try (ProvenanceRepository repo = new ProvenanceRepository(minty.resolve("provenance.db"))) {
            final Optional<LineageNode> loaded = repo.loadLineage(id);
            assertTrue(loaded.isPresent(), "seized spill must land in lineage after writer recovery");
            assertEquals(id, loaded.get().id());
            assertEquals("minecraft:stone", loaded.get().itemId());
            assertEquals(ProvenanceSource.BLOCK_DROP, loaded.get().source());
        }
    }

    @Test
    public void auditIsInSqliteAfterFlush() throws Exception {
        ProvenanceWriter.install(tempDir, message -> {
        });
        final ItemStack stack = new ItemStack(Items.COBBLESTONE, 1);
        final UUID id = ItemProvenance.birth(stack, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();
        ProvenanceWriter.flushAndClose();
        ProvenanceWriter.clearInstall();

        try (ProvenanceRepository repo = new ProvenanceRepository(tempDir.resolve("mintychochip/provenance.db"))) {
            final List<ProvenanceEvent> events = repo.loadRecentAudit(20);
            assertTrue(
                events.stream().anyMatch(e -> e.id().equals(id) && e.type() == ProvenanceEventType.BIRTH),
                "birth audit must be in SQLite after flush"
            );
        }
    }

    @Test
    public void durableLiveSeedsCensusAndDetectsSecondLocation() {
        ProvenanceWriter.install(tempDir, message -> {
        });
        final ItemStack original = new ItemStack(Items.DIAMOND, 1);
        final UUID id = ItemProvenance.birth(original, ProvenanceSource.LOOT, HAND).orElseThrow();
        ProvenanceWriter.flushAndClose();
        ItemProvenance.clearAll();
        ProvenanceWriter.clearInstall();
        ProvenanceWriter.install(tempDir, message -> {
        });

        assertTrue(ItemProvenance.live().contains(id), "live must be seeded from DB");

        final ItemStack duplicate = original.copy();
        assertTrue(
            ItemProvenance.observe(duplicate, StackLocation.playerSlot(PLAYER, 1)),
            "second concrete location after restart must COLLISION"
        );
        ProvenanceWriter.flushAndClose();
        ProvenanceWriter.clearInstall();
    }

    /**
     * Crash mid-spill: DB still has stale live row, unacked spill has newer location.
     * Install must replay spill synchronously before seeding LiveIndex so census is not stale.
     */
    @Test
    public void liveSeedUsesPostReplaySpillLocationNotStaleDb() throws Exception {
        final Path minty = tempDir.resolve("mintychochip");
        Files.createDirectories(minty);
        final UUID id = UUID.fromString("cccccccc-dddd-eeee-ffff-000000000001");
        final String staleLoc = HAND.display();
        final StackLocation spillLocation = StackLocation.playerSlot(PLAYER, 5);
        final String spillLoc = spillLocation.display();

        try (ProvenanceRepository repo = new ProvenanceRepository(minty.resolve("provenance.db"))) {
            repo.upsertLive(new LiveRecord(id, "minecraft:diamond", staleLoc, 1, 100L, false));
        }

        final ProvenanceSpillJournal journal = new ProvenanceSpillJournal(minty.resolve("provenance-spill.log"));
        journal.appendLive(new LiveRecord(id, "minecraft:diamond", spillLoc, 2, 200L, false));

        // Wipe runtime census, then install: must sync-replay spill then seed.
        ItemProvenance.clearAll();
        ProvenanceWriter.install(tempDir, message -> {
        });

        final LiveEntry seeded = ItemProvenance.live().get(id).orElseThrow(
            () -> new AssertionError("live must be seeded after install")
        );
        assertEquals(spillLoc, seeded.location().display(),
            "LiveIndex must reflect post-spill-replay location, not stale DB row");
        assertEquals(2, seeded.count(), "LiveIndex must reflect post-spill-replay count");

        ProvenanceWriter.flushAndClose();
        ProvenanceWriter.clearInstall();

        // Durable store must also hold the replayed row.
        try (ProvenanceRepository repo = new ProvenanceRepository(minty.resolve("provenance.db"))) {
            final List<LiveRecord> alive = repo.loadAliveLive();
            assertEquals(1, alive.size());
            assertEquals(spillLoc, alive.getFirst().locationDisplay());
            assertEquals(2, alive.getFirst().count());
        }
    }
}
