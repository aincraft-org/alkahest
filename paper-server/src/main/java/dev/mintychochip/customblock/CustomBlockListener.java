package dev.mintychochip.customblock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Bukkit wiring for custom-block place / break. Additive — does not replace Material APIs.
 */
public final class CustomBlockListener implements Listener {

    /** Break event → definition for MONITOR drop (thread: main only). */
    private final Map<Location, CustomBlockDefinition> pendingBreakDrops = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(final BlockPlaceEvent event) {
        CustomBlockLifecycle.handlePlace(event);
    }

    /**
     * Place custom-block items whose base material is not a block (e.g. PAPER host item).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractPlace(final PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || event.getItem() == null) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND && event.getHand() != EquipmentSlot.OFF_HAND) {
            return;
        }
        switch (event.getAction()) {
            case RIGHT_CLICK_BLOCK -> {
                // fall through
            }
            default -> {
                return;
            }
        }
        final ItemStack item = event.getItem();
        if (!CustomBlocks.isCustomBlockItem(item)) {
            return;
        }
        final Optional<CustomBlockDefinition> def = CustomBlocks.of(item);
        if (def.isEmpty() || def.get().itemMaterial().isBlock()) {
            // Placeable materials: vanilla place + onPlace handles them.
            return;
        }
        final boolean placed = CustomBlockLifecycle.handleManualPlace(
            event.getPlayer(),
            item,
            event.getClickedBlock(),
            event.getBlockFace(),
            event.getHand()
        );
        if (placed) {
            // Deny item-use so the client does not play the "use" animation for non-block bases.
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreakPrepare(final BlockBreakEvent event) {
        final Optional<CustomBlockDefinition> def = CustomBlockLifecycle.prepareBreak(event);
        if (def.isPresent()) {
            this.pendingBreakDrops.put(blockKey(event.getBlock()), def.get());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakFinish(final BlockBreakEvent event) {
        final CustomBlockDefinition def = this.pendingBreakDrops.remove(blockKey(event.getBlock()));
        CustomBlockLifecycle.finishBreak(event, def);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBreakCancelledCleanup(final BlockBreakEvent event) {
        if (event.isCancelled()) {
            this.pendingBreakDrops.remove(blockKey(event.getBlock()));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(final BlockPistonExtendEvent event) {
        if (containsCustom(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(final BlockPistonRetractEvent event) {
        if (containsCustom(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(final BlockExplodeEvent event) {
        clearExploded(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(final EntityExplodeEvent event) {
        clearExploded(event.blockList());
    }

    private static boolean containsCustom(final List<Block> blocks) {
        for (final Block block : blocks) {
            if (CustomBlockLifecycle.isCustom(block)) {
                return true;
            }
        }
        return false;
    }

    private static void clearExploded(final List<Block> blocks) {
        final CustomBlockLookup lookup = CustomBlocks.lookup();
        final var displays = dev.mintychochip.customblock.display.PacketDisplayService.get();
        for (final Block block : blocks) {
            displays.despawn(block);
            lookup.clearAt(block);
            // mintychochip - item provenance: explode no-drop must not leave placement orphans
            CustomBlockProvenance.clearPlacement(block);
        }
    }

    private static @NotNull Location blockKey(final Block block) {
        final Location loc = block.getLocation();
        loc.setX(loc.getBlockX());
        loc.setY(loc.getBlockY());
        loc.setZ(loc.getBlockZ());
        return loc;
    }
}
