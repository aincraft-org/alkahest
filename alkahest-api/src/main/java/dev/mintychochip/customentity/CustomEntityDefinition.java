package dev.mintychochip.customentity;

import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable definition of a custom entity type (identity + host + optional display name).
 *
 * <p>Implements {@link EntityType} so callers can use the same spawn APIs as vanilla:
 * {@code world.spawnEntity(loc, definition)} or {@code definition.spawn(loc)}.
 *
 * <p>The vanilla carrier remains {@link BlockDisplay} / {@link EntityType#BLOCK_DISPLAY};
 * {@link Entity#getType()} on a live instance is that carrier. Logical identity is this key
 * (PDC + {@link Entity#getCustomKey()}).
 */
public final class CustomEntityDefinition implements EntityType {

    private final NamespacedKey key;
    private final EntityHostSpec host;
    private final @Nullable Component displayName;

    private CustomEntityDefinition(
        final NamespacedKey key,
        final EntityHostSpec host,
        final @Nullable Component displayName
    ) {
        this.key = Objects.requireNonNull(key, "key");
        this.host = Objects.requireNonNull(host, "host");
        this.displayName = displayName;
    }

    public static Builder builder(final NamespacedKey key) {
        return new Builder(key);
    }

    public static Builder builder(final String namespacedKey) {
        final NamespacedKey key = NamespacedKey.fromString(namespacedKey);
        if (key == null) {
            throw new IllegalArgumentException("invalid namespaced key: " + namespacedKey);
        }
        return builder(key);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return this.key;
    }

    @Override
    public @NotNull String name() {
        return this.key.toString();
    }

    public NamespacedKey namespacedKey() {
        return this.key;
    }

    public EntityHostType hostType() {
        return this.host.type();
    }

    public EntityHostSpec host() {
        return this.host;
    }

    public @Nullable Component displayName() {
        return this.displayName;
    }

    public boolean isBlockModel() {
        return this.host.type().isBlockModel();
    }

    /**
     * Block material shown when hosted as a block-model entity.
     *
     * @throws IllegalStateException if the host is not {@link EntityHostType#BLOCK_MODEL}
     */
    public Material blockMaterial() {
        if (this.host instanceof BlockModelHostSpec blockHost) {
            return blockHost.blockMaterial();
        }
        throw new IllegalStateException("host is not block-model: " + this.host.type());
    }

    // ---- EntityType ----

    @Override
    @Deprecated(since = "1.6.2")
    public @Nullable String getName() {
        return this.key.getKey();
    }

    @Override
    public @Nullable Class<? extends Entity> getEntityClass() {
        // Carrier class for block-model hosts; other hosts would map similarly later.
        if (this.host.type().isBlockModel()) {
            return BlockDisplay.class;
        }
        return BlockDisplay.class;
    }

    @Override
    @Deprecated(since = "1.6.2", forRemoval = true)
    public short getTypeId() {
        return -1;
    }

    @Override
    public boolean isSpawnable() {
        return this.host.type().isBlockModel();
    }

    @Override
    public boolean isAlive() {
        return false;
    }

    @Override
    @Deprecated(forRemoval = true)
    public @NotNull String getTranslationKey() {
        return this.translationKey();
    }

    @Override
    public @NotNull String translationKey() {
        // No client lang entry required; fall back to namespaced id.
        return this.key.toString();
    }

    @Override
    public @NotNull SpawnCategory getSpawnCategory() {
        return SpawnCategory.MISC;
    }

    @Override
    public boolean hasDefaultAttributes() {
        return false;
    }

    @Override
    public @NotNull org.bukkit.attribute.Attributable getDefaultAttributes() {
        throw new IllegalArgumentException(this.key + " doesn't have default attributes");
    }

    @Override
    public boolean isVanilla() {
        return false;
    }

    @Override
    public boolean isCustom() {
        return true;
    }

    @Override
    public @NotNull Entity spawn(@NotNull final Location location) {
        Objects.requireNonNull(location, "location");
        final World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("location has no world");
        }
        return world.spawnEntity(location, this);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CustomEntityDefinition that)) {
            return false;
        }
        return this.key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public String toString() {
        return "CustomEntityDefinition[" + this.key + "]";
    }

    public static final class Builder {
        private final NamespacedKey key;
        private EntityHostSpec host;
        private @Nullable Component displayName;

        private Builder(final NamespacedKey key) {
            this.key = Objects.requireNonNull(key, "key");
        }

        public Builder host(final EntityHostSpec host) {
            this.host = Objects.requireNonNull(host, "host");
            return this;
        }

        /** Shorthand for {@code host(BlockModelHostSpec.of(material))}. */
        public Builder blockModel(final Material blockMaterial) {
            this.host = BlockModelHostSpec.of(blockMaterial);
            return this;
        }

        public Builder displayName(final @Nullable Component displayName) {
            this.displayName = displayName;
            return this;
        }

        public CustomEntityDefinition build() {
            if (this.host == null) {
                throw new IllegalStateException("host required");
            }
            return new CustomEntityDefinition(this.key, this.host, this.displayName);
        }
    }
}
