# Provenance Durable Store Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make full lineage, last-seen live census, collisions, and audit strongly durable via deferred SQLite writes, a never-drop critical queue, and a spill journal — without any main-thread disk I/O.

**Architecture:** Extend existing `ProvenanceRepository` (SQLite WAL) with `live` and `audit` tables. `ProvenanceWriter` remains the only disk touchpoint: bounded memory queue, spill-to-file when full for critical events, background batch commit. On install, replay spill and seed `LiveIndex` from `live`. Game code only enqueues.

**Tech Stack:** Java 25, JDBC SQLite, JUnit 5 `@Normal` Paper test suite, existing `dev.mintychochip.provenance` package.

**Spec:** `docs/superpowers/specs/2026-08-08-provenance-durable-store-design.md`

## Global Constraints

- No JDBC / fsync / file flush on the server (game) thread except spill **append** when the memory queue is full (must stay non-blocking and fast).
- Critical events (lineage, live, collision) must never be dropped; audit may drop only if spill also fails.
- Do not change provenance identity rules (split/craft/claim/collision semantics).
- Owned code under `paper-server/src/main/java/dev/mintychochip/provenance` (+ tests). No vanilla patches for this work.
- Preserve unrelated worktree content (`bench/`, `serve/`, heap dumps, inventory patch WIP for craft/merge repair).
- TDD: write failing tests first; commit after each task green.

## File Structure

| File | Responsibility |
|------|----------------|
| `paper-server/.../provenance/ProvenanceRepository.java` | Schema + upsert/load for `lineage`, `live`, `collisions`, `audit` |
| `paper-server/.../provenance/ProvenanceSpillJournal.java` | **New.** Append-only spill file + replay iterator + ack/truncate |
| `paper-server/.../provenance/ProvenanceWriter.java` | Never-drop enqueue, spill, batch process Live/Audit→DB, seed live, status |
| `paper-server/.../provenance/LiveIndex.java` | Optional: bulk seed helper; keep put/remove as-is |
| `paper-server/.../provenance/ItemProvenance.java` | Enqueue live upsert on birth/transfer/observe/death census changes |
| `paper-server/.../provenance/ProvenanceBukkitCommand.java` | Prefer DB-backed recent audit when store installed |
| `paper-server/.../test/.../ProvenancePersistenceTest.java` | Durability / spill / live / audit permanent tests |
| `docs/superpowers/specs/2026-08-07-item-provenance-design.md` | One-line pointer to durable-store spec (optional small doc touch) |

**Do not create:** separate audit file sink class; keep JSONL as optional mirror inside `ProvenanceWriter` (existing code path can remain for human tailing).

### Shared types used in tasks

```java
// Live row for DB (package-private record in ProvenanceRepository or top-level package type)
public record LiveRecord(
    UUID id,
    String itemId,
    String locationDisplay, // StackLocation.display()
    int count,
    long epochMs,
    boolean dead
) {}
```

Spill frame version 1 (UTF-8 JSON lines, one record per line, for simple tests):

```text
{"v":1,"k":"lineage","payload":{...}}
{"v":1,"k":"live","payload":{...}}
{"v":1,"k":"collision","payload":{...}}
{"v":1,"k":"audit","payload":{...}}
```

Alternatively length-prefixed binary is fine if tests cover round-trip; prefer **JSONL spill** for debuggability unless performance tests force binary.

---

### Task 1: Repository — `live` + `audit` tables

**Files:**
- Modify: `paper-server/src/main/java/dev/mintychochip/provenance/ProvenanceRepository.java`
- Test: `paper-server/src/test/java/dev/mintychochip/provenance/ProvenancePersistenceTest.java`

- [ ] **Step 1: Write failing repository-level tests**

Add to `ProvenancePersistenceTest` (or a focused `ProvenanceRepositoryTest` in the same package if preferred — same module is fine):

```java
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
```

Note: `LiveRecord` may live as a public record in `ProvenanceRepository` file or `package-private` top-level — pick one name and use it consistently.

- [ ] **Step 2: Run tests — expect fail**

```bash
./gradlew :paper-server:test --tests 'dev.mintychochip.provenance.ProvenancePersistenceTest.liveUpsertAndLoadAliveSurvivesReopen' --tests 'dev.mintychochip.provenance.ProvenancePersistenceTest.auditInsertAndLoadRecentSurvivesReopen'
```

Expected: compile error or missing methods.

- [ ] **Step 3: Implement schema + methods in `ProvenanceRepository`**

In constructor DDL (after existing tables):

