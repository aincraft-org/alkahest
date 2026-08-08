package dev.mintychochip.customblock;

import java.util.Optional;
import java.util.UUID;
import dev.mintychochip.provenance.ItemProvenance;
import dev.mintychochip.provenance.ProvenanceSource;
import dev.mintychochip.provenance.StackStamp;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bridges custom-block mint / place / break into {@link ItemProvenance}.
 *
 * <p>Custom-block PDC identity ({@code mintychochip:custom_block}) stays separate from the
 * stack UUID stamp. Packet-display visual stacks must not call these mint APIs.
 */
public final class CustomBlockProvenance {

    private CustomBlockProvenance() {
    }

    /**
     * Factory used by give/lifecycle: custom-block PDC stamp + provenance birth (GIVE).
     */
    public static @NotNull ItemStack createMinted(
        final @NotNull CustomBlockDefinition definition,
        final int amount,
        final @Nullable String holder
    ) {
        final ItemStack stack = CustomBlocks.createItemStack(definition, amount);
        mint(stack, holder);
        return stack;
    }

    public static @NotNull ItemStack createMinted(
        final @NotNull CustomBlockDefinition definition,
        final @Nullable String holder
    ) {
        return createMinted(definition, 1, holder);
    }

    /**
     * Ensure a custom-block stack has a provenance UUID. Does not clear custom-block PDC.
     */
    public static void mint(final @NotNull ItemStack bukkit, final @Nullable String holder) {
        if (bukkit == null || bukkit.isEmpty()) {
            return;
        }
        final net.minecraft.world.item.ItemStack nms = CraftItemStack.unwrap(bukkit);
        if (nms.isEmpty()) {
            return;
        }
        ItemProvenance.birthIfAbsent(nms, ProvenanceSource.GIVE, locationOf(holder));
    }

    private static dev.mintychochip.provenance.StackLocation locationOf(final @Nullable String holder) {
        if (holder == null) {
            return dev.mintychochip.provenance.StackLocation.labeled("custom_block_mint");
        }
        try {
            return dev.mintychochip.provenance.StackLocation.playerSlot(java.util.UUID.fromString(holder), -1);
        } catch (final IllegalArgumentException ignored) {
            return dev.mintychochip.provenance.StackLocation.labeled(holder);
        }
    }

    /**
     * Successful custom-block place: record placement memory from the hand stack UUID.
     */
    public static void recordPlace(
        final @NotNull Block block,
        final @NotNull ItemStack hand,
        final @Nullable Player player
    ) {
        if (hand.isEmpty()) {
            return;
        }
        final World world = block.getWorld();
        if (!(world instanceof CraftWorld craftWorld)) {
            return;
        }
        final ServerLevel level = craftWorld.getHandle();
        final BlockPos pos = new BlockPos(block.getX(), block.getY(), block.getZ());
        final net.minecraft.world.item.ItemStack nmsHand = CraftItemStack.unwrap(hand);
        final net.minecraft.world.entity.player.Player nmsPlayer =
            player instanceof CraftPlayer craftPlayer ? craftPlayer.getHandle() : null;
        ItemProvenance.onBlockPlaced(level, pos, nmsHand, nmsPlayer);
    }

    /**
     * After manual place consumed one from the hand (non-block item path).
     *
     * <p>Uses Bukkit {@link ItemStack#getAmount()} as source of truth: when the last unit was
     * placed, callers zero the hand (or pass amount 0) so the placed UUID leaves the live census.
     */
    public static void afterHandConsume(final @NotNull ItemStack hand) {
        if (hand == null) {
            return;
        }
        final net.minecraft.world.item.ItemStack nms = CraftItemStack.unwrap(hand);
        if (nms.isEmpty() && hand.getAmount() > 0) {
            return;
        }
        // Capture stamp before count sync (empty stacks still hold CUSTOM_DATA).
        final Optional<UUID> id = StackStamp.readId(nms);
        final int bukkitAmount = Math.max(0, hand.getAmount());
        if (bukkitAmount <= 0) {
            // Last unit placed: death even if NMS handle was not shrunk yet.
            if (!nms.isEmpty() && nms.getCount() > 0) {
                nms.setCount(0);
            }
            id.ifPresent(uuid ->
                ItemProvenance.death(uuid, dev.mintychochip.provenance.ProvenanceReason.CONSUMED, null)
            );
            return;
        }
        if (nms.getCount() != bukkitAmount) {
            nms.setCount(bukkitAmount);
        }
        ItemProvenance.afterConsume(nms);
    }

