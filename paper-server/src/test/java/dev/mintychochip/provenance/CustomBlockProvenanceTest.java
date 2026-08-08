package dev.mintychochip.provenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import dev.mintychochip.customblock.CustomBlockDefinition;
import dev.mintychochip.customblock.CustomBlockProvenance;
import dev.mintychochip.customblock.CustomBlocks;
import dev.mintychochip.customblock.PacketHostSpec;
import net.minecraft.core.BlockPos;
import org.bukkit.VanillaMaterial;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.support.environment.Normal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Custom-block mint / place / break-drop provenance via shipped
 * {@link CustomBlockProvenance} production entry points.
 */
@Normal
public class CustomBlockProvenanceTest {

    private static final String DIM = "minecraft:overworld";

    @BeforeEach
    public void setUp() {
        ItemProvenance.setEnabled(true);
        ItemProvenance.clearAll();
        CustomBlocks.reset();
        CustomBlocks.register(
            CustomBlockDefinition.builder("mintychochip:test_ore")
                .host(PacketHostSpec.defaults())
                .itemMaterial(VanillaMaterial.PAPER)
                .build()
        );
    }

    @AfterEach
    public void tearDown() {
        ItemProvenance.clearAll();
        CustomBlocks.reset();
        ItemProvenance.setEnabled(true);
    }

    @Test
    public void mintLeavesProvenanceIdAndCustomBlockKey() {
        final CustomBlockDefinition def = CustomBlocks.get("mintychochip:test_ore").orElseThrow();
        final org.bukkit.inventory.ItemStack bukkit = CustomBlockProvenance.createMinted(def, 1, "give:test");

        assertTrue(CustomBlocks.isCustomBlockItem(bukkit), "custom-block PDC must remain");
        assertEquals(def.namespacedKey(), CustomBlocks.keyOf(bukkit).orElseThrow());

        final Optional<UUID> id = CustomBlockProvenance.idOf(bukkit);
        assertTrue(id.isPresent(), "mint must stamp provenance UUID");
        assertTrue(ItemProvenance.live().contains(id.get()));
        assertEquals(ProvenanceSource.GIVE, StackStamp.read(CraftItemStack.unwrap(bukkit)).orElseThrow().source());

        final org.bukkit.inventory.ItemStack other = CustomBlockProvenance.createMinted(def, 1, "give:test2");
        assertNotEquals(id.get(), CustomBlockProvenance.idOf(other).orElseThrow());
        assertTrue(
            StackStamp.sameItemSameComponentsIgnoringProvenance(
                CraftItemStack.unwrap(bukkit),
                CraftItemStack.unwrap(other)
            )
        );
    }

    /**
     * Production {@link CustomBlockProvenance#afterHandConsume}: last unit (amount→0) must
     * death the UUID — mirrors handleManualPlace after consumeOne zeros the hand.
     */
    @Test
    public void afterHandConsumeLastItemDeathsUuid() {
        final CustomBlockDefinition def = CustomBlocks.get("mintychochip:test_ore").orElseThrow();
        final org.bukkit.inventory.ItemStack hand = CustomBlockProvenance.createMinted(def, 1, "player:placer");
        final UUID placedId = CustomBlockProvenance.idOf(hand).orElseThrow();
        assertTrue(ItemProvenance.live().contains(placedId));

        // Production consumeOne zeros hand before inventory slot replace
        hand.setAmount(0);
        CustomBlockProvenance.afterHandConsume(hand);

        assertFalse(ItemProvenance.live().contains(placedId), "last place must death hand UUID");
        assertTrue(ItemProvenance.lineage().get(placedId).orElseThrow().dead());
        assertEquals(ProvenanceReason.CONSUMED, ItemProvenance.lineage().get(placedId).orElseThrow().deathReason());
    }

    /**
     * Regression: if hand were left at count 1 after inventory clear (old bug), afterHandConsume
     * with amount 0 still deaths; partial place keeps live with updated count.
     */
    @Test
    public void afterHandConsumePartialKeepsLive() {
        final CustomBlockDefinition def = CustomBlocks.get("mintychochip:test_ore").orElseThrow();
        final org.bukkit.inventory.ItemStack hand = CustomBlockProvenance.createMinted(def, 3, "player:placer");
        final UUID id = CustomBlockProvenance.idOf(hand).orElseThrow();

        hand.setAmount(2);
        CustomBlockProvenance.afterHandConsume(hand);

        assertTrue(ItemProvenance.live().contains(id));
        assertEquals(2, ItemProvenance.live().get(id).orElseThrow().count());
    }

    @Test
    public void placeRecordsParentUuidForPosition() {
        final CustomBlockDefinition def = CustomBlocks.get("mintychochip:test_ore").orElseThrow();
        final org.bukkit.inventory.ItemStack hand = CustomBlockProvenance.createMinted(def, 1, "player:placer");
        final UUID placedId = CustomBlockProvenance.idOf(hand).orElseThrow();
        final BlockPos pos = new BlockPos(3, 64, -7);

        // Production recordPlace(dim,…) — same placement store as live ServerLevel path
        CustomBlockProvenance.recordPlace(DIM, pos, CraftItemStack.unwrap(hand), "player:placer");

        assertTrue(ItemProvenance.placements().get(DIM, pos).isPresent());
        assertEquals(placedId, ItemProvenance.placements().get(DIM, pos).orElseThrow().parentStackId());
        assertTrue(ItemProvenance.live().contains(placedId));
    }

    @Test
    public void breakDropRecoversParentAndClearsPlacement() {
        final CustomBlockDefinition def = CustomBlocks.get("mintychochip:test_ore").orElseThrow();
        final org.bukkit.inventory.ItemStack hand = CustomBlockProvenance.createMinted(def, 1, "player:miner");
        final UUID placedId = CustomBlockProvenance.idOf(hand).orElseThrow();
        final BlockPos pos = new BlockPos(1, 70, 2);

        CustomBlockProvenance.recordPlace(DIM, pos, CraftItemStack.unwrap(hand), "player:miner");

        // Production manual-place consume path
        hand.setAmount(0);
        CustomBlockProvenance.afterHandConsume(hand);
        assertFalse(ItemProvenance.live().contains(placedId));

        // Production recover path (createRecoverDrop → stampRecoverAndClear)
        final org.bukkit.inventory.ItemStack drop = CustomBlocks.createItemStack(def);
        CustomBlockProvenance.stampRecoverAndClear(DIM, pos, CraftItemStack.unwrap(drop));

        final StackProvenance stamp = StackStamp.read(CraftItemStack.unwrap(drop)).orElseThrow();
        assertEquals(ProvenanceSource.BLOCK_RECOVER, stamp.source());
        assertEquals(List.of(placedId), stamp.parents());
        assertTrue(ItemProvenance.live().contains(stamp.id()));
        assertNotEquals(placedId, stamp.id());
        assertTrue(ItemProvenance.placements().get(DIM, pos).isEmpty(), "placement cleared after recover");
        assertTrue(
            ItemProvenance.explain(stamp.id()).stream().anyMatch(n -> n.id().equals(placedId)),
            "lineage walks drop ← placed parent"
        );
        assertTrue(CustomBlocks.isCustomBlockItem(drop), "drop keeps custom-block PDC");
    }

    @Test
    public void clearPlacementRemovesOrphanMemory() {
        final CustomBlockDefinition def = CustomBlocks.get("mintychochip:test_ore").orElseThrow();
        final org.bukkit.inventory.ItemStack hand = CustomBlockProvenance.createMinted(def, 1, "p");
        final BlockPos pos = new BlockPos(9, 9, 9);
        CustomBlockProvenance.recordPlace(DIM, pos, CraftItemStack.unwrap(hand), "p");
        assertTrue(ItemProvenance.placements().get(DIM, pos).isPresent());

        // Production explode / creative clear path (Listener.clearExploded → clearPlacement)
        CustomBlockProvenance.clearPlacement(DIM, pos);
        assertTrue(ItemProvenance.placements().get(DIM, pos).isEmpty());
    }

    /**
     * Full mint → place memory → last-item hand death → BLOCK_RECOVER drop chain using only
     * shipped {@link CustomBlockProvenance} APIs (same as lifecycle give/place/break).
     */
    @Test
    public void fullMintPlaceConsumeRecoverChain() {
        final CustomBlockDefinition def = CustomBlocks.get("mintychochip:test_ore").orElseThrow();
        final org.bukkit.inventory.ItemStack hand = CustomBlockProvenance.createMinted(def, 1, "player:chain");
        final UUID placedId = CustomBlockProvenance.idOf(hand).orElseThrow();
        final BlockPos pos = new BlockPos(5, 80, 5);

        CustomBlockProvenance.recordPlace(DIM, pos, CraftItemStack.unwrap(hand), "player:chain");
        assertEquals(placedId, ItemProvenance.placements().get(DIM, pos).orElseThrow().parentStackId());

        // handleManualPlace: consumeOne zeros hand, then afterHandConsume
        hand.setAmount(0);
        CustomBlockProvenance.afterHandConsume(hand);
        assertFalse(ItemProvenance.live().contains(placedId));

        // finishBreak: createRecoverDrop body = createItemStack + stampRecoverAndClear
        final org.bukkit.inventory.ItemStack drop = CustomBlocks.createItemStack(def);
        CustomBlockProvenance.stampRecoverAndClear(DIM, pos, CraftItemStack.unwrap(drop));

        final StackProvenance stamp = StackStamp.read(CraftItemStack.unwrap(drop)).orElseThrow();
        assertEquals(ProvenanceSource.BLOCK_RECOVER, stamp.source());
        assertEquals(List.of(placedId), stamp.parents());
        assertTrue(ItemProvenance.placements().get(DIM, pos).isEmpty());
        assertTrue(CustomBlocks.isCustomBlockItem(drop));
        assertTrue(ItemProvenance.explain(stamp.id()).stream().anyMatch(n -> n.id().equals(placedId)));
    }

}
