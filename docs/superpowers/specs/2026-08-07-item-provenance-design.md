# Item provenance — design

**Status:** implemented (core + broad hooks)  
**Package:** `dev.mintychochip.provenance`  
**Goal:** Track every item stack instance from birth through transforms so dupes are detectable and history is explainable.

## Model

| Concept | Meaning |
|---------|---------|
| **UUID** | Unique stack-instance id stamped in `CUSTOM_DATA` → `MintyProvenance` |
| **Birth** | Stack enters the economy (drop, craft, give, …) |
| **Death** | UUID leaves live census (consumed, merged, destroyed) |
| **Lineage** | Ordered parent UUID list for split/craft/smelt/trade/merge history walks |
| **Live census** | `uuid → holder` map; second distinct holder → **COLLISION** (dupe) |

### Rules

1. `ItemStack.copy()` **preserves** UUID (same identity).
2. Partial `split` **mints** child UUID with `parent=[parentId]`.
3. Full split (parent emptied) **moves** identity with the items.
4. Craft / smelt / special / trade **mint** result UUID with parent ingredient UUIDs.
5. A successful merge **mints** a `MERGE` UUID with ordered parents
   `[targetIdBefore, sourceIdBefore]`; the old target UUID dies and the source UUID
   dies only when fully absorbed. A partial source remainder keeps its UUID and
   updated count.
6. Merging two stacks with the same UUID records `DUPLICATE_MERGE`; it never
   launders quantity or mints a replacement identity.
7. Provenance stamp is **ignored** for `isSameItemSameComponents` so stacks still merge.
8. Restart **rehydrates** live stamped stacks into the census (not a new birth);
   a stale stamp for a dead UUID receives a replacement identity and never
   resurrects the retired UUID.
9. `birthIfAbsent` avoids double-mint when multiple hooks fire on the same stack.
10. **Placement memory:** place stamps the block pos with the stack UUID; break
   re-emits {@code BLOCK_RECOVER} children (anti place→break wash).
11. **Persistence:** per-dimension {@code ProvenancePlacementsData} SavedData
   (`mintychochip/provenance_placements`) auto-saves with the world.
12. **Pistons:** placement moves with pushed blocks.
13. **Dispensers / machine place:** any {@code BlockItem.place} path (player or null player)
    records placement with placer {@code machine} when no player.

## Example narrative

```
cobble stack (×3)  BIRTH BLOCK_DROP id=C
  split → c1,c2,c3 (SPLIT children of C / remainder)
sticks             BIRTH …
stone_pickaxe      TRANSFORM CRAFT parents=[c1,c2,c3,sticks] id=P
c1,c2,c3,sticks    DEATH CONSUMED
P claimed by steve
P claimed by hacker  → COLLISION id=P
explain(P) walks pickaxe → cobble/sticks ancestors
```

Merge example:

```
target cobble       BIRTH BLOCK_DROP id=T
source cobble       BIRTH LOOT id=S
target + source     BIRTH MERGE parents=[T,S] id=M
T, S                DEATH MERGED (full absorption)
explain(M) walks M → target/source ancestors
```

## Layout

| Layer | Path |
|-------|------|
| API DTOs/enums | `paper-api/.../provenance/` |
| Engine + stamp | `paper-server/.../provenance/ItemProvenance`, `StackStamp`, … |
| Vanilla hooks | see table below |
| Command | `/provenance inspect\|live\|audit\|collisions\|dupe-sim\|clear` |

### Persistence

- Placements: per-dimension SavedData (`mintychochip/provenance_placements`)
- Durable store: see `2026-08-08-provenance-durable-store-design.md`
  (`provenance.db` lineage + live + collisions + audit; spill journal; no main-thread JDBC)

## Vanilla hooks

