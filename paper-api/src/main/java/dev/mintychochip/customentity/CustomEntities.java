package dev.mintychochip.customentity;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Process-wide entry for custom entity definitions and identity queries (API, no NMS).
 *
 * <p>{@link CustomEntityDefinition} implements {@link EntityType}: after {@link #register},
 * definitions are reachable via {@link EntityType#getByKey} and
 * {@link org.bukkit.Registry#ENTITY_TYPE}, and can be used wherever an {@code EntityType}
 * is expected for identity/lookups. {@link EntityType#values()} stays vanilla-only.
 *
 * <p>Live {@link Entity#getType()} still returns the vanilla carrier; logical content is
 * resolved via {@link #of(Entity)} and {@link #keyOf} (or the additive
 * {@code getCustomEntity()} helpers on entities).
 *
 * <p>Entity identity is stamped in PDC under {@link CustomEntityKeys#ENTITY_ID}.
 * World spawn/apply of block-model carriers is {@link CustomEntityLifecycle}.
 */
public final class CustomEntities {

    private static volatile CustomEntityCatalog catalog = CustomEntityCatalog.create();

    private CustomEntities() {
    }

    // ---- catalog ----

    /** Active definition catalog. */
    public static CustomEntityCatalog catalog() {
        return catalog;
    }

    /**
     * Replace the active catalog (e.g. tests or full reload). Prefer {@link #register}
     * for normal bootstrap.
     */
    public static void setCatalog(final CustomEntityCatalog catalog) {
        CustomEntities.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    /**
     * Reset catalog to empty defaults (tests).
     */
    public static void reset() {
        catalog = CustomEntityCatalog.create();
    }

    public static void register(final CustomEntityDefinition definition) {
        catalog().register(definition);
    }

    public static Optional<CustomEntityDefinition> get(final NamespacedKey key) {
        return catalog().get(key);
    }

    public static Optional<CustomEntityDefinition> get(final Key key) {
        return catalog().get(key);
    }

    public static Optional<CustomEntityDefinition> get(final String namespacedKey) {
        return catalog().get(namespacedKey);
    }

    public static @Nullable CustomEntityDefinition getOrNull(final NamespacedKey key) {
        return catalog().getOrNull(key);
    }

    public static boolean contains(final NamespacedKey key) {
        return catalog().contains(key);
    }

    public static Collection<CustomEntityDefinition> all() {
        return catalog().all();
    }

    // ---- identity: entity ----

    /**
     * Logical custom-entity key stamped on this entity, if any.
     * Does not change {@link Entity#getType()} (vanilla carrier).
     */
    public static @NotNull Optional<NamespacedKey> keyOf(@Nullable final Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        return CustomEntityTags.read(entity.getPersistentDataContainer());
    }

    /**
     * Definition for the custom entity, if registered.
     */
    public static @NotNull Optional<CustomEntityDefinition> of(@Nullable final Entity entity) {
        return keyOf(entity).flatMap(CustomEntities::get);
    }

    public static boolean isCustomEntity(@Nullable final Entity entity) {
        return keyOf(entity).isPresent();
    }

    // ---- stamping ----

    /**
     * Stamp custom-entity identity (and optional display name) onto an existing entity.
     * Does not change {@link Entity#getType()} or visual carrier state.
     */
    public static void stamp(@NotNull final Entity entity, @NotNull final CustomEntityDefinition definition) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(definition, "definition");
        CustomEntityTags.write(entity.getPersistentDataContainer(), definition);
        final Component name = definition.displayName();
        if (name != null) {
            entity.customName(name);
            entity.setCustomNameVisible(true);
        }
    }

    /**
     * Stamp only the custom-entity id (no name). Useful when presentation is applied elsewhere.
     */
    public static void stampId(@NotNull final Entity entity, @NotNull final NamespacedKey entityId) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(entityId, "entityId");
        CustomEntityTags.write(entity.getPersistentDataContainer(), entityId);
    }

    /** Remove custom-entity identity from an entity (does not reset name / visuals). */
    public static void clearStamp(@NotNull final Entity entity) {
        Objects.requireNonNull(entity, "entity");
        CustomEntityTags.clear(entity.getPersistentDataContainer());
    }

    // ---- spawn (EntityType parity) ----

    /**
     * Spawn a registered custom entity by key. Same idea as
     * {@code world.spawnEntity(loc, EntityType.PIG)} but for custom types.
     *
     * <p>Prefer {@code world.spawnEntity(loc, definition)} once you hold an
     * {@link EntityType} / {@link CustomEntityDefinition} instance.
     */
    public static @NotNull Entity spawn(@NotNull final Location location, @NotNull final NamespacedKey key) {
        return CustomEntityLifecycle.spawn(location, key);
    }

    public static @NotNull Entity spawn(@NotNull final Location location, @NotNull final String namespacedKey) {
        final NamespacedKey key = NamespacedKey.fromString(namespacedKey);
        if (key == null) {
            throw new IllegalArgumentException("invalid namespaced key: " + namespacedKey);
        }
        return spawn(location, key);
    }

    public static @NotNull Entity spawn(
        @NotNull final Location location,
        @NotNull final CustomEntityDefinition definition
    ) {
        return CustomEntityLifecycle.spawn(location, definition);
    }

    /**
     * Spawn any {@link EntityType} (vanilla constant or custom definition).
     * Delegates to {@link org.bukkit.World#spawnEntity(Location, EntityType)}.
     */
    public static @NotNull Entity spawn(@NotNull final Location location, @NotNull final EntityType type) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(type, "type");
        final org.bukkit.World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("location has no world");
        }
        return world.spawnEntity(location, type);
    }
}
