package dev.mintychochip.customblock;

import java.util.Objects;
import java.util.Optional;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import io.papermc.paper.persistence.PersistentDataViewHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Read/write custom-block identity on item PDC (and any {@link PersistentDataHolder}).
 *
 * <p>This is the held-form of a custom block: vanilla {@code getType()} stays the base
 * material; the logical content id lives under {@link CustomBlockKeys#ITEM_ID}.
 */
public final class CustomBlockItemTags {

    private CustomBlockItemTags() {
    }

    public static void write(@NotNull PersistentDataContainer pdc, @NotNull NamespacedKey blockId) {
        Objects.requireNonNull(pdc, "pdc");
        Objects.requireNonNull(blockId, "blockId");
        pdc.set(CustomBlockKeys.ITEM_ID, PersistentDataType.STRING, blockId.toString());
    }

    public static void write(@NotNull PersistentDataContainer pdc, @NotNull CustomBlockDefinition definition) {
        write(pdc, definition.namespacedKey());
    }

    public static void clear(@NotNull PersistentDataContainer pdc) {
        Objects.requireNonNull(pdc, "pdc");
        pdc.remove(CustomBlockKeys.ITEM_ID);
    }

    public static @NotNull Optional<NamespacedKey> read(@Nullable PersistentDataViewHolder holder) {
        if (holder == null) {
            return Optional.empty();
        }
        return read(holder.getPersistentDataContainer());
    }

    public static @NotNull Optional<NamespacedKey> read(
        @Nullable io.papermc.paper.persistence.PersistentDataContainerView view
    ) {
        if (view == null) {
            return Optional.empty();
        }
        final String raw = view.get(CustomBlockKeys.ITEM_ID, PersistentDataType.STRING);
        return parse(raw);
    }

    public static @NotNull Optional<NamespacedKey> read(@Nullable PersistentDataContainer pdc) {
        if (pdc == null) {
            return Optional.empty();
        }
        final String raw = pdc.get(CustomBlockKeys.ITEM_ID, PersistentDataType.STRING);
        return parse(raw);
    }

    public static @NotNull Optional<NamespacedKey> parse(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        final NamespacedKey key = NamespacedKey.fromString(raw);
        return Optional.ofNullable(key);
    }

    public static boolean has(@Nullable io.papermc.paper.persistence.PersistentDataContainerView view) {
        return read(view).isPresent();
    }
}
