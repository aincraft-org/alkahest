package org.bukkit.entity;

import java.util.Optional;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.RegionAccessor;
import org.bukkit.Translatable;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An entity type that can be spawned and identified by {@link NamespacedKey}.
 *
 * <p>Vanilla types are the constants on this interface (e.g. {@link #PIG}); they are instances of
 * {@link VanillaEntityType}. Custom types (e.g. mintychochip custom entities) implement this
 * interface so they can be used anywhere an {@code EntityType} is accepted — including
 * {@link RegionAccessor#spawnEntity(Location, EntityType)}.
 *
 * <p>{@link Entity#getType()} returns the <strong>carrier</strong> vanilla type for custom entities;
 * use {@link Entity#getCustomEntity()} / {@link Entity#getCustomKey()} for logical custom identity.
 */
public interface EntityType extends Keyed, Translatable, net.kyori.adventure.translation.Translatable, io.papermc.paper.world.flag.FeatureDependant {

    // ---- vanilla constants (source-compatible with former enum constants) ----
    EntityType ACACIA_BOAT = VanillaEntityType.ACACIA_BOAT;
    EntityType ACACIA_CHEST_BOAT = VanillaEntityType.ACACIA_CHEST_BOAT;
    EntityType ALLAY = VanillaEntityType.ALLAY;
    EntityType AREA_EFFECT_CLOUD = VanillaEntityType.AREA_EFFECT_CLOUD;
    EntityType ARMADILLO = VanillaEntityType.ARMADILLO;
    EntityType ARMOR_STAND = VanillaEntityType.ARMOR_STAND;
    EntityType ARROW = VanillaEntityType.ARROW;
    EntityType AXOLOTL = VanillaEntityType.AXOLOTL;
    EntityType BAMBOO_CHEST_RAFT = VanillaEntityType.BAMBOO_CHEST_RAFT;
    EntityType BAMBOO_RAFT = VanillaEntityType.BAMBOO_RAFT;
    EntityType BAT = VanillaEntityType.BAT;
    EntityType BEE = VanillaEntityType.BEE;
    EntityType BIRCH_BOAT = VanillaEntityType.BIRCH_BOAT;
    EntityType BIRCH_CHEST_BOAT = VanillaEntityType.BIRCH_CHEST_BOAT;
    EntityType BLAZE = VanillaEntityType.BLAZE;
    EntityType BLOCK_DISPLAY = VanillaEntityType.BLOCK_DISPLAY;
    EntityType BOGGED = VanillaEntityType.BOGGED;
    EntityType BREEZE = VanillaEntityType.BREEZE;
    EntityType BREEZE_WIND_CHARGE = VanillaEntityType.BREEZE_WIND_CHARGE;
    EntityType CAMEL = VanillaEntityType.CAMEL;
    EntityType CAMEL_HUSK = VanillaEntityType.CAMEL_HUSK;
    EntityType CAT = VanillaEntityType.CAT;
    EntityType CAVE_SPIDER = VanillaEntityType.CAVE_SPIDER;
    EntityType CHERRY_BOAT = VanillaEntityType.CHERRY_BOAT;
    EntityType CHERRY_CHEST_BOAT = VanillaEntityType.CHERRY_CHEST_BOAT;
    EntityType CHEST_MINECART = VanillaEntityType.CHEST_MINECART;
    EntityType CHICKEN = VanillaEntityType.CHICKEN;
    EntityType COD = VanillaEntityType.COD;
    EntityType COMMAND_BLOCK_MINECART = VanillaEntityType.COMMAND_BLOCK_MINECART;
    EntityType COPPER_GOLEM = VanillaEntityType.COPPER_GOLEM;
    EntityType COW = VanillaEntityType.COW;
    EntityType CREAKING = VanillaEntityType.CREAKING;
    EntityType CREEPER = VanillaEntityType.CREEPER;
    EntityType DARK_OAK_BOAT = VanillaEntityType.DARK_OAK_BOAT;
    EntityType DARK_OAK_CHEST_BOAT = VanillaEntityType.DARK_OAK_CHEST_BOAT;
    EntityType DOLPHIN = VanillaEntityType.DOLPHIN;
    EntityType DONKEY = VanillaEntityType.DONKEY;
    EntityType DRAGON_FIREBALL = VanillaEntityType.DRAGON_FIREBALL;
    EntityType DROWNED = VanillaEntityType.DROWNED;
    EntityType EGG = VanillaEntityType.EGG;
    EntityType ELDER_GUARDIAN = VanillaEntityType.ELDER_GUARDIAN;
    EntityType END_CRYSTAL = VanillaEntityType.END_CRYSTAL;
    EntityType ENDER_DRAGON = VanillaEntityType.ENDER_DRAGON;
    EntityType ENDER_PEARL = VanillaEntityType.ENDER_PEARL;
    EntityType ENDERMAN = VanillaEntityType.ENDERMAN;
    EntityType ENDERMITE = VanillaEntityType.ENDERMITE;
    EntityType EVOKER = VanillaEntityType.EVOKER;
    EntityType EVOKER_FANGS = VanillaEntityType.EVOKER_FANGS;
    EntityType EXPERIENCE_BOTTLE = VanillaEntityType.EXPERIENCE_BOTTLE;
    EntityType EXPERIENCE_ORB = VanillaEntityType.EXPERIENCE_ORB;
    EntityType EYE_OF_ENDER = VanillaEntityType.EYE_OF_ENDER;
    EntityType FALLING_BLOCK = VanillaEntityType.FALLING_BLOCK;
    EntityType FIREBALL = VanillaEntityType.FIREBALL;
    EntityType FIREWORK_ROCKET = VanillaEntityType.FIREWORK_ROCKET;
    EntityType FISHING_BOBBER = VanillaEntityType.FISHING_BOBBER;
    EntityType FOX = VanillaEntityType.FOX;
    EntityType FROG = VanillaEntityType.FROG;
    EntityType FURNACE_MINECART = VanillaEntityType.FURNACE_MINECART;
    EntityType GHAST = VanillaEntityType.GHAST;
    EntityType GIANT = VanillaEntityType.GIANT;
    EntityType GLOW_ITEM_FRAME = VanillaEntityType.GLOW_ITEM_FRAME;
    EntityType GLOW_SQUID = VanillaEntityType.GLOW_SQUID;
    EntityType GOAT = VanillaEntityType.GOAT;
    EntityType GUARDIAN = VanillaEntityType.GUARDIAN;
    EntityType HAPPY_GHAST = VanillaEntityType.HAPPY_GHAST;
    EntityType HOGLIN = VanillaEntityType.HOGLIN;
    EntityType HOPPER_MINECART = VanillaEntityType.HOPPER_MINECART;
    EntityType HORSE = VanillaEntityType.HORSE;
    EntityType HUSK = VanillaEntityType.HUSK;
    EntityType ILLUSIONER = VanillaEntityType.ILLUSIONER;
    EntityType INTERACTION = VanillaEntityType.INTERACTION;
    EntityType IRON_GOLEM = VanillaEntityType.IRON_GOLEM;
    EntityType ITEM = VanillaEntityType.ITEM;
    EntityType ITEM_DISPLAY = VanillaEntityType.ITEM_DISPLAY;
    EntityType ITEM_FRAME = VanillaEntityType.ITEM_FRAME;
    EntityType JUNGLE_BOAT = VanillaEntityType.JUNGLE_BOAT;
    EntityType JUNGLE_CHEST_BOAT = VanillaEntityType.JUNGLE_CHEST_BOAT;
    EntityType LEASH_KNOT = VanillaEntityType.LEASH_KNOT;
    EntityType LIGHTNING_BOLT = VanillaEntityType.LIGHTNING_BOLT;
    EntityType LINGERING_POTION = VanillaEntityType.LINGERING_POTION;
    EntityType LLAMA = VanillaEntityType.LLAMA;
    EntityType LLAMA_SPIT = VanillaEntityType.LLAMA_SPIT;
    EntityType MAGMA_CUBE = VanillaEntityType.MAGMA_CUBE;
    EntityType MANGROVE_BOAT = VanillaEntityType.MANGROVE_BOAT;
    EntityType MANGROVE_CHEST_BOAT = VanillaEntityType.MANGROVE_CHEST_BOAT;
    EntityType MANNEQUIN = VanillaEntityType.MANNEQUIN;
    EntityType MARKER = VanillaEntityType.MARKER;
    EntityType MINECART = VanillaEntityType.MINECART;
    EntityType MOOSHROOM = VanillaEntityType.MOOSHROOM;
    EntityType MULE = VanillaEntityType.MULE;
    EntityType NAUTILUS = VanillaEntityType.NAUTILUS;
    EntityType OAK_BOAT = VanillaEntityType.OAK_BOAT;
    EntityType OAK_CHEST_BOAT = VanillaEntityType.OAK_CHEST_BOAT;
    EntityType OCELOT = VanillaEntityType.OCELOT;
    EntityType OMINOUS_ITEM_SPAWNER = VanillaEntityType.OMINOUS_ITEM_SPAWNER;
    EntityType PAINTING = VanillaEntityType.PAINTING;
    EntityType PALE_OAK_BOAT = VanillaEntityType.PALE_OAK_BOAT;
    EntityType PALE_OAK_CHEST_BOAT = VanillaEntityType.PALE_OAK_CHEST_BOAT;
    EntityType PANDA = VanillaEntityType.PANDA;
    EntityType PARCHED = VanillaEntityType.PARCHED;
    EntityType PARROT = VanillaEntityType.PARROT;
    EntityType PHANTOM = VanillaEntityType.PHANTOM;
    EntityType PIG = VanillaEntityType.PIG;
    EntityType PIGLIN = VanillaEntityType.PIGLIN;
    EntityType PIGLIN_BRUTE = VanillaEntityType.PIGLIN_BRUTE;
    EntityType PILLAGER = VanillaEntityType.PILLAGER;
    EntityType PLAYER = VanillaEntityType.PLAYER;
    EntityType POLAR_BEAR = VanillaEntityType.POLAR_BEAR;
    EntityType PUFFERFISH = VanillaEntityType.PUFFERFISH;
    EntityType RABBIT = VanillaEntityType.RABBIT;
    EntityType RAVAGER = VanillaEntityType.RAVAGER;
    EntityType SALMON = VanillaEntityType.SALMON;
    EntityType SHEEP = VanillaEntityType.SHEEP;
    EntityType SHULKER = VanillaEntityType.SHULKER;
    EntityType SHULKER_BULLET = VanillaEntityType.SHULKER_BULLET;
    EntityType SILVERFISH = VanillaEntityType.SILVERFISH;
    EntityType SKELETON = VanillaEntityType.SKELETON;
    EntityType SKELETON_HORSE = VanillaEntityType.SKELETON_HORSE;
    EntityType SLIME = VanillaEntityType.SLIME;
    EntityType SMALL_FIREBALL = VanillaEntityType.SMALL_FIREBALL;
    EntityType SNIFFER = VanillaEntityType.SNIFFER;
    EntityType SNOW_GOLEM = VanillaEntityType.SNOW_GOLEM;
    EntityType SNOWBALL = VanillaEntityType.SNOWBALL;
    EntityType SPAWNER_MINECART = VanillaEntityType.SPAWNER_MINECART;
    EntityType SPECTRAL_ARROW = VanillaEntityType.SPECTRAL_ARROW;
    EntityType SPIDER = VanillaEntityType.SPIDER;
    EntityType SPLASH_POTION = VanillaEntityType.SPLASH_POTION;
    EntityType SPRUCE_BOAT = VanillaEntityType.SPRUCE_BOAT;
    EntityType SPRUCE_CHEST_BOAT = VanillaEntityType.SPRUCE_CHEST_BOAT;
    EntityType SQUID = VanillaEntityType.SQUID;
    EntityType STRAY = VanillaEntityType.STRAY;
    EntityType STRIDER = VanillaEntityType.STRIDER;
    EntityType SULFUR_CUBE = VanillaEntityType.SULFUR_CUBE;
    EntityType TADPOLE = VanillaEntityType.TADPOLE;
    EntityType TEXT_DISPLAY = VanillaEntityType.TEXT_DISPLAY;
    EntityType TNT = VanillaEntityType.TNT;
    EntityType TNT_MINECART = VanillaEntityType.TNT_MINECART;
    EntityType TRADER_LLAMA = VanillaEntityType.TRADER_LLAMA;
    EntityType TRIDENT = VanillaEntityType.TRIDENT;
    EntityType TROPICAL_FISH = VanillaEntityType.TROPICAL_FISH;
    EntityType TURTLE = VanillaEntityType.TURTLE;
    EntityType VEX = VanillaEntityType.VEX;
    EntityType VILLAGER = VanillaEntityType.VILLAGER;
    EntityType VINDICATOR = VanillaEntityType.VINDICATOR;
    EntityType WANDERING_TRADER = VanillaEntityType.WANDERING_TRADER;
    EntityType WARDEN = VanillaEntityType.WARDEN;
    EntityType WIND_CHARGE = VanillaEntityType.WIND_CHARGE;
    EntityType WITCH = VanillaEntityType.WITCH;
    EntityType WITHER = VanillaEntityType.WITHER;
    EntityType WITHER_SKELETON = VanillaEntityType.WITHER_SKELETON;
    EntityType WITHER_SKULL = VanillaEntityType.WITHER_SKULL;
    EntityType WOLF = VanillaEntityType.WOLF;
    EntityType ZOGLIN = VanillaEntityType.ZOGLIN;
    EntityType ZOMBIE = VanillaEntityType.ZOMBIE;
    EntityType ZOMBIE_HORSE = VanillaEntityType.ZOMBIE_HORSE;
    EntityType ZOMBIE_NAUTILUS = VanillaEntityType.ZOMBIE_NAUTILUS;
    EntityType ZOMBIE_VILLAGER = VanillaEntityType.ZOMBIE_VILLAGER;
    EntityType ZOMBIFIED_PIGLIN = VanillaEntityType.ZOMBIFIED_PIGLIN;
    EntityType UNKNOWN = VanillaEntityType.UNKNOWN;

    /**
     * Gets the entity type name (path of the key without namespace for vanilla).
     *
     * @return the entity type's name
     * @deprecated Magic value
     */
    @Deprecated(since = "1.6.2")
    @Nullable
    String getName();

    @Override
    @NotNull
    NamespacedKey getKey();

    /**
     * Bukkit entity class used as the spawn carrier for this type.
     * Custom block-model types typically return {@link BlockDisplay}.
     */
    @Nullable
    Class<? extends Entity> getEntityClass();

    /**
     * Gets the entity type id.
     *
     * @return the raw type id
     * @deprecated Magic value
     */
    @Deprecated(since = "1.6.2", forRemoval = true)
    short getTypeId();

    /**
     * Some entities cannot be spawned using {@link
     * World#spawnEntity(Location, EntityType)} or {@link
     * World#spawn(Location, Class)}, usually because they require additional
     * information in order to spawn.
     *
     * @return False if the entity type cannot be spawned
     */
    boolean isSpawnable();

    boolean isAlive();

    @Override
    @NotNull
    @Deprecated(forRemoval = true)
    String getTranslationKey();

    @Override
    @NotNull
    String translationKey();

    /**
     * Gets the spawn category of this entity type.
     *
     * @return the spawn category
     * @throws IllegalArgumentException if the entity does not have a spawn category
     */
    @NotNull
    SpawnCategory getSpawnCategory();

    /**
     * Checks if the entity type has default attributes.
     *
     * @return true if it has default attributes
     */
    boolean hasDefaultAttributes();

    /**
     * Gets the default attributes for the entity type.
     *
     * @return an unmodifiable instance of Attributable for reading default attributes.
     * @throws IllegalArgumentException if it doesn't have default attributes (use {@link #hasDefaultAttributes()} first)
     */
    @NotNull
    org.bukkit.attribute.Attributable getDefaultAttributes();

    /**
     * Enum-style constant name for vanilla types (e.g. {@code "PIG"}).
     * Custom types return {@link #getKey()} as a string.
     *
     * <p>Source-compatible with former {@code Enum#name()}.
     */
    @NotNull
    String name();

    /**
     * {@code true} when this is a vanilla Minecraft entity type constant.
     */
    boolean isVanilla();

    /**
     * {@code true} when this is a registered custom entity type (not a vanilla constant).
     */
    boolean isCustom();

    /**
     * Spawn this type at {@code location} using the default spawn path.
     * Vanilla types create the entity class; custom types apply host presentation + identity.
     */
    @NotNull
    Entity spawn(@NotNull Location location);

    // ---- static lookup (compat with former enum statics) ----

    /**
     * All <em>vanilla</em> entity type constants (not custom registrations).
     * Prefer iterating {@link org.bukkit.Registry#ENTITY_TYPE} when available.
     */
    @NotNull
    static EntityType[] values() {
        final VanillaEntityType[] vanilla = VanillaEntityType.values();
        final EntityType[] out = new EntityType[vanilla.length];
        System.arraycopy(vanilla, 0, out, 0, vanilla.length);
        return out;
    }

    /**
     * Looks up a <em>vanilla</em> entity type by its enum constant name (e.g. {@code "PIG"}).
     * Does not resolve custom entity keys — use {@link #getByKey(NamespacedKey)} for that.
     */
    @NotNull
    static EntityType valueOf(@NotNull final String name) {
        return VanillaEntityType.valueOf(name);
    }

    /**
     * Gets an entity type from its legacy name path (e.g. {@code "pig"}).
     *
     * @param name the entity type's name
     * @return the matching vanilla entity type or null
     * @apiNote Internal Use Only
     */
    @ApiStatus.Internal
    @Contract("null -> null")
    @Nullable
    static EntityType fromName(@Nullable String name) {
        return VanillaEntityType.fromName(name);
    }

    /**
     * Gets a vanilla entity type from its legacy numeric id.
     *
     * @param id the raw type id
     * @return the matching entity type or null
     * @deprecated Magic value
     */
    @Deprecated(since = "1.6.2", forRemoval = true)
    @Nullable
    static EntityType fromId(int id) {
        return VanillaEntityType.fromId(id);
    }

    /**
     * Resolve any entity type (vanilla or mintychochip custom catalog) by key.
     *
     * <p>Custom catalog is checked first so registered
     * {@link dev.mintychochip.customentity.CustomEntityDefinition}s resolve without full
     * {@link org.bukkit.Registry} bootstrap. Then {@link org.bukkit.Registry#ENTITY_TYPE}
     * (vanilla non-{@link VanillaEntityType#UNKNOWN} + same customs). On registry
     * miss / bootstrap failure, falls back to vanilla name lookup.
     */
    @NotNull
    static Optional<EntityType> getByKey(@Nullable final NamespacedKey key) {
        if (key == null) {
            return Optional.empty();
        }
        // Custom first — works without Registry clinit (API unit tests / early bootstrap)
        final Optional<dev.mintychochip.customentity.CustomEntityDefinition> custom =
            dev.mintychochip.customentity.CustomEntities.get(key);
        if (custom.isPresent()) {
            return Optional.of(custom.get());
        }
        try {
            final EntityType reg = org.bukkit.Registry.ENTITY_TYPE.get(key);
            if (reg != null) {
                return Optional.of(reg);
            }
        } catch (final Throwable ignored) {
            // bootstrap — byName fallback below
        }
        final VanillaEntityType byName = VanillaEntityType.fromName(key.getKey());
        return Optional.ofNullable(byName);
    }
}
