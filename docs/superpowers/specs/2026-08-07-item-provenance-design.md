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
| **Lineage** | Parent UUID list (split/craft/smelt/trade) for history walks |
| **Live census** | `uuid → holder` map; second distinct holder → **COLLISION** (dupe) |

### Rules

1. `ItemStack.copy()` **preserves** UUID (same identity).
2. Partial `split` **mints** child UUID with `parent=[parentId]`.
3. Full split (parent emptied) **moves** identity with the items.
4. Craft / smelt / special / trade **mint** result UUID with parent ingredient UUIDs.
5. Provenance stamp is **ignored** for `isSameItemSameComponents` so stacks still merge.
6. Restart **rehydrates** stamped stacks into the live census (not a new birth).
7. `birthIfAbsent` avoids double-mint when multiple hooks fire on the same stack.
8. **Placement memory:** place stamps the block pos with the stack UUID; break
   re-emits {@code BLOCK_RECOVER} children (anti place→break wash).
9. **Persistence:** per-dimension {@code ProvenancePlacementsData} SavedData
   (`mintychochip/provenance_placements`) auto-saves with the world.
10. **Pistons:** placement moves with pushed blocks.
11. **Dispensers / machine place:** any {@code BlockItem.place} path (player or null player)
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

## Layout

| Layer | Path |
|-------|------|
| API DTOs/enums | `paper-api/.../provenance/` |
| Engine + stamp | `paper-server/.../provenance/ItemProvenance`, `StackStamp`, … |
| Vanilla hooks | see table below |
| Command | `/provenance inspect\|live\|audit\|collisions\|dupe-sim\|clear` |

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
| `Inventory.addResource` | player inv merge → {@code afterContainerMerge} |
| `HopperBlockEntity.tryMoveInItem` | hopper/chest/any container merge |
| `TransportItemsBetweenContainers` | copper golem chest pickup + deposit/merge |
| `Slot.safeInsert` | all menu/GUI slot merges |
| `AbstractContainerMenu` cursor merge | shift/click into cursor |
| `FallingBlockEntity` / `EnderMan` NBT | entity-carried placement survives save |
| `AuditFileSink` | {@code <world>/mintychochip/provenance-audit.jsonl} |
| `ProvenancePlacementsData` | world SavedData persistence |
| `ResultSlot.onTake` | CRAFT + ingredient death |
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
| `ItemEntity.merge` | MERGE death |
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