```sql
CREATE TABLE IF NOT EXISTS live (
    id TEXT PRIMARY KEY,
    item TEXT NOT NULL,
    location TEXT NOT NULL,
    count INTEGER NOT NULL,
    epoch INTEGER NOT NULL,
    dead INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS audit (
    seq INTEGER PRIMARY KEY AUTOINCREMENT,
    epoch INTEGER NOT NULL,
    kind TEXT NOT NULL,
    payload TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_audit_epoch ON audit(epoch);
```

Add methods (all `synchronized`, same failure style as lineage):

```java
public synchronized void upsertLive(@NotNull LiveRecord record) { /* INSERT ON CONFLICT DO UPDATE */ }

public synchronized @NotNull List<LiveRecord> loadAliveLive() {
    // SELECT ... FROM live WHERE dead = 0
}

public synchronized void insertAudit(@NotNull ProvenanceEvent event) {
    // store kind = event.type().name(), payload = same JSON as ProvenanceWriter.toJsonLine
    // Prefer package-visible helper: ProvenanceWriter.auditJson(event) extracted public/package
}

public synchronized @NotNull List<ProvenanceEvent> loadRecentAudit(int limit) {
    // ORDER BY seq DESC LIMIT n, reverse to chronological or keep newest-first consistently with AuditLog.latest
}
```

For `loadRecentAudit`, either parse payload JSON back into `ProvenanceEvent` (minimal parser for fields `t,type,id,item,source,reason,related,location,detail`) or store columns denormalized. **Recommended for Task 1:** denormalize columns on `audit` to avoid a full JSON parser:

```sql
CREATE TABLE IF NOT EXISTS audit (
    seq INTEGER PRIMARY KEY AUTOINCREMENT,
    epoch INTEGER NOT NULL,
    kind TEXT NOT NULL,
    id TEXT NOT NULL,
    item TEXT,
    source TEXT,
    reason TEXT,
    related TEXT,   -- comma UUIDs
    holder TEXT,
    detail TEXT
);
```

Then map rows → `ProvenanceEvent` without JSON parse. Keep JSONL mirror using existing `toJsonLine` separately.

Update class javadoc: live census is **durable** and seeds runtime on restart.

- [ ] **Step 4: Run tests — expect pass**

```bash
./gradlew :paper-server:test --tests 'dev.mintychochip.provenance.ProvenancePersistenceTest'
```

Expected: PASS (including existing restart/collision/jsonl tests).

- [ ] **Step 5: Commit**

```bash
git add paper-server/src/main/java/dev/mintychochip/provenance/ProvenanceRepository.java \
        paper-server/src/test/java/dev/mintychochip/provenance/ProvenancePersistenceTest.java
git commit -m "feat(provenance): durable live and audit tables in SQLite"
```

---

### Task 2: Spill journal

**Files:**
- Create: `paper-server/src/main/java/dev/mintychochip/provenance/ProvenanceSpillJournal.java`
- Test: `paper-server/src/test/java/dev/mintychochip/provenance/ProvenanceSpillJournalTest.java`

- [ ] **Step 1: Write failing spill round-trip test**

```java
package dev.mintychochip.provenance;

@Normal
public class ProvenanceSpillJournalTest {
    @TempDir Path tempDir;

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

        journal.truncate();
        assertTrue(Files.notExists(path) || Files.size(path) == 0);
    }
}
```

- [ ] **Step 2: Run test — expect fail (class missing)**

```bash
./gradlew :paper-server:test --tests 'dev.mintychochip.provenance.ProvenanceSpillJournalTest'
```

- [ ] **Step 3: Implement `ProvenanceSpillJournal`**

```java
public final class ProvenanceSpillJournal {
    public sealed interface SpillRecord {
        record Lineage(LineageNode node) implements SpillRecord {}
        record Live(LiveRecord record) implements SpillRecord {}
        record Collision(CollisionRecord record) implements SpillRecord {}
        record Audit(ProvenanceEvent event) implements SpillRecord {}
    }

    public ProvenanceSpillJournal(@NotNull Path path) { ... }

    /** Append one record; must be safe to call from game thread (append + force optional). Prefer FileChannel.write with StandardOpenOption.APPEND CREATE. */
    public synchronized void appendLineage(LineageNode node) throws IOException { ... }
    public synchronized void appendLive(LiveRecord record) throws IOException { ... }
    public synchronized void appendCollision(CollisionRecord record) throws IOException { ... }
    public synchronized void appendAudit(ProvenanceEvent event) throws IOException { ... }

    public synchronized @NotNull List<SpillRecord> readAll() throws IOException { ... }
    public synchronized void truncate() throws IOException { ... }
    public long sizeBytes() { ... }
}
```

