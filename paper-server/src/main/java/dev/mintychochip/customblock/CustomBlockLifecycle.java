package dev.mintychochip.customblock;

import java.util.Optional;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Place / break core for custom blocks. Called from {@link CustomBlockListener}.
 *
 * <p>Keeps Bukkit {@code BlockBreakEvent} / {@code BlockPlaceEvent} intact — only adds
 * identity registration, carrier correction, and custom drops.
 */
public final class CustomBlockLifecycle {

    private CustomBlockLifecycle() {
    }

    /**
     * After a successful vanilla place of a stamped custom-block item.
     * Corrects carrier to the host material and registers identity.
     *
     * @return {@code true} if this was a custom block placement
     */
    public static boolean handlePlace(@NotNull final BlockPlaceEvent event) {
        final ItemStack hand = event.getItemInHand();
        final Optional<CustomBlockDefinition> defOpt = CustomBlocks.of(hand);
        if (defOpt.isEmpty()) {
            return false;
        }
        final CustomBlockDefinition def = defOpt.get();
        final Block block = event.getBlockPlaced();
        CustomBlockPlacement.applyCarrier(block, def);
        registerPlacement(block, def);
        // Packet visuals (item display + sendBlockChange) come in a later slice.
        return true;
    }

    /**
     * Manual place for items whose base material is not a block (e.g. PAPER).
     *
     * @return {@code true} if a custom block was placed
     */
    public static boolean handleManualPlace(
        @NotNull final Player player,
        @NotNull final ItemStack hand,
        @NotNull final Block against,
        @NotNull final BlockFace face,
        @Nullable final EquipmentSlot handSlot
    ) {
        final Optional<CustomBlockDefinition> defOpt = CustomBlocks.of(hand);
        if (defOpt.isEmpty()) {
            return false;
        }
        final CustomBlockDefinition def = defOpt.get();
        // Placeable base materials go through BlockPlaceEvent instead.
        if (def.itemMaterial().isBlock()) {
            return false;
        }

        final Block target = against.getRelative(face);
        if (!target.getType().isAir() && !target.isReplaceable()) {
            return false;
        }
        if (!canBuild(player, target)) {
            return false;
        }

        CustomBlockPlacement.applyCarrier(target, def);
        registerPlacement(target, def);
        consumeOne(player, hand, handSlot);
        // Non-block base items would otherwise show the "use" animation. Force a place feel:
        // arm swing + place sound. Prefer itemMaterial().isBlock() so vanilla place path runs.
        final EquipmentSlot swingSlot = handSlot != null ? handSlot : EquipmentSlot.HAND;
        player.swingHand(swingSlot);
        final Material carrier = CustomBlockPlacement.carrierMaterial(def);
        final Sound placeSound = placeSoundFor(carrier);
        target.getWorld().playSound(
            target.getLocation().add(0.5, 0.5, 0.5),
            placeSound,
            SoundCategory.BLOCKS,
            1.0f,
            0.8f + (float) (Math.random() * 0.4f)
        );
        return true;
    }

    private static Sound placeSoundFor(final Material carrier) {
        // Best-effort; glass is the default packet-host collision.
        if (carrier == Material.GLASS || carrier.name().contains("GLASS")) {
            return Sound.BLOCK_GLASS_PLACE;
        }
        if (carrier == Material.TRIPWIRE) {
            return Sound.BLOCK_STONE_PLACE;
        }
        if (carrier.name().contains("MUSHROOM") || carrier == Material.CHORUS_PLANT) {
            return Sound.BLOCK_WOOD_PLACE;
        }
        return Sound.BLOCK_STONE_PLACE;
    }

    private static void registerPlacement(final Block block, final CustomBlockDefinition def) {
        final CustomBlockLookup lookup = CustomBlocks.lookup();
        if (lookup instanceof MemoryCustomBlockLookup memory) {
            memory.put(block, def);
        } else {
            // Ensure bootstrap installed when events fire on a live server.
            CustomBlockBootstrap.placements().put(block, def);
        }
    }

    /**
     * HIGH priority: suppress vanilla carrier drops for custom blocks; remember definition for drop.
     *
     * @return definition if this break is a custom block, else empty
     */
    public static Optional<CustomBlockDefinition> prepareBreak(@NotNull final BlockBreakEvent event) {
        final Optional<CustomBlockDefinition> defOpt = CustomBlocks.of(event.getBlock());
        if (defOpt.isEmpty()) {
            return Optional.empty();
        }
        final CustomBlockDefinition def = defOpt.get();
        // Unbreakable feel: cancel break entirely (bedrock-style).
        if (def.feel().isUnbreakable() && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
            return Optional.empty();
        }
        // Carrier (glass/chorus/…) must not drop vanilla loot.
        if (event.isDropItems()) {
            event.setDropItems(false);
        }
        event.setExpToDrop(0);
        return defOpt;
    }

    /**
     * MONITOR: clear placement identity and drop the custom block item when tool rules allow.
     */
    public static void finishBreak(
        @NotNull final BlockBreakEvent event,
        @Nullable final CustomBlockDefinition definition
    ) {
        final Block block = event.getBlock();
        CustomBlocks.lookup().clearAt(block);
        if (definition == null) {
            return;
        }
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }
        // Same as vanilla: wrong tool + requiresCorrectTool → no drop.
        if (!CustomBlockMining.canHarvest(event.getPlayer(), definition.feel())) {
            return;
        }
        final Location dropAt = block.getLocation().add(0.5, 0.5, 0.5);
        final ItemStack drop = CustomBlocks.createItemStack(definition);
        block.getWorld().dropItemNaturally(dropAt, drop);
    }

    /** Cancel piston moves that would shift a custom block. */
    public static boolean isCustom(@NotNull final Block block) {
        return CustomBlocks.isCustomBlock(block);
    }

    private static boolean canBuild(final Player player, final Block target) {
        // Mirror a light permission check; full region plugins still see interact/place events.
        return player.getGameMode() != GameMode.ADVENTURE || player.hasPermission("minecraft.admin.command_block");
    }

    private static void consumeOne(final Player player, final ItemStack hand, final @Nullable EquipmentSlot slot) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        final int amount = hand.getAmount();
        if (amount <= 1) {
            if (slot == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(ItemStack.empty());
            } else {
                player.getInventory().setItemInMainHand(ItemStack.empty());
            }
        } else {
            hand.setAmount(amount - 1);
        }
    }
}
