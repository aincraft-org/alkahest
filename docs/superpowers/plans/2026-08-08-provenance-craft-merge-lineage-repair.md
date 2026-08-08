# Provenance Craft and Merge Lineage Repair Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure crafted results are stamped before successful transfer and make every legitimate stack merge produce a new lineage node containing all contributing histories.

**Architecture:** Keep vanilla hooks thin. `ResultSlot` prepares `CRAFT` identity at the last safe point before transfer; `ItemProvenance` owns one shared merge transaction that writes a `MERGE` stack, deaths predecessor identities, updates the live census, and emits the audit event. All container, bundle, hopper, copper-golem, cursor, quick-move, and item-entity hooks capture both UUIDs before mutating counts and call that transaction afterward.

**Tech Stack:** Java 25, Paper/Paperweight source patches, JUnit 5 Normal test suite, Mockito, NBT `CUSTOM_DATA`, SQLite lineage repository.

## Global Constraints

- Owned code stays under `paper-api/src/main/java/dev/mintychochip/provenance` or `paper-server/src/main/java/dev/mintychochip/provenance`; only `net.minecraft` hooks use Paper source patches.
- Edit applied vanilla sources under `paper-server/src/minecraft/java/net/minecraft`, then run `fixupSourcePatches` when that applied git tree is dirty and `rebuildPatches` afterward.
- `ProvenanceSource.MERGE` is an additive API enum value; existing serialized source names remain unchanged.
- Different-UUID merges mint a new identity with both prior IDs as parents. Same-UUID merges remain `DUPLICATE_MERGE` collisions.
- Preserve vanilla `Slot.onTake` and plugin-visible event ordering.
- Failed craft transfers leave virtual result previews unstamped.
- Tests assert runtime behavior and state transitions; remove source-text/patch-presence tests.
- Final code is a clean cutover: remove obsolete merge overloads and old survivor-keeps-identity semantics.
- Preserve unrelated worktree content (`bench/`, `serve/`, heap dumps, and any user changes outside listed paths).

## File Structure

| File | Responsibility |
|---|---|
| `paper-api/src/main/java/dev/mintychochip/provenance/ProvenanceSource.java` | Add the persisted `MERGE` derivation source. |
| `paper-server/src/main/java/dev/mintychochip/provenance/ItemProvenance.java` | Idempotent craft birth and authoritative merge transaction. |
| `paper-server/src/main/java/dev/mintychochip/provenance/ProvenanceBukkitCommand.java` | Render `MERGE` and correct inspect tree direction text. |
| `paper-server/src/test/java/dev/mintychochip/provenance/CraftingMenuProvenanceTest.java` | Real 2×2/3×3 quick-move, failed transfer, and merge behavior. |
| `paper-server/src/test/java/dev/mintychochip/provenance/ItemProvenanceTest.java` | Full/partial merge identity and ancestry contracts. |
| `paper-server/src/test/java/dev/mintychochip/provenance/ProvenanceInvariantTest.java` | Same-ID collision cannot be laundered. |
| `paper-server/src/test/java/dev/mintychochip/provenance/ProvenancePersistenceTest.java` | `MERGE` stamp/lineage persistence. |
| `paper-server/src/test/java/dev/mintychochip/provenance/CraftProvenanceHooksPresentTest.java` | Delete; source-text assertions do not test behavior. |
| `paper-server/src/minecraft/java/net/minecraft/world/inventory/ResultSlot.java` | Prepare craft identity from current ingredients. |
| `paper-server/src/minecraft/java/net/minecraft/world/inventory/CraftingMenu.java` | Preflight and stamp 3×3 quick-move before real transfer. |
| `paper-server/src/minecraft/java/net/minecraft/world/inventory/InventoryMenu.java` | Preflight and stamp 2×2 quick-move before real transfer. |
| `paper-server/src/minecraft/java/net/minecraft/world/inventory/AbstractContainerMenu.java` | Cursor and generic quick-move merge hooks. |
| `paper-server/src/minecraft/java/net/minecraft/world/inventory/Slot.java` | Generic menu-slot merge hook. |
| `paper-server/src/minecraft/java/net/minecraft/world/entity/player/Inventory.java` | Player inventory merge hook. |
| `paper-server/src/minecraft/java/net/minecraft/world/level/block/entity/HopperBlockEntity.java` | Hopper/container merge hook. |
| `paper-server/src/minecraft/java/net/minecraft/world/item/component/BundleContents.java` | Bundle-internal merge hook. |
| `paper-server/src/minecraft/java/net/minecraft/world/entity/ai/behavior/TransportItemsBetweenContainers.java` | Copper-golem transport merge hook. |
| `paper-server/src/minecraft/java/net/minecraft/world/entity/item/ItemEntity.java` | Full and partial ground-item merge hook. |
| `docs/superpowers/specs/2026-08-07-item-provenance-design.md` | Update implemented merge rule and hook descriptions. |

