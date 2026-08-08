package dev.mintychochip.customblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Per-dimension persistent placement map for custom blocks (packet + baked hosts).
 *
 * <p>Stored via {@link ServerLevel#getDataStorage()} under
 * {@code mintychochip/custom_block_placements} (auto-saved with the world).
 *
 * <p>Restores logical identity after restart so packet item-displays can be respawned.
 */
public final class CustomBlockPlacementsData extends SavedData {

    public static final Codec<CustomBlockPlacementsData> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .optionalFieldOf("entries", Map.of())
                    .forGetter(CustomBlockPlacementsData::toStringMap)
            )
            .apply(i, CustomBlockPlacementsData::fromStringMap)
    );

    public static final SavedDataType<CustomBlockPlacementsData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("mintychochip", "custom_block_placements"),
        CustomBlockPlacementsData::new,
        CODEC,
        DataFixTypes.NONE
    );

    private final ConcurrentHashMap<Long, String> byPos = new ConcurrentHashMap<>();

    public CustomBlockPlacementsData() {
    }

    private CustomBlockPlacementsData(final Map<Long, String> initial) {
        this.byPos.putAll(initial);
    }

    public static @NotNull CustomBlockPlacementsData get(final @NotNull ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void put(final @NotNull BlockPos pos, final @NotNull NamespacedKey blockId) {
        this.byPos.put(pos.asLong(), Objects.requireNonNull(blockId, "blockId").toString());
        this.setDirty();
    }

    public void put(final @NotNull BlockPos pos, final @NotNull String namespacedId) {
        this.byPos.put(pos.asLong(), Objects.requireNonNull(namespacedId, "namespacedId"));
        this.setDirty();
    }

    public @NotNull Optional<NamespacedKey> get(final @NotNull BlockPos pos) {
        final String raw = this.byPos.get(pos.asLong());
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(NamespacedKey.fromString(raw));
    }

    public @Nullable NamespacedKey remove(final @NotNull BlockPos pos) {
        final String raw = this.byPos.remove(pos.asLong());
        if (raw != null) {
            this.setDirty();
            return NamespacedKey.fromString(raw);
        }
        return null;
    }

    /**
     * Entries in a chunk section footprint (block coords → chunk coords).
     */
    public @NotNull List<Map.Entry<BlockPos, NamespacedKey>> entriesInChunk(final int chunkX, final int chunkZ) {
        final List<Map.Entry<BlockPos, NamespacedKey>> out = new ArrayList<>();
        for (final var e : this.byPos.entrySet()) {
            final BlockPos pos = BlockPos.of(e.getKey());
            if ((pos.getX() >> 4) != chunkX || (pos.getZ() >> 4) != chunkZ) {
                continue;
            }
            final NamespacedKey key = NamespacedKey.fromString(e.getValue());
            if (key != null) {
                out.add(Map.entry(pos, key));
            }
        }
        return out;
    }

    public int size() {
        return this.byPos.size();
    }

    private Map<String, String> toStringMap() {
        final Map<String, String> out = new HashMap<>(this.byPos.size());
        for (final var e : this.byPos.entrySet()) {
            final BlockPos pos = BlockPos.of(e.getKey());
            out.put(pos.getX() + "," + pos.getY() + "," + pos.getZ(), e.getValue());
        }
        return out;
    }

    private static CustomBlockPlacementsData fromStringMap(final Map<String, String> map) {
        final Map<Long, String> parsed = new HashMap<>();
        for (final var e : map.entrySet()) {
            final String[] parts = e.getKey().split(",");
            if (parts.length != 3) {
                continue;
            }
            try {
                final BlockPos pos = new BlockPos(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2])
                );
                parsed.put(pos.asLong(), e.getValue());
            } catch (final NumberFormatException ignored) {
            }
        }
        return new CustomBlockPlacementsData(parsed);
    }
}
