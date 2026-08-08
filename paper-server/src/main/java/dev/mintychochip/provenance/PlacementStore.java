package dev.mintychochip.provenance;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Placement memory facade: persists via {@link ProvenancePlacementsData} on
 * {@link ServerLevel}, with an in-memory fallback for tests / non-server levels.
 */
public final class PlacementStore {

    /** Test / offline map keyed by {@code dimensionId:x,y,z}. */
    private final ConcurrentHashMap<String, PlacementRecord> memory = new ConcurrentHashMap<>();

    public void put(
        final @NotNull Level level,
        final @NotNull BlockPos pos,
        final @NotNull PlacementRecord record
    ) {
        if (level instanceof ServerLevel serverLevel) {
            ProvenancePlacementsData.get(serverLevel).put(pos.immutable(), record);
            return;
        }
        this.put(dimensionId(level), pos, record);
    }

    /** Test API: dimension id e.g. {@code minecraft:overworld}. */
    public void put(
        final @NotNull String dimensionId,
        final @NotNull BlockPos pos,
        final @NotNull PlacementRecord record
    ) {
        this.memory.put(key(dimensionId, pos), Objects.requireNonNull(record, "record"));
    }

    public @NotNull Optional<PlacementRecord> get(final @NotNull Level level, final @NotNull BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            return ProvenancePlacementsData.get(serverLevel).get(pos);
        }
        return this.get(dimensionId(level), pos);
    }

    public @NotNull Optional<PlacementRecord> get(final @NotNull String dimensionId, final @NotNull BlockPos pos) {
        return Optional.ofNullable(this.memory.get(key(dimensionId, pos)));
    }

    public @Nullable PlacementRecord remove(final @NotNull Level level, final @NotNull BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            return ProvenancePlacementsData.get(serverLevel).remove(pos);
        }
        return this.remove(dimensionId(level), pos);
    }

    public @Nullable PlacementRecord remove(final @NotNull String dimensionId, final @NotNull BlockPos pos) {
        return this.memory.remove(key(dimensionId, pos));
    }

    public void move(final @NotNull Level level, final @NotNull BlockPos from, final @NotNull BlockPos to) {
        if (level instanceof ServerLevel serverLevel) {
            ProvenancePlacementsData.get(serverLevel).move(from.immutable(), to.immutable());
            return;
        }
        final String dim = dimensionId(level);
        final PlacementRecord record = this.memory.remove(key(dim, from));
        if (record != null) {
            this.memory.put(key(dim, to), record);
        }
    }

    public void move(
        final @NotNull String dimensionId,
        final @NotNull BlockPos from,
        final @NotNull BlockPos to
    ) {
        final PlacementRecord record = this.memory.remove(key(dimensionId, from));
        if (record != null) {
            this.memory.put(key(dimensionId, to), record);
        }
    }

    public void clear() {
        this.memory.clear();
    }

    public int size() {
        return this.memory.size();
    }

    /** Live count for a server dimension (persistent store). */
    public int size(final @NotNull ServerLevel level) {
        return ProvenancePlacementsData.get(level).size();
    }

    private static @NotNull String dimensionId(final Level level) {
        return level.dimension().identifier().toString();
    }

    private static @NotNull String key(final String dimensionId, final BlockPos pos) {
        return dimensionId + ':' + pos.getX() + ',' + pos.getY() + ',' + pos.getZ();
    }
}