---

### Task 1: Make craft stamping transactional

**Files:**
- Modify: `paper-server/src/main/java/dev/mintychochip/provenance/ItemProvenance.java:947-963`
- Modify applied source: `paper-server/src/minecraft/java/net/minecraft/world/inventory/ResultSlot.java:93-174`
- Modify applied source: `paper-server/src/minecraft/java/net/minecraft/world/inventory/CraftingMenu.java:137-181`
- Modify applied source: `paper-server/src/minecraft/java/net/minecraft/world/inventory/InventoryMenu.java:105-165`
- Modify applied source: `paper-server/src/minecraft/java/net/minecraft/world/inventory/AbstractContainerMenu.java:587-630`
- Test: `paper-server/src/test/java/dev/mintychochip/provenance/CraftingMenuProvenanceTest.java`
- Test: `paper-server/src/test/java/dev/mintychochip/provenance/ItemProvenanceTest.java`
- Delete: `paper-server/src/test/java/dev/mintychochip/provenance/CraftProvenanceHooksPresentTest.java`
- Rebuild patches: `paper-server/patches/sources/net/minecraft/world/inventory/{ResultSlot,CraftingMenu,InventoryMenu,AbstractContainerMenu}.java.patch`

**Interfaces:**
- Consumes: `ItemProvenance.onCrafted(ItemStack, List<UUID>, StackLocation)`.
- Produces: `ResultSlot.stampCraftResult(Player, ItemStack)`; repeated calls on the same `CRAFT` stack preserve its UUID.

- [ ] **Step 1: Keep the existing real 3×3 shift-click regression and add a failing full-inventory test**

Extend `CraftingMenuProvenanceTest` with a menu helper and this contract:

```java
@Test
public void failedShiftClickDoesNotStampVirtualResult() {
    for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
        this.inventory.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
    }
    final CraftingMenu menu = this.craftingMenuWithLogToPlanks();
    final ItemStack resultPreview = menu.resultSlots.getItem(0);

    assertTrue(menu.quickMoveStack(this.player, CraftingMenu.RESULT_SLOT).isEmpty());
    assertTrue(StackStamp.read(resultPreview).isEmpty(), "failed transfer must not stamp virtual output");
    assertTrue(ItemProvenance.live().values().stream()
        .noneMatch(entry -> entry.itemId().equals("minecraft:oak_planks")));
}

private CraftingMenu craftingMenuWithLogToPlanks() {
    final CraftingMenu menu = new CraftingMenu(1, this.inventory);
    final ItemStack ingredient = new ItemStack(Items.OAK_LOG, 1);
    ItemProvenance.birth(
        ingredient,
        ProvenanceSource.BLOCK_DROP,
        StackLocation.playerSlot(this.player.getUUID(), 0)
    ).orElseThrow();
    menu.craftSlots.setItem(0, ingredient);
    menu.resultSlots.setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
    return menu;
}
```


- [ ] **Step 2: Run the suite and verify the new test fails for the intended reason**

Run:

```bash
./gradlew :paper-server:cleanTest :paper-server:test --tests 'org.bukkit.support.suite.ProvenanceTestSuite'
```

Expected: `failedShiftClickDoesNotStampVirtualResult` fails because the current pre-transfer hook writes `CRAFT` before discovering that no destination has room. The existing `shiftClickCraftRetainsCraftIdentityInInventory` must continue passing.

- [ ] **Step 3: Preflight both quick-move paths, then stamp and run the real transfer**

In `CraftingMenu.quickMoveStack`, replace the result branch with the exact ordering below:

```java
if (slotIndex == RESULT_SLOT) {
    stack.getItem().onCraftedBy(stack, player);
    if (!this.moveItemStackTo(stack, 10, 46, true, true)) {
        return ItemStack.EMPTY;
    }
    if (slot instanceof ResultSlot resultSlot) {
        resultSlot.stampCraftResult(player, stack);
    }
    if (!this.moveItemStackTo(stack, 10, 46, true)) {
        throw new IllegalStateException("craft quick-move preflight succeeded but transfer failed");
    }
    slot.onQuickCraft(stack, clicked);
}
```

In `InventoryMenu.quickMoveStack`, use the 2×2 destination range:

```java
if (slotIndex == RESULT_SLOT) {
    if (!this.moveItemStackTo(stack, 9, 45, true, true)) {
        return ItemStack.EMPTY;
    }
    if (slot instanceof ResultSlot resultSlot) {
        resultSlot.stampCraftResult(player, stack);
    }
    if (!this.moveItemStackTo(stack, 9, 45, true)) {
        throw new IllegalStateException("inventory craft preflight succeeded but transfer failed");
    }
    slot.onQuickCraft(stack, clicked);
}
```

