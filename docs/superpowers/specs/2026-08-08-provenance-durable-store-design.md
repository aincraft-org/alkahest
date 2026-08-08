# Provenance durable store — design

**Status:** implemented  
**Package:** `dev.mintychochip.provenance`  
**Depends on:** [item provenance core](./2026-08-07-item-provenance-design.md)  
**Goal:** Strong, deferred durability for full lineage, last-seen live census, collisions, and permanent audit — without freezing the server tick.

## Problem

Today provenance already has:

| Data | Where | Weakness |
|------|--------|----------|
| Stack UUID + parents | Item `CUSTOM_DATA` → `MintyProvenance` | OK (travels with item) |
| Lineage + collisions | `<world>/mintychochip/provenance.db` | Async queue **drops** under pressure; crash window before commit |
| Live census | In-memory `LiveIndex` only | Lost on restart; unloaded stacks invisible |
| Audit | Rotating JSONL (64 MB × 3) + RAM ring | Not permanent; not the long-term ledger |
| Placements | Per-dimension SavedData | Unchanged (already durable) |

Dupe detection and `explain()` need **full lineage and last-seen holders** to survive restarts and load spikes. Main-thread SQLite/fsync is **not** acceptable (tick freezes).

## Constraints

1. **No main-thread disk I/O** — game thread only enqueues (or appends spill).
2. **Full lineage strongly durable** — birth, parents, death, craft chains must land on disk.
3. **Deferred writes OK** — background writer + commit is correct.
4. **Never drop critical events** — lineage, live/last-seen, collision, death.
5. **Permanent audit by default** — DB is source of truth; no silent rotation of the only copy.
6. Preserve existing provenance **semantics** (split/craft/claim/collision rules, on-stack stamp, placement SavedData).

## Approach (chosen)

**Hardened single SQLite store + never-drop critical queue + spill journal.**

Rejected alternatives:

- **Critical-path sync SQLite** — freezes ticks under disk latency.
- **Append-only event log + projection only** — stronger crash story but two representations and more machinery than needed for a game server.
- **Lineage-only harden** — does not deliver durable live or permanent audit.

## Architecture

```
Game thread                          Writer thread
────────────                         ─────────────
ItemProvenance mutations ──enqueue──► Memory queue (bounded)
     │                                      │
     │ full?                                ├─► SQLite txn
     └─► Spill journal (append) ────────────┘   (lineage / live / collisions / audit)
         never drop critical

On install / restart:
  open provenance.db
  recover spill → apply into DB
  seed LiveIndex from `live` table
```

Unchanged:

- Stack identity on item NBT (`StackStamp` / `MintyProvenance`)
- Placement memory (`ProvenancePlacementsData` SavedData)
- Engine rules in `ItemProvenance`

Changed:

- Write guarantees, schema depth, restart seeding of live census, audit primary store

## Storage layout

Root (existing install path — primary world folder):

```text
<world>/mintychochip/
  provenance.db              # SQLite (WAL) — source of truth
  provenance-spill.log       # append-only critical spill when queue full
  provenance-audit.jsonl     # optional human mirror; not sole durability
```

### Schema

#### `lineage` (existing, keep)

| Column | Notes |
|--------|--------|
| `id` TEXT PK | Stack UUID |
| `item` TEXT | Item id |
| `source` TEXT | `ProvenanceSource` name |
| `parents` TEXT | Comma-separated parent UUIDs |
| `born` INTEGER | Epoch ms |
| `holder` TEXT | Born holder label (existing) |
| `dead` INTEGER | 0/1 |
| `death_reason` TEXT | nullable |
| `death_epoch` INTEGER | |

Upsert on every lineage mutation (birth, craft child, death mark, etc.).

#### `live` (new)

Last-seen / durable census. Seeds `LiveIndex` on restart.

| Column | Notes |
|--------|--------|
| `id` TEXT PK | Stack UUID |
| `item` TEXT | Item id |
| `location` TEXT | `StackLocation.display()` form (same encoding as collisions) |
| `count` INTEGER | Last observed stack count |
| `epoch` INTEGER | Last update epoch ms |
| `dead` INTEGER | 0 = should be live; 1 = left census |

Updated on: birth, rehydrate, transfer, observe/claim, count change, death.

#### `collisions` (existing, keep)

Unchanged insert semantics; still durable via critical path.

#### `audit` (new — permanent event log)

| Column | Notes |
|--------|--------|
| `seq` INTEGER PK AUTOINCREMENT | Stable order |
| `epoch` INTEGER | Event time |
| `kind` TEXT | Event type / reason |
| `payload` TEXT | JSON line (same fields as current audit export) |

**Retention:** keep forever by default. Optional later: config `auditRetentionDays` (0 = forever) with prune on writer thread. Out of scope for first implementation unless trivial.

### JSONL

- **Primary audit = DB.**
- JSONL may remain a thin mirror for tailing/tools, or be reduced after `/provenance audit` reads DB.
- Do **not** rotate away the only copy of audit history; if JSONL is kept, rotation is cosmetic only.

## Write path

### Critical vs non-critical

| Kind | Drop under pressure? | Spill if queue full? |
|------|----------------------|----------------------|
| Lineage upsert | **Never** | Yes |
| Live/last-seen upsert | **Never** | Yes |
| Collision insert | **Never** | Yes |
| Death (lineage + live) | **Never** | Yes |
| Audit event | Prefer not | Spill preferred; drop **only** if spill also fails (counter + rate-limited log) |

### Main thread