Encoding: one JSON object per line with `k` discriminator and enough fields to rebuild the record. Keep the serializer private and tested via round-trip only.

- [ ] **Step 4: Run test — expect pass**

```bash
./gradlew :paper-server:test --tests 'dev.mintychochip.provenance.ProvenanceSpillJournalTest'
```

- [ ] **Step 5: Commit**

```bash
git add paper-server/src/main/java/dev/mintychochip/provenance/ProvenanceSpillJournal.java \
        paper-server/src/test/java/dev/mintychochip/provenance/ProvenanceSpillJournalTest.java
git commit -m "feat(provenance): spill journal for never-drop critical writes"
```

---

### Task 3: Writer — never-drop critical path + live/audit DB apply

**Files:**
- Modify: `paper-server/src/main/java/dev/mintychochip/provenance/ProvenanceWriter.java`
- Modify: `paper-server/src/test/java/dev/mintychochip/provenance/ProvenancePersistenceTest.java`

- [ ] **Step 1: Write failing tests for never-drop and DB audit**

```java
@Test
public void criticalWritesNeverDropUnderQueuePressure() throws Exception {
    ProvenanceWriter.install(tempDir, message -> {});
    // Flood with lineage-producing births faster than drain if possible;
    // practical approach: use a tiny test capacity OR call internal spill path.
    // Preferred public contract:
    final int n = 2_000;
    for (int i = 0; i < n; i++) {
        final ItemStack s = new ItemStack(Items.COBBLESTONE, 1);
        ItemProvenance.birth(s, ProvenanceSource.BLOCK_DROP, HAND);
    }
    ProvenanceWriter.flushAndClose();
    ProvenanceWriter.clearInstall();

    assertFalse(ProvenanceWriter.status().contains("critical-dropped=")); // or parse metrics
    // Reopen DB and count lineage rows >= n (or sample that last ids exist)
    try (ProvenanceRepository repo = new ProvenanceRepository(tempDir.resolve("mintychochip/provenance.db"))) {
        // Add repo.countLineage() helper if needed
        assertTrue(repo.countLineage() >= n, "all births must land in lineage");
    }
}

@Test
public void auditIsInSqliteAfterFlush() throws Exception {
    ProvenanceWriter.install(tempDir, message -> {});
    final ItemStack stack = new ItemStack(Items.COBBLESTONE, 1);
    final UUID id = ItemProvenance.birth(stack, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();
    ProvenanceWriter.flushAndClose();
    ProvenanceWriter.clearInstall();

    try (ProvenanceRepository repo = new ProvenanceRepository(tempDir.resolve("mintychochip/provenance.db"))) {
        final List<ProvenanceEvent> events = repo.loadRecentAudit(20);
        assertTrue(events.stream().anyMatch(e -> e.id().equals(id) && e.type() == ProvenanceEventType.BIRTH));
    }
}
```

If flooding cannot fill an 8k queue in-process before drain, add package-visible test hook:

```java
// ProvenanceWriter — test only
static void installForTest(Path worldFolder, Consumer<String> logger, int queueCapacity) { ... }
```

Use `queueCapacity = 4` for the flood test so spill path is exercised.

- [ ] **Step 2: Run tests — expect fail**

```bash
./gradlew :paper-server:test --tests 'dev.mintychochip.provenance.ProvenancePersistenceTest.criticalWritesNeverDropUnderQueuePressure' --tests 'dev.mintychochip.provenance.ProvenancePersistenceTest.auditIsInSqliteAfterFlush'
```

- [ ] **Step 3: Implement writer changes**

1. Add `WriteItem.Live(LiveRecord record)`.
2. `enqueueLive(LiveRecord)` public static.
3. Replace `offer` drop behavior:

```java
private void offerCritical(WriteItem item) {
    if (!running.get()) {
        spillOrCount(item); // still try spill during shutdown race
        return;
    }
    if (queue.offer(item)) {
        return;
    }
    spillCritical(item); // never increment dropped for critical
}

private void offerAudit(WriteItem.Audit item) {
    if (queue.offer(item)) {
        return;
    }
    try {
        spillJournal.appendAudit(item.event());
    } catch (IOException ex) {
        dropped.incrementAndGet(); // audit only
        recordError("audit spill failed: " + ex.getMessage());
    }
}
```

4. On construction: create `ProvenanceSpillJournal` at `dir.resolve("provenance-spill.log")`.
5. Before starting drain thread (or first thing in drain): `replaySpill()` → apply each record to repository → `truncate()`.
6. `process`:
   - Lineage → `repo.upsertLineage`
   - Live → `repo.upsertLive`
   - Collision → `repo.insertCollision`
   - Audit → `repo.insertAudit` **and** existing JSONL `appendAudit` (mirror OK)
7. Batching (optional in this task, required if easy): collect up to 64 items or 50ms, wrap in `connection.setAutoCommit(false)` / commit. If auto-commit left on, still OK for first cut **if** spill never-drop works; prefer single `begin/commit` helper on repository:

```java
// ProvenanceRepository
public synchronized void runInTransaction(Runnable work) { ... }
```

8. Extend `status()`:

```text
queue-depth=N spill-bytes=B written=W audit-dropped=D store=sqlite|in-memory last-error=...
```

Remove or repurpose old `queue-dropped` so critical drops are not conflated. Use `audit-dropped` only.

9. `shutdown`: set running false, join with longer timeout (e.g. 10s), ensure spill replayed/drained.

- [ ] **Step 4: Run persistence tests — expect pass**

```bash
./gradlew :paper-server:test --tests 'dev.mintychochip.provenance.ProvenancePersistenceTest'
```

- [ ] **Step 5: Commit**

```bash
git add paper-server/src/main/java/dev/mintychochip/provenance/ProvenanceWriter.java \
        paper-server/src/main/java/dev/mintychochip/provenance/ProvenanceRepository.java \
        paper-server/src/test/java/dev/mintychochip/provenance/ProvenancePersistenceTest.java
git commit -m "feat(provenance): never-drop writer with spill and DB audit"
```

---

### Task 4: Engine enqueues live updates

**Files:**
- Modify: `paper-server/src/main/java/dev/mintychochip/provenance/ItemProvenance.java`
- Modify: `paper-server/src/main/java/dev/mintychochip/provenance/LiveIndex.java` (optional helper)
- Test: `paper-server/src/test/java/dev/mintychochip/provenance/ProvenancePersistenceTest.java`

- [ ] **Step 1: Write failing durable-live collision test**

```java
@Test
public void durableLiveSeedsCensusAndDetectsSecondLocation() {
    ProvenanceWriter.install(tempDir, message -> {});
    final ItemStack original = new ItemStack(Items.DIAMOND, 1);
    final UUID id = ItemProvenance.birth(original, ProvenanceSource.LOOT, HAND).orElseThrow();
    ProvenanceWriter.flushAndClose();
    // Simulate restart: wipe RAM, keep DB, reinstall writer (replays + seeds live)
    ItemProvenance.clearAll();
    ProvenanceWriter.clearInstall();
    ProvenanceWriter.install(tempDir, message -> {});

    assertTrue(ItemProvenance.live().contains(id), "live must be seeded from DB");

    final ItemStack duplicate = original.copy();
    assertTrue(
        ItemProvenance.observe(duplicate, StackLocation.playerSlot(PLAYER, 1)),
        "second concrete location after restart must COLLISION"
    );
    ProvenanceWriter.flushAndClose();
    ProvenanceWriter.clearInstall();
}
```

- [ ] **Step 2: Run — expect fail** (live empty after reinstall)

```bash
./gradlew :paper-server:test --tests 'dev.mintychochip.provenance.ProvenancePersistenceTest.durableLiveSeedsCensusAndDetectsSecondLocation'
```

- [ ] **Step 3: Wire live enqueue + seed**

**A. Enqueue on census mutation** — private helper in `ItemProvenance`:

```java
private static void persistLive(@NotNull LiveEntry entry, boolean dead) {
    final LiveRecord record = new LiveRecord(
        entry.id(),
        entry.itemId(),
        entry.location().display(),
        entry.count(),
        System.currentTimeMillis(),
        dead
    );
    ProvenanceWriter.enqueueLive(record);
}
```

Call after:
- `LIVE.put(...)` in birth / rehydrate
- location/count updates in `transfer` / successful `observe` / claim paths
- `LIVE.remove` in `death` → `persistLive` with `dead=true` (build record from removed entry)

**B. Seed on install** — in `ProvenanceWriter` constructor after repo open + spill replay:

```java
if (repo != null) {
    for (LiveRecord row : repo.loadAliveLive()) {
        final StackLocation loc = ProvenanceRepository.parseLocationPublic(row.locationDisplay());
        // expose package parseLocation as package-private static parseLocationDisplay
        ItemProvenance.live().put(new LiveEntry(row.id(), row.itemId(), loc, row.count(), row.epochMs()));
    }
}
```

Make `parseLocation` package-visible (rename to package-private `parseLocationDisplay`) so writer/seed can use it.

**C. `clearInstall`** must not require durable wipe; runtime `clearAll` clears RAM only (document in javadoc).