The first call is the five-argument non-mutating `isCheck=true` overload. The second is the existing four-argument real transfer whose final `true` means `backwards`.

- [ ] **Step 4: Preserve cursor mutation and `onTake` ordering**

In the matching-cursor branch of `AbstractContainerMenu`, stamp only the removed result before growth, then retain the original `slot.onTake` position:

```java
newCarried.ifPresent(itemsTaken -> {
    if (slot instanceof ResultSlot resultSlot) {
        resultSlot.stampCraftResult(player, itemsTaken);
    }
    final Optional<UUID> absorbedId = StackStamp.readId(itemsTaken);
    final int moved = itemsTaken.getCount();
    carried.grow(moved);
    ItemProvenance.afterContainerMerge(
        carried,
        itemsTaken.copyWithCount(0),
        absorbedId,
        moved,
        StackLocation.labeled("menu-slot:" + slot.index),
        StackLocation.playerSlot(player.getUUID(), -1)
    );
    slot.onTake(player, itemsTaken);
});
```

Do not call `onTake` before `carried.grow`, and do not call it twice.

- [ ] **Step 5: Keep craft birth idempotent without reviving an absorbed result**

Add this regression to `ItemProvenanceTest`:

```java
@Test
public void repeatedCraftHookDoesNotReviveMergedIdentity() {
    final ItemStack ingredient = new ItemStack(Items.OAK_LOG, 1);
    final UUID ingredientId = ItemProvenance.birth(
        ingredient, ProvenanceSource.BLOCK_DROP, HAND
    ).orElseThrow();
    final ItemStack result = new ItemStack(Items.OAK_PLANKS, 4);
    ItemProvenance.onCrafted(result, List.of(ingredientId), HAND);
    final UUID craftId = StackStamp.readId(result).orElseThrow();
    ItemProvenance.death(craftId, ProvenanceReason.MERGED, UUID.randomUUID());

    ItemProvenance.onCrafted(result, List.of(ingredientId), HAND);

    assertFalse(ItemProvenance.live().contains(craftId));
    assertTrue(ItemProvenance.lineage().get(craftId).orElseThrow().dead());
    assertTrue(ItemProvenance.audit().snapshot().stream()
        .noneMatch(event -> event.type() == ProvenanceEventType.ZOMBIE && event.id().equals(craftId)));
}
```

Run the suite. Expected: FAIL because the current idempotency guard calls `rehydrateIfNeeded` and resurrects the dead craft UUID.

Use a pure no-op guard in `ItemProvenance.onCrafted`:

```java
final Optional<StackProvenance> existing = StackStamp.read(result);
if (existing.isPresent() && existing.get().source() == ProvenanceSource.CRAFT) {
    return;
}
birth(result, ProvenanceSource.CRAFT, location, ingredientIds);
```

Retain `onCraftedIsIdempotentWhenAlreadyCraftStamped` and `unstampedResultBecomesCraftNotLegacy`. Rehydration belongs to load/observe paths, not a repeated craft callback after the result may already have been absorbed.

- [ ] **Step 6: Replace structural assertions with direct, 2×2, and 3×3 behavior**

Delete `CraftProvenanceHooksPresentTest.java`. In test setup, return a Bukkit player for the real `ItemCraftedEvent` path:

```java
when(this.player.getBukkitEntity()).thenReturn(mock(CraftPlayer.class));
```

Add direct cursor pickup:

```java
@Test
public void directPickupRetainsCraftIdentityOnCursor() {
    final CraftingMenu menu = this.craftingMenuWithLogToPlanks();
    final UUID ingredientId = StackStamp.readId(menu.craftSlots.getItem(0)).orElseThrow();

    menu.clicked(CraftingMenu.RESULT_SLOT, 0, ClickType.PICKUP, this.player);

    final StackProvenance stamp = StackStamp.read(menu.getCarried()).orElseThrow();
    assertEquals(ProvenanceSource.CRAFT, stamp.source());
    assertEquals(List.of(ingredientId), stamp.parents());
}
```

Add a 2×2 test using `InventoryMenu`:

```java
@Test
public void playerGridShiftClickRetainsCraftIdentityInInventory() {
    final InventoryMenu menu = new InventoryMenu(this.inventory, true, this.player);
    final ItemStack ingredient = new ItemStack(Items.OAK_LOG, 1);
    final UUID ingredientId = ItemProvenance.birth(
        ingredient,
        ProvenanceSource.BLOCK_DROP,
        StackLocation.playerSlot(this.player.getUUID(), 0)
    ).orElseThrow();
    menu.craftSlots.setItem(0, ingredient);
    menu.resultSlots.setItem(0, new ItemStack(Items.OAK_PLANKS, 4));

    assertFalse(menu.quickMoveStack(this.player, InventoryMenu.RESULT_SLOT).isEmpty());
    final StackProvenance stamp = StackStamp.read(this.findInventoryStack(Items.OAK_PLANKS)).orElseThrow();
    assertEquals(ProvenanceSource.CRAFT, stamp.source());
    assertEquals(List.of(ingredientId), stamp.parents());
}
```

