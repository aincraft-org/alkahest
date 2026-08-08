package dev.mintychochip.provenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.support.environment.Normal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Normal
public class CraftingMenuProvenanceTest {

    private Player player;
    private Inventory inventory;

    @BeforeEach
    public void setUp() {
        ItemProvenance.setEnabled(true);
        ItemProvenance.clearAll();

        this.player = mock(Player.class);
        when(this.player.getUUID()).thenReturn(UUID.fromString("10000000-0000-0000-0000-000000000001"));
        final Level level = mock(Level.class);
        when(level.enabledFeatures()).thenReturn(FeatureFlags.DEFAULT_FLAGS);
        when(this.player.level()).thenReturn(level);
        when(this.player.getBukkitEntity()).thenReturn(mock(CraftPlayer.class));
        this.inventory = new Inventory(this.player, mock(EntityEquipment.class));
        when(this.player.getInventory()).thenReturn(this.inventory);
    }

    @AfterEach
    public void tearDown() {
        ItemProvenance.clearAll();
        ItemProvenance.setEnabled(true);
    }

    @Test
    public void shiftClickCraftRetainsCraftIdentityInInventory() {
        final CraftingMenu menu = this.craftingMenuWithLogToPlanks();
        final UUID ingredientId = StackStamp.readId(menu.craftSlots.getItem(0)).orElseThrow();

        final ItemStack clicked = menu.quickMoveStack(this.player, CraftingMenu.RESULT_SLOT);

        assertFalse(clicked.isEmpty(), "shift-click should move the craft result");
        final ItemStack crafted = this.findInventoryStack(Items.OAK_PLANKS);
        final StackProvenance stamp = StackStamp.read(crafted).orElseThrow();
        assertEquals(ProvenanceSource.CRAFT, stamp.source());
        assertEquals(List.of(ingredientId), stamp.parents());
    }

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

    @Test
    public void directPickupRetainsCraftIdentityOnCursor() {
        final CraftingMenu menu = this.craftingMenuWithLogToPlanks();
        final UUID ingredientId = StackStamp.readId(menu.craftSlots.getItem(0)).orElseThrow();

        menu.clicked(CraftingMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, this.player);

        final StackProvenance stamp = StackStamp.read(menu.getCarried()).orElseThrow();
        assertEquals(ProvenanceSource.CRAFT, stamp.source());
        assertEquals(List.of(ingredientId), stamp.parents());
    }

    @Test
    public void shiftClickCraftMergingIntoExistingStackPreservesBothParents() {
        final CraftingMenu menu = this.craftingMenuWithLogToPlanks();
        final UUID ingredientId = StackStamp.readId(menu.craftSlots.getItem(0)).orElseThrow();
        final ItemStack existing = new ItemStack(Items.OAK_PLANKS, 2);
        final UUID existingId = ItemProvenance.ensure(
            existing,
            StackLocation.labeled("menu-slot:10")
        ).orElseThrow();
        this.inventory.setItem(0, existing);

        assertFalse(menu.quickMoveStack(this.player, CraftingMenu.RESULT_SLOT).isEmpty());

        this.assertCraftMerge(
            this.findInventoryStack(Items.OAK_PLANKS),
            existingId,
            ingredientId
        );
    }

    @Test
    public void directPickupCraftMergingIntoCursorPreservesBothParents() {
        final CraftingMenu menu = this.craftingMenuWithLogToPlanks();
        final UUID ingredientId = StackStamp.readId(menu.craftSlots.getItem(0)).orElseThrow();
        final ItemStack existing = new ItemStack(Items.OAK_PLANKS, 2);
        final UUID existingId = ItemProvenance.ensure(
            existing,
            StackLocation.playerSlot(this.player.getUUID(), -1)
        ).orElseThrow();
        menu.setCarried(existing);

        menu.clicked(CraftingMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, this.player);

        this.assertCraftMerge(menu.getCarried(), existingId, ingredientId);
    }

    private void assertCraftMerge(
        final ItemStack mergedStack,
        final UUID existingId,
        final UUID ingredientId
    ) {
        final StackProvenance merged = StackStamp.read(mergedStack).orElseThrow();
        assertEquals(ProvenanceSource.MERGE, merged.source());
        assertTrue(merged.parents().contains(existingId));
        assertEquals(2, merged.parents().size());
        final UUID craftId = merged.parents().stream()
            .filter(parent -> !parent.equals(existingId))
            .findFirst()
            .orElseThrow();
        final LineageNode craft = ItemProvenance.lineage().get(craftId).orElseThrow();
        assertEquals(ProvenanceSource.CRAFT, craft.source());
        assertEquals(List.of(ingredientId), craft.parents());
        assertFalse(ItemProvenance.live().contains(existingId));
    }

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

    private ItemStack findInventoryStack(final net.minecraft.world.item.Item item) {
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            final ItemStack stack = this.inventory.getItem(slot);
            if (stack.is(item)) {
                return stack;
            }
        }
        throw new AssertionError("missing inventory stack " + item);
    }
}
