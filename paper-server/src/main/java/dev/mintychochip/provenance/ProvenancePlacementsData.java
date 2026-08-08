package dev.mintychochip.provenance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Per-dimension persistent placement memory for item provenance.
 *
 * <p>Stored via {@link ServerLevel#getDataStorage()} under
 * {@code mintychochip/provenance_placements.dat} (auto-saved with the world).
 */
public final class ProvenancePlacementsData extends SavedData {

    private static final Codec<PlacementRecord> RECORD_CODEC = RecordCodecBuilder.create(
        i -> i.group(
                UUIDUtil.STRING_CODEC.fieldOf("parent").forGetter(PlacementRecord::parentStackId),
                Codec.STRING.fieldOf("item").forGetter(PlacementRecord::blockItemId),
                Codec.STRING.optionalFieldOf("placer").forGetter(r -> Optional.ofNullable(r.placer())),
                Codec.LONG.fieldOf("t").forGetter(PlacementRecord::placedEpochMs)
            )
            .apply(
                i,
                (parent, item, placer, t) -> new PlacementRecord(parent, item, placer.orElse(null), t)
            )
    );

    public static final Codec<ProvenancePlacementsData> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                Codec.unboundedMap(Codec.STRING, RECORD_CODEC)
                    .optionalFieldOf("entries", Map.of())
                    .forGetter(ProvenancePlacementsData::toStringMap)
            )
            .apply(i, ProvenancePlacementsData::fromStringMap)
    );

    public static final SavedDataType<ProvenancePlacementsData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("mintychochip", "provenance_placements"),
        ProvenancePlacementsData::new,
        CODEC,
        DataFixTypes.NONE
    );

    private final ConcurrentHashMap<Long, PlacementRecord> byPos = new ConcurrentHashMap<>();

    public ProvenancePlacementsData() {
        // empty
    }

    private ProvenancePlacementsData(final Map<Long, PlacementRecord> initial) {
        this.byPos.putAll(initial);
    }

    public static @NotNull ProvenancePlacementsData get(final @NotNull ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void put(final @NotNull BlockPos pos, final @NotNull PlacementRecord record) {
        this.byPos.put(pos.asLong(), Objects.requireNonNull(record, "record"));
        this.setDirty();
    }

    public @NotNull Optional<PlacementRecord> get(final @NotNull BlockPos pos) {
        return Optional.ofNullable(this.byPos.get(pos.asLong()));
    }

    public @Nullable PlacementRecord remove(final @NotNull BlockPos pos) {
        final PlacementRecord removed = this.byPos.remove(pos.asLong());
        if (removed != null) {
            this.setDirty();
        }
        return removed;
    }

    /**
     * Relocate placement memory with a piston (or similar). No-op if nothing at {@code from}.
     */
    public void move(final @NotNull BlockPos from, final @NotNull BlockPos to) {
        if (from.asLong() == to.asLong()) {
            return;
        }
        final PlacementRecord record = this.byPos.remove(from.asLong());
        if (record == null) {
            return;
        }
        this.byPos.put(to.asLong(), record);
        this.setDirty();
    }

    public void clear() {
        if (!this.byPos.isEmpty()) {
            this.byPos.clear();
            this.setDirty();
        }
    }

    public int size() {
        return this.byPos.size();
    }

    private Map<String, PlacementRecord> toStringMap() {
        final Map<String, PlacementRecord> out = new HashMap<>(this.byPos.size());
        for (final var e : this.byPos.entrySet()) {
            final BlockPos pos = BlockPos.of(e.getKey());
            out.put(pos.getX() + "," + pos.getY() + "," + pos.getZ(), e.getValue());
        }
        return out;
    }

    private static ProvenancePlacementsData fromStringMap(final Map<String, PlacementRecord> map) {
        final Map<Long, PlacementRecord> packed = new HashMap<>(map.size());
        for (final var e : map.entrySet()) {
            final BlockPos pos = parsePos(e.getKey());
            if (pos != null) {
                packed.put(pos.asLong(), e.getValue());
            }
        }
        return new ProvenancePlacementsData(packed);
    }

    private static @Nullable BlockPos parsePos(final String key) {
        final String[] parts = key.split(",");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new BlockPos(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
            );
        } catch (final NumberFormatException ex) {
            return null;
        }
    }
}