- [ ] **Step 7: Verify craft behavior is green**

Run the complete provenance suite again. Expected: all tests pass, including direct pickup, real 2×2/3×3 transfers, the no-zombie invariant, and the failed-transfer invariant.

- [ ] **Step 8: Rebuild the four touched source patches**

Run:

```bash
./gradlew fixupSourcePatches
./gradlew rebuildPatches
```

If `fixupSourcePatches` reports `nothing to commit`, confirm the applied source tree already contains the intended edits and run `rebuildPatches` alone. Do not treat that paperweight no-op as a production failure.

- [ ] **Step 9: Commit the craft-transfer unit**

Stage only the four rebuilt patches, `ItemProvenance.java`, `ItemProvenanceTest.java`, and `CraftingMenuProvenanceTest.java`. Remove the untracked structural test; stage its deletion only if it had become tracked.

```bash
git commit -m "fix(provenance): stamp crafted results before transfer"
```

---

### Task 2: Model merges as first-class lineage nodes

**Files:**
- Modify: `paper-api/src/main/java/dev/mintychochip/provenance/ProvenanceSource.java:6-36`
- Modify: `paper-server/src/main/java/dev/mintychochip/provenance/ItemProvenance.java:192-231,656-792`
- Modify: `paper-server/src/main/java/dev/mintychochip/provenance/ProvenanceBukkitCommand.java:316-320,466-475`
- Test: `paper-server/src/test/java/dev/mintychochip/provenance/ItemProvenanceTest.java:174-283`
- Test: `paper-server/src/test/java/dev/mintychochip/provenance/ProvenanceInvariantTest.java:77-91`

**Interfaces:**
- Consumes: `birth`, `death`, `transfer`, `StackStamp.write`, `LineageStore`, and `AuditLog`.
- Produces:

```java
public static boolean afterContainerMerge(
    ItemStack target,
    ItemStack sourceRemaining,
    Optional<UUID> targetIdBefore,
    Optional<UUID> sourceIdBefore,
    int amountMoved,
    StackLocation sourceLocation,
    StackLocation targetLocation
)
```

- [ ] **Step 1: Rewrite full and partial merge tests to require a third identity**

Replace the current survivor-keeps-ID assertions with:

```java
@Test
public void fullMergeMintsIdentityWithBothParents() {
    final ItemStack target = new ItemStack(Items.COBBLESTONE, 40);
    final ItemStack source = new ItemStack(Items.COBBLESTONE, 24);
    final UUID targetId = ItemProvenance.birth(target, ProvenanceSource.BLOCK_DROP, CHEST).orElseThrow();
    final UUID sourceId = ItemProvenance.birth(source, ProvenanceSource.CRAFT, HAND).orElseThrow();
    final Optional<UUID> targetBefore = StackStamp.readId(target);
    final Optional<UUID> sourceBefore = StackStamp.readId(source);

    source.shrink(24);
    target.grow(24);
    assertFalse(ItemProvenance.afterContainerMerge(
        target, source, targetBefore, sourceBefore, 24, HAND, CHEST
    ));

    final StackProvenance merged = StackStamp.read(target).orElseThrow();
    assertNotEquals(targetId, merged.id());
    assertNotEquals(sourceId, merged.id());
    assertEquals(ProvenanceSource.MERGE, merged.source());
    assertEquals(List.of(targetId, sourceId), merged.parents());
    assertTrue(ItemProvenance.lineage().get(targetId).orElseThrow().dead());
    assertTrue(ItemProvenance.lineage().get(sourceId).orElseThrow().dead());
    assertTrue(ItemProvenance.live().contains(merged.id()));
    assertTrue(ItemProvenance.audit().snapshot().stream()
        .anyMatch(event -> event.type() == ProvenanceEventType.MERGE && event.id().equals(merged.id())));
}
```

Add the partial counterpart:

