package dev.mintychochip.provenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bukkit.support.environment.Normal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Normal
public class ProvenanceInvariantTest {

    private static final UUID PLAYER = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @BeforeEach
    public void setUp() {
        ItemProvenance.setEnabled(true);
        ItemProvenance.clearAll();
    }

    @AfterEach
    public void tearDown() {
        ItemProvenance.clearAll();
        ItemProvenance.setEnabled(true);
    }

    @Test
    public void explicitTransferDoesNotCreateCollision() {
        final ItemStack stack = new ItemStack(Items.DIAMOND, 4);
        final StackLocation playerSlot = StackLocation.playerSlot(PLAYER, 0);
        final StackLocation droppedEntity = StackLocation.itemEntity(
            UUID.fromString("20000000-0000-0000-0000-000000000002")
        );
        ItemProvenance.birth(stack, ProvenanceSource.LOOT, playerSlot).orElseThrow();

        assertFalse(ItemProvenance.transfer(stack, droppedEntity));
        assertFalse(ItemProvenance.transfer(stack, playerSlot));
        assertTrue(ItemProvenance.collisions().isEmpty());
        assertEquals(playerSlot, ItemProvenance.live().get(StackStamp.readId(stack).orElseThrow()).orElseThrow().location());
    }

    @Test
    public void sameUuidObservedInTwoSlotsCreatesOneCollision() {
        final ItemStack original = new ItemStack(Items.DIAMOND, 4);
        final StackLocation first = StackLocation.playerSlot(PLAYER, 0);
        final StackLocation second = StackLocation.playerSlot(PLAYER, 1);
        ItemProvenance.birth(original, ProvenanceSource.LOOT, first).orElseThrow();
        final ItemStack duplicate = original.copy();

        assertTrue(ItemProvenance.observe(duplicate, second));
        assertEquals(1, ItemProvenance.collisions().size());
        assertEquals(ProvenanceCollisionKind.DUPLICATE_LOCATION, ItemProvenance.collisions().getFirst().kind());
        assertEquals(first, ItemProvenance.collisions().getFirst().existingLocation());
        assertEquals(second, ItemProvenance.collisions().getFirst().observedLocation());
    }

    @Test
    public void repeatedObservationDoesNotDuplicateCollisionRecord() {
        final ItemStack original = new ItemStack(Items.DIAMOND, 4);
        final StackLocation first = StackLocation.playerSlot(PLAYER, 0);
        final StackLocation second = StackLocation.playerSlot(PLAYER, 1);
        ItemProvenance.birth(original, ProvenanceSource.LOOT, first).orElseThrow();
        final ItemStack duplicate = original.copy();

        assertTrue(ItemProvenance.observe(duplicate, second));
        assertTrue(ItemProvenance.observe(duplicate, second));
        assertEquals(1, ItemProvenance.collisions().size());
    }

    @Test
    public void sameIdMergeCannotLaunderDuplicateQuantity() {
        final ItemStack survivor = new ItemStack(Items.DIAMOND, 4);
        final StackLocation target = StackLocation.playerSlot(PLAYER, 0);
        final StackLocation source = StackLocation.playerSlot(PLAYER, 1);
        ItemProvenance.birth(survivor, ProvenanceSource.LOOT, target).orElseThrow();
        final ItemStack duplicate = survivor.copy();

        final Optional<UUID> targetIdBefore = StackStamp.readId(survivor);
        final Optional<UUID> sourceIdBefore = StackStamp.readId(duplicate);
        survivor.grow(duplicate.getCount());
        duplicate.setCount(0);

        assertTrue(ItemProvenance.afterContainerMerge(
            survivor,
            duplicate,
            targetIdBefore,
            sourceIdBefore,
            4,
            source,
            target
        ));
        assertEquals(targetIdBefore.orElseThrow(), StackStamp.readId(survivor).orElseThrow());
        assertEquals(1, ItemProvenance.collisions().size());
        assertEquals(ProvenanceCollisionKind.DUPLICATE_MERGE, ItemProvenance.collisions().getFirst().kind());
    }

    @Test
    public void consumeAndReturnMintsPartialSplitChild() {
        final ItemStack source = new ItemStack(Items.COD, 3);
        final StackLocation hand = StackLocation.playerSlot(PLAYER, 0);
        final UUID parentId = ItemProvenance.birth(source, ProvenanceSource.LOOT, hand).orElseThrow();

        final ItemStack child = source.consumeAndReturn(1, null);

        assertEquals(2, source.getCount());
        assertNotEquals(parentId, StackStamp.readId(child).orElseThrow());
        assertEquals(parentId, StackStamp.read(child).orElseThrow().parents().getFirst());
    }
}
