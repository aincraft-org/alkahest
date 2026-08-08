package dev.mintychochip.customblock;

import io.papermc.paper.datacomponent.DataComponentTypes;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Process-wide entry for custom block definitions and identity queries (API, no NMS).
 *
 * <p>{@link CustomBlockDefinition} implements {@link Material}: after {@link #register},
 * definitions are reachable via {@link Material#getByKey} / {@link Material#matchMaterial}
 * and can be used wherever a {@code Material} is expected for identity/lookups.
 * Live world and inventory still expose the vanilla carrier via
 * {@link Block#getType()} / {@link ItemStack#getType()}; logical content is resolved via
 * {@link #of(Block)}, {@link #of(ItemStack)}, and {@link #keyOf} (or the additive
 * {@code getCustomBlock()} helpers on those types).
 *
 * <p>World placement is resolved through {@link CustomBlockLookup} (server sets a real
 * implementation). Item identity is stamped in PDC under {@link CustomBlockKeys#ITEM_ID}.
 */
public final class CustomBlocks {

    private static volatile CustomBlockCatalog catalog = CustomBlockCatalog.create();
    private static volatile CustomBlockLookup lookup = CustomBlockLookup.NOOP;

    private CustomBlocks() {
    }

    // ---- catalog ----

    /** Active definition catalog. */
    public static CustomBlockCatalog catalog() {
        return catalog;
    }

    /**
     * Replace the active catalog (e.g. tests or full reload). Prefer {@link #register}
     * for normal bootstrap.
     */
    public static void setCatalog(final CustomBlockCatalog catalog) {
        CustomBlocks.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    /** Active world/item lookup (placement store). Defaults to {@link CustomBlockLookup#NOOP}. */
    public static CustomBlockLookup lookup() {
        return lookup;
    }

    /** Install server placement lookup. Additive; call once at bootstrap. */
    public static void setLookup(final CustomBlockLookup lookup) {
        CustomBlocks.lookup = Objects.requireNonNull(lookup, "lookup");
    }

    /**
     * Reset catalog + lookup to empty defaults (tests).
     */
    public static void reset() {
        catalog = CustomBlockCatalog.create();
        lookup = CustomBlockLookup.NOOP;
    }

    public static void register(final CustomBlockDefinition definition) {
        catalog().register(definition);
    }

    public static Optional<CustomBlockDefinition> get(final NamespacedKey key) {
        return catalog().get(key);
    }

    public static Optional<CustomBlockDefinition> get(final Key key) {
        return catalog().get(key);
    }

    public static Optional<CustomBlockDefinition> get(final String namespacedKey) {
        return catalog().get(namespacedKey);
    }

    public static @Nullable CustomBlockDefinition getOrNull(final NamespacedKey key) {
        return catalog().getOrNull(key);
    }

    public static boolean contains(final NamespacedKey key) {
        return catalog().contains(key);
    }

    public static Collection<CustomBlockDefinition> all() {
        return catalog().all();
    }

    // ---- identity: block / item ----

    /**
     * Logical custom-block key at this world position, if any.
     * Does not change {@link Block#getType()} (vanilla carrier).
     */
    public static @NotNull Optional<NamespacedKey> keyOf(@NotNull final Block block) {
        Objects.requireNonNull(block, "block");
        return lookup().keyAt(block);
    }

    /**
     * Logical custom-block key stamped on this stack, if any.
     * Does not change {@link ItemStack#getType()} (vanilla base item).
     */
    public static @NotNull Optional<NamespacedKey> keyOf(@Nullable final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        final Optional<NamespacedKey> fromLookup = lookup().keyOfItem(stack);
        if (fromLookup.isPresent()) {
            return fromLookup;
        }
        return CustomBlockItemTags.read(stack.getPersistentDataContainer());
    }

    /**
     * Definition for the custom block at this world position, if registered.
     */
    public static @NotNull Optional<CustomBlockDefinition> of(@NotNull final Block block) {
        return keyOf(block).flatMap(CustomBlocks::get);
    }

    /**
     * Definition for the custom block item in hand / inventory, if registered.
     */
    public static @NotNull Optional<CustomBlockDefinition> of(@Nullable final ItemStack stack) {
        return keyOf(stack).flatMap(CustomBlocks::get);
    }

    public static boolean isCustomBlock(@NotNull final Block block) {
        return keyOf(block).isPresent();
    }

    public static boolean isCustomBlockItem(@Nullable final ItemStack stack) {
        return keyOf(stack).isPresent();
    }

    // ---- item stamping (held form) ----

    /**
     * Create a new stack representing this custom block for inventory.
     * Base material is {@link CustomBlockDefinition#itemMaterial()}; identity is PDC + item model.
     */
    public static @NotNull ItemStack createItemStack(@NotNull final CustomBlockDefinition definition) {
        return createItemStack(definition, 1);
    }

    public static @NotNull ItemStack createItemStack(
        @NotNull final CustomBlockDefinition definition,
        final int amount
    ) {
        Objects.requireNonNull(definition, "definition");
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be >= 1");
        }
        final Material material = definition.itemMaterial();
        final ItemStack stack = ItemStack.of(material, amount);
        stamp(stack, definition);
        return stack;
    }

    /**
     * Stamp an existing stack as this custom block (mutates stack).
     * Sets PDC identity, item model, and optional display name / lore from the definition.
     */
    public static void stamp(@NotNull final ItemStack stack, @NotNull final CustomBlockDefinition definition) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(definition, "definition");
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("cannot stamp empty stack");
        }
        stack.editPersistentDataContainer(pdc -> CustomBlockItemTags.write(pdc, definition));
        stack.setData(DataComponentTypes.ITEM_MODEL, definition.itemModel());
        final Component name = definition.displayName();
        if (name != null) {
            stack.editMeta(meta -> meta.displayName(name));
        }
        final List<Component> lore = definition.itemLore();
        if (lore != null) {
            stack.editMeta(meta -> meta.lore(lore));
        }
    }

    /**
     * Stamp only the custom-block id (no model/name). Useful when presentation is applied elsewhere.
     */
    public static void stampId(@NotNull final ItemStack stack, @NotNull final NamespacedKey blockId) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(blockId, "blockId");
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("cannot stamp empty stack");
        }
        stack.editPersistentDataContainer(pdc -> CustomBlockItemTags.write(pdc, blockId));
    }

    /** Remove custom-block identity from a stack (does not reset model/name). */
    public static void clearStamp(@NotNull final ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.isEmpty()) {
            return;
        }
        stack.editPersistentDataContainer(CustomBlockItemTags::clear);
    }
}