```java
@Test
public void partialMergeMintsTargetIdentityAndKeepsSourceRemainderLive() {
    final ItemStack target = new ItemStack(Items.COBBLESTONE, 50);
    final ItemStack source = new ItemStack(Items.COBBLESTONE, 30);
    final UUID targetId = ItemProvenance.birth(target, ProvenanceSource.BLOCK_DROP, CHEST).orElseThrow();
    final UUID sourceId = ItemProvenance.birth(source, ProvenanceSource.CRAFT, HAND).orElseThrow();
    final Optional<UUID> targetBefore = StackStamp.readId(target);
    final Optional<UUID> sourceBefore = StackStamp.readId(source);

    source.shrink(14);
    target.grow(14);
    ItemProvenance.afterContainerMerge(target, source, targetBefore, sourceBefore, 14, HAND, CHEST);

    final StackProvenance merged = StackStamp.read(target).orElseThrow();
    assertEquals(ProvenanceSource.MERGE, merged.source());
    assertEquals(List.of(targetId, sourceId), merged.parents());
    assertTrue(ItemProvenance.lineage().get(targetId).orElseThrow().dead());
    assertTrue(ItemProvenance.live().contains(sourceId));
    assertEquals(16, ItemProvenance.live().get(sourceId).orElseThrow().count());
    assertTrue(ItemProvenance.live().contains(merged.id()));
}
```

- [ ] **Step 2: Update the laundering invariant to the new signature and verify RED**

Capture both IDs before count mutation:

```java
final Optional<UUID> targetId = StackStamp.readId(survivor);
final Optional<UUID> sourceId = StackStamp.readId(duplicate);
survivor.grow(duplicate.getCount());
duplicate.setCount(0);
assertTrue(ItemProvenance.afterContainerMerge(
    survivor, duplicate, targetId, sourceId, 4, source, target
));
assertEquals(targetId.orElseThrow(), StackStamp.readId(survivor).orElseThrow());
```

Run the suite. Expected: compile failure until `MERGE` and the seven-argument merge API exist, followed by behavioral failures if only the signature is added.

- [ ] **Step 3: Add the persisted `MERGE` source and command rendering**

Add this enum constant after `SPLIT`:

```java
/** Combined from two independently tracked stack identities. */
MERGE,
```

Update the exhaustive command switch:

```java
case CRAFT, SMELT, SPECIAL_RECIPE, MERGE -> AQUA;
```

Change the inspect heading from `parents above → root` to `current stack → contributors`.

- [ ] **Step 4: Emit `MERGE`, not `BIRTH`, when `birth` creates a merge node**

Replace the event-type selection with:

```java
final ProvenanceEventType eventType = switch (source) {
    case MERGE -> ProvenanceEventType.MERGE;
    case CRAFT, SMELT, SPECIAL_RECIPE, TRADE, BLOCK_RECOVER -> ProvenanceEventType.TRANSFORM;
    default -> ProvenanceEventType.BIRTH;
};
```

- [ ] **Step 5: Implement the authoritative merge transaction**

Add the seven-argument overload:

```java
public static boolean afterContainerMerge(
    final @NotNull ItemStack target,
    final @NotNull ItemStack sourceRemaining,
    final @NotNull Optional<UUID> targetIdBefore,
    final @NotNull Optional<UUID> sourceIdBefore,
    final int amountMoved,
    final @NotNull StackLocation sourceLocation,
    final @NotNull StackLocation targetLocation
) {
    if (!enabled || amountMoved <= 0 || target.isEmpty()) {
        return false;
    }
    if (targetIdBefore.isEmpty() || sourceIdBefore.isEmpty()) {
        return false;
    }

    final UUID targetId = targetIdBefore.get();
    final UUID sourceId = sourceIdBefore.get();
    if (targetId.equals(sourceId)) {
        recordCollision(sourceId, ProvenanceCollisionKind.DUPLICATE_MERGE, sourceLocation, targetLocation);
        return true;
    }

    final boolean fullyAbsorbed = sourceRemaining.isEmpty() || sourceRemaining.getCount() <= 0;
    final UUID mergedId = birth(
        target,
        ProvenanceSource.MERGE,
        targetLocation,
        List.of(targetId, sourceId)
    ).orElseThrow();
    death(targetId, ProvenanceReason.MERGED, mergedId);
    if (fullyAbsorbed) {
        death(sourceId, ProvenanceReason.MERGED, mergedId);
    } else {
        transfer(sourceRemaining, sourceLocation);
    }
    return false;
}
```

`birth` writes the new stamp/live node before predecessor deaths; therefore `death(targetId, ...)` cannot remove the newly minted identity.

- [ ] **Step 6: Temporarily delegate old APIs so the tree stays compilable**

Until Task 3 migrates every vanilla hook, delegate using IDs still present on the stacks:

```java
return afterContainerMerge(
    targetSurvivor,
    sourceRemaining,
    StackStamp.readId(targetSurvivor),
    absorbedIdBefore,
    amountMoved,
    sourceLocation,
    targetLocation
);
```

Delegate `onMerge` with the count-zero absorbed stack and a positive sentinel amount (the transaction uses the amount only as a no-op guard):

```java
return afterContainerMerge(
    survivor,
    absorbed,
    StackStamp.readId(survivor),
    StackStamp.readId(absorbed),
    1,
    absorbedLocation,
    survivorLocation
);
```

