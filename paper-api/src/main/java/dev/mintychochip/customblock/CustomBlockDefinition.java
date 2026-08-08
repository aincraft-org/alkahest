package dev.mintychochip.customblock;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.CreativeCategory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemType;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Immutable definition of a custom block type (identity + host + item + feel).
 *
 * <p>Implements {@link Material} so callers can use the same APIs as vanilla constants
 * (lookups via {@link Material#getByKey}, hardness, stack size, …). Live
 * {@link org.bukkit.block.Block#getType()} / {@link org.bukkit.inventory.ItemStack#getType()}
 * still return the vanilla carrier/base; logical identity is this definition.
 *
 * <p>{@link BlockFeel} controls hardness, preferred tool, and blast resistance so the
 * custom block can emulate a vanilla block even when hosted on a different carrier.
 */
public final class CustomBlockDefinition implements Material {

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
        if (itemMaterial.isCustom()) {
            throw new IllegalArgumentException("itemMaterial must be vanilla Material, not custom");
        }
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

    /**
     * Adventure {@link Key} form of the definition id (same as {@link #getKey()}).
     */
    public @NotNull Key key() {
        return this.key;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
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

    /**
     * Vanilla world carrier material for this definition's host.
     *
     * <p>API-side (no NMS); server placement delegates here.
     *
     * <p>Uses {@link org.bukkit.VanillaMaterial} constants — {@code Material.*} interface
     * statics can still be null during early bootstrap / circular {@code <clinit>}.
     */
    public @NotNull Material carrierMaterial() {
        return switch (this.host.type()) {
            case CHORUS -> org.bukkit.VanillaMaterial.CHORUS_PLANT;
            case MUSHROOM -> {
                final MushroomHostSpec mush = (MushroomHostSpec) this.host;
                yield mush.variant() == MushroomVariant.RED
                    ? org.bukkit.VanillaMaterial.RED_MUSHROOM_BLOCK
                    : org.bukkit.VanillaMaterial.BROWN_MUSHROOM_BLOCK;
            }
            case TRIPWIRE -> org.bukkit.VanillaMaterial.TRIPWIRE;
            case PACKET -> packetCollisionMaterial();
        };
    }

    private Material packetCollisionMaterial() {
        final PacketHostSpec packet = (PacketHostSpec) this.host;
        final String key = packet.collisionMaterialKey();
        // Fast-path common defaults without touching Material registry bootstrap.
        if ("minecraft:glass".equals(key) || "glass".equalsIgnoreCase(key)) {
            return org.bukkit.VanillaMaterial.GLASS;
        }
        if ("minecraft:barrier".equals(key) || "barrier".equalsIgnoreCase(key)) {
            return org.bukkit.VanillaMaterial.BARRIER;
        }
        // Prefer getMaterial (not matchMaterial) to avoid custom-key recursion.
        final String enumName;
        if (key.startsWith("minecraft:")) {
            enumName = key.substring("minecraft:".length()).toUpperCase(Locale.ROOT);
        } else {
            enumName = key.toUpperCase(Locale.ROOT).replace(':', '_');
        }
        final Material parsed = Material.getMaterial(enumName);
        if (parsed != null && parsed.isVanilla() && parsed.isBlock()
            && parsed != org.bukkit.VanillaMaterial.AIR
            && parsed != org.bukkit.VanillaMaterial.CAVE_AIR
            && parsed != org.bukkit.VanillaMaterial.VOID_AIR) {
            return parsed;
        }
        return org.bukkit.VanillaMaterial.GLASS;
    }

    private Material carrier() {
        return carrierMaterial();
    }

    private Material item() {
        return this.itemMaterial;
    }

    // ---- Material ----

    @Override
    public boolean isLegacy() {
        return false;
    }

    @Override
    public boolean isBlock() {
        return true;
    }

    @Override
    public boolean isItem() {
        return true;
    }

    @Override
    public boolean isAir() {
        return false;
    }

    @Override
    @Deprecated(since = "1.21.5")
    public boolean isEmpty() {
        return false;
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
    public float getHardness() {
        return this.feel.hardness();
    }

    @Override
    public float getBlastResistance() {
        return this.feel.blastResistance();
    }

    @Override
    public float getSlipperiness() {
        return carrier().getSlipperiness();
    }

    @Override
    public int getMaxStackSize() {
        return item().getMaxStackSize();
    }

    @Override
    public short getMaxDurability() {
        return item().getMaxDurability();
    }

    @Override
    public @NotNull BlockData createBlockData() {
        return Bukkit.createBlockData(carrier());
    }

    @Override
    public @NotNull BlockData createBlockData(@Nullable final Consumer<? super BlockData> consumer) {
        return Bukkit.createBlockData(carrier(), consumer);
    }

    @Override
    public @NotNull BlockData createBlockData(@Nullable final String data) {
        return Bukkit.createBlockData(carrier(), data);
    }

    @Override
    public @Nullable ItemType asItemType() {
        return null;
    }

    @Override
    public @Nullable BlockType asBlockType() {
        return null;
    }

    @Override
    public int getId() {
        throw new IllegalArgumentException("Cannot get ID of custom Material " + this.key);
    }

    @Override
    @Deprecated // Paper
    public @NotNull Class<? extends MaterialData> getData() {
        throw new IllegalArgumentException("Cannot get data class of custom Material " + this.key);
    }

    @Override
    @Deprecated(since = "1.6.2")
    public @NotNull MaterialData getNewData(final byte raw) {
        throw new IllegalArgumentException("Cannot get new data of custom Material " + this.key);
    }

    @Override
    public @NotNull String translationKey() {
        return this.key.toString();
    }

    @Override
    @Deprecated(forRemoval = true)
    public @NotNull String getTranslationKey() {
        return translationKey();
    }

    @Override
    public @Nullable String getBlockTranslationKey() {
        return translationKey();
    }

    @Override
    public @Nullable String getItemTranslationKey() {
        return translationKey();
    }

    @Override
    @Deprecated(forRemoval = true, since = "1.20.5")
    public io.papermc.paper.inventory.@NotNull ItemRarity getItemRarity() {
        return item().getItemRarity();
    }

    @Override
    @Deprecated(forRemoval = true, since = "1.20.5")
    public @NotNull Multimap<Attribute, AttributeModifier> getItemAttributes(@NotNull final EquipmentSlot equipmentSlot) {
        return item().getItemAttributes(equipmentSlot);
    }

    @Override
    public boolean isCollidable() {
        return carrier().isCollidable();
    }

    @Override
    public boolean isEdible() {
        return item().isEdible();
    }

    @Override
    public boolean isRecord() {
        return item().isRecord();
    }

    @Override
    public boolean isSolid() {
        return carrier().isSolid();
    }

    @Override
    @Deprecated(since = "1.13", forRemoval = true)
    public boolean isTransparent() {
        return carrier().isTransparent();
    }

    @Override
    public boolean isFlammable() {
        return carrier().isFlammable();
    }

    @Override
    public boolean isBurnable() {
        return carrier().isBurnable();
    }

    @Override
    public boolean isFuel() {
        return item().isFuel();
    }

    @Override
    public boolean isOccluding() {
        return carrier().isOccluding();
    }

    @Override
    public boolean hasGravity() {
        return carrier().hasGravity();
    }

    @Override
    @Deprecated // Paper
    public boolean isInteractable() {
        return carrier().isInteractable();
    }

    @Override
    public @Nullable Material getCraftingRemainingItem() {
        return item().getCraftingRemainingItem();
    }

    @Override
    public @NotNull EquipmentSlot getEquipmentSlot() {
        return item().getEquipmentSlot();
    }

    @Override
    public @NotNull @Unmodifiable Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers() {
        return item().getDefaultAttributeModifiers();
    }

    @Override
    public @NotNull Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(@NotNull final EquipmentSlot slot) {
        return item().getDefaultAttributeModifiers(slot);
    }

    @Override
    @Deprecated(since = "1.20.6", forRemoval = true)
    public @Nullable CreativeCategory getCreativeCategory() {
        return item().getCreativeCategory();
    }

    @Override
    public boolean isCompostable() {
        return item().isCompostable();
    }

    @Override
    public float getCompostChance() {
        return item().getCompostChance();
    }

    @Override
    public @Nullable <T> T getDefaultData(final io.papermc.paper.datacomponent.DataComponentType.@NotNull Valued<T> type) {
        final ItemType itemType = item().asItemType();
        Preconditions.checkArgument(itemType != null, "The Material is not an item!");
        return itemType.getDefaultData(type);
    }

    @Override
    public boolean hasDefaultData(final io.papermc.paper.datacomponent.@NotNull DataComponentType type) {
        final ItemType itemType = item().asItemType();
        Preconditions.checkArgument(itemType != null, "The Material is not an item!");
        return itemType.hasDefaultData(type);
    }

    @Override
    public @Unmodifiable @NotNull Set<io.papermc.paper.datacomponent.DataComponentType> getDefaultDataTypes() {
        final ItemType itemType = item().asItemType();
        Preconditions.checkArgument(itemType != null, "The Material is not an item!");
        return itemType.getDefaultDataTypes();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CustomBlockDefinition that)) {
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
        return "CustomBlockDefinition[" + this.key + "]";
    }

    public static final class Builder {
        private final NamespacedKey key;
        private HostSpec host;
        /**
         * Placeable base so clients play place animation (not paper use-item).
         * Lazy default: do not touch {@code Material.GLASS} in a field initializer — interface
         * statics can be null if this class loads during Material {@code <clinit>}.
         */
        private @Nullable Material itemMaterial;
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
            final Material item = this.itemMaterial != null
                ? this.itemMaterial
                : org.bukkit.VanillaMaterial.GLASS;
            return new CustomBlockDefinition(
                this.key,
                this.host,
                item,
                this.itemModel,
                this.displayName,
                this.itemLore,
                this.feel
            );
        }
    }
}
