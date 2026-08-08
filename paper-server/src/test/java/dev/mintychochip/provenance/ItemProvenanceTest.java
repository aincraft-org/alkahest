package dev.mintychochip.provenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bukkit.support.environment.Normal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Core provenance engine: birth → transform lineage → synthetic dupe.
 */
@Normal
public class ItemProvenanceTest {

    private static final UUID PLAYER = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final StackLocation HAND = StackLocation.playerSlot(PLAYER, 0);
    private static final StackLocation CHEST = StackLocation.labeled("container:test");

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
    public void birthStampsUuidAndRegistersLive() {
        final ItemStack cobble = new ItemStack(Items.COBBLESTONE, 3);
        final Optional<UUID> id = ItemProvenance.birth(cobble, ProvenanceSource.BLOCK_DROP, HAND);
        assertTrue(id.isPresent());
        assertEquals(id.get(), StackStamp.readId(cobble).orElseThrow());
        assertTrue(ItemProvenance.live().contains(id.get()));
        assertEquals(1, ItemProvenance.live().size());
        assertEquals(ProvenanceSource.BLOCK_DROP, StackStamp.read(cobble).orElseThrow().source());
        assertEquals(HAND, ItemProvenance.live().get(id.get()).orElseThrow().location());
    }

    @Test
    public void copyPreservesUuid() {
        final ItemStack cobble = new ItemStack(Items.COBBLESTONE, 3);
        final UUID id = ItemProvenance.birth(cobble, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();
        final ItemStack copy = cobble.copy();
        assertEquals(id, StackStamp.readId(copy).orElseThrow());
    }

    @Test
    public void partialSplitMintsChildWithParentLink() {
        final ItemStack cobble = new ItemStack(Items.COBBLESTONE, 3);
        final UUID parentId = ItemProvenance.birth(cobble, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();

        final ItemStack child = cobble.split(1);
        assertEquals(2, cobble.getCount());
        assertEquals(1, child.getCount());
        assertEquals(parentId, StackStamp.readId(cobble).orElseThrow());

        final UUID childId = StackStamp.readId(child).orElseThrow();
        assertNotEquals(parentId, childId);
        assertEquals(List.of(parentId), StackStamp.read(child).orElseThrow().parents());
        assertEquals(ProvenanceSource.SPLIT, StackStamp.read(child).orElseThrow().source());
        assertEquals(2, ItemProvenance.live().size());
    }

    @Test
    public void fullSplitMovesIdentity() {
        final ItemStack cobble = new ItemStack(Items.COBBLESTONE, 3);
        final UUID id = ItemProvenance.birth(cobble, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();
        final ItemStack taken = cobble.split(3);
        assertTrue(cobble.isEmpty());
        assertEquals(id, StackStamp.readId(taken).orElseThrow());
    }

    @Test
    public void onCraftedIsIdempotentWhenAlreadyCraftStamped() {
        // Shift-click stamps before moveItemStackTo; onTake must not mint a second CRAFT id.
        final ItemStack sticks = new ItemStack(Items.STICK, 4);
        final UUID stickId = ItemProvenance.birth(sticks, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();
        final ItemStack planks = new ItemStack(Items.OAK_PLANKS, 4);
        ItemProvenance.onCrafted(planks, List.of(stickId), HAND);
        final UUID firstId = StackStamp.readId(planks).orElseThrow();
        assertEquals(ProvenanceSource.CRAFT, StackStamp.read(planks).orElseThrow().source());

        ItemProvenance.onCrafted(planks, List.of(stickId), HAND);
        assertEquals(firstId, StackStamp.readId(planks).orElseThrow());
        assertEquals(ProvenanceSource.CRAFT, StackStamp.read(planks).orElseThrow().source());
        assertEquals(List.of(stickId), StackStamp.read(planks).orElseThrow().parents());
    }

    @Test
    public void unstampedResultBecomesCraftNotLegacy() {
        // Simulates the craft path for a fresh recipe result (no prior LEGACY stamp).
        final ItemStack log = new ItemStack(Items.OAK_LOG, 1);
        final UUID logId = ItemProvenance.birth(log, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();
        final ItemStack planks = new ItemStack(Items.OAK_PLANKS, 4); // unstamped recipe output
        assertTrue(StackStamp.read(planks).isEmpty());

        ItemProvenance.onCrafted(planks, List.of(logId), HAND);
        assertEquals(ProvenanceSource.CRAFT, StackStamp.read(planks).orElseThrow().source());
        assertNotEquals(ProvenanceSource.LEGACY, StackStamp.read(planks).orElseThrow().source());
        assertEquals(List.of(logId), StackStamp.read(planks).orElseThrow().parents());
    }

    @Test
    public void repeatedCraftHookDoesNotReviveMergedIdentity() {
        final ItemStack ingredient = new ItemStack(Items.OAK_LOG, 1);
        final UUID ingredientId = ItemProvenance.birth(
            ingredient,
            ProvenanceSource.BLOCK_DROP,
            HAND
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

    @Test
    public void cobbleCraftedIntoPickaxeKeepsLineageThenDupeIsDetected() {
        final ItemStack cobble = new ItemStack(Items.COBBLESTONE, 3);
        final UUID cobbleId = ItemProvenance.birth(cobble, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();

        final ItemStack c1 = cobble.split(1);
        final ItemStack c2 = cobble.split(1);
        final ItemStack c3 = cobble;
        assertEquals(1, c1.getCount());
        assertEquals(1, c2.getCount());
        assertEquals(1, c3.getCount());

        final ItemStack sticks = new ItemStack(Items.STICK, 2);
        final UUID stickId = ItemProvenance.birth(sticks, ProvenanceSource.CRAFT, HAND).orElseThrow();

        final List<UUID> parents = ItemProvenance.collectParents(List.of(c1, c2, c3, sticks));
        assertTrue(parents.contains(StackStamp.readId(c1).orElseThrow()));
        assertTrue(parents.contains(stickId));

        final ItemStack pickaxe = new ItemStack(Items.STONE_PICKAXE, 1);
        ItemProvenance.onCrafted(pickaxe, parents, HAND);
        final UUID pickId = StackStamp.readId(pickaxe).orElseThrow();
        assertNotEquals(cobbleId, pickId);
        assertEquals(ProvenanceSource.CRAFT, StackStamp.read(pickaxe).orElseThrow().source());

        for (final ItemStack piece : List.of(c1, c2, c3)) {
            final UUID pieceId = StackStamp.readId(piece).orElseThrow();
            ItemProvenance.death(pieceId, ProvenanceReason.CONSUMED, pickId);
        }
        ItemProvenance.death(stickId, ProvenanceReason.CONSUMED, pickId);

        final List<LineageNode> lineage = ItemProvenance.explain(pickId);
        assertFalse(lineage.isEmpty());
        assertEquals(pickId, lineage.getFirst().id());
        assertEquals("minecraft:stone_pickaxe", lineage.getFirst().itemId());
        final boolean seesCobbleAncestor = lineage.stream()
            .anyMatch(n -> n.itemId().equals("minecraft:cobblestone"));
        assertTrue(seesCobbleAncestor, "lineage should include cobblestone ancestors: " + lineage);
        assertTrue(
            ItemProvenance.explainText(pickId).contains("cobblestone"),
            ItemProvenance.explainText(pickId)
        );

        assertTrue(
            ItemProvenance.simulateDupe(pickaxe, HAND, StackLocation.playerSlot(PLAYER, 1)),
            "synthetic dupe must record COLLISION"
        );
        assertFalse(ItemProvenance.collisions().isEmpty());
        assertEquals(pickId, ItemProvenance.collisions().getFirst().id());
        assertEquals(ProvenanceCollisionKind.DUPLICATE_LOCATION, ItemProvenance.collisions().getFirst().kind());

        final boolean collisionAudited = ItemProvenance.audit().snapshot().stream()
            .anyMatch(e -> e.type() == ProvenanceEventType.COLLISION && e.id().equals(pickId));
        assertTrue(collisionAudited);
    }

    @Test
    public void mergeAbsorbsUuid() {
        final ItemStack a = new ItemStack(Items.COBBLESTONE, 10);
        final ItemStack b = new ItemStack(Items.COBBLESTONE, 5);
        final UUID idA = ItemProvenance.birth(a, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();
        final UUID idB = ItemProvenance.birth(b, ProvenanceSource.BLOCK_DROP, CHEST).orElseThrow();

        a.grow(b.getCount());
        b.setCount(0);
        ItemProvenance.onMerge(a, b, HAND, CHEST);

        assertTrue(ItemProvenance.live().contains(idA));
        assertFalse(ItemProvenance.live().contains(idB));
        assertTrue(ItemProvenance.lineage().get(idB).orElseThrow().dead());
        assertTrue(ItemProvenance.collisions().isEmpty());
    }

    @Test
    public void stacksWithDifferentProvenanceStillStackable() {
        final ItemStack a = new ItemStack(Items.COBBLESTONE, 1);
        final ItemStack b = new ItemStack(Items.COBBLESTONE, 1);
        ItemProvenance.birth(a, ProvenanceSource.BLOCK_DROP, HAND);
        ItemProvenance.birth(b, ProvenanceSource.BLOCK_DROP, CHEST);
        assertNotEquals(StackStamp.readId(a), StackStamp.readId(b));
        assertTrue(ItemStack.isSameItemSameComponents(a, b), "provenance must not block stacking");
    }

    @Test
    public void stampSurvivesCopyAndClearPath() {
        final ItemStack a = new ItemStack(Items.DIAMOND, 1);
        final UUID id = ItemProvenance.birth(a, ProvenanceSource.GIVE, HAND).orElseThrow();
        final ItemStack moved = a.copyAndClear();
        assertEquals(id, StackStamp.readId(moved).orElseThrow());
    }

    @Test
    public void entityCarriedDropRecoversParentLineage() {
        final UUID minedId = ItemProvenance.birth(
            new ItemStack(Items.COBBLESTONE, 1),
            ProvenanceSource.BLOCK_DROP,
            HAND
        ).orElseThrow();
        final UUID entityId = UUID.randomUUID();
        ItemProvenance.putCarriedForTest(
            entityId,
            new PlacementRecord(minedId, "minecraft:cobblestone", "miner", System.currentTimeMillis())
        );

        final ItemStack drop = new ItemStack(Items.COBBLESTONE, 1);
        ItemProvenance.releaseCarriedOntoStack(entityId, drop);

        final StackProvenance stamp = StackStamp.read(drop).orElseThrow();
        assertEquals(ProvenanceSource.BLOCK_RECOVER, stamp.source());
        assertEquals(List.of(minedId), stamp.parents());
        assertTrue(ItemProvenance.explain(stamp.id()).stream().anyMatch(n -> n.id().equals(minedId)));
    }

    @Test
    public void inventoryFullMergeDeathsAbsorbedUuid() {
        final ItemStack a = new ItemStack(Items.DIRT, 32);
        final ItemStack b = new ItemStack(Items.DIRT, 16);
        final UUID idA = ItemProvenance.birth(a, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();
        final UUID idB = ItemProvenance.birth(b, ProvenanceSource.BLOCK_DROP, CHEST).orElseThrow();
        a.grow(b.getCount());
        ItemProvenance.onInventoryMergeFullyAbsorbed(a, b, HAND, CHEST);
        assertTrue(ItemProvenance.live().contains(idA));
        assertFalse(ItemProvenance.live().contains(idB));
        assertTrue(ItemProvenance.lineage().get(idB).orElseThrow().dead());
    }

    @Test
    public void afterContainerMergeWorksForHopperStyleFullAbsorb() {
        final ItemStack chest = new ItemStack(Items.COBBLESTONE, 40);
        final ItemStack hopper = new ItemStack(Items.COBBLESTONE, 24);
        final UUID chestId = ItemProvenance.birth(chest, ProvenanceSource.BLOCK_DROP, CHEST).orElseThrow();
        final UUID hopId = ItemProvenance.birth(hopper, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();

        final int moved = 24;
        final Optional<UUID> absorbed = StackStamp.readId(hopper);
        hopper.shrink(moved);
        chest.grow(moved);
        ItemProvenance.afterContainerMerge(chest, hopper, absorbed, moved, HAND, CHEST);

        assertTrue(hopper.isEmpty() || hopper.getCount() == 0);
        assertEquals(64, chest.getCount());
        assertTrue(ItemProvenance.live().contains(chestId));
        assertFalse(ItemProvenance.live().contains(hopId));
        assertTrue(ItemProvenance.lineage().get(hopId).orElseThrow().dead());
        assertTrue(ItemProvenance.collisions().isEmpty());
    }

    @Test
    public void afterContainerMergePartialKeepsBothLive() {
        final ItemStack a = new ItemStack(Items.COBBLESTONE, 50);
        final ItemStack b = new ItemStack(Items.COBBLESTONE, 30);
        final UUID idA = ItemProvenance.birth(a, ProvenanceSource.BLOCK_DROP, CHEST).orElseThrow();
        final UUID idB = ItemProvenance.birth(b, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();

        final int moved = 14;
        final Optional<UUID> absorbed = StackStamp.readId(b);
        b.shrink(moved);
        a.grow(moved);
        ItemProvenance.afterContainerMerge(a, b, absorbed, moved, HAND, CHEST);

        assertEquals(64, a.getCount());
        assertEquals(16, b.getCount());
        assertTrue(ItemProvenance.live().contains(idA));
        assertTrue(ItemProvenance.live().contains(idB));
        assertTrue(ItemProvenance.collisions().isEmpty());
    }

    @Test
    public void pistonMoveKeepsPlacementMemoryAtNewPos() {
        final ItemStack cobble = new ItemStack(Items.COBBLESTONE, 1);
        final UUID minedId = ItemProvenance.birth(cobble, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();
        final String dim = "minecraft:overworld";
        final net.minecraft.core.BlockPos from = new net.minecraft.core.BlockPos(0, 64, 0);
        final net.minecraft.core.BlockPos to = new net.minecraft.core.BlockPos(1, 64, 0);

        ItemProvenance.placements().put(
            dim,
            from,
            new PlacementRecord(minedId, "minecraft:cobblestone", "miner", System.currentTimeMillis())
        );
        ItemProvenance.movePlacement(dim, from, to);

        assertTrue(ItemProvenance.placements().get(dim, from).isEmpty());
        assertTrue(ItemProvenance.placements().get(dim, to).isPresent());
        assertEquals(minedId, ItemProvenance.placements().get(dim, to).orElseThrow().parentStackId());

        final ItemStack drop = new ItemStack(Items.COBBLESTONE, 1);
        ItemProvenance.stampBlockDrop(dim, to, drop);
        assertEquals(ProvenanceSource.BLOCK_RECOVER, StackStamp.read(drop).orElseThrow().source());
        assertEquals(List.of(minedId), StackStamp.read(drop).orElseThrow().parents());
    }

    @Test
    public void placementRecordCodecRoundTrip() {
        final PlacementRecord original = new PlacementRecord(
            UUID.randomUUID(),
            "minecraft:cobblestone",
            "player:test",
            123456789L
        );
        final ProvenancePlacementsData data = new ProvenancePlacementsData();
        data.put(new net.minecraft.core.BlockPos(1, 2, 3), original);
        final var tagResult = ProvenancePlacementsData.CODEC.encodeStart(
            net.minecraft.nbt.NbtOps.INSTANCE,
            data
        );
        assertTrue(tagResult.isSuccess(), () -> tagResult.error().map(Object::toString).orElse("encode failed"));
        final var decoded = ProvenancePlacementsData.CODEC.parse(
            net.minecraft.nbt.NbtOps.INSTANCE,
            tagResult.getOrThrow()
        );
        assertTrue(decoded.isSuccess());
        final ProvenancePlacementsData back = decoded.getOrThrow();
        final PlacementRecord got = back.get(new net.minecraft.core.BlockPos(1, 2, 3)).orElseThrow();
        assertEquals(original.parentStackId(), got.parentStackId());
        assertEquals(original.blockItemId(), got.blockItemId());
        assertEquals(original.placer(), got.placer());
        assertEquals(original.placedEpochMs(), got.placedEpochMs());
    }

    @Test
    public void placeThenBreakRecoversParentLineageNotCleanBirth() {
        final ItemStack mined = new ItemStack(Items.COBBLESTONE, 1);
        final UUID minedId = ItemProvenance.birth(mined, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();

        final net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(10, 64, -3);
        final String dim = "minecraft:overworld";
        ItemProvenance.placements().put(
            dim,
            pos,
            new PlacementRecord(minedId, "minecraft:cobblestone", "miner", System.currentTimeMillis())
        );

        final ItemStack drop = new ItemStack(Items.COBBLESTONE, 1);
        ItemProvenance.stampBlockDrop(dim, pos, drop);
        ItemProvenance.placements().remove(dim, pos);

        final StackProvenance stamp = StackStamp.read(drop).orElseThrow();
        assertEquals(ProvenanceSource.BLOCK_RECOVER, stamp.source());
        assertEquals(List.of(minedId), stamp.parents());
        assertNotEquals(minedId, stamp.id());

        final List<LineageNode> lineage = ItemProvenance.explain(stamp.id());
        assertTrue(
            lineage.stream().anyMatch(n -> n.id().equals(minedId)),
            "recover drop must keep mined cobble in ancestry: " + lineage
        );

        final ItemStack natural = new ItemStack(Items.COBBLESTONE, 1);
        ItemProvenance.stampBlockDrop(dim, pos, natural);
        assertEquals(ProvenanceSource.BLOCK_DROP, StackStamp.read(natural).orElseThrow().source());
    }

    @Test
    public void birthIfAbsentDoesNotDoubleMint() {
        final ItemStack stack = new ItemStack(Items.IRON_INGOT, 1);
        final UUID first = ItemProvenance.birth(stack, ProvenanceSource.SMELT, CHEST).orElseThrow();
        final Optional<UUID> second = ItemProvenance.birthIfAbsent(stack, ProvenanceSource.LOOT, HAND);
        assertEquals(first, second.orElseThrow());
        assertEquals(1, ItemProvenance.live().size());
    }

    @Test
    public void smeltAndTradeSourcesRecordTransform() {
        final ItemStack ore = new ItemStack(Items.IRON_ORE, 1);
        final UUID oreId = ItemProvenance.birth(ore, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();
        final ItemStack ingot = new ItemStack(Items.IRON_INGOT, 1);
        ItemProvenance.onSmelted(ingot, oreId, CHEST);
        assertEquals(ProvenanceSource.SMELT, StackStamp.read(ingot).orElseThrow().source());
        assertEquals(List.of(oreId), StackStamp.read(ingot).orElseThrow().parents());

        final ItemStack payment = new ItemStack(Items.EMERALD, 1);
        final UUID payId = ItemProvenance.birth(payment, ProvenanceSource.GIVE, HAND).orElseThrow();
        final ItemStack result = new ItemStack(Items.DIAMOND, 1);
        ItemProvenance.onTrade(result, List.of(payId), HAND);
        assertEquals(ProvenanceSource.TRADE, StackStamp.read(result).orElseThrow().source());
        assertTrue(ItemProvenance.explain(StackStamp.readId(result).orElseThrow()).size() >= 1);
    }

    @Test
    public void rehydrateDoesNotCreateSecondBirth() {
        final ItemStack a = new ItemStack(Items.DIRT, 1);
        final UUID id = ItemProvenance.birth(a, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();
        ItemProvenance.live().remove(id);
        ItemProvenance.rehydrate(a, StackLocation.unknown());
        assertEquals(1, ItemProvenance.live().size());
        assertEquals(id, StackStamp.readId(a).orElseThrow());
        final long births = ItemProvenance.audit().snapshot().stream()
            .filter(e -> e.type() == ProvenanceEventType.BIRTH)
            .count();
        final long rehydrates = ItemProvenance.audit().snapshot().stream()
            .filter(e -> e.type() == ProvenanceEventType.REHYDRATE)
            .count();
        assertEquals(1, births);
        assertEquals(1, rehydrates);
    }

    @Test
    public void specialRecipeTransformRecordsParentsAndIngredientDeath() {
        final ItemStack base = new ItemStack(Items.IRON_SWORD, 1);
        final ItemStack addition = new ItemStack(Items.ENCHANTED_BOOK, 1);
        final UUID baseId = ItemProvenance.birth(base, ProvenanceSource.CRAFT, HAND).orElseThrow();
        final UUID bookId = ItemProvenance.birth(addition, ProvenanceSource.LOOT, HAND).orElseThrow();

        final ItemStack result = new ItemStack(Items.IRON_SWORD, 1);
        final List<UUID> parents = ItemProvenance.collectParents(List.of(base, addition));
        ItemProvenance.onSpecialRecipe(result, parents, HAND);

        final UUID resultId = StackStamp.readId(result).orElseThrow();
        assertEquals(ProvenanceSource.SPECIAL_RECIPE, StackStamp.read(result).orElseThrow().source());
        assertTrue(StackStamp.read(result).orElseThrow().parents().contains(baseId));
        assertTrue(StackStamp.read(result).orElseThrow().parents().contains(bookId));
        assertTrue(ItemProvenance.live().contains(resultId));

        ItemProvenance.death(baseId, ProvenanceReason.CONSUMED, resultId);
        ItemProvenance.death(bookId, ProvenanceReason.CONSUMED, resultId);
        assertFalse(ItemProvenance.live().contains(baseId));
        assertFalse(ItemProvenance.live().contains(bookId));
        assertTrue(ItemProvenance.lineage().get(baseId).orElseThrow().dead());
        ItemProvenance.death(baseId, ProvenanceReason.CONSUMED, null);
        assertTrue(ItemProvenance.lineage().get(baseId).orElseThrow().dead());
    }

    @Test
    public void afterConsumeAndOnBrokenRemoveFromLiveCensus() {
        final ItemStack food = new ItemStack(Items.APPLE, 1);
        final UUID foodId = ItemProvenance.birth(food, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();
        food.setCount(0);
        ItemProvenance.afterConsume(food);
        assertFalse(ItemProvenance.live().contains(foodId));
        assertEquals(ProvenanceReason.CONSUMED, ItemProvenance.lineage().get(foodId).orElseThrow().deathReason());

        final ItemStack pick = new ItemStack(Items.IRON_PICKAXE, 1);
        final UUID pickId = ItemProvenance.birth(pick, ProvenanceSource.CRAFT, HAND).orElseThrow();
        pick.setCount(0);
        ItemProvenance.onBroken(pick);
        assertFalse(ItemProvenance.live().contains(pickId));
        assertEquals(ProvenanceReason.DESTROYED, ItemProvenance.lineage().get(pickId).orElseThrow().deathReason());
    }

    @Test
    public void identityHandoffLinksBucketAndCraftRemainderParents() {
        final ItemStack empty = new ItemStack(Items.BUCKET, 1);
        final UUID emptyId = ItemProvenance.birth(empty, ProvenanceSource.CRAFT, HAND).orElseThrow();
        final ItemStack water = new ItemStack(Items.WATER_BUCKET, 1);
        final UUID waterId = ItemProvenance.onIdentityHandoff(empty, water, HAND).orElseThrow();
        assertNotEquals(emptyId, waterId);
        assertEquals(ProvenanceSource.SPECIAL_RECIPE, StackStamp.read(water).orElseThrow().source());
        assertEquals(List.of(emptyId), StackStamp.read(water).orElseThrow().parents());

        empty.setCount(0);
        ItemProvenance.afterConsume(empty);
        assertFalse(ItemProvenance.live().contains(emptyId));
        assertTrue(ItemProvenance.live().contains(waterId));
        assertTrue(ItemProvenance.explain(waterId).stream().anyMatch(n -> n.id().equals(emptyId)));

        final ItemStack milk = new ItemStack(Items.MILK_BUCKET, 1);
        final UUID milkId = ItemProvenance.birth(milk, ProvenanceSource.GIVE, HAND).orElseThrow();
        final ItemStack bowl = new ItemStack(Items.BUCKET, 1);
        ItemProvenance.onIdentityHandoff(bowl, List.of(milkId), HAND);
        assertEquals(List.of(milkId), StackStamp.read(bowl).orElseThrow().parents());
        assertEquals(ProvenanceSource.SPECIAL_RECIPE, StackStamp.read(bowl).orElseThrow().source());
    }

    @Test
    public void parkedAndSecondaryBirthEnsureIdentity() {
        final ItemStack disc = new ItemStack(Items.MUSIC_DISC_CAT, 1);
        assertTrue(ItemProvenance.of(disc).isEmpty());
        ItemProvenance.onParked(disc, StackLocation.labeled("jukebox:0,0,0"));
        assertTrue(ItemProvenance.of(disc).isPresent());
        assertEquals(ProvenanceSource.LEGACY, StackStamp.read(disc).orElseThrow().source());
        assertTrue(ItemProvenance.live().contains(StackStamp.readId(disc).orElseThrow()));

        final ItemStack boneMeal = new ItemStack(Items.BONE_MEAL, 1);
        ItemProvenance.birthIfAbsent(boneMeal, ProvenanceSource.LOOT, StackLocation.labeled("composter"));
        assertEquals(ProvenanceSource.LOOT, StackStamp.read(boneMeal).orElseThrow().source());
        assertTrue(ItemProvenance.live().contains(StackStamp.readId(boneMeal).orElseThrow()));
    }

    @Test
    public void smeltedAccumulateKeepsEveryInputParent() {
        final ItemStack ore1 = new ItemStack(Items.IRON_ORE, 1);
        final UUID ore1Id = ItemProvenance.birth(ore1, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();
        final ItemStack ore2 = new ItemStack(Items.IRON_ORE, 1);
        final UUID ore2Id = ItemProvenance.birth(ore2, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();
        final StackLocation furnace = StackLocation.labeled("furnace:0,64,0");

        final ItemStack ingot = new ItemStack(Items.IRON_INGOT, 1);
        ItemProvenance.onSmelted(ingot, ore1Id, furnace);
        final UUID ingotId = StackStamp.readId(ingot).orElseThrow();
        ingot.grow(1);
        ItemProvenance.onSmeltedAccumulate(ingot, ore2Id, furnace);

        final List<UUID> parents = StackStamp.read(ingot).orElseThrow().parents();
        assertTrue(parents.contains(ore1Id), "accumulated output keeps first parent: " + parents);
        assertTrue(parents.contains(ore2Id), "accumulated output keeps second parent: " + parents);
        assertTrue(ItemProvenance.explain(ingotId).stream().anyMatch(n -> n.id().equals(ore2Id)));
    }

    @Test
    public void mintChildCreatesDistinctIdentity() {
        final ItemStack source = new ItemStack(Items.COBBLESTONE, 12);
        final UUID parentId = ItemProvenance.birth(source, ProvenanceSource.LOOT, HAND).orElseThrow();
        final ItemStack destination = source.copyWithCount(4);

        final Optional<UUID> childId = ItemProvenance.mintChild(destination, parentId, 8);

        assertTrue(childId.isPresent());
        assertFalse(childId.get().equals(parentId));
        assertEquals(parentId, StackStamp.read(destination).orElseThrow().parents().getFirst());
        assertEquals(ProvenanceSource.SPLIT, StackStamp.read(destination).orElseThrow().source());
        assertEquals(8, ItemProvenance.live().get(parentId).orElseThrow().count());
    }

    @Test
    public void explosionClearsPlacementAtEveryDestroyedPosition() {
        final ItemStack stack = new ItemStack(Items.COBBLESTONE, 1);
        final UUID placedId = ItemProvenance.birth(stack, ProvenanceSource.BLOCK_DROP, HAND).orElseThrow();
        final String dim = "minecraft:overworld";
        final net.minecraft.core.BlockPos a = new net.minecraft.core.BlockPos(20, 64, 0);
        final net.minecraft.core.BlockPos b = new net.minecraft.core.BlockPos(21, 64, 0);
        ItemProvenance.placements().put(dim, a, new PlacementRecord(placedId, "minecraft:cobblestone", "miner", 1L));
        ItemProvenance.placements().put(dim, b, new PlacementRecord(placedId, "minecraft:cobblestone", "miner", 1L));

        // What the ServerExplosion hook does for each destroyed position.
        ItemProvenance.placements().remove(dim, a);
        ItemProvenance.placements().remove(dim, b);

        assertTrue(ItemProvenance.placements().get(dim, a).isEmpty(), "destroyed position A must lose placement memory");
        assertTrue(ItemProvenance.placements().get(dim, b).isEmpty(), "destroyed position B must lose placement memory");

        // A later natural break must not recover the placed identity as BLOCK_RECOVER.
        final ItemStack natural = new ItemStack(Items.COBBLESTONE, 1);
        ItemProvenance.stampBlockDrop(dim, a, natural);
        assertEquals(ProvenanceSource.BLOCK_DROP, StackStamp.read(natural).orElseThrow().source());
    }

    @Test
    public void transferNeverCollidesAndMovesLocation() {
        final ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        final StackLocation slot = StackLocation.playerSlot(PLAYER, 0);
        final StackLocation entity = StackLocation.itemEntity(UUID.randomUUID());
        ItemProvenance.birth(stack, ProvenanceSource.LOOT, slot).orElseThrow();

        assertFalse(ItemProvenance.transfer(stack, entity));
        assertFalse(ItemProvenance.transfer(stack, slot));
        assertTrue(ItemProvenance.collisions().isEmpty());
        assertEquals(slot, ItemProvenance.live().get(StackStamp.readId(stack).orElseThrow()).orElseThrow().location());
    }

    @Test
    public void identityHandoffAfterDeathDoesNotZombieReviveParent() {
        final ItemStack stew = new ItemStack(Items.MUSHROOM_STEW, 1);
        final UUID stewId = ItemProvenance.birth(stew, ProvenanceSource.CRAFT, HAND).orElseThrow();
        final ItemStack stackBeforeUsing = stew.copy();
        assertEquals(stewId, StackStamp.readId(stackBeforeUsing).orElseThrow());

        stew.setCount(0);
        ItemProvenance.afterConsume(stew);
        assertFalse(ItemProvenance.live().contains(stewId), "parent must leave live census");
        assertTrue(ItemProvenance.lineage().get(stewId).orElseThrow().dead());

        final ItemStack bowl = new ItemStack(Items.BOWL, 1);
        final UUID bowlId = ItemProvenance.onIdentityHandoff(stackBeforeUsing, bowl, HAND).orElseThrow();
        assertNotEquals(stewId, bowlId);
        assertEquals(ProvenanceSource.SPECIAL_RECIPE, StackStamp.read(bowl).orElseThrow().source());
        assertEquals(List.of(stewId), StackStamp.read(bowl).orElseThrow().parents());
        assertTrue(ItemProvenance.live().contains(bowlId));
        assertFalse(ItemProvenance.live().contains(stewId), "handoff must not rehydrate dead parent");
        assertTrue(ItemProvenance.lineage().get(stewId).orElseThrow().dead());
        final long zombies = ItemProvenance.audit().snapshot().stream()
            .filter(e -> e.type() == ProvenanceEventType.ZOMBIE)
            .count();
        assertEquals(0, zombies, "death-then-handoff must not audit ZOMBIE");
    }
}