    /**
     * Build a break drop stamped as {@link ProvenanceSource#BLOCK_RECOVER} when placement
     * memory exists for this position; clears placement afterward.
     */
    public static @NotNull ItemStack createRecoverDrop(
        final @NotNull Block block,
        final @NotNull CustomBlockDefinition definition
    ) {
        final ItemStack drop = CustomBlocks.createItemStack(definition);
        stampRecoverAndClear(block, drop);
        return drop;
    }

    /**
     * Stamp {@code drop} from placement memory at {@code block}, then clear placement.
     */
    public static void stampRecoverAndClear(final @NotNull Block block, final @NotNull ItemStack drop) {
        final World world = block.getWorld();
        if (!(world instanceof CraftWorld craftWorld)) {
            mint(drop, "custom_block_drop");
            return;
        }
        final ServerLevel level = craftWorld.getHandle();
        final BlockPos pos = new BlockPos(block.getX(), block.getY(), block.getZ());
        stampRecoverAndClear(level, pos, CraftItemStack.unwrap(drop));
    }

    /**
     * Production recover path (live {@link ServerLevel}).
     */
    public static void stampRecoverAndClear(
        final @NotNull ServerLevel level,
        final @NotNull BlockPos pos,
        final @NotNull net.minecraft.world.item.ItemStack drop
    ) {
        ItemProvenance.stampBlockDrop(level, pos, drop);
        ItemProvenance.clearPlacement(level, pos);
    }

    /**
     * Production recover path keyed by dimension id (tests + offline placement store).
     */
    public static void stampRecoverAndClear(
        final @NotNull String dimensionId,
        final @NotNull BlockPos pos,
        final @NotNull net.minecraft.world.item.ItemStack drop
    ) {
        ItemProvenance.stampBlockDrop(dimensionId, pos, drop);
        ItemProvenance.placements().remove(dimensionId, pos);
    }

    /**
     * Creative / no-drop break: drop placement memory only (placed stack already consumed).
     */
    public static void clearPlacement(final @NotNull Block block) {
        final World world = block.getWorld();
        if (!(world instanceof CraftWorld craftWorld)) {
            return;
        }
        final ServerLevel level = craftWorld.getHandle();
        final BlockPos pos = new BlockPos(block.getX(), block.getY(), block.getZ());
        ItemProvenance.clearPlacement(level, pos);
    }

    /**
     * Clear placement by dimension id (explode / tests using offline placement store).
     */
    public static void clearPlacement(
        final @NotNull String dimensionId,
        final @NotNull BlockPos pos
    ) {
        ItemProvenance.placements().remove(dimensionId, pos);
    }

    /**
     * Record placement from hand UUID at dimension+pos (same census store as live place).
     * Used by tests without a {@link ServerLevel}; live path uses {@link #recordPlace(Block, ItemStack, Player)}.
     */
    public static void recordPlace(
        final @NotNull String dimensionId,
        final @NotNull BlockPos pos,
        final @NotNull net.minecraft.world.item.ItemStack hand,
        final @Nullable String placer
    ) {
        final dev.mintychochip.provenance.StackLocation location = locationOf(placer);
        final Optional<UUID> id = ItemProvenance.ensure(hand, location);
        if (id.isEmpty()) {
            return;
        }
        ItemProvenance.placements().put(
            dimensionId,
            pos.immutable(),
            new dev.mintychochip.provenance.PlacementRecord(
                id.get(),
                ItemProvenance.itemId(hand),
                placer != null ? placer : "player",
                System.currentTimeMillis()
            )
        );
    }

    public static @NotNull Optional<UUID> idOf(final @NotNull ItemStack bukkit) {
        return StackStamp.readId(CraftItemStack.unwrap(bukkit));
    }

    public static @NotNull Optional<UUID> idOf(final @NotNull net.minecraft.world.item.ItemStack nms) {
        return StackStamp.readId(nms);
    }
}