Delegate `onInventoryMergeFullyAbsorbed` with an explicit empty remaining view:

```java
return afterContainerMerge(
    survivor,
    absorbed.copyWithCount(0),
    StackStamp.readId(survivor),
    StackStamp.readId(absorbed),
    absorbed.getCount(),
    absorbedLocation,
    survivorLocation
);
```

These delegates are migration scaffolding and are removed in Task 3; do not deprecate or expose them as compatibility APIs.

- [ ] **Step 7: Run the suite and commit the merge model**

Run the forced provenance suite. Expected: the full, partial, and same-ID tests pass; no existing collision tests regress.

```bash
git commit -m "feat(provenance): model merges as lineage nodes"
```

Stage only the API enum, engine, command, and their tests.

---

### Task 3: Migrate every merge boundary to the new contract

**Files:**
- Modify applied source: `paper-server/src/minecraft/java/net/minecraft/world/inventory/AbstractContainerMenu.java`
- Modify applied source: `paper-server/src/minecraft/java/net/minecraft/world/inventory/Slot.java`
- Modify applied source: `paper-server/src/minecraft/java/net/minecraft/world/entity/player/Inventory.java`
- Modify applied source: `paper-server/src/minecraft/java/net/minecraft/world/level/block/entity/HopperBlockEntity.java`
- Modify applied source: `paper-server/src/minecraft/java/net/minecraft/world/item/component/BundleContents.java`
- Modify applied source: `paper-server/src/minecraft/java/net/minecraft/world/entity/ai/behavior/TransportItemsBetweenContainers.java`
- Modify applied source: `paper-server/src/minecraft/java/net/minecraft/world/entity/item/ItemEntity.java`
- Modify: `paper-server/src/main/java/dev/mintychochip/provenance/ItemProvenance.java:656-798`
- Test: `paper-server/src/test/java/dev/mintychochip/provenance/CraftingMenuProvenanceTest.java`
- Rebuild corresponding patches under `paper-server/patches/sources/net/minecraft/world/**`

**Interfaces:**
- Consumes: seven-argument `ItemProvenance.afterContainerMerge` from Task 2.
- Produces: no old merge overloads; every materialized merge captures target and source UUIDs before mutation.

- [ ] **Step 1: Add a failing crafted-result merge test**

In `CraftingMenuProvenanceTest`, pre-populate the first quick-move destination with an older plank stack, then require both branches:

```java
@Test
public void shiftClickIntoExistingStackKeepsOldAndCraftHistories() {
    final ItemStack existing = new ItemStack(Items.OAK_PLANKS, 4);
    assertTrue(StackStamp.read(existing).isEmpty());
    this.inventory.setItem(8, existing); // CraftingMenu scans backward; hotbar slot 8 is first.
    final CraftingMenu menu = this.craftingMenuWithLogToPlanks();
    final UUID ingredientId = StackStamp.readId(menu.craftSlots.getItem(0)).orElseThrow();

    assertFalse(menu.quickMoveStack(this.player, CraftingMenu.RESULT_SLOT).isEmpty());

    final StackProvenance merged = StackStamp.read(this.inventory.getItem(8)).orElseThrow();
    assertEquals(ProvenanceSource.MERGE, merged.source());
    final List<LineageNode> lineage = ItemProvenance.explain(merged.id());
    assertTrue(lineage.stream().anyMatch(node -> node.source() == ProvenanceSource.LEGACY));
    assertTrue(lineage.stream().anyMatch(node -> node.source() == ProvenanceSource.CRAFT));
    assertTrue(lineage.stream().anyMatch(node -> node.id().equals(ingredientId)));
}
```

Add the matching-cursor path:

```java
@Test
public void cursorMergeKeepsOldAndCraftHistoriesWithoutZombie() {
    final CraftingMenu menu = this.craftingMenuWithLogToPlanks();
    final UUID ingredientId = StackStamp.readId(menu.craftSlots.getItem(0)).orElseThrow();
    menu.setCarried(new ItemStack(Items.OAK_PLANKS, 4));

    menu.clicked(CraftingMenu.RESULT_SLOT, 0, ClickType.PICKUP, this.player);

    final StackProvenance merged = StackStamp.read(menu.getCarried()).orElseThrow();
    assertEquals(ProvenanceSource.MERGE, merged.source());
    final List<LineageNode> lineage = ItemProvenance.explain(merged.id());
    assertTrue(lineage.stream().anyMatch(node -> node.source() == ProvenanceSource.LEGACY));
    assertTrue(lineage.stream().anyMatch(node -> node.source() == ProvenanceSource.CRAFT));
    assertTrue(lineage.stream().anyMatch(node -> node.id().equals(ingredientId)));
    assertTrue(ItemProvenance.audit().snapshot().stream()
        .noneMatch(event -> event.type() == ProvenanceEventType.ZOMBIE));
}
```

