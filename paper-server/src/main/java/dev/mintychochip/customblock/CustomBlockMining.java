package dev.mintychochip.customblock;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.jetbrains.annotations.Nullable;

/**
 * Server-side mining / blast feel for custom blocks (NMS helpers for thin vanilla hooks).
 */
public final class CustomBlockMining {

    private CustomBlockMining() {
    }

    /**
     * Custom destroy progress, or {@link Float#NaN} if this is not a custom block.
     * Mirrors vanilla {@code getDestroyProgress}: {@code speed / hardness / (correct ? 30 : 100)}.
     */
    public static float customDestroyProgress(
        final BlockState state,
        final Player player,
        final BlockGetter level,
        final BlockPos pos
    ) {
        final Optional<CustomBlockDefinition> def = definitionAt(level, pos);
        if (def.isEmpty()) {
            return Float.NaN;
        }
        if (player instanceof ServerPlayer serverPlayer
            && serverPlayer.getBukkitEntity().getGameMode() == GameMode.CREATIVE) {
            // Creative still uses vanilla insta rules elsewhere; leave a fast progress.
            return 1.0F;
        }

        final BlockFeel feel = def.get().feel();
        final float hardness = feel.hardness();
        if (hardness < 0.0F) {
            return 0.0F; // unbreakable
        }
        if (hardness == 0.0F) {
            return 1.0F;
        }

        final BlockState toolState = toolTemplateState(feel, state);
        final float speed = player.getDestroySpeed(toolState);
        final boolean correct = isCorrectTool(player, feel, toolState);
        final int modifier = correct ? 30 : 100;
        return speed / hardness / modifier;
    }

    /**
     * Custom explosion resistance at a position, or {@code null} if not a custom block.
     */
    public static @Nullable Float customExplosionResistance(final BlockGetter level, final BlockPos pos) {
        final Optional<CustomBlockDefinition> def = definitionAt(level, pos);
        if (def.isEmpty()) {
            return null;
        }
        return def.get().feel().blastResistance();
    }

    /** Whether the player may receive the custom-block drop for this feel. */
    public static boolean canHarvest(final org.bukkit.entity.Player player, final BlockFeel feel) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }
        if (!feel.requiresCorrectToolForDrops()) {
            return true;
        }
        if (!(player instanceof org.bukkit.craftbukkit.entity.CraftPlayer craftPlayer)) {
            return true;
        }
        final ServerPlayer handle = craftPlayer.getHandle();
        final BlockState toolState = toolTemplateState(feel, null);
        return isCorrectTool(handle, feel, toolState);
    }

    private static boolean isCorrectTool(final Player player, final BlockFeel feel, final BlockState toolState) {
        if (!feel.requiresCorrectToolForDrops()) {
            return true;
        }
        // Vanilla: correct if state does not require tool OR item is correct for that state.
        return player.hasCorrectToolForDrops(toolState);
    }

    private static BlockState toolTemplateState(final BlockFeel feel, final @Nullable BlockState fallback) {
        final Material template = feel.toolTemplate();
        if (template != null) {
            final net.minecraft.world.level.block.Block nms = CraftMagicNumbers.getBlock(template);
            if (nms != null) {
                return nms.defaultBlockState();
            }
        }
        // Fall back to actual carrier state (less accurate for tool tags).
        if (fallback != null) {
            return fallback;
        }
        return net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
    }

    private static Optional<CustomBlockDefinition> definitionAt(final BlockGetter level, final BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        // Cheap position-based gate for the default memory lookup: most mined blocks are
        // vanilla carriers, so avoid allocating a CraftBlock wrapper unless the placement
        // index actually has an entry here. Custom blocks deliberately reuse vanilla
        // carrier materials, so this must key on the placement store, not the block type.
        // Only applies to MemoryCustomBlockLookup; other lookup impls retain the full path.
        if (CustomBlocks.lookup() instanceof MemoryCustomBlockLookup
            && CustomBlockPlacementsData.get(serverLevel).get(pos).isEmpty()) {
            return Optional.empty();
        }
        try {
            return CustomBlocks.of(CraftBlock.at(serverLevel, pos));
        } catch (final Throwable t) {
            return Optional.empty();
        }
    }
}
