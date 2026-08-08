package dev.mintychochip.customblock;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Placement map for custom blocks with per-dimension {@link CustomBlockPlacementsData} persistence.
 *
 * <p>In-memory cache is warm on put/get; world SavedData restores identity after restart so
 * packet item-displays can be respawned on chunk show.
 */
public final class MemoryCustomBlockLookup implements CustomBlockLookup {

    private final Map<String, NamespacedKey> byLocation = new ConcurrentHashMap<>();

    @Override
    public @NotNull Optional<NamespacedKey> keyAt(@NotNull final Block block) {
        Objects.requireNonNull(block, "block");
        final String memKey = locationKey(block.getLocation());
        final NamespacedKey cached = this.byLocation.get(memKey);
        if (cached != null) {
            return Optional.of(cached);
        }
        // Cold path: restore from world SavedData after restart
        final Optional<NamespacedKey> stored = loadFromSavedData(block);
        stored.ifPresent(key -> this.byLocation.put(memKey, key));
        return stored;
    }

    @Override
    public boolean clearAt(@NotNull final Block block) {
        return remove(block);
    }

    public void put(@NotNull final Location location, @NotNull final NamespacedKey blockId) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(blockId, "blockId");
        this.byLocation.put(locationKey(location), blockId);
        persist(location, blockId);
    }

    public void put(@NotNull final Block block, @NotNull final NamespacedKey blockId) {
        put(block.getLocation(), blockId);
    }

    public void put(@NotNull final Block block, @NotNull final CustomBlockDefinition definition) {
        put(block, definition.namespacedKey());
    }

    public boolean remove(@NotNull final Location location) {
        final boolean removed = this.byLocation.remove(locationKey(location)) != null;
        final boolean persisted = unpersist(location) != null;
        return removed || persisted;
    }

    public boolean remove(@NotNull final Block block) {
        return remove(block.getLocation());
    }

    public @Nullable NamespacedKey getOrNull(@NotNull final Location location) {
        return keyAt(location.getBlock()).orElse(null);
    }

    public void clear() {
        this.byLocation.clear();
    }

    public int size() {
        return this.byLocation.size();
    }

    /**
     * Entries known for a chunk (memory + SavedData). Used to respawn packet displays.
     */
    public @NotNull Map<BlockPos, NamespacedKey> entriesInChunk(
        @NotNull final World world,
        final int chunkX,
        final int chunkZ
    ) {
        final Map<BlockPos, NamespacedKey> out = new ConcurrentHashMap<>();
        if (!(world instanceof CraftWorld craftWorld)) {
            return out;
        }
        final ServerLevel level = craftWorld.getHandle();
        for (final var e : CustomBlockPlacementsData.get(level).entriesInChunk(chunkX, chunkZ)) {
            out.put(e.getKey(), e.getValue());
            // Warm memory cache
            final Location loc = new Location(world, e.getKey().getX(), e.getKey().getY(), e.getKey().getZ());
            this.byLocation.put(locationKey(loc), e.getValue());
        }
        return out;
    }

    private static Optional<NamespacedKey> loadFromSavedData(final Block block) {
        final World world = block.getWorld();
        if (!(world instanceof CraftWorld craftWorld)) {
            return Optional.empty();
        }
        final BlockPos pos = new BlockPos(block.getX(), block.getY(), block.getZ());
        return CustomBlockPlacementsData.get(craftWorld.getHandle()).get(pos);
    }

    private static void persist(final Location location, final NamespacedKey blockId) {
        final World world = location.getWorld();
        if (!(world instanceof CraftWorld craftWorld)) {
            return;
        }
        final BlockPos pos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        CustomBlockPlacementsData.get(craftWorld.getHandle()).put(pos, blockId);
    }

    private static @Nullable NamespacedKey unpersist(final Location location) {
        final World world = location.getWorld();
        if (!(world instanceof CraftWorld craftWorld)) {
            return null;
        }
        final BlockPos pos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        return CustomBlockPlacementsData.get(craftWorld.getHandle()).remove(pos);
    }

    private static String locationKey(final Location location) {
        final World world = location.getWorld();
        final String worldName = world != null ? world.getName() : "null";
        return worldName + ':' + location.getBlockX() + ',' + location.getBlockY() + ',' + location.getBlockZ();
    }
}
