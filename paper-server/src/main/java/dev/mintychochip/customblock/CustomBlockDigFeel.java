package dev.mintychochip.customblock;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.SoundGroup;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Makes custom-block digging feel like a real block:
 * <ul>
 *   <li>While digging, the digger sees the emulated vanilla block (so client dig
 *       prediction matches server hardness / tool rules).</li>
 *   <li>Place / break use the emulated block's sounds and break particles.</li>
 * </ul>
 */
public final class CustomBlockDigFeel implements Listener {

    /** digger uuid → location key currently skinned for dig */
    private final Map<UUID, String> activeDigSkin = new ConcurrentHashMap<>();
    /** digger uuid → dig skin material (for break FX after identity is cleared) */
    private final Map<UUID, Material> digSkinMaterial = new ConcurrentHashMap<>();

    /**
     * Material the digger's client should use for dig prediction (hardness / tools).
     * Prefer the emulated block; otherwise pick a full cube with similar hardness.
     */
    public static @NotNull Material digSkin(@NotNull final BlockFeel feel) {
        final Material emulate = feel.emulate();
        if (emulate != null) {
            return emulate;
        }
        final float h = feel.hardness();
        // VanillaMaterial: Material.* interface statics can be null after the Material split.
        if (h < 0.0F) {
            return org.bukkit.VanillaMaterial.BEDROCK;
        }
        if (h <= 0.4F) {
            return org.bukkit.VanillaMaterial.GLASS;
        }
        if (h <= 0.8F) {
            return org.bukkit.VanillaMaterial.DIRT;
        }
        if (h <= 1.6F) {
            return org.bukkit.VanillaMaterial.STONE;
        }
        if (h <= 3.5F) {
            return org.bukkit.VanillaMaterial.IRON_ORE;
        }
        if (h <= 10.0F) {
            return org.bukkit.VanillaMaterial.END_STONE;
        }
        if (h <= 30.0F) {
            return org.bukkit.VanillaMaterial.ANCIENT_DEBRIS;
        }
        return org.bukkit.VanillaMaterial.OBSIDIAN;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageStart(final BlockDamageEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        final Optional<CustomBlockDefinition> defOpt = CustomBlocks.of(event.getBlock());
        if (defOpt.isEmpty()) {
            return;
        }
        final Block block = event.getBlock();
        final Material skin = digSkin(defOpt.get().feel());
        final BlockData data = skin.createBlockData();
        // Client now digs "iron ore" (or whatever) — prediction matches server feel.
        player.sendBlockChange(block.getLocation(), data);
        this.activeDigSkin.put(player.getUniqueId(), locKey(block));
        this.digSkinMaterial.put(player.getUniqueId(), skin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamageAbort(final BlockDamageAbortEvent event) {
        restoreDigSkin(event.getPlayer(), event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        this.activeDigSkin.remove(player.getUniqueId());
        // Prefer skin remembered from dig start; fall back to live definition.
        Material skin = this.digSkinMaterial.remove(player.getUniqueId());
        if (skin == null) {
            final Optional<CustomBlockDefinition> defOpt = CustomBlocks.of(block);
            if (defOpt.isEmpty()) {
                return;
            }
            skin = digSkin(defOpt.get().feel());
        }
        // Defer one tick so the block is air and FX don't clip the carrier.
        final Location center = block.getLocation().add(0.5, 0.5, 0.5);
        final BlockData particleData = skin.createBlockData();
        final World world = block.getWorld();
        org.bukkit.Bukkit.getScheduler().runTask(
            dev.mintychochip.MintyInternalPlugin.get(),
            () -> playBreakFx(world, center, particleData)
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(final BlockPlaceEvent event) {
        final Optional<CustomBlockDefinition> defOpt = CustomBlocks.of(event.getItemInHand());
        if (defOpt.isEmpty()) {
            // Also check after our place handler registered identity on the block
            final Optional<CustomBlockDefinition> placed = CustomBlocks.of(event.getBlockPlaced());
            if (placed.isEmpty()) {
                return;
            }
            playPlaceFx(event.getBlockPlaced(), digSkin(placed.get().feel()));
            return;
        }
        playPlaceFx(event.getBlockPlaced(), digSkin(defOpt.get().feel()));
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        final UUID id = event.getPlayer().getUniqueId();
        this.activeDigSkin.remove(id);
        this.digSkinMaterial.remove(id);
    }

    private void restoreDigSkin(final Player player, final Block block) {
        final UUID id = player.getUniqueId();
        final String key = this.activeDigSkin.remove(id);
        this.digSkinMaterial.remove(id);
        if (key == null && !CustomBlocks.isCustomBlock(block)) {
            return;
        }
        // Resend real world state so digger no longer sees the dig skin.
        player.sendBlockChange(block.getLocation(), block.getBlockData());
    }

    static void playPlaceFx(final Block block, final Material skin) {
        try {
            final SoundGroup group = skin.createBlockData().getSoundGroup();
            final Location loc = block.getLocation().add(0.5, 0.5, 0.5);
            block.getWorld().playSound(
                loc,
                group.getPlaceSound(),
                SoundCategory.BLOCKS,
                group.getVolume(),
                group.getPitch()
            );
        } catch (final Throwable ignored) {
            // SoundGroup may fail if material isn't a full block type in edge cases.
        }
    }

    static void playBreakFx(final World world, final Location center, final BlockData particleData) {
        try {
            final SoundGroup group = particleData.getSoundGroup();
            world.playSound(
                center,
                group.getBreakSound(),
                SoundCategory.BLOCKS,
                group.getVolume(),
                group.getPitch()
            );
            world.spawnParticle(
                Particle.BLOCK,
                center,
                32,
                0.35, 0.35, 0.35,
                0.05,
                particleData
            );
        } catch (final Throwable ignored) {
        }
    }

    private static String locKey(final Block block) {
        final Location l = block.getLocation();
        final World w = l.getWorld();
        return (w != null ? w.getUID() : "null")
            + ":" + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }
}