Run the suite. Expected: FAIL because the old quick-move hook captures only the crafted source UUID; the unstamped existing target has no pre-mutation UUID, so the migration delegate cannot create a two-parent `MERGE` node.

- [ ] **Step 2: Migrate cursor and quick-move merges**

Before each mutation in `AbstractContainerMenu`, capture both identities at unknown locations:

```java
final Optional<UUID> targetId = ItemProvenance.ensure(carried, StackLocation.unknown());
final Optional<UUID> sourceId = ItemProvenance.ensure(itemsTaken, StackLocation.unknown());
carried.grow(moved);
ItemProvenance.afterContainerMerge(
    carried,
    itemsTaken.copyWithCount(0),
    targetId,
    sourceId,
    moved,
    StackLocation.labeled("menu-slot:" + slot.index),
    StackLocation.playerSlot(player.getUUID(), -1)
);
```

Repeat in both `moveItemStackTo` merge branches using `target` and `itemStack` before `setCount`, `shrink`, or `grow`:

```java
final Optional<UUID> targetId = ItemProvenance.ensure(target, StackLocation.unknown());
final Optional<UUID> sourceId = ItemProvenance.ensure(itemStack, StackLocation.unknown());
```

Pass both captured IDs to the seven-argument method after mutation. Keep `isCheck` paths provenance-free.

- [ ] **Step 3: Migrate `Slot.safeInsert`**

Before `inputStack.shrink` and `slotStack.grow`, capture:

```java
final Optional<UUID> targetId = ItemProvenance.ensure(slotStack, StackLocation.unknown());
final Optional<UUID> sourceId = ItemProvenance.ensure(inputStack, StackLocation.unknown());
```

After mutation call:

```java
ItemProvenance.afterContainerMerge(
    slotStack,
    inputStack,
    targetId,
    sourceId,
    transferableItemCount,
    StackLocation.labeled("cursor"),
    StackLocation.labeled("menu-slot:" + this.index)
);
```

- [ ] **Step 4: Migrate player inventory and hopper merges**

In `Inventory.addResource`, capture both IDs only when `mergingIntoExisting`:

```java
final Optional<UUID> targetId = mergingIntoExisting
    ? ItemProvenance.ensure(itemStackInSlot, StackLocation.unknown())
    : Optional.empty();
final Optional<UUID> sourceId = mergingIntoExisting
    ? ItemProvenance.ensure(itemStack, StackLocation.unknown())
    : Optional.empty();
```

Pass them with `remainingView` after growth. In `HopperBlockEntity`, capture the same pair from `current` and `itemStack` before shrink/grow and pass them afterward. Empty-slot transfers remain `transfer`, not `MERGE`.

- [ ] **Step 5: Migrate bundle and copper-golem merges**

For `BundleContents.Mutable.tryInsert`, capture the target from `removedStack` and source from `itemsToAdd` before constructing `mergedStack`:

```java
final Optional<UUID> targetId = ItemProvenance.ensure(removedStack, StackLocation.unknown());
final Optional<UUID> sourceId = ItemProvenance.ensure(itemsToAdd, StackLocation.unknown());
```

Pass `mergedStack`, remaining `itemsToAdd`, and both IDs after shrink.

For `TransportItemsBetweenContainers`, capture from `containerItemStack` and `itemStack` before count changes. Pass both IDs after `container.setItem`. Preserve vanilla’s existing count calculation; provenance records `countToAdd` and must not change gameplay counts.

- [ ] **Step 6: Make full and partial item-entity merges use the same transaction**

Replace the full-only `onMerge` branch with pre/post accounting:

```java
final Optional<UUID> targetId = ItemProvenance.ensure(toStack, StackLocation.unknown());
final Optional<UUID> sourceId = ItemProvenance.ensure(fromStack, StackLocation.unknown());
final int sourceCountBefore = fromStack.getCount();
merge(toItem, toStack, fromStack);
final int moved = sourceCountBefore - fromStack.getCount();
if (moved > 0) {
    ItemProvenance.afterContainerMerge(
        toItem.getItem(),
        fromStack,
        targetId,
        sourceId,
        moved,
        StackLocation.itemEntity(fromItem.getUUID()),
        StackLocation.itemEntity(toItem.getUUID())
    );
}
```

Then preserve pickup-delay/age updates and discard `fromItem` only when its stack is empty. Remove the old `noteConsumed` partial branch.

- [ ] **Step 7: Remove obsolete merge APIs and update all tests**

Delete:

```java
onMerge(...)
onInventoryMergeFullyAbsorbed(...)
afterContainerMerge(target, source, absorbedIdBefore, amountMoved, sourceLocation, targetLocation)
afterContainerMerge(target, source, amountMoved, sourceLocation, targetLocation)
```

