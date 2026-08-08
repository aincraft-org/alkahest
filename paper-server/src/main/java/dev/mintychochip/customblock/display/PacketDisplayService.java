package dev.mintychochip.customblock.display;

import dev.mintychochip.customblock.CustomBlockDefinition;
import dev.mintychochip.customblock.CustomBlockLookup;
import dev.mintychochip.customblock.CustomBlocks;
import dev.mintychochip.customblock.MemoryCustomBlockLookup;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.VanillaMaterial;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * In-memory map of packet item-displays for PACKET-host custom blocks.
 */
public final class PacketDisplayService {

    private static final PacketDisplayService INSTANCE = new PacketDisplayService();

    private final Map<String, PacketItemDisplay> byLocation = new ConcurrentHashMap<>();

    private PacketDisplayService() {
    }

    public static PacketDisplayService get() {
        return INSTANCE;
    }

    public void spawn(@NotNull final Block block, @NotNull final CustomBlockDefinition definition) {
        if (!definition.isPacket()) {
            return;
        }
        final String key = key(block.getLocation());
        final PacketItemDisplay old = this.byLocation.remove(key);
        if (old != null) {
            old.hideAll();
        }
        final PacketItemDisplay display = PacketItemDisplay.create(block.getLocation(), definition);
        this.byLocation.put(key, display);

        // Show to every player who can see this chunk (placer included).
        final Location loc = block.getLocation().clone();
        final World world = Objects.requireNonNull(loc.getWorld(), "world");
        final Chunk chunk = loc.getChunk();
        final java.util.Collection<Player> viewers = chunk.getPlayersSeeingChunk();
        if (viewers.isEmpty()) {
            // Fallback: nearby online players in the same world (chunk map can lag 1 tick).
            for (final Player player : world.getPlayers()) {
                if (player.getLocation().getWorld() == world
                    && player.getLocation().distanceSquared(loc) < 96.0 * 96.0) {
                    display.show(player);
                    sendFakeCollision(player, loc, definition);
                }
            }
        } else {
            for (final Player player : viewers) {
                display.show(player);
                sendFakeCollision(player, loc, definition);
            }
        }

        // Re-send after place so client chunk/block updates cannot clobber the fake glass
        // or drop the display entity before metadata lands.
        Bukkit.getScheduler().runTaskLater(
            dev.mintychochip.MintyInternalPlugin.get(),
            () -> {
                if (this.byLocation.get(key) != display) {
                    return;
                }
                for (final Player player : chunk.getPlayersSeeingChunk()) {
                    if (!display.isViewing(player)) {
                        display.show(player);
                    }
                    sendFakeCollision(player, loc, definition);
                }
            },
            1L
        );
    }

    public void despawn(@NotNull final Location location) {
        final PacketItemDisplay display = this.byLocation.remove(key(location));
        if (display != null) {
            display.hideAll();
        }
    }

    public void despawn(@NotNull final Block block) {
        despawn(block.getLocation());
    }

    public @Nullable PacketItemDisplay get(@NotNull final Location location) {
        return this.byLocation.get(key(location));
    }

    /**
     * Ensure packet displays exist for persisted placements in this chunk, then show them
     * to {@code player}. Call on chunk show so client entities return after server restart.
     */
    public void showChunk(@NotNull final Player player, @NotNull final Chunk chunk) {
        ensureDisplaysForChunk(chunk);

        final World world = chunk.getWorld();
        final int cx = chunk.getX();
        final int cz = chunk.getZ();
        for (final Map.Entry<String, PacketItemDisplay> e : this.byLocation.entrySet()) {
            final Location loc = parseKey(e.getKey(), world);
            if (loc == null) {
                continue;
            }
            if (loc.getBlockX() >> 4 != cx || loc.getBlockZ() >> 4 != cz) {
                continue;
            }
            e.getValue().show(player);
            // Re-apply glass after real chunk data (PacketBlocks uses a 2-tick delay).
            final PacketItemDisplay display = e.getValue();
            final Location blockLoc = loc.clone();
            Bukkit.getScheduler().runTaskLater(
                dev.mintychochip.MintyInternalPlugin.get(),
                () -> {
                    if (player.isOnline() && display.isViewing(player)) {
                        final var def = CustomBlocks.of(blockLoc.getBlock());
                        def.ifPresent(d -> sendFakeCollision(player, blockLoc, d));
                    }
                },
                2L
            );
        }
    }

    /**
     * Respawn in-memory packet displays from the persistent placement store for a chunk.
     * Safe to call repeatedly (no-ops if already spawned).
     */
    public void ensureDisplaysForChunk(@NotNull final Chunk chunk) {
        final World world = chunk.getWorld();
        final CustomBlockLookup lookup = CustomBlocks.lookup();
        if (!(lookup instanceof MemoryCustomBlockLookup memory)) {
            return;
        }
        final var entries = memory.entriesInChunk(world, chunk.getX(), chunk.getZ());
        for (final var e : entries.entrySet()) {
            final var defOpt = CustomBlocks.get(e.getValue());
            if (defOpt.isEmpty() || !defOpt.get().isPacket()) {
                continue;
            }
            final Location loc = new Location(
                world,
                e.getKey().getX(),
                e.getKey().getY(),
                e.getKey().getZ()
            );
            final String k = key(loc);
            if (this.byLocation.containsKey(k)) {
                continue;
            }
            try {
                final PacketItemDisplay display = PacketItemDisplay.create(loc, defOpt.get());
                this.byLocation.put(k, display);
            } catch (final Throwable t) {
                Bukkit.getLogger().log(
                    java.util.logging.Level.WARNING,
                    "[mintychochip] failed to restore packet display at " + loc,
                    t
                );
            }
        }
    }

    public void hideChunk(@NotNull final Player player, @NotNull final Chunk chunk) {
        final World world = chunk.getWorld();
        final int cx = chunk.getX();
        final int cz = chunk.getZ();
        for (final Map.Entry<String, PacketItemDisplay> e : this.byLocation.entrySet()) {
            final Location loc = parseKey(e.getKey(), world);
            if (loc == null) {
                continue;
            }
            if (loc.getBlockX() >> 4 != cx || loc.getBlockZ() >> 4 != cz) {
                continue;
            }
            e.getValue().hide(player);
        }
    }

    public void clear() {
        for (final PacketItemDisplay d : this.byLocation.values()) {
            d.hideAll();
        }
        this.byLocation.clear();
    }

    public static void sendFakeCollision(
        @NotNull final Player player,
        @NotNull final Location loc,
        @NotNull final CustomBlockDefinition definition
    ) {
        // Prefer definition.carrierMaterial() (already VanillaMaterial-backed).
        Material mat = definition.carrierMaterial();
        if (mat == null) {
            mat = VanillaMaterial.GLASS;
        }
        try {
            player.sendBlockChange(loc, mat.createBlockData());
        } catch (final Throwable ignored) {
        }
    }

    private static String key(final Location loc) {
        final World w = Objects.requireNonNull(loc.getWorld(), "world");
        return w.getUID() + ":" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private static @Nullable Location parseKey(final String key, final World expectedWorld) {
        // uuid:x,y,z
        final int colon = key.indexOf(':');
        if (colon < 0) {
            return null;
        }
        final String worldUid = key.substring(0, colon);
        if (!expectedWorld.getUID().toString().equals(worldUid)) {
            return null;
        }
        final String[] parts = key.substring(colon + 1).split(",");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new Location(
                expectedWorld,
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
            );
        } catch (final NumberFormatException e) {
            return null;
        }
    }
}
