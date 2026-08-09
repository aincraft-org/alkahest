package dev.mintychochip.customentity;

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
 * Read/write custom-entity identity on entity PDC (and any {@link PersistentDataHolder}).
 *
 * <p>Vanilla {@code getType()} stays the carrier entity type (e.g. {@code BLOCK_DISPLAY});
 * the logical content id lives under {@link CustomEntityKeys#ENTITY_ID}.
 */
public final class CustomEntityTags {

    private CustomEntityTags() {
    }

    public static void write(@NotNull PersistentDataContainer pdc, @NotNull NamespacedKey entityId) {
        Objects.requireNonNull(pdc, "pdc");
        Objects.requireNonNull(entityId, "entityId");
        pdc.set(CustomEntityKeys.ENTITY_ID, PersistentDataType.STRING, entityId.toString());
    }

    public static void write(@NotNull PersistentDataContainer pdc, @NotNull CustomEntityDefinition definition) {
        write(pdc, definition.namespacedKey());
    }

    public static void clear(@NotNull PersistentDataContainer pdc) {
        Objects.requireNonNull(pdc, "pdc");
        pdc.remove(CustomEntityKeys.ENTITY_ID);
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
        final String raw = view.get(CustomEntityKeys.ENTITY_ID, PersistentDataType.STRING);
        return parse(raw);
    }

    public static @NotNull Optional<NamespacedKey> read(@Nullable PersistentDataContainer pdc) {
        if (pdc == null) {
            return Optional.empty();
        }
        final String raw = pdc.get(CustomEntityKeys.ENTITY_ID, PersistentDataType.STRING);
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