Search for all three old call shapes and ensure no caller remains. Update `ItemProvenanceTest` to use only the seven-argument method.

- [ ] **Step 8: Rebuild all touched source patches**

Run `fixupSourcePatches` and `rebuildPatches`. Confirm rebuilt patches exist only for the seven listed vanilla classes plus the four crafting classes from Task 1; do not rewrite unrelated Paper feature patches.

- [ ] **Step 9: Run the complete provenance suite and commit the cutover**

Run the forced suite. Expected: all craft, full merge, partial merge, persistence, placement, and collision tests pass.

```bash
git commit -m "feat(provenance): preserve lineage across stack merges"
```

Stage the rebuilt merge patches, `ItemProvenance.java`, and behavior tests only.

---

### Task 4: Prove persistence, inspection, and packaged-server compatibility

**Files:**
- Modify: `paper-server/src/test/java/dev/mintychochip/provenance/ProvenancePersistenceTest.java`
- Modify: `docs/superpowers/specs/2026-08-07-item-provenance-design.md:17-32,56-111`
- Verify: all files changed by Tasks 1–3

**Interfaces:**
- Consumes: `ProvenanceSource.MERGE`, seven-argument merge transaction, repository writer.
- Produces: durable merge ancestry and user-facing inspect direction consistent with the implementation.

- [ ] **Step 1: Add durable merge-lineage compatibility coverage**

```java
@Test
public void mergeSourceAndParentsSurviveRepositoryReload() throws Exception {
    ProvenanceWriter.install(tempDir, message -> {
    });
    final ItemStack target = new ItemStack(Items.IRON_INGOT, 4);
    final ItemStack source = new ItemStack(Items.IRON_INGOT, 2);
    final StackLocation chest = StackLocation.labeled("chest");
    final UUID targetId = ItemProvenance.birth(target, ProvenanceSource.SMELT, HAND).orElseThrow();
    final UUID sourceId = ItemProvenance.birth(source, ProvenanceSource.CRAFT, chest).orElseThrow();
    final Optional<UUID> targetBefore = StackStamp.readId(target);
    final Optional<UUID> sourceBefore = StackStamp.readId(source);
    source.setCount(0);
    target.grow(2);
    ItemProvenance.afterContainerMerge(target, source, targetBefore, sourceBefore, 2, chest, HAND);
    final UUID mergedId = StackStamp.readId(target).orElseThrow();

    ProvenanceWriter.flushAndClose();
    ProvenanceWriter.clearInstall();
    try (ProvenanceRepository repository = new ProvenanceRepository(
        tempDir.resolve("mintychochip/provenance.db")
    )) {
        final LineageNode stored = repository.loadLineage(mergedId).orElseThrow();
        assertEquals(ProvenanceSource.MERGE, stored.source());
        assertEquals(List.of(targetId, sourceId), stored.parents());
    }
}
```


Run this test through `ProvenanceTestSuite`. Expected: PASS because `StackStamp` and `ProvenanceRepository` serialize enum names generically. Retain the test as compatibility coverage; do not change serialization code unless the observed result contradicts that contract.

- [ ] **Step 2: Update the implemented provenance design**

Change the rule “merging preserves survivor identity” to:

```text
Different-UUID merges mint a MERGE identity with both predecessor UUIDs as parents;
full merges death both predecessors, partial merges keep the source remainder live.
Same-UUID merges remain DUPLICATE_MERGE collisions.
```

Update the vanilla-hook table entries for `Inventory`, `Slot`, `AbstractContainerMenu`, `HopperBlockEntity`, `BundleContents`, `TransportItemsBetweenContainers`, and `ItemEntity` to state that they call the shared merge transaction.

- [ ] **Step 3: Run final static and behavioral verification**

Run:

```bash
./gradlew :paper-server:cleanTest :paper-server:test --tests 'org.bukkit.support.suite.ProvenanceTestSuite'
./gradlew createPaperclipJar
```

Expected: zero test failures and a successful Alkahest paperclip jar build.

- [ ] **Step 4: Smoke-start the packaged server**

Use the harness process manager, not a background shell. Start the produced paperclip jar in the existing `run/` work directory with `--nogui`, wait for the normal server-ready log line, then stop it gracefully. Confirm there is no enum deserialization, patch, provenance bootstrap, or command-registration error.

The menu behavior itself is exercised by `CraftingMenuProvenanceTest`; server boot proves the packaged API/server enum and persistence wiring load together.

- [ ] **Step 5: Review, partition, and commit final persistence/docs work**

Inventory staged and unstaged changes. Keep unrelated heap dumps and helper directories untracked. Commit compatibility coverage separately:

```bash
git commit -m "test(provenance): verify durable merge lineage"
```

Then commit the implemented-design update:

```bash
git commit -m "docs(provenance): document merge lineage semantics"
```
