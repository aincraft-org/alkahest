package dev.mintychochip.customblock;

import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves custom-block identity from world blocks and item stacks.
 *
 * <p>Default is {@link #NOOP} (nothing placed). Server sets a real implementation when
 * placement is wired. Item identity is still readable from PDC without a lookup.
 */
public interface CustomBlockLookup {

    /**
     * Resolve the custom block key at a world block, if any.
     * Does not consult the definition catalog — only placement/baked identity.
     */
    @NotNull
    Optional<org.bukkit.NamespacedKey> keyAt(@NotNull Block block);

    /**
     * Clear custom identity at this block (after break / explosion).
     *
     * @return {@code true} if an identity was removed
     */
    default boolean clearAt(@NotNull Block block) {
        return false;
    }

    /**
     * Optional hook: resolve key from an item without using the default PDC path.
     * Default returns empty so {@link CustomBlocks#keyOf(ItemStack)} uses PDC.
     */
    default @NotNull Optional<org.bukkit.NamespacedKey> keyOfItem(@NotNull ItemStack stack) {
        return Optional.empty();
    }

    /** No world placements; item keys fall through to PDC. */
    CustomBlockLookup NOOP = block -> Optional.empty();
}