```text
ProvenanceWriter.enqueue(item)
```

- No JDBC, no fsync, no blocking wait on writer drain.
- Bounded memory queue (order of 8k–32k items).
- Critical + full queue → append to spill journal, return.
- Audit + full queue → try spill; else drop + metric.

### Writer thread (`mintychochip-provenance-writer`)

1. Drain memory queue; also consume unreplayed spill prefix.
2. Batch into one SQLite transaction (cap by count and/or ~50ms).
3. Apply upserts/inserts for lineage, live, collisions, audit.
4. `commit` (WAL). Periodic `PRAGMA wal_checkpoint(TRUNCATE)` under light load.
5. After successful apply of spilled records, **ack/truncate** spill so replay is idempotent.

### Spill journal

- Path: `<world>/mintychochip/provenance-spill.log`
- Append-only framing: length-prefixed records with type + payload (implementation may use length-prefixed JSON; keep format versioned and dumb).
- On install: if spill exists, **replay into DB before** treating store as clean.
- Idempotent applies: lineage/live by primary key; collisions/audit use stable keys or tolerate duplicates without corrupting meaning (prefer natural keys / ignore-duplicate where needed).

### Shutdown

- `flushAndClose`: stop clean accept (or drain until empty), apply memory + spill, commit, close DB.
- Wait for drain with timeout; log loudly if incomplete.
- Wire into existing server stop path (`ProvenanceBootstrap` / writer install).

### Crash matrix

| When crash happens | After restart |
|--------------------|---------------|
| NBT stamped, write not yet spilled | Stamp remains; lineage may be incomplete until rehydrate rebuilds from on-stack parents; next observe updates live |
| Enqueued or spilled, not committed | Spill/queue recovery → full row restore |
| Committed to SQLite | Immediate full history + last-seen |

## Restart / rehydrate

1. Open `provenance.db`; create tables if missing (migrate add `live`, `audit`).
2. Replay spill journal into DB.
3. Load `live` where `dead=0` into `LiveIndex` (location, count, item, epoch).
4. Existing stack load hooks still call `rehydrate` / `observe`; if id already live from DB at another concrete location → **COLLISION**.
5. `LineageStore` remains bounded RAM cache + DB load-on-miss; do not load entire graph into memory.

## Engine integration (thin)

| Hook point | Durable write |
|------------|----------------|
| Birth / craft / split mint | lineage upsert + live upsert + audit |
| Transfer / claim / observe | live upsert (+ collision insert if any) |
| Death | lineage mark dead + live dead=1 + audit |
| Collision | collisions insert + audit |

`LineageStore.put` / `AuditLog.append` / live mutations continue to enqueue via `ProvenanceWriter` (no direct JDBC from game logic).

## Ops / API

- Extend `ProvenanceWriter.status()`: queue depth, spill bytes, critical never-drop invariant (or spill failures), dropped audit count, last error, store mode.
- `/provenance inspect|explain|live|audit|collisions` keep current meaning; `live` may include DB-seeded entries; `audit` should read durable store (DB), not only RAM ring.
- `ItemProvenance.clearAll()` remains **runtime only**; durable wipe is a separate deliberate admin action (out of scope unless requested).

## Success criteria

1. Kill mid-session → restart → `explain(id)` walks full ancestors for stacks whose lineage was at least spilled/committed.
2. Queue flood → **0** critical drops; spill absorbs; after drain all critical rows in DB.
3. Restart with last-seen only in DB → second concrete location → COLLISION.
4. N audit events → restart → all N still queryable from `audit` table.
5. Main-thread path remains enqueue/spill-only (no JDBC on caller).
6. `flushAndClose` after enqueue → all critical rows present before process exit.

## Tests

Extend `ProvenancePersistenceTest` (and suite) with:

| Test | Asserts |
|------|---------|
| Restart lineage | Birth → child → wipe runtime → rehydrate → ancestors from DB |
| Spill under pressure | Saturate queue → critical lands in DB after drain |
| Never drop critical | Critical drop counter stays 0 under flood |
| Durable live | Seed last-seen in DB → clear runtime → reload → second location → COLLISION |
| Audit permanent | N events → restart → N rows in `audit` |
| Shutdown flush | Enqueue then `flushAndClose` → rows present |

```bash
./gradlew :paper-server:test --tests 'org.bukkit.support.suite.ProvenanceTestSuite'
./gradlew :paper-server:test --tests 'dev.mintychochip.provenance.ProvenancePersistenceTest'
```

## Implementation sketch (for plan)

1. Schema migration in `ProvenanceRepository`: add `live`, `audit`; helpers upsert/load.
2. Spill journal type + replay/ack in `ProvenanceWriter`.
3. Never-drop enqueue policy; batch commit; shutdown wait.
4. `LiveIndex` seed from `live` on install; engine enqueues live updates on census changes.
5. Audit dual-write to DB; wire admin audit read to DB (RAM ring remains hot tail).
6. Persistence tests for criteria above.
7. Optional: thin JSONL mirror or leave as-is without relying on it for durability.

## Out of scope

- Hard quarantine on COLLISION (still later)
- Entity (mob) provenance
- Main-thread synchronous SQLite
- Multi-world separate DBs (keep single install root = primary world folder)
- Audit retention prune config (default forever; knob later)
- Deliberate durable wipe admin command

## Relationship to existing design

This document **extends** durability of the system in `2026-08-07-item-provenance-design.md`. Gameplay hooks and identity rules there remain authoritative; this doc only hardens where data is stored and how it survives restarts and load.
