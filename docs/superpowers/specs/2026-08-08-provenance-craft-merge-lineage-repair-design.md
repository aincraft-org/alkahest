# Provenance craft and merge lineage repair — design

**Status:** approved  
**Package:** `dev.mintychochip.provenance`  
**Goal:** Make `/provenance inspect` show every stack history that contributed to a crafted or merged stack, without weakening duplicate-UUID detection.

## Problem

Craft results were stamped in `ResultSlot.onTake`, after some vanilla transfer paths had already materialized the result elsewhere:

- Shift-click called `moveItemStackTo` before `onTake`; the source result stack was normally empty by the time provenance ran.
- Clicking a result onto a matching cursor stack grew the cursor before `onTake`; provenance was written to the discarded removed stack rather than the cursor.

The destination therefore kept an older UUID or remained unstamped and became `LEGACY` on first inspection.

The in-progress craft fix stamps before these transfers. A second limitation remains: legitimate merges preserve the target UUID and mark only the incoming UUID dead. Since lineage walks only follow parents, `/provenance inspect` cannot show the absorbed craft branch from the surviving target.

## Required behavior

1. A crafting result receives a new `CRAFT` UUID with all ingredient UUIDs as parents before any successful transfer or merge.
2. A legitimate merge of two different stack UUIDs creates a new stack UUID with both pre-merge UUIDs as parents.
3. The merged identity uses the explicit source `MERGE` and emits a `ProvenanceEventType.MERGE` audit event.
4. A merge of two copies carrying the same UUID remains a `DUPLICATE_MERGE` collision. It must not mint a legitimate replacement identity.
5. Normal pickup, matching-cursor pickup, 2×2 shift-click crafting, 3×3 shift-click crafting, inventory merges, hopper/container merges, bundle merges, and item-entity merges use the same merge invariant.
6. Existing lineage and collision persistence remains readable.
7. Vanilla `Slot.onTake` and plugin-visible event ordering remains unchanged.
8. A failed transfer must not mint a live identity for a virtual crafting preview.

## Identity model

`ProvenanceSource` gains `MERGE`. This is an API/model change, not merely a hook change. Every exhaustive source switch, command color mapping, stamp codec path, repository serialization path, and affected test must accept the new value.

For different UUIDs `A` and `B`:

```text
A + B  ->  C(source=MERGE, parents=[A, B])
```

`C` is written to the materialized target stack and becomes the only live identity for that target. The merge appends a `MERGE` audit event for `C`; predecessor deaths remain explicit audit events.

### Full merge

Both input stacks cease to exist as independent stacks:

```text
A --DEAD[MERGED]--> C
B --DEAD[MERGED]--> C
C --LIVE----------> target count
```

### Partial merge

The old target identity ceases because the target stack changed composition. The source identity remains live at its reduced count:

```text
A --DEAD[MERGED]--> C
B --LIVE----------> source remainder
B ----------------> parent of C
C --LIVE----------> enlarged target
```

A live parent is valid: crafting already permits one unit from a larger live ingredient stack to parent a result. This design remains stack-level rather than per-unit accounting.

### Same-UUID merge

```text
A + copy(A)  ->  DUPLICATE_MERGE collision
```

No replacement UUID is minted. This preserves the laundering detector.

## Merge boundary contract

Every merge hook identifies both inputs before counts or components are mutated. The shared merge operation receives:

- target stack after growth;
- source stack after shrink;
- target UUID captured before mutation;
- source UUID captured before mutation;
- amount moved;
- source and target locations.

Unstamped non-empty inputs are ensured at an unknown or transient location before mutation so both contributing histories exist without creating a false concrete-location claim.

The shared operation owns all identity changes, live-count updates, predecessor deaths, the new lineage node, and the `MERGE` audit event. Individual vanilla hooks remain thin and do not duplicate lifecycle logic.

## Craft transfer ordering

`ResultSlot` provides one idempotent operation that snapshots ingredient UUIDs and stamps the live result stack as `CRAFT`.

It runs only once transfer success is known and before provenance-aware mutation:

- before an ordinary result extraction can merge into the cursor;
- before the successful `CraftingMenu.moveItemStackTo` transfer for the 3×3 table;
- before the successful `InventoryMenu.moveItemStackTo` transfer for the 2×2 grid;
- from `onTake` as a fallback for paths that have not prepared the result.

The cursor path calls the stamp helper directly; it does not move `Slot.onTake` ahead of vanilla’s cursor mutation. Calling the helper twice on the same already-`CRAFT` stack preserves that UUID rather than creating a second craft node. Input consumption remains in `ResultSlot.onTake` and occurs once.

A quick-move first calls the five-argument `moveItemStackTo(stack, start, end, backwards, true)` as a non-mutating preflight. On success it stamps the result, then calls the normal four-argument `moveItemStackTo(stack, start, end, backwards)` for the real transfer (`isCheck=false`). Both calls execute synchronously on the server thread, so destination state cannot change between them. A full inventory leaves the virtual result unstamped and adds no live lineage node.

## Inspect output

The command remains a root-first ancestry tree. Its label is corrected to describe the actual direction: current stack first, contributors below.

A crafted result merged into an older stack is represented as:

```text
current item  MERGE C
├─ current item  old source A
└─ current item  CRAFT B
   ├─ ingredient  BLOCK_DROP D
   └─ ingredient  LEGACY E
```

The tree does not infer quantities for individual branches.

## Persistence and compatibility

- `StackStamp` continues storing enum names; `MERGE` must round-trip through the on-stack payload.
- `ProvenanceRepository` continues storing enum names; `MERGE` lineage nodes must survive restart and rehydration.
- Existing stored source names remain unchanged and readable.
- No compatibility alias or migration is required because this is an additive enum value.

## Verification

Behavioral regression coverage must prove:

1. Direct result pickup creates a `CRAFT` root with ingredient parents.
2. Pickup onto a matching cursor stack creates a `MERGE` root containing both the older cursor and `CRAFT` branches without changing vanilla event order.
3. 2×2 and 3×3 shift-click results retain `CRAFT` ancestry after landing in an empty slot and after merging into an occupied slot.
4. A failed quick-move leaves the virtual result unstamped and creates no live identity.
5. Full and partial generic container merges create a third UUID with both contributors and correct live/dead states.
6. Hopper, bundle, and item-entity merge entry points obey the same invariant.
7. Same-UUID location and merge collisions remain detected exactly once.
8. `MERGE` stamps and lineage nodes survive serialization and rehydration.
9. `/provenance inspect` traversal contains the current `MERGE` node, the `CRAFT` node, and ingredient ancestors.

Tests assert runtime behavior and state transitions. Source-text or patch-presence assertions are not acceptable substitutes.

After vanilla source edits, rebuild source patches and run the complete provenance suite. `fixupSourcePatches` is required only when the applied source tree has uncommitted source changes for paperweight to fix up.

## Out of scope

- Per-item token identities inside one stack.
- Quantity-conservation enforcement across placement, crafting, and recovery.
- Collision quarantine or automatic item removal.
- Historical reconstruction from reverse audit-log scans.
