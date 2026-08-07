package dev.mintychochip.customblock;

import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Immutable definition of a custom block type (identity + host + item + feel).
 *
 * <p>{@link BlockFeel} controls hardness, preferred tool, and blast resistance so the
 * custom block can emulate a vanilla block even when hosted on a different carrier.
 */
public final class CustomBlockDefinition implements Keyed {

    private final NamespacedKey key;
    private final HostSpec host;
    private final Material itemMaterial;
    private final Key itemModel;
    private final @Nullable Component displayName;
    private final @Nullable List<Component> itemLore;
    private final BlockFeel feel;

    private CustomBlockDefinition(
        final NamespacedKey key,
        final HostSpec host,
        final Material itemMaterial,
        final Key itemModel,
        final @Nullable Component displayName,
        final @Nullable List<Component> itemLore,
        final BlockFeel feel
    ) {
        this.key = Objects.requireNonNull(key, "key");
        this.host = Objects.requireNonNull(host, "host");
        this.itemMaterial = Objects.requireNonNull(itemMaterial, "itemMaterial");
        this.itemModel = Objects.requireNonNull(itemModel, "itemModel");
        this.feel = Objects.requireNonNull(feel, "feel");
        // Avoid Material#isAir() — it touches BlockType registry (needs full server bootstrap).
        if (itemMaterial == Material.AIR
            || itemMaterial == Material.CAVE_AIR
            || itemMaterial == Material.VOID_AIR) {
            throw new IllegalArgumentException("itemMaterial must not be air");
        }
        this.displayName = displayName;
        this.itemLore = itemLore == null ? null : List.copyOf(itemLore);
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
    public @NotNull Key key() {
        return this.key;
    }

    public NamespacedKey namespacedKey() {
        return this.key;
    }

    public BlockHostType hostType() {
        return this.host.type();
    }

    public HostSpec host() {
        return this.host;
    }

    public Material itemMaterial() {
        return this.itemMaterial;
    }

    public Key itemModel() {
        return this.itemModel;
    }

    public @Nullable Component displayName() {
        return this.displayName;
    }

    public @Nullable @Unmodifiable List<Component> itemLore() {
        return this.itemLore;
    }

    /** Mining / blast “feel” (often emulating a vanilla block). */
    public BlockFeel feel() {
        return this.feel;
    }

    public boolean isBaked() {
        return this.host.type().isBaked();
    }

    public boolean isPacket() {
        return this.host.type().isPacket();
    }

    public static final class Builder {
        private final NamespacedKey key;
        private HostSpec host;
        /** Placeable base so clients play place animation (not paper use-item). */
        private Material itemMaterial = Material.GLASS;
        private Key itemModel;
        private @Nullable Component displayName;
        private @Nullable List<Component> itemLore;
        private BlockFeel feel = BlockFeel.DEFAULT;

        private Builder(final NamespacedKey key) {
            this.key = Objects.requireNonNull(key, "key");
            // Default item model path matches the definition key (pack convention).
            this.itemModel = key;
        }

        public Builder host(final HostSpec host) {
            this.host = Objects.requireNonNull(host, "host");
            return this;
        }

        public Builder itemMaterial(final Material itemMaterial) {
            this.itemMaterial = Objects.requireNonNull(itemMaterial, "itemMaterial");
            return this;
        }

        public Builder itemModel(final Key itemModel) {
            this.itemModel = Objects.requireNonNull(itemModel, "itemModel");
            return this;
        }

        public Builder itemModel(final String namespacedItemModel) {
            final Key parsed = Key.key(namespacedItemModel);
            this.itemModel = parsed;
            return this;
        }

        public Builder displayName(final @Nullable Component displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder itemLore(final @Nullable List<? extends Component> itemLore) {
            this.itemLore = itemLore == null ? null : List.copyOf(itemLore);
            return this;
        }

        /**
         * Mining/blast feel. Prefer {@link BlockFeel#emulate(Material)} so the block
         * digs and explodes like a known vanilla block (e.g. iron ore).
         */
        public Builder feel(final BlockFeel feel) {
            this.feel = Objects.requireNonNull(feel, "feel");
            return this;
        }

        /** Shorthand for {@code feel(BlockFeel.emulate(material))}. */
        public Builder emulate(final Material blockMaterial) {
            this.feel = BlockFeel.emulate(blockMaterial);
            return this;
        }

        public CustomBlockDefinition build() {
            if (this.host == null) {
                throw new IllegalStateException("host required");
            }
            return new CustomBlockDefinition(
                this.key,
                this.host,
                this.itemMaterial,
                this.itemModel,
                this.displayName,
                this.itemLore,
                this.feel
            );
        }
    }
}
