package dev.mintychochip.customentity;

import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;

/**
 * Spawn / apply path for custom entities on vanilla carriers (Bukkit API only, no NMS).
 *
 * <p>Primary host: {@link EntityHostType#BLOCK_MODEL} → {@link BlockDisplay} with block model + transform + PDC identity.
 */
public final class CustomEntityLifecycle {

    private CustomEntityLifecycle() {
    }

    /**
     * Apply a registered (or any) block-model definition onto an existing {@link BlockDisplay}.
     * Sets block data, transformation, and stamps custom identity.
     */
    public static void apply(
        @NotNull final BlockDisplay display,
        @NotNull final CustomEntityDefinition definition
    ) {
        Objects.requireNonNull(display, "display");
        Objects.requireNonNull(definition, "definition");
        final BlockModelHostSpec host = requireBlockModel(definition);
        final BlockData blockData = host.blockMaterial().createBlockData();
        apply(display, definition, blockData);
    }

    /**
     * Core apply path with injected {@link BlockData} so unit tests can drive the shipped
     * presentation + identity logic without a full block registry bootstrap.
     */
    public static void apply(
        @NotNull final BlockDisplay display,
        @NotNull final CustomEntityDefinition definition,
        @NotNull final BlockData blockData
    ) {
        Objects.requireNonNull(display, "display");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(blockData, "blockData");
        final BlockModelHostSpec host = requireBlockModel(definition);

        display.setBlock(blockData);
        final Transformation transform = host.toTransformation();
        display.setTransformation(transform);
        CustomEntities.stamp(display, definition);
    }

    /**
     * Spawn a new {@link BlockDisplay} at {@code location} and apply the definition.
     *
     * @return the live in-world carrier (type remains {@link EntityType#BLOCK_DISPLAY})
     */
    public static @NotNull BlockDisplay spawn(
        @NotNull final Location location,
        @NotNull final CustomEntityDefinition definition
    ) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(definition, "definition");
        final World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("location has no world");
        }
        requireBlockModel(definition);

        final BlockDisplay display = world.spawn(location, BlockDisplay.class, entity -> apply(entity, definition));
        return display;
    }

    /**
     * Spawn by registered key.
     *
     * @throws IllegalArgumentException if the key is not registered or not block-model
     */
    public static @NotNull BlockDisplay spawn(
        @NotNull final Location location,
        @NotNull final org.bukkit.NamespacedKey key
    ) {
        Objects.requireNonNull(key, "key");
        final CustomEntityDefinition def = CustomEntities.get(key)
            .orElseThrow(() -> new IllegalArgumentException("custom entity not registered: " + key));
        return spawn(location, def);
    }

    /**
     * Whether this entity is a block-display carrier suitable for block-model custom entities.
     */
    public static boolean isBlockModelCarrier(@NotNull final Entity entity) {
        Objects.requireNonNull(entity, "entity");
        return entity instanceof BlockDisplay || entity.getType() == EntityType.BLOCK_DISPLAY;
    }

    private static BlockModelHostSpec requireBlockModel(final CustomEntityDefinition definition) {
        final EntityHostSpec host = definition.host();
        if (!(host instanceof BlockModelHostSpec blockHost)) {
            throw new IllegalArgumentException(
                "custom entity " + definition.namespacedKey() + " host is not BLOCK_MODEL: " + definition.hostType()
            );
        }
        return blockHost;
    }
}