| Path | Source / action |
|------|-----------------|
| `ItemStack.split` / stackability | SPLIT; stamp ignored for merge |
| `BlockItem` place | placement memory at pos (player + dispenser/shulker) |
| `Block.popResource` | BLOCK_RECOVER if placement memory, else BLOCK_DROP |
| `Block.dropResources` / creative / destroy no-drop | clear placement memory |
| `PistonBaseBlock.moveBlocks` | move placement from→to |
| `Level.destroyBlock` (no drops) | clear placement |
| `FallingBlockEntity` fall/land/drop | carry placement → restore or BLOCK_RECOVER item |
| `EnderMan` take/place | carry placement while held |
| `PiglinAi.throwItemsTowardPos` | LOOT birth (barter) |
| `BrushableBlockEntity.dropContent` | LOOT birth (archaeology) |
| `Inventory.addResource` | player inventory merge → capture both pre-merge UUIDs; emit `MERGE` node |
| `HopperBlockEntity.tryMoveInItem` | hopper/chest/any container merge with both pre-merge UUIDs |
| `TransportItemsBetweenContainers` | copper golem chest pickup + deposit/merge with lineage |
| `Slot.safeInsert` | all menu/GUI slot merges with lineage |
| `AbstractContainerMenu` cursor merge | shift/click into cursor; craft result is stamped before transfer |
| `FallingBlockEntity` / `EnderMan` NBT | entity-carried placement survives save |
| `AuditFileSink` | {@code <world>/mintychochip/provenance-audit.jsonl} |
| `ProvenancePlacementsData` | world SavedData persistence |
| `ResultSlot` craft transfer / `onTake` | preflight, `CRAFT` stamp before move, idempotent follow-up |
| `CrafterBlock.dispenseFrom` | CRAFT |
| `AbstractFurnaceBlockEntity.burn` | SMELT |
| `CampfireBlockEntity.cookTick` | SMELT |
| `SmithingMenu` / `StonecutterMenu` | SPECIAL_RECIPE |
| `MerchantResultSlot.onTake` | TRADE |
| `GiveCommand` | GIVE |
| Creative slot packet | GIVE |
| `LootTable.fill` | LOOT (chests) |
| `FishingHook.retrieve` | LOOT |
| `Entity.spawnAtLocation` | ENTITY_DROP if unstamped |
| `ItemEntity` pickup / remove | CLAIM; DEATH on despawn/void/destroy |
| `ItemEntity.merge` | `MERGE` node from both pre-merge UUIDs; source remains live on partial merge |
| `AnvilMenu.onTake` | SPECIAL_RECIPE + input death |
| `GrindstoneMenu` result take | SPECIAL_RECIPE + input death |
| `LoomMenu` result take | SPECIAL_RECIPE + banner/dye consume |
| `CartographyTableMenu` result take | SPECIAL_RECIPE + map/material consume |
| `BrewingStandBlockEntity.doBrew` | SPECIAL_RECIPE per bottle + ingredient/fuel death |
| `ItemStack.consume` | CONSUMED death / count update (food, throw, bucket) |
| `ItemStack.applyDamage` break | DESTROYED when tool/armor breaks |
| `ItemUtils.createFilledResult` | SPECIAL_RECIPE identity handoff (bucket/bottle/cauldron) |
| `ResultSlot` craft remainders | SPECIAL_RECIPE empty-bucket/bottle byproducts |
| `AbstractFurnaceBlockEntity.consumeFuel` | fuel CONSUMED + remainder handoff |
| Wet sponge → water bucket | SPECIAL_RECIPE with sponge+bucket parents |
| `ComposterBlock` insert/extract | compost consume; LOOT bone-meal birth |
| `DefaultDispenseItemBehavior.spawnItem` | ensure on dispenser/dropper eject |
| Jukebox / lectern / bookshelf / pot | `onParked` ensure on insert |
| Item frame / armor stand | `onParked` ensure on set item |
| `BundleContents.tryInsert` | ensure + merge tracking |
| Use-remainder (stew→bowl) | identity handoff after consume |
| Custom-block mint (`CustomBlockProvenance.createMinted`) | GIVE birth + keep PDC key |
| Custom-block place (`CustomBlockLifecycle` / `recordPlace`) | placement memory from hand UUID |
| Custom-block break drop (`createRecoverDrop`) | BLOCK_RECOVER + clear placement |

## Tests

```bash
./gradlew :paper-server:test --tests 'org.bukkit.support.suite.ProvenanceTestSuite'
```

## Out of scope (later)

- Hard quarantine on COLLISION
- Entity provenance (mobs themselves, not items)
