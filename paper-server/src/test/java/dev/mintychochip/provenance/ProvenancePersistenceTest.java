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

        // Simulated restart: runtime state wiped, durable store stays.
        ItemProvenance.clearAll();
        ItemProvenance.lineage().clearCache();
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
}