- [ ] **Step 4: Run tests — expect pass**

```bash
./gradlew :paper-server:test --tests 'dev.mintychochip.provenance.ProvenancePersistenceTest'
```

- [ ] **Step 5: Commit**

```bash
git add paper-server/src/main/java/dev/mintychochip/provenance/ItemProvenance.java \
        paper-server/src/main/java/dev/mintychochip/provenance/ProvenanceWriter.java \
        paper-server/src/main/java/dev/mintychochip/provenance/ProvenanceRepository.java \
        paper-server/src/test/java/dev/mintychochip/provenance/ProvenancePersistenceTest.java
git commit -m "feat(provenance): durable last-seen live census seed on install"
```

---

### Task 5: Admin audit reads DB + status polish

**Files:**
- Modify: `paper-server/src/main/java/dev/mintychochip/provenance/ProvenanceBukkitCommand.java`
- Modify: `paper-server/src/main/java/dev/mintychochip/provenance/ProvenanceWriter.java` (expose `loadRecentAudit` via repository accessor if needed)
- Test: optional unit test not required if persistence already covers DB audit; smoke via existing suite

- [ ] **Step 1: Prefer durable audit in `/provenance audit`**

```java
// In audit subcommand handler:
List<ProvenanceEvent> events = ProvenanceWriter.recentAudit(n)
    .orElseGet(() -> ItemProvenance.audit().latest(n));
```

Add:

```java
// ProvenanceWriter
public static Optional<List<ProvenanceEvent>> recentAudit(int n) {
    final ProvenanceWriter w = instance;
    if (w == null || w.repository == null || w.repository.isFailed()) {
        return Optional.empty();
    }
    return Optional.of(w.repository.loadRecentAudit(n));
}
```

Note: DB may lag slightly behind RAM ring until flush; for admin ops this is OK. Document in command help string if desired.

- [ ] **Step 2: Ensure `status()` includes spill + audit-dropped** (if not done in Task 3)

- [ ] **Step 3: Run full provenance suite**

```bash
./gradlew :paper-server:test --tests 'org.bukkit.support.suite.ProvenanceTestSuite' --tests 'dev.mintychochip.provenance.ProvenancePersistenceTest' --tests 'dev.mintychochip.provenance.ProvenanceSpillJournalTest'
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add paper-server/src/main/java/dev/mintychochip/provenance/ProvenanceBukkitCommand.java \
        paper-server/src/main/java/dev/mintychochip/provenance/ProvenanceWriter.java
git commit -m "feat(provenance): admin audit reads durable SQLite log"
```

---

### Task 6: Spec cross-link + suite inclusion

**Files:**
- Modify: `docs/superpowers/specs/2026-08-07-item-provenance-design.md` (persistence bullet)
- Check: `paper-server/src/test/java/org/bukkit/support/suite/ProvenanceTestSuite.java` includes persistence tests

- [ ] **Step 1: Update core design persistence bullets**

Under Persistence / layout, add:

```markdown
- Durable store: see `2026-08-08-provenance-durable-store-design.md`
  (`provenance.db` lineage + live + collisions + audit; spill journal; no main-thread JDBC)
```

- [ ] **Step 2: Confirm test suite includes new tests**

If `ProvenanceTestSuite` is a classpath suite of package tests, no change. If explicit class list, add `ProvenanceSpillJournalTest`.

- [ ] **Step 3: Final verification**

```bash
./gradlew :paper-server:test --tests 'dev.mintychochip.provenance.*'
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add docs/superpowers/specs/2026-08-07-item-provenance-design.md \
        paper-server/src/test/java/org/bukkit/support/suite/ProvenanceTestSuite.java
git commit -m "docs(provenance): link durable store design from core spec"
```

---

## Success criteria checklist (from spec)

| Criterion | Task |
|-----------|------|
| Full lineage survives restart / spill | 1, 3, existing restart test |
| 0 critical drops under flood | 3 |
| Durable live → COLLISION after restart | 4 |
| Permanent audit in DB | 1, 3, 5 |
| Main thread enqueue/spill only | 3 (design + code review) |
| flushAndClose drains critical | 3 + existing flush tests |

## Out of scope (do not implement in this plan)

- Hard quarantine on COLLISION
- Main-thread sync SQLite
- Multi-world DBs
- Audit retention prune config
- Durable wipe admin command
- Craft/merge lineage repair (separate plan)

## Self-review notes

- Spec sections map to tasks 1–6.
- No TBD placeholders in steps.
- `LiveRecord`, spill kinds, and enqueue APIs are named consistently across tasks.
- JSONL remains a mirror; DB is source of truth for audit permanence.
