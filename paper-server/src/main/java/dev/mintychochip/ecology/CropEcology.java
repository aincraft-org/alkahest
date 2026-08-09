package dev.mintychochip.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

/**
 * Crop ecology simulation. Catalog/thresholds load from
 * {@code config/mintychochip/ecology.json}.
 */
public final class CropEcology {
    private CropEcology() {
    }

    private static EcologySettings settings(final ServerLevel level) {
        return EcologyConfig.ensureLoaded(level.getServer().getServerDirectory());
    }

    public static boolean shouldGrow(
        final ServerLevel level,
        final BlockPos pos,
        final Block block,
        final RandomSource random
    ) {
        final EcologySettings cfg = settings(level);
        final CropProfile crop = profile(cfg, block);
        if (crop == null) {
            return true;
        }
        final ClimateSample climate = sampleClimate(cfg, level, pos);
        final double p = SuitabilityEngine.acceptProbability(crop, climate);
        if (p <= 0.0) {
            return false;
        }
        return random.nextDouble() < p;
    }

    public static boolean isSuitable(final LevelReader level, final BlockPos pos, final Block block) {
        final CropProfile crop = profile(block);
        if (crop == null) {
            return true;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return true;
        }
        return SuitabilityEngine.isSuitable(crop, sampleClimate(settings(serverLevel), serverLevel, pos));
    }

    public static boolean allowsForcedGrowth(final Level level, final BlockPos pos, final Block block) {
        return isSuitable(level, pos, block);
    }

    /**
     * Pops an unsuitable crop as drops without neighbor/shape location updates.
     * Client is still notified so the block disappears; adjacent blocks are not told to re-check.
     */
    public static boolean popIfUnsuitable(final Level level, final BlockPos pos, final Block block) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        final EcologySettings cfg = settings(serverLevel);
        final CropProfile crop = profile(cfg, block);
        if (crop == null) {
            return false;
        }
        if (SuitabilityEngine.isSuitable(crop, sampleClimate(cfg, serverLevel, pos))) {
            return false;
        }
        popQuietly(serverLevel, pos);
        return true;
    }

    /**
     * Drop resources and clear the block without {@link Block#UPDATE_NEIGHBORS} or shape cascades.
     * Avoids farm/cane/crop neighbor break chains from ecology rejection.
     */
    private static void popQuietly(final ServerLevel level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        final FluidState fluidState = level.getFluidState(pos);
        final BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        Block.dropResources(state, level, pos, blockEntity, null, ItemStack.EMPTY, false);
        if (!(state.getBlock() instanceof BaseFireBlock)) {
            level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
        }
        // CLIENTS: sync removal; KNOWN_SHAPE: skip neighbour shape/location updates
        final boolean removed = level.setBlock(
            pos,
            fluidState.createLegacyBlock(),
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
        );
        if (removed) {
            level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(state));
        }
    }

    public static @Nullable CropProfile profile(final Block block) {
        return profile(EcologyConfig.get(), block);
    }

    private static @Nullable CropProfile profile(final EcologySettings cfg, final Block block) {
        final Identifier key = BuiltInRegistries.BLOCK.getKey(block);
        if (key == null) {
            return null;
        }
        return cfg.catalog().forBlock(key.toString());
    }

    public static ClimateSample sampleClimate(final ServerLevel level, final BlockPos pos) {
        return sampleClimate(settings(level), level, pos);
    }

    public static ClimateSample sampleClimate(final EcologySettings cfg, final ServerLevel level, final BlockPos pos) {
        final Holder<Biome> biomeHolder = level.getBiome(pos);
        final Biome biome = biomeHolder.value();
        final String biomeKey = biomeHolder.unwrapKey()
            .map(k -> k.identifier().toString())
            .orElse("minecraft:plains");
        final String region = BiomeCategories.category(biomeKey);
        final double base = clamp01(biome.climateSettings.downfall());
        final double water = WaterProximity.bonus(
            cfg.waterRadius(),
            cfg.waterBonusCap(),
            new LevelColumnProbe(level, pos.getX(), pos.getZ())
        );
        // isRainingAt is liquid rain only (Biome.Precipitation.RAIN) — snow does not bump humidity
        final boolean raining = level.isRainingAt(pos);
        final double humidity = ClimateHumidity.compose(
            base,
            water,
            raining,
            cfg.rainHumidityBonus()
        );
        return new ClimateSample(humidity, region);
    }

    private static double clamp01(final double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static final class LevelColumnProbe implements WaterProximity.ColumnProbe {
        private final ServerLevel level;
        private final int originX;
        private final int originZ;

        LevelColumnProbe(final ServerLevel level, final int originX, final int originZ) {
            this.level = level;
            this.originX = originX;
            this.originZ = originZ;
        }

        @Override
        public boolean loadedAt(final int blockX, final int blockZ) {
            final int x = this.originX + blockX;
            final int z = this.originZ + blockZ;
            return this.level.getChunkSource().hasChunk(x >> 4, z >> 4);
        }

        @Override
        public boolean waterAt(final int blockX, final int blockZ) {
            final int x = this.originX + blockX;
            final int z = this.originZ + blockZ;
            // WaterProximity only calls waterAt after loadedAt succeeds, so no chunk guard is needed here.
            final int y = this.level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            final BlockPos surface = new BlockPos(x, y, z);
            if (this.level.getFluidState(surface).is(FluidTags.WATER)) {
                return true;
            }
            return this.level.getFluidState(surface.below()).is(FluidTags.WATER);
        }
    }
}