package org.bukkit;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.CreativeCategory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemType;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A material identity (block and/or item) accepted by Bukkit APIs.
 *
 * <p>Vanilla types are the constants on this interface (e.g. {@link #STONE}); they are instances of
 * {@link VanillaMaterial}. Custom types ({@link dev.mintychochip.customblock.CustomBlockDefinition})
 * implement this interface so they can be used anywhere a {@code Material} is accepted.
 *
 * <p>{@link org.bukkit.block.Block#getType()} / {@link org.bukkit.inventory.ItemStack#getType()} return
 * the <strong>carrier</strong> vanilla material for custom blocks/items; use
 * {@code getCustomBlock()} / {@code getCustomKey()} for logical custom identity.
 */
@SuppressWarnings({"DeprecatedIsStillUsed", "deprecation"}) // Paper
public interface Material extends Keyed, Translatable, net.kyori.adventure.translation.Translatable {

    // ---- vanilla constants (source-compatible with former enum constants) ----
    Material ACACIA_BOAT = VanillaMaterial.ACACIA_BOAT;
    Material ACACIA_CHEST_BOAT = VanillaMaterial.ACACIA_CHEST_BOAT;
    Material AIR = VanillaMaterial.AIR;
    Material ALLAY_SPAWN_EGG = VanillaMaterial.ALLAY_SPAWN_EGG;
    Material AMETHYST_SHARD = VanillaMaterial.AMETHYST_SHARD;
    Material ANGLER_POTTERY_SHERD = VanillaMaterial.ANGLER_POTTERY_SHERD;
    Material APPLE = VanillaMaterial.APPLE;
    Material ARCHER_POTTERY_SHERD = VanillaMaterial.ARCHER_POTTERY_SHERD;
    Material ARMADILLO_SCUTE = VanillaMaterial.ARMADILLO_SCUTE;
    Material ARMADILLO_SPAWN_EGG = VanillaMaterial.ARMADILLO_SPAWN_EGG;
    Material ARMOR_STAND = VanillaMaterial.ARMOR_STAND;
    Material ARMS_UP_POTTERY_SHERD = VanillaMaterial.ARMS_UP_POTTERY_SHERD;
    Material ARROW = VanillaMaterial.ARROW;
    Material AXOLOTL_BUCKET = VanillaMaterial.AXOLOTL_BUCKET;
    Material AXOLOTL_SPAWN_EGG = VanillaMaterial.AXOLOTL_SPAWN_EGG;
    Material BAKED_POTATO = VanillaMaterial.BAKED_POTATO;
    Material BAMBOO_CHEST_RAFT = VanillaMaterial.BAMBOO_CHEST_RAFT;
    Material BAMBOO_RAFT = VanillaMaterial.BAMBOO_RAFT;
    Material BAT_SPAWN_EGG = VanillaMaterial.BAT_SPAWN_EGG;
    Material BEE_SPAWN_EGG = VanillaMaterial.BEE_SPAWN_EGG;
    Material BEEF = VanillaMaterial.BEEF;
    Material BEETROOT = VanillaMaterial.BEETROOT;
    Material BEETROOT_SEEDS = VanillaMaterial.BEETROOT_SEEDS;
    Material BEETROOT_SOUP = VanillaMaterial.BEETROOT_SOUP;
    Material BIRCH_BOAT = VanillaMaterial.BIRCH_BOAT;
    Material BIRCH_CHEST_BOAT = VanillaMaterial.BIRCH_CHEST_BOAT;
    Material BLACK_BUNDLE = VanillaMaterial.BLACK_BUNDLE;
    Material BLACK_DYE = VanillaMaterial.BLACK_DYE;
    Material BLACK_HARNESS = VanillaMaterial.BLACK_HARNESS;
    Material BLADE_POTTERY_SHERD = VanillaMaterial.BLADE_POTTERY_SHERD;
    Material BLAZE_POWDER = VanillaMaterial.BLAZE_POWDER;
    Material BLAZE_ROD = VanillaMaterial.BLAZE_ROD;
    Material BLAZE_SPAWN_EGG = VanillaMaterial.BLAZE_SPAWN_EGG;
    Material BLUE_BUNDLE = VanillaMaterial.BLUE_BUNDLE;
    Material BLUE_DYE = VanillaMaterial.BLUE_DYE;
    Material BLUE_EGG = VanillaMaterial.BLUE_EGG;
    Material BLUE_HARNESS = VanillaMaterial.BLUE_HARNESS;
    Material BOGGED_SPAWN_EGG = VanillaMaterial.BOGGED_SPAWN_EGG;
    Material BOLT_ARMOR_TRIM_SMITHING_TEMPLATE = VanillaMaterial.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE;
    Material BONE = VanillaMaterial.BONE;
    Material BONE_MEAL = VanillaMaterial.BONE_MEAL;
    Material BOOK = VanillaMaterial.BOOK;
    Material BORDURE_INDENTED_BANNER_PATTERN = VanillaMaterial.BORDURE_INDENTED_BANNER_PATTERN;
    Material BOW = VanillaMaterial.BOW;
    Material BOWL = VanillaMaterial.BOWL;
    Material BREAD = VanillaMaterial.BREAD;
    Material BREEZE_ROD = VanillaMaterial.BREEZE_ROD;
    Material BREEZE_SPAWN_EGG = VanillaMaterial.BREEZE_SPAWN_EGG;
    Material BREWER_POTTERY_SHERD = VanillaMaterial.BREWER_POTTERY_SHERD;
    Material BRICK = VanillaMaterial.BRICK;
    Material BROWN_BUNDLE = VanillaMaterial.BROWN_BUNDLE;
    Material BROWN_DYE = VanillaMaterial.BROWN_DYE;
    Material BROWN_EGG = VanillaMaterial.BROWN_EGG;
    Material BROWN_HARNESS = VanillaMaterial.BROWN_HARNESS;
    Material BRUSH = VanillaMaterial.BRUSH;
    Material BUCKET = VanillaMaterial.BUCKET;
    Material BUNDLE = VanillaMaterial.BUNDLE;
    Material BURN_POTTERY_SHERD = VanillaMaterial.BURN_POTTERY_SHERD;
    Material CAMEL_HUSK_SPAWN_EGG = VanillaMaterial.CAMEL_HUSK_SPAWN_EGG;
    Material CAMEL_SPAWN_EGG = VanillaMaterial.CAMEL_SPAWN_EGG;
    Material CARROT = VanillaMaterial.CARROT;
    Material CARROT_ON_A_STICK = VanillaMaterial.CARROT_ON_A_STICK;
    Material CAT_SPAWN_EGG = VanillaMaterial.CAT_SPAWN_EGG;
    Material CAVE_SPIDER_SPAWN_EGG = VanillaMaterial.CAVE_SPIDER_SPAWN_EGG;
    Material CHAINMAIL_BOOTS = VanillaMaterial.CHAINMAIL_BOOTS;
    Material CHAINMAIL_CHESTPLATE = VanillaMaterial.CHAINMAIL_CHESTPLATE;
    Material CHAINMAIL_HELMET = VanillaMaterial.CHAINMAIL_HELMET;
    Material CHAINMAIL_LEGGINGS = VanillaMaterial.CHAINMAIL_LEGGINGS;
    Material CHARCOAL = VanillaMaterial.CHARCOAL;
    Material CHERRY_BOAT = VanillaMaterial.CHERRY_BOAT;
    Material CHERRY_CHEST_BOAT = VanillaMaterial.CHERRY_CHEST_BOAT;
    Material CHEST_MINECART = VanillaMaterial.CHEST_MINECART;
    Material CHICKEN = VanillaMaterial.CHICKEN;
    Material CHICKEN_SPAWN_EGG = VanillaMaterial.CHICKEN_SPAWN_EGG;
    Material CHORUS_FRUIT = VanillaMaterial.CHORUS_FRUIT;
    Material CLAY_BALL = VanillaMaterial.CLAY_BALL;
    Material CLOCK = VanillaMaterial.CLOCK;
    Material COAL = VanillaMaterial.COAL;
    Material COAST_ARMOR_TRIM_SMITHING_TEMPLATE = VanillaMaterial.COAST_ARMOR_TRIM_SMITHING_TEMPLATE;
    Material COCOA_BEANS = VanillaMaterial.COCOA_BEANS;
    Material COD = VanillaMaterial.COD;
    Material COD_BUCKET = VanillaMaterial.COD_BUCKET;
    Material COD_SPAWN_EGG = VanillaMaterial.COD_SPAWN_EGG;
    Material COMMAND_BLOCK_MINECART = VanillaMaterial.COMMAND_BLOCK_MINECART;
    Material COMPASS = VanillaMaterial.COMPASS;
    Material COOKED_BEEF = VanillaMaterial.COOKED_BEEF;
    Material COOKED_CHICKEN = VanillaMaterial.COOKED_CHICKEN;
    Material COOKED_COD = VanillaMaterial.COOKED_COD;
    Material COOKED_MUTTON = VanillaMaterial.COOKED_MUTTON;
    Material COOKED_PORKCHOP = VanillaMaterial.COOKED_PORKCHOP;
    Material COOKED_RABBIT = VanillaMaterial.COOKED_RABBIT;
    Material COOKED_SALMON = VanillaMaterial.COOKED_SALMON;
    Material COOKIE = VanillaMaterial.COOKIE;
    Material COPPER_AXE = VanillaMaterial.COPPER_AXE;
    Material COPPER_BOOTS = VanillaMaterial.COPPER_BOOTS;
    Material COPPER_CHESTPLATE = VanillaMaterial.COPPER_CHESTPLATE;
    Material COPPER_GOLEM_SPAWN_EGG = VanillaMaterial.COPPER_GOLEM_SPAWN_EGG;
    Material COPPER_HELMET = VanillaMaterial.COPPER_HELMET;
    Material COPPER_HOE = VanillaMaterial.COPPER_HOE;
    Material COPPER_HORSE_ARMOR = VanillaMaterial.COPPER_HORSE_ARMOR;
    Material COPPER_INGOT = VanillaMaterial.COPPER_INGOT;
    Material COPPER_LEGGINGS = VanillaMaterial.COPPER_LEGGINGS;
    Material COPPER_NAUTILUS_ARMOR = VanillaMaterial.COPPER_NAUTILUS_ARMOR;
    Material COPPER_NUGGET = VanillaMaterial.COPPER_NUGGET;
    Material COPPER_PICKAXE = VanillaMaterial.COPPER_PICKAXE;
    Material COPPER_SHOVEL = VanillaMaterial.COPPER_SHOVEL;
    Material COPPER_SPEAR = VanillaMaterial.COPPER_SPEAR;
    Material COPPER_SWORD = VanillaMaterial.COPPER_SWORD;
    Material COW_SPAWN_EGG = VanillaMaterial.COW_SPAWN_EGG;
    Material CREAKING_SPAWN_EGG = VanillaMaterial.CREAKING_SPAWN_EGG;
    Material CREEPER_BANNER_PATTERN = VanillaMaterial.CREEPER_BANNER_PATTERN;
    Material CREEPER_SPAWN_EGG = VanillaMaterial.CREEPER_SPAWN_EGG;
    Material CROSSBOW = VanillaMaterial.CROSSBOW;
    Material CYAN_BUNDLE = VanillaMaterial.CYAN_BUNDLE;
    Material CYAN_DYE = VanillaMaterial.CYAN_DYE;
    Material CYAN_HARNESS = VanillaMaterial.CYAN_HARNESS;
    Material DANGER_POTTERY_SHERD = VanillaMaterial.DANGER_POTTERY_SHERD;
    Material DARK_OAK_BOAT = VanillaMaterial.DARK_OAK_BOAT;
    Material DARK_OAK_CHEST_BOAT = VanillaMaterial.DARK_OAK_CHEST_BOAT;
    Material DEBUG_STICK = VanillaMaterial.DEBUG_STICK;
    Material DIAMOND = VanillaMaterial.DIAMOND;
    Material DIAMOND_AXE = VanillaMaterial.DIAMOND_AXE;
    Material DIAMOND_BOOTS = VanillaMaterial.DIAMOND_BOOTS;
    Material DIAMOND_CHESTPLATE = VanillaMaterial.DIAMOND_CHESTPLATE;
    Material DIAMOND_HELMET = VanillaMaterial.DIAMOND_HELMET;
    Material DIAMOND_HOE = VanillaMaterial.DIAMOND_HOE;
    Material DIAMOND_HORSE_ARMOR = VanillaMaterial.DIAMOND_HORSE_ARMOR;
    Material DIAMOND_LEGGINGS = VanillaMaterial.DIAMOND_LEGGINGS;
    Material DIAMOND_NAUTILUS_ARMOR = VanillaMaterial.DIAMOND_NAUTILUS_ARMOR;
    Material DIAMOND_PICKAXE = VanillaMaterial.DIAMOND_PICKAXE;
    Material DIAMOND_SHOVEL = VanillaMaterial.DIAMOND_SHOVEL;
    Material DIAMOND_SPEAR = VanillaMaterial.DIAMOND_SPEAR;
    Material DIAMOND_SWORD = VanillaMaterial.DIAMOND_SWORD;
    Material DISC_FRAGMENT_5 = VanillaMaterial.DISC_FRAGMENT_5;
    Material DOLPHIN_SPAWN_EGG = VanillaMaterial.DOLPHIN_SPAWN_EGG;
    Material DONKEY_SPAWN_EGG = VanillaMaterial.DONKEY_SPAWN_EGG;
    Material DRAGON_BREATH = VanillaMaterial.DRAGON_BREATH;
    Material DRIED_KELP = VanillaMaterial.DRIED_KELP;
    Material DROWNED_SPAWN_EGG = VanillaMaterial.DROWNED_SPAWN_EGG;
    Material DUNE_ARMOR_TRIM_SMITHING_TEMPLATE = VanillaMaterial.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE;
    Material ECHO_SHARD = VanillaMaterial.ECHO_SHARD;
    Material EGG = VanillaMaterial.EGG;
    Material ELDER_GUARDIAN_SPAWN_EGG = VanillaMaterial.ELDER_GUARDIAN_SPAWN_EGG;
    Material ELYTRA = VanillaMaterial.ELYTRA;
    Material EMERALD = VanillaMaterial.EMERALD;
    Material ENCHANTED_BOOK = VanillaMaterial.ENCHANTED_BOOK;
    Material ENCHANTED_GOLDEN_APPLE = VanillaMaterial.ENCHANTED_GOLDEN_APPLE;
    Material END_CRYSTAL = VanillaMaterial.END_CRYSTAL;
    Material ENDER_DRAGON_SPAWN_EGG = VanillaMaterial.ENDER_DRAGON_SPAWN_EGG;
    Material ENDER_EYE = VanillaMaterial.ENDER_EYE;
    Material ENDER_PEARL = VanillaMaterial.ENDER_PEARL;
    Material ENDERMAN_SPAWN_EGG = VanillaMaterial.ENDERMAN_SPAWN_EGG;
    Material ENDERMITE_SPAWN_EGG = VanillaMaterial.ENDERMITE_SPAWN_EGG;
    Material EVOKER_SPAWN_EGG = VanillaMaterial.EVOKER_SPAWN_EGG;
    Material EXPERIENCE_BOTTLE = VanillaMaterial.EXPERIENCE_BOTTLE;
    Material EXPLORER_POTTERY_SHERD = VanillaMaterial.EXPLORER_POTTERY_SHERD;
    Material EYE_ARMOR_TRIM_SMITHING_TEMPLATE = VanillaMaterial.EYE_ARMOR_TRIM_SMITHING_TEMPLATE;
    Material FEATHER = VanillaMaterial.FEATHER;
    Material FERMENTED_SPIDER_EYE = VanillaMaterial.FERMENTED_SPIDER_EYE;
    Material FIELD_MASONED_BANNER_PATTERN = VanillaMaterial.FIELD_MASONED_BANNER_PATTERN;
    Material FILLED_MAP = VanillaMaterial.FILLED_MAP;
    Material FIRE_CHARGE = VanillaMaterial.FIRE_CHARGE;
    Material FIREWORK_ROCKET = VanillaMaterial.FIREWORK_ROCKET;
    Material FIREWORK_STAR = VanillaMaterial.FIREWORK_STAR;
    Material FISHING_ROD = VanillaMaterial.FISHING_ROD;
    Material FLINT = VanillaMaterial.FLINT;
    Material FLINT_AND_STEEL = VanillaMaterial.FLINT_AND_STEEL;
    Material FLOW_ARMOR_TRIM_SMITHING_TEMPLATE = VanillaMaterial.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE;
    Material FLOW_BANNER_PATTERN = VanillaMaterial.FLOW_BANNER_PATTERN;
    Material FLOW_POTTERY_SHERD = VanillaMaterial.FLOW_POTTERY_SHERD;
    Material FLOWER_BANNER_PATTERN = VanillaMaterial.FLOWER_BANNER_PATTERN;
    Material FOX_SPAWN_EGG = VanillaMaterial.FOX_SPAWN_EGG;
    Material FRIEND_POTTERY_SHERD = VanillaMaterial.FRIEND_POTTERY_SHERD;
    Material FROG_SPAWN_EGG = VanillaMaterial.FROG_SPAWN_EGG;
    Material FURNACE_MINECART = VanillaMaterial.FURNACE_MINECART;
    Material GHAST_SPAWN_EGG = VanillaMaterial.GHAST_SPAWN_EGG;
    Material GHAST_TEAR = VanillaMaterial.GHAST_TEAR;
    Material GLASS_BOTTLE = VanillaMaterial.GLASS_BOTTLE;
    Material GLISTERING_MELON_SLICE = VanillaMaterial.GLISTERING_MELON_SLICE;
    Material GLOBE_BANNER_PATTERN = VanillaMaterial.GLOBE_BANNER_PATTERN;
    Material GLOW_BERRIES = VanillaMaterial.GLOW_BERRIES;
    Material GLOW_INK_SAC = VanillaMaterial.GLOW_INK_SAC;
    Material GLOW_ITEM_FRAME = VanillaMaterial.GLOW_ITEM_FRAME;
    Material GLOW_SQUID_SPAWN_EGG = VanillaMaterial.GLOW_SQUID_SPAWN_EGG;
    Material GLOWSTONE_DUST = VanillaMaterial.GLOWSTONE_DUST;
    Material GOAT_HORN = VanillaMaterial.GOAT_HORN;
    Material GOAT_SPAWN_EGG = VanillaMaterial.GOAT_SPAWN_EGG;
    Material GOLD_INGOT = VanillaMaterial.GOLD_INGOT;
    Material GOLD_NUGGET = VanillaMaterial.GOLD_NUGGET;
    Material GOLDEN_APPLE = VanillaMaterial.GOLDEN_APPLE;
    Material GOLDEN_AXE = VanillaMaterial.GOLDEN_AXE;
    Material GOLDEN_BOOTS = VanillaMaterial.GOLDEN_BOOTS;
    Material GOLDEN_CARROT = VanillaMaterial.GOLDEN_CARROT;
    Material GOLDEN_CHESTPLATE = VanillaMaterial.GOLDEN_CHESTPLATE;
    Material GOLDEN_HELMET = VanillaMaterial.GOLDEN_HELMET;
    Material GOLDEN_HOE = VanillaMaterial.GOLDEN_HOE;
    Material GOLDEN_HORSE_ARMOR = VanillaMaterial.GOLDEN_HORSE_ARMOR;
    Material GOLDEN_LEGGINGS = VanillaMaterial.GOLDEN_LEGGINGS;
    Material GOLDEN_NAUTILUS_ARMOR = VanillaMaterial.GOLDEN_NAUTILUS_ARMOR;
    Material GOLDEN_PICKAXE = VanillaMaterial.GOLDEN_PICKAXE;
    Material GOLDEN_SHOVEL = VanillaMaterial.GOLDEN_SHOVEL;
    Material GOLDEN_SPEAR = VanillaMaterial.GOLDEN_SPEAR;
    Material GOLDEN_SWORD = VanillaMaterial.GOLDEN_SWORD;
    Material GRAY_BUNDLE = VanillaMaterial.GRAY_BUNDLE;
    Material GRAY_DYE = VanillaMaterial.GRAY_DYE;
    Material GRAY_HARNESS = VanillaMaterial.GRAY_HARNESS;
    Material GREEN_BUNDLE = VanillaMaterial.GREEN_BUNDLE;
    Material GREEN_DYE = VanillaMaterial.GREEN_DYE;
    Material GREEN_HARNESS = VanillaMaterial.GREEN_HARNESS;
    Material GUARDIAN_SPAWN_EGG = VanillaMaterial.GUARDIAN_SPAWN_EGG;
    Material GUNPOWDER = VanillaMaterial.GUNPOWDER;
    Material GUSTER_BANNER_PATTERN = VanillaMaterial.GUSTER_BANNER_PATTERN;
    Material GUSTER_POTTERY_SHERD = VanillaMaterial.GUSTER_POTTERY_SHERD;
    Material HAPPY_GHAST_SPAWN_EGG = VanillaMaterial.HAPPY_GHAST_SPAWN_EGG;
    Material HEART_OF_THE_SEA = VanillaMaterial.HEART_OF_THE_SEA;
    Material HEART_POTTERY_SHERD = VanillaMaterial.HEART_POTTERY_SHERD;
    Material HEARTBREAK_POTTERY_SHERD = VanillaMaterial.HEARTBREAK_POTTERY_SHERD;
    Material HOGLIN_SPAWN_EGG = VanillaMaterial.HOGLIN_SPAWN_EGG;
    Material HONEY_BOTTLE = VanillaMaterial.HONEY_BOTTLE;
    Material HONEYCOMB = VanillaMaterial.HONEYCOMB;
    Material HOPPER_MINECART = VanillaMaterial.HOPPER_MINECART;
    Material HORSE_SPAWN_EGG = VanillaMaterial.HORSE_SPAWN_EGG;
    Material HOST_ARMOR_TRIM_SMITHING_TEMPLATE = VanillaMaterial.HOST_ARMOR_TRIM_SMITHING_TEMPLATE;
    Material HOWL_POTTERY_SHERD = VanillaMaterial.HOWL_POTTERY_SHERD;
    Material HUSK_SPAWN_EGG = VanillaMaterial.HUSK_SPAWN_EGG;
    Material INK_SAC = VanillaMaterial.INK_SAC;
    Material IRON_AXE = VanillaMaterial.IRON_AXE;
    Material IRON_BOOTS = VanillaMaterial.IRON_BOOTS;
    Material IRON_CHESTPLATE = VanillaMaterial.IRON_CHESTPLATE;
    Material IRON_GOLEM_SPAWN_EGG = VanillaMaterial.IRON_GOLEM_SPAWN_EGG;
    Material IRON_HELMET = VanillaMaterial.IRON_HELMET;
    Material IRON_HOE = VanillaMaterial.IRON_HOE;
    Material IRON_HORSE_ARMOR = VanillaMaterial.IRON_HORSE_ARMOR;
    Material IRON_INGOT = VanillaMaterial.IRON_INGOT;
    Material IRON_LEGGINGS = VanillaMaterial.IRON_LEGGINGS;
    Material IRON_NAUTILUS_ARMOR = VanillaMaterial.IRON_NAUTILUS_ARMOR;
    Material IRON_NUGGET = VanillaMaterial.IRON_NUGGET;
    Material IRON_PICKAXE = VanillaMaterial.IRON_PICKAXE;
    Material IRON_SHOVEL = VanillaMaterial.IRON_SHOVEL;
    Material IRON_SPEAR = VanillaMaterial.IRON_SPEAR;
    Material IRON_SWORD = VanillaMaterial.IRON_SWORD;
    Material ITEM_FRAME = VanillaMaterial.ITEM_FRAME;
    Material JUNGLE_BOAT = VanillaMaterial.JUNGLE_BOAT;
    Material JUNGLE_CHEST_BOAT = VanillaMaterial.JUNGLE_CHEST_BOAT;
    Material KNOWLEDGE_BOOK = VanillaMaterial.KNOWLEDGE_BOOK;
    Material LAPIS_LAZULI = VanillaMaterial.LAPIS_LAZULI;
    Material LAVA_BUCKET = VanillaMaterial.LAVA_BUCKET;
    Material LEAD = VanillaMaterial.LEAD;
    Material LEATHER = VanillaMaterial.LEATHER;
    Material LEATHER_BOOTS = VanillaMaterial.LEATHER_BOOTS;
    Material LEATHER_CHESTPLATE = VanillaMaterial.LEATHER_CHESTPLATE;
    Material LEATHER_HELMET = VanillaMaterial.LEATHER_HELMET;
    Material LEATHER_HORSE_ARMOR = VanillaMaterial.LEATHER_HORSE_ARMOR;
    Material LEATHER_LEGGINGS = VanillaMaterial.LEATHER_LEGGINGS;
    Material LIGHT_BLUE_BUNDLE = VanillaMaterial.LIGHT_BLUE_BUNDLE;
    Material LIGHT_BLUE_DYE = VanillaMaterial.LIGHT_BLUE_DYE;
    Material LIGHT_BLUE_HARNESS = VanillaMaterial.LIGHT_BLUE_HARNESS;
    Material LIGHT_GRAY_BUNDLE = VanillaMaterial.LIGHT_GRAY_BUNDLE;
    Material LIGHT_GRAY_DYE = VanillaMaterial.LIGHT_GRAY_DYE;
    Material LIGHT_GRAY_HARNESS = VanillaMaterial.LIGHT_GRAY_HARNESS;
    Material LIME_BUNDLE = VanillaMaterial.LIME_BUNDLE;
    Material LIME_DYE = VanillaMaterial.LIME_DYE;
    Material LIME_HARNESS = VanillaMaterial.LIME_HARNESS;
    Material LINGERING_POTION = VanillaMaterial.LINGERING_POTION;
    Material LLAMA_SPAWN_EGG = VanillaMaterial.LLAMA_SPAWN_EGG;
    Material MACE = VanillaMaterial.MACE;
    Material MAGENTA_BUNDLE = VanillaMaterial.MAGENTA_BUNDLE;
    Material MAGENTA_DYE = VanillaMaterial.MAGENTA_DYE;
    Material MAGENTA_HARNESS = VanillaMaterial.MAGENTA_HARNESS;
    Material MAGMA_CREAM = VanillaMaterial.MAGMA_CREAM;
    Material MAGMA_CUBE_SPAWN_EGG = VanillaMaterial.MAGMA_CUBE_SPAWN_EGG;
    Material MANGROVE_BOAT = VanillaMaterial.MANGROVE_BOAT;
    Material MANGROVE_CHEST_BOAT = VanillaMaterial.MANGROVE_CHEST_BOAT;
    Material MAP = VanillaMaterial.MAP;
    Material MELON_SEEDS = VanillaMaterial.MELON_SEEDS;
    Material MELON_SLICE = VanillaMaterial.MELON_SLICE;
    Material MILK_BUCKET = VanillaMaterial.MILK_BUCKET;
    Material MINECART = VanillaMaterial.MINECART;
    Material MINER_POTTERY_SHERD = VanillaMaterial.MINER_POTTERY_SHERD;
    Material MOJANG_BANNER_PATTERN = VanillaMaterial.MOJANG_BANNER_PATTERN;
    Material MOOSHROOM_SPAWN_EGG = VanillaMaterial.MOOSHROOM_SPAWN_EGG;
    Material MOURNER_POTTERY_SHERD = VanillaMaterial.MOURNER_POTTERY_SHERD;
    Material MULE_SPAWN_EGG = VanillaMaterial.MULE_SPAWN_EGG;
    Material MUSHROOM_STEW = VanillaMaterial.MUSHROOM_STEW;
    Material MUSIC_DISC_5 = VanillaMaterial.MUSIC_DISC_5;
    Material MUSIC_DISC_11 = VanillaMaterial.MUSIC_DISC_11;
    Material MUSIC_DISC_13 = VanillaMaterial.MUSIC_DISC_13;
    Material MUSIC_DISC_BLOCKS = VanillaMaterial.MUSIC_DISC_BLOCKS;
    Material MUSIC_DISC_BOUNCE = VanillaMaterial.MUSIC_DISC_BOUNCE;
    Material MUSIC_DISC_CAT = VanillaMaterial.MUSIC_DISC_CAT;
    Material MUSIC_DISC_CHIRP = VanillaMaterial.MUSIC_DISC_CHIRP;
    Material MUSIC_DISC_CREATOR = VanillaMaterial.MUSIC_DISC_CREATOR;
    Material MUSIC_DISC_CREATOR_MUSIC_BOX = VanillaMaterial.MUSIC_DISC_CREATOR_MUSIC_BOX;
    Material MUSIC_DISC_FAR = VanillaMaterial.MUSIC_DISC_FAR;
    Material MUSIC_DISC_LAVA_CHICKEN = VanillaMaterial.MUSIC_DISC_LAVA_CHICKEN;
    Material MUSIC_DISC_MALL = VanillaMaterial.MUSIC_DISC_MALL;
    Material MUSIC_DISC_MELLOHI = VanillaMaterial.MUSIC_DISC_MELLOHI;
    Material MUSIC_DISC_OTHERSIDE = VanillaMaterial.MUSIC_DISC_OTHERSIDE;
    Material MUSIC_DISC_PIGSTEP = VanillaMaterial.MUSIC_DISC_PIGSTEP;
    Material MUSIC_DISC_PRECIPICE = VanillaMaterial.MUSIC_DISC_PRECIPICE;
    Material MUSIC_DISC_RELIC = VanillaMaterial.MUSIC_DISC_RELIC;
    Material MUSIC_DISC_STAL = VanillaMaterial.MUSIC_DISC_STAL;
    Material MUSIC_DISC_STRAD = VanillaMaterial.MUSIC_DISC_STRAD;
    Material MUSIC_DISC_TEARS = VanillaMaterial.MUSIC_DISC_TEARS;
    Material MUSIC_DISC_WAIT = VanillaMaterial.MUSIC_DISC_WAIT;
    Material MUSIC_DISC_WARD = VanillaMaterial.MUSIC_DISC_WARD;
    Material MUTTON = VanillaMaterial.MUTTON;
    Material NAME_TAG = VanillaMaterial.NAME_TAG;
    Material NAUTILUS_SHELL = VanillaMaterial.NAUTILUS_SHELL;
    Material NAUTILUS_SPAWN_EGG = VanillaMaterial.NAUTILUS_SPAWN_EGG;
    Material NETHER_BRICK = VanillaMaterial.NETHER_BRICK;
    Material NETHER_STAR = VanillaMaterial.NETHER_STAR;
    Material NETHERITE_AXE = VanillaMaterial.NETHERITE_AXE;
    Material NETHERITE_BOOTS = VanillaMaterial.NETHERITE_BOOTS;
    Material NETHERITE_CHESTPLATE = VanillaMaterial.NETHERITE_CHESTPLATE;
    Material NETHERITE_HELMET = VanillaMaterial.NETHERITE_HELMET;
    Material NETHERITE_HOE = VanillaMaterial.NETHERITE_HOE;
    Material NETHERITE_HORSE_ARMOR = VanillaMaterial.NETHERITE_HORSE_ARMOR;
    Material NETHERITE_INGOT = VanillaMaterial.NETHERITE_INGOT;
    Material NETHERITE_LEGGINGS = VanillaMaterial.NETHERITE_LEGGINGS;
    Material NETHERITE_NAUTILUS_ARMOR = VanillaMaterial.NETHERITE_NAUTILUS_ARMOR;
    Material NETHERITE_PICKAXE = VanillaMaterial.NETHERITE_PICKAXE;
    Material NETHERITE_SCRAP = VanillaMaterial.NETHERITE_SCRAP;
    Material NETHERITE_SHOVEL = VanillaMaterial.NETHERITE_SHOVEL;
    Material NETHERITE_SPEAR = VanillaMaterial.NETHERITE_SPEAR;
    Material NETHERITE_SWORD = VanillaMaterial.NETHERITE_SWORD;
    Material NETHERITE_UPGRADE_SMITHING_TEMPLATE = VanillaMaterial.NETHERITE_UPGRADE_SMITHING_TEMPLATE;
    Material OAK_BOAT = VanillaMaterial.OAK_BOAT;
    Material OAK_CHEST_BOAT = VanillaMaterial.OAK_CHEST_BOAT;
    Material OCELOT_SPAWN_EGG = VanillaMaterial.OCELOT_SPAWN_EGG;
    Material OMINOUS_BOTTLE = VanillaMaterial.OMINOUS_BOTTLE;
    Material OMINOUS_TRIAL_KEY = VanillaMaterial.OMINOUS_TRIAL_KEY;
    Material ORANGE_BUNDLE = VanillaMaterial.ORANGE_BUNDLE;
    Material ORANGE_DYE = VanillaMaterial.ORANGE_DYE;
    Material ORANGE_HARNESS = VanillaMaterial.ORANGE_HARNESS;
    Material PAINTING = VanillaMaterial.PAINTING;
    Material PALE_OAK_BOAT = VanillaMaterial.PALE_OAK_BOAT;
    Material PALE_OAK_CHEST_BOAT = VanillaMaterial.PALE_OAK_CHEST_BOAT;
    Material PANDA_SPAWN_EGG = VanillaMaterial.PANDA_SPAWN_EGG;
    Material PAPER = VanillaMaterial.PAPER;
    Material PARCHED_SPAWN_EGG = VanillaMaterial.PARCHED_SPAWN_EGG;
    Material PARROT_SPAWN_EGG = VanillaMaterial.PARROT_SPAWN_EGG;
    Material PHANTOM_MEMBRANE = VanillaMaterial.PHANTOM_MEMBRANE;
    Material PHANTOM_SPAWN_EGG = VanillaMaterial.PHANTOM_SPAWN_EGG;
    Material PIG_SPAWN_EGG = VanillaMaterial.PIG_SPAWN_EGG;
    Material PIGLIN_BANNER_PATTERN = VanillaMaterial.PIGLIN_BANNER_PATTERN;
    Material PIGLIN_BRUTE_SPAWN_EGG = VanillaMaterial.PIGLIN_BRUTE_SPAWN_EGG;
    Material PIGLIN_SPAWN_EGG = VanillaMaterial.PIGLIN_SPAWN_EGG;
    Material PILLAGER_SPAWN_EGG = VanillaMaterial.PILLAGER_SPAWN_EGG;
    Material PINK_BUNDLE = VanillaMaterial.PINK_BUNDLE;
    Material PINK_DYE = VanillaMaterial.PINK_DYE;
    Material PINK_HARNESS = VanillaMaterial.PINK_HARNESS;
    Material PITCHER_POD = VanillaMaterial.PITCHER_POD;
    Material PLENTY_POTTERY_SHERD = VanillaMaterial.PLENTY_POTTERY_SHERD;
    Material POISONOUS_POTATO = VanillaMaterial.POISONOUS_POTATO;
    Material POLAR_BEAR_SPAWN_EGG = VanillaMaterial.POLAR_BEAR_SPAWN_EGG;
    Material POPPED_CHORUS_FRUIT = VanillaMaterial.POPPED_CHORUS_FRUIT;
    Material PORKCHOP = VanillaMaterial.PORKCHOP;
    Material POTATO = VanillaMaterial.POTATO;
    Material POTION = VanillaMaterial.POTION;
    Material POWDER_SNOW_BUCKET = VanillaMaterial.POWDER_SNOW_BUCKET;
    Material PRISMARINE_CRYSTALS = VanillaMaterial.PRISMARINE_CRYSTALS;
    Material PRISMARINE_SHARD = VanillaMaterial.PRISMARINE_SHARD;
    Material PRIZE_POTTERY_SHERD = VanillaMaterial.PRIZE_POTTERY_SHERD;
    Material PUFFERFISH = VanillaMaterial.PUFFERFISH;
    Material PUFFERFISH_BUCKET = VanillaMaterial.PUFFERFISH_BUCKET;
    Material PUFFERFISH_SPAWN_EGG = VanillaMaterial.PUFFERFISH_SPAWN_EGG;
    Material PUMPKIN_PIE = VanillaMaterial.PUMPKIN_PIE;
    Material PUMPKIN_SEEDS = VanillaMaterial.PUMPKIN_SEEDS;
    Material PURPLE_BUNDLE = VanillaMaterial.PURPLE_BUNDLE;
    Material PURPLE_DYE = VanillaMaterial.PURPLE_DYE;
    Material PURPLE_HARNESS = VanillaMaterial.PURPLE_HARNESS;
    Material QUARTZ = VanillaMaterial.QUARTZ;
    Material RABBIT = VanillaMaterial.RABBIT;
    Material RABBIT_FOOT = VanillaMaterial.RABBIT_FOOT;
    Material RABBIT_HIDE = VanillaMaterial.RABBIT_HIDE;
    Material RABBIT_SPAWN_EGG = VanillaMaterial.RABBIT_SPAWN_EGG;
    Material RABBIT_STEW = VanillaMaterial.RABBIT_STEW;
    Material RAISER_ARMOR_TRIM_SMITHING_TEMPLATE = VanillaMaterial.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE;
    Material RAVAGER_SPAWN_EGG = VanillaMaterial.RAVAGER_SPAWN_EGG;
    Material RAW_COPPER = VanillaMaterial.RAW_COPPER;
    Material RAW_GOLD = VanillaMaterial.RAW_GOLD;
    Material RAW_IRON = VanillaMaterial.RAW_IRON;
    Material RECOVERY_COMPASS = VanillaMaterial.RECOVERY_COMPASS;
    Material RED_BUNDLE = VanillaMaterial.RED_BUNDLE;
    Material RED_DYE = VanillaMaterial.RED_DYE;
    Material RED_HARNESS = VanillaMaterial.RED_HARNESS;
    Material REDSTONE = VanillaMaterial.REDSTONE;
    Material RESIN_BRICK = VanillaMaterial.RESIN_BRICK;
    Material RIB_ARMOR_TRIM_SMITHING_TEMPLATE = VanillaMaterial.RIB_ARMOR_TRIM_SMITHING_TEMPLATE;
    Material ROTTEN_FLESH = VanillaMaterial.ROTTEN_FLESH;
    Material SADDLE = VanillaMaterial.SADDLE;
    Material SALMON = VanillaMaterial.SALMON;
    Material SALMON_BUCKET = VanillaMaterial.SALMON_BUCKET;
    Material SALMON_SPAWN_EGG = VanillaMaterial.SALMON_SPAWN_EGG;
    Material SCRAPE_POTTERY_SHERD = VanillaMaterial.SCRAPE_POTTERY_SHERD;
    Material SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE = VanillaMaterial.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE;
    Material SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE = VanillaMaterial.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE;
    Material SHEAF_POTTERY_SHERD = VanillaMaterial.SHEAF_POTTERY_SHERD;
    Material SHEARS = VanillaMaterial.SHEARS;
    Material SHEEP_SPAWN_EGG = VanillaMaterial.SHEEP_SPAWN_EGG;
    Material SHELTER_POTTERY_SHERD = VanillaMaterial.SHELTER_POTTERY_SHERD;
    Material SHIELD = VanillaMaterial.SHIELD;
    Material SHULKER_SHELL = VanillaMaterial.SHULKER_SHELL;
    Material SHULKER_SPAWN_EGG = VanillaMaterial.SHULKER_SPAWN_EGG;
    Material SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE = VanillaMaterial.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE;
    Material SILVERFISH_SPAWN_EGG = VanillaMaterial.SILVERFISH_SPAWN_EGG;
    Material SKELETON_HORSE_SPAWN_EGG = VanillaMaterial.SKELETON_HORSE_SPAWN_EGG;
    Material SKELETON_SPAWN_EGG = VanillaMaterial.SKELETON_SPAWN_EGG;
    Material SKULL_BANNER_PATTERN = VanillaMaterial.SKULL_BANNER_PATTERN;
    Material SKULL_POTTERY_SHERD = VanillaMaterial.SKULL_POTTERY_SHERD;
    Material SLIME_BALL = VanillaMaterial.SLIME_BALL;
    Material SLIME_SPAWN_EGG = VanillaMaterial.SLIME_SPAWN_EGG;
    Material SNIFFER_SPAWN_EGG = VanillaMaterial.SNIFFER_SPAWN_EGG;
    Material SNORT_POTTERY_SHERD = VanillaMaterial.SNORT_POTTERY_SHERD;
    Material SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE = VanillaMaterial.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE;
    Material SNOW_GOLEM_SPAWN_EGG = VanillaMaterial.SNOW_GOLEM_SPAWN_EGG;
    Material SNOWBALL = VanillaMaterial.SNOWBALL;
    Material SPECTRAL_ARROW = VanillaMaterial.SPECTRAL_ARROW;
    Material SPIDER_EYE = VanillaMaterial.SPIDER_EYE;
    Material SPIDER_SPAWN_EGG = VanillaMaterial.SPIDER_SPAWN_EGG;
    Material SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE = VanillaMaterial.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE;
    Material SPLASH_POTION = VanillaMaterial.SPLASH_POTION;
    Material SPRUCE_BOAT = VanillaMaterial.SPRUCE_BOAT;
    Material SPRUCE_CHEST_BOAT = VanillaMaterial.SPRUCE_CHEST_BOAT;
    Material SPYGLASS = VanillaMaterial.SPYGLASS;
    Material SQUID_SPAWN_EGG = VanillaMaterial.SQUID_SPAWN_EGG;
    Material STICK = VanillaMaterial.STICK;
    Material STONE_AXE = VanillaMaterial.STONE_AXE;
    Material STONE_HOE = VanillaMaterial.STONE_HOE;
    Material STONE_PICKAXE = VanillaMaterial.STONE_PICKAXE;
    Material STONE_SHOVEL = VanillaMaterial.STONE_SHOVEL;
    Material STONE_SPEAR = VanillaMaterial.STONE_SPEAR;
    Material STONE_SWORD = VanillaMaterial.STONE_SWORD;
    Material STRAY_SPAWN_EGG = VanillaMaterial.STRAY_SPAWN_EGG;
    Material STRIDER_SPAWN_EGG = VanillaMaterial.STRIDER_SPAWN_EGG;
    Material STRING = VanillaMaterial.STRING;
    Material SUGAR = VanillaMaterial.SUGAR;
    Material SULFUR_CUBE_BUCKET = VanillaMaterial.SULFUR_CUBE_BUCKET;
    Material SULFUR_CUBE_SPAWN_EGG = VanillaMaterial.SULFUR_CUBE_SPAWN_EGG;
    Material SUSPICIOUS_STEW = VanillaMaterial.SUSPICIOUS_STEW;
    Material SWEET_BERRIES = VanillaMaterial.SWEET_BERRIES;
    Material TADPOLE_BUCKET = VanillaMaterial.TADPOLE_BUCKET;
    Material TADPOLE_SPAWN_EGG = VanillaMaterial.TADPOLE_SPAWN_EGG;
    Material TIDE_ARMOR_TRIM_SMITHING_TEMPLATE = VanillaMaterial.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE;
    Material TIPPED_ARROW = VanillaMaterial.TIPPED_ARROW;
    Material TNT_MINECART = VanillaMaterial.TNT_MINECART;
    Material TORCHFLOWER_SEEDS = VanillaMaterial.TORCHFLOWER_SEEDS;
    Material TOTEM_OF_UNDYING = VanillaMaterial.TOTEM_OF_UNDYING;
    Material TRADER_LLAMA_SPAWN_EGG = VanillaMaterial.TRADER_LLAMA_SPAWN_EGG;
    Material TRIAL_KEY = VanillaMaterial.TRIAL_KEY;
    Material TRIDENT = VanillaMaterial.TRIDENT;
    Material TROPICAL_FISH = VanillaMaterial.TROPICAL_FISH;
    Material TROPICAL_FISH_BUCKET = VanillaMaterial.TROPICAL_FISH_BUCKET;
    Material TROPICAL_FISH_SPAWN_EGG = VanillaMaterial.TROPICAL_FISH_SPAWN_EGG;
    Material TURTLE_HELMET = VanillaMaterial.TURTLE_HELMET;
    Material TURTLE_SCUTE = VanillaMaterial.TURTLE_SCUTE;
    Material TURTLE_SPAWN_EGG = VanillaMaterial.TURTLE_SPAWN_EGG;
    Material VEX_ARMOR_TRIM_SMITHING_TEMPLATE = VanillaMaterial.VEX_ARMOR_TRIM_SMITHING_TEMPLATE;
    Material VEX_SPAWN_EGG = VanillaMaterial.VEX_SPAWN_EGG;
    Material VILLAGER_SPAWN_EGG = VanillaMaterial.VILLAGER_SPAWN_EGG;
    Material VINDICATOR_SPAWN_EGG = VanillaMaterial.VINDICATOR_SPAWN_EGG;
    Material WANDERING_TRADER_SPAWN_EGG = VanillaMaterial.WANDERING_TRADER_SPAWN_EGG;
    Material WARD_ARMOR_TRIM_SMITHING_TEMPLATE = VanillaMaterial.WARD_ARMOR_TRIM_SMITHING_TEMPLATE;
    Material WARDEN_SPAWN_EGG = VanillaMaterial.WARDEN_SPAWN_EGG;
    Material WARPED_FUNGUS_ON_A_STICK = VanillaMaterial.WARPED_FUNGUS_ON_A_STICK;
    Material WATER_BUCKET = VanillaMaterial.WATER_BUCKET;
    Material WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE = VanillaMaterial.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE;
    Material WHEAT_SEEDS = VanillaMaterial.WHEAT_SEEDS;
    Material WHITE_BUNDLE = VanillaMaterial.WHITE_BUNDLE;
    Material WHITE_DYE = VanillaMaterial.WHITE_DYE;
    Material WHITE_HARNESS = VanillaMaterial.WHITE_HARNESS;
    Material WILD_ARMOR_TRIM_SMITHING_TEMPLATE = VanillaMaterial.WILD_ARMOR_TRIM_SMITHING_TEMPLATE;
    Material WIND_CHARGE = VanillaMaterial.WIND_CHARGE;
    Material WITCH_SPAWN_EGG = VanillaMaterial.WITCH_SPAWN_EGG;
    Material WITHER_SKELETON_SPAWN_EGG = VanillaMaterial.WITHER_SKELETON_SPAWN_EGG;
    Material WITHER_SPAWN_EGG = VanillaMaterial.WITHER_SPAWN_EGG;
    Material WOLF_ARMOR = VanillaMaterial.WOLF_ARMOR;
    Material WOLF_SPAWN_EGG = VanillaMaterial.WOLF_SPAWN_EGG;
    Material WOODEN_AXE = VanillaMaterial.WOODEN_AXE;
    Material WOODEN_HOE = VanillaMaterial.WOODEN_HOE;
    Material WOODEN_PICKAXE = VanillaMaterial.WOODEN_PICKAXE;
    Material WOODEN_SHOVEL = VanillaMaterial.WOODEN_SHOVEL;
    Material WOODEN_SPEAR = VanillaMaterial.WOODEN_SPEAR;
    Material WOODEN_SWORD = VanillaMaterial.WOODEN_SWORD;
    Material WRITABLE_BOOK = VanillaMaterial.WRITABLE_BOOK;
    Material WRITTEN_BOOK = VanillaMaterial.WRITTEN_BOOK;
    Material YELLOW_BUNDLE = VanillaMaterial.YELLOW_BUNDLE;
    Material YELLOW_DYE = VanillaMaterial.YELLOW_DYE;
    Material YELLOW_HARNESS = VanillaMaterial.YELLOW_HARNESS;
    Material ZOGLIN_SPAWN_EGG = VanillaMaterial.ZOGLIN_SPAWN_EGG;
    Material ZOMBIE_HORSE_SPAWN_EGG = VanillaMaterial.ZOMBIE_HORSE_SPAWN_EGG;
    Material ZOMBIE_NAUTILUS_SPAWN_EGG = VanillaMaterial.ZOMBIE_NAUTILUS_SPAWN_EGG;
    Material ZOMBIE_SPAWN_EGG = VanillaMaterial.ZOMBIE_SPAWN_EGG;
    Material ZOMBIE_VILLAGER_SPAWN_EGG = VanillaMaterial.ZOMBIE_VILLAGER_SPAWN_EGG;
    Material ZOMBIFIED_PIGLIN_SPAWN_EGG = VanillaMaterial.ZOMBIFIED_PIGLIN_SPAWN_EGG;
    Material ACACIA_BUTTON = VanillaMaterial.ACACIA_BUTTON;
    Material ACACIA_DOOR = VanillaMaterial.ACACIA_DOOR;
    Material ACACIA_FENCE = VanillaMaterial.ACACIA_FENCE;
    Material ACACIA_FENCE_GATE = VanillaMaterial.ACACIA_FENCE_GATE;
    Material ACACIA_HANGING_SIGN = VanillaMaterial.ACACIA_HANGING_SIGN;
    Material ACACIA_LEAVES = VanillaMaterial.ACACIA_LEAVES;
    Material ACACIA_LOG = VanillaMaterial.ACACIA_LOG;
    Material ACACIA_PLANKS = VanillaMaterial.ACACIA_PLANKS;
    Material ACACIA_PRESSURE_PLATE = VanillaMaterial.ACACIA_PRESSURE_PLATE;
    Material ACACIA_SAPLING = VanillaMaterial.ACACIA_SAPLING;
    Material ACACIA_SHELF = VanillaMaterial.ACACIA_SHELF;
    Material ACACIA_SIGN = VanillaMaterial.ACACIA_SIGN;
    Material ACACIA_SLAB = VanillaMaterial.ACACIA_SLAB;
    Material ACACIA_STAIRS = VanillaMaterial.ACACIA_STAIRS;
    Material ACACIA_TRAPDOOR = VanillaMaterial.ACACIA_TRAPDOOR;
    Material ACACIA_WALL_HANGING_SIGN = VanillaMaterial.ACACIA_WALL_HANGING_SIGN;
    Material ACACIA_WALL_SIGN = VanillaMaterial.ACACIA_WALL_SIGN;
    Material ACACIA_WOOD = VanillaMaterial.ACACIA_WOOD;
    Material ACTIVATOR_RAIL = VanillaMaterial.ACTIVATOR_RAIL;
    Material ALLIUM = VanillaMaterial.ALLIUM;
    Material AMETHYST_BLOCK = VanillaMaterial.AMETHYST_BLOCK;
    Material AMETHYST_CLUSTER = VanillaMaterial.AMETHYST_CLUSTER;
    Material ANCIENT_DEBRIS = VanillaMaterial.ANCIENT_DEBRIS;
    Material ANDESITE = VanillaMaterial.ANDESITE;
    Material ANDESITE_SLAB = VanillaMaterial.ANDESITE_SLAB;
    Material ANDESITE_STAIRS = VanillaMaterial.ANDESITE_STAIRS;
    Material ANDESITE_WALL = VanillaMaterial.ANDESITE_WALL;
    Material ANVIL = VanillaMaterial.ANVIL;
    Material ATTACHED_MELON_STEM = VanillaMaterial.ATTACHED_MELON_STEM;
    Material ATTACHED_PUMPKIN_STEM = VanillaMaterial.ATTACHED_PUMPKIN_STEM;
    Material AZALEA = VanillaMaterial.AZALEA;
    Material AZALEA_LEAVES = VanillaMaterial.AZALEA_LEAVES;
    Material AZURE_BLUET = VanillaMaterial.AZURE_BLUET;
    Material BAMBOO = VanillaMaterial.BAMBOO;
    Material BAMBOO_BLOCK = VanillaMaterial.BAMBOO_BLOCK;
    Material BAMBOO_BUTTON = VanillaMaterial.BAMBOO_BUTTON;
    Material BAMBOO_DOOR = VanillaMaterial.BAMBOO_DOOR;
    Material BAMBOO_FENCE = VanillaMaterial.BAMBOO_FENCE;
    Material BAMBOO_FENCE_GATE = VanillaMaterial.BAMBOO_FENCE_GATE;
    Material BAMBOO_HANGING_SIGN = VanillaMaterial.BAMBOO_HANGING_SIGN;
    Material BAMBOO_MOSAIC = VanillaMaterial.BAMBOO_MOSAIC;
    Material BAMBOO_MOSAIC_SLAB = VanillaMaterial.BAMBOO_MOSAIC_SLAB;
    Material BAMBOO_MOSAIC_STAIRS = VanillaMaterial.BAMBOO_MOSAIC_STAIRS;
    Material BAMBOO_PLANKS = VanillaMaterial.BAMBOO_PLANKS;
    Material BAMBOO_PRESSURE_PLATE = VanillaMaterial.BAMBOO_PRESSURE_PLATE;
    Material BAMBOO_SAPLING = VanillaMaterial.BAMBOO_SAPLING;
    Material BAMBOO_SHELF = VanillaMaterial.BAMBOO_SHELF;
    Material BAMBOO_SIGN = VanillaMaterial.BAMBOO_SIGN;
    Material BAMBOO_SLAB = VanillaMaterial.BAMBOO_SLAB;
    Material BAMBOO_STAIRS = VanillaMaterial.BAMBOO_STAIRS;
    Material BAMBOO_TRAPDOOR = VanillaMaterial.BAMBOO_TRAPDOOR;
    Material BAMBOO_WALL_HANGING_SIGN = VanillaMaterial.BAMBOO_WALL_HANGING_SIGN;
    Material BAMBOO_WALL_SIGN = VanillaMaterial.BAMBOO_WALL_SIGN;
    Material BARREL = VanillaMaterial.BARREL;
    Material BARRIER = VanillaMaterial.BARRIER;
    Material BASALT = VanillaMaterial.BASALT;
    Material BEACON = VanillaMaterial.BEACON;
    Material BEDROCK = VanillaMaterial.BEDROCK;
    Material BEE_NEST = VanillaMaterial.BEE_NEST;
    Material BEEHIVE = VanillaMaterial.BEEHIVE;
    Material BEETROOTS = VanillaMaterial.BEETROOTS;
    Material BELL = VanillaMaterial.BELL;
    Material BIG_DRIPLEAF = VanillaMaterial.BIG_DRIPLEAF;
    Material BIG_DRIPLEAF_STEM = VanillaMaterial.BIG_DRIPLEAF_STEM;
    Material BIRCH_BUTTON = VanillaMaterial.BIRCH_BUTTON;
    Material BIRCH_DOOR = VanillaMaterial.BIRCH_DOOR;
    Material BIRCH_FENCE = VanillaMaterial.BIRCH_FENCE;
    Material BIRCH_FENCE_GATE = VanillaMaterial.BIRCH_FENCE_GATE;
    Material BIRCH_HANGING_SIGN = VanillaMaterial.BIRCH_HANGING_SIGN;
    Material BIRCH_LEAVES = VanillaMaterial.BIRCH_LEAVES;
    Material BIRCH_LOG = VanillaMaterial.BIRCH_LOG;
    Material BIRCH_PLANKS = VanillaMaterial.BIRCH_PLANKS;
    Material BIRCH_PRESSURE_PLATE = VanillaMaterial.BIRCH_PRESSURE_PLATE;
    Material BIRCH_SAPLING = VanillaMaterial.BIRCH_SAPLING;
    Material BIRCH_SHELF = VanillaMaterial.BIRCH_SHELF;
    Material BIRCH_SIGN = VanillaMaterial.BIRCH_SIGN;
    Material BIRCH_SLAB = VanillaMaterial.BIRCH_SLAB;
    Material BIRCH_STAIRS = VanillaMaterial.BIRCH_STAIRS;
    Material BIRCH_TRAPDOOR = VanillaMaterial.BIRCH_TRAPDOOR;
    Material BIRCH_WALL_HANGING_SIGN = VanillaMaterial.BIRCH_WALL_HANGING_SIGN;
    Material BIRCH_WALL_SIGN = VanillaMaterial.BIRCH_WALL_SIGN;
    Material BIRCH_WOOD = VanillaMaterial.BIRCH_WOOD;
    Material BLACK_BANNER = VanillaMaterial.BLACK_BANNER;
    Material BLACK_BED = VanillaMaterial.BLACK_BED;
    Material BLACK_CANDLE = VanillaMaterial.BLACK_CANDLE;
    Material BLACK_CANDLE_CAKE = VanillaMaterial.BLACK_CANDLE_CAKE;
    Material BLACK_CARPET = VanillaMaterial.BLACK_CARPET;
    Material BLACK_CONCRETE = VanillaMaterial.BLACK_CONCRETE;
    Material BLACK_CONCRETE_POWDER = VanillaMaterial.BLACK_CONCRETE_POWDER;
    Material BLACK_GLAZED_TERRACOTTA = VanillaMaterial.BLACK_GLAZED_TERRACOTTA;
    Material BLACK_SHULKER_BOX = VanillaMaterial.BLACK_SHULKER_BOX;
    Material BLACK_STAINED_GLASS = VanillaMaterial.BLACK_STAINED_GLASS;
    Material BLACK_STAINED_GLASS_PANE = VanillaMaterial.BLACK_STAINED_GLASS_PANE;
    Material BLACK_TERRACOTTA = VanillaMaterial.BLACK_TERRACOTTA;
    Material BLACK_WALL_BANNER = VanillaMaterial.BLACK_WALL_BANNER;
    Material BLACK_WOOL = VanillaMaterial.BLACK_WOOL;
    Material BLACKSTONE = VanillaMaterial.BLACKSTONE;
    Material BLACKSTONE_SLAB = VanillaMaterial.BLACKSTONE_SLAB;
    Material BLACKSTONE_STAIRS = VanillaMaterial.BLACKSTONE_STAIRS;
    Material BLACKSTONE_WALL = VanillaMaterial.BLACKSTONE_WALL;
    Material BLAST_FURNACE = VanillaMaterial.BLAST_FURNACE;
    Material BLUE_BANNER = VanillaMaterial.BLUE_BANNER;
    Material BLUE_BED = VanillaMaterial.BLUE_BED;
    Material BLUE_CANDLE = VanillaMaterial.BLUE_CANDLE;
    Material BLUE_CANDLE_CAKE = VanillaMaterial.BLUE_CANDLE_CAKE;
    Material BLUE_CARPET = VanillaMaterial.BLUE_CARPET;
    Material BLUE_CONCRETE = VanillaMaterial.BLUE_CONCRETE;
    Material BLUE_CONCRETE_POWDER = VanillaMaterial.BLUE_CONCRETE_POWDER;
    Material BLUE_GLAZED_TERRACOTTA = VanillaMaterial.BLUE_GLAZED_TERRACOTTA;
    Material BLUE_ICE = VanillaMaterial.BLUE_ICE;
    Material BLUE_ORCHID = VanillaMaterial.BLUE_ORCHID;
    Material BLUE_SHULKER_BOX = VanillaMaterial.BLUE_SHULKER_BOX;
    Material BLUE_STAINED_GLASS = VanillaMaterial.BLUE_STAINED_GLASS;
    Material BLUE_STAINED_GLASS_PANE = VanillaMaterial.BLUE_STAINED_GLASS_PANE;
    Material BLUE_TERRACOTTA = VanillaMaterial.BLUE_TERRACOTTA;
    Material BLUE_WALL_BANNER = VanillaMaterial.BLUE_WALL_BANNER;
    Material BLUE_WOOL = VanillaMaterial.BLUE_WOOL;
    Material BONE_BLOCK = VanillaMaterial.BONE_BLOCK;
    Material BOOKSHELF = VanillaMaterial.BOOKSHELF;
    Material BRAIN_CORAL = VanillaMaterial.BRAIN_CORAL;
    Material BRAIN_CORAL_BLOCK = VanillaMaterial.BRAIN_CORAL_BLOCK;
    Material BRAIN_CORAL_FAN = VanillaMaterial.BRAIN_CORAL_FAN;
    Material BRAIN_CORAL_WALL_FAN = VanillaMaterial.BRAIN_CORAL_WALL_FAN;
    Material BREWING_STAND = VanillaMaterial.BREWING_STAND;
    Material BRICK_SLAB = VanillaMaterial.BRICK_SLAB;
    Material BRICK_STAIRS = VanillaMaterial.BRICK_STAIRS;
    Material BRICK_WALL = VanillaMaterial.BRICK_WALL;
    Material BRICKS = VanillaMaterial.BRICKS;
    Material BROWN_BANNER = VanillaMaterial.BROWN_BANNER;
    Material BROWN_BED = VanillaMaterial.BROWN_BED;
    Material BROWN_CANDLE = VanillaMaterial.BROWN_CANDLE;
    Material BROWN_CANDLE_CAKE = VanillaMaterial.BROWN_CANDLE_CAKE;
    Material BROWN_CARPET = VanillaMaterial.BROWN_CARPET;
    Material BROWN_CONCRETE = VanillaMaterial.BROWN_CONCRETE;
    Material BROWN_CONCRETE_POWDER = VanillaMaterial.BROWN_CONCRETE_POWDER;
    Material BROWN_GLAZED_TERRACOTTA = VanillaMaterial.BROWN_GLAZED_TERRACOTTA;
    Material BROWN_MUSHROOM = VanillaMaterial.BROWN_MUSHROOM;
    Material BROWN_MUSHROOM_BLOCK = VanillaMaterial.BROWN_MUSHROOM_BLOCK;
    Material BROWN_SHULKER_BOX = VanillaMaterial.BROWN_SHULKER_BOX;
    Material BROWN_STAINED_GLASS = VanillaMaterial.BROWN_STAINED_GLASS;
    Material BROWN_STAINED_GLASS_PANE = VanillaMaterial.BROWN_STAINED_GLASS_PANE;
    Material BROWN_TERRACOTTA = VanillaMaterial.BROWN_TERRACOTTA;
    Material BROWN_WALL_BANNER = VanillaMaterial.BROWN_WALL_BANNER;
    Material BROWN_WOOL = VanillaMaterial.BROWN_WOOL;
    Material BUBBLE_COLUMN = VanillaMaterial.BUBBLE_COLUMN;
    Material BUBBLE_CORAL = VanillaMaterial.BUBBLE_CORAL;
    Material BUBBLE_CORAL_BLOCK = VanillaMaterial.BUBBLE_CORAL_BLOCK;
    Material BUBBLE_CORAL_FAN = VanillaMaterial.BUBBLE_CORAL_FAN;
    Material BUBBLE_CORAL_WALL_FAN = VanillaMaterial.BUBBLE_CORAL_WALL_FAN;
    Material BUDDING_AMETHYST = VanillaMaterial.BUDDING_AMETHYST;
    Material BUSH = VanillaMaterial.BUSH;
    Material CACTUS = VanillaMaterial.CACTUS;
    Material CACTUS_FLOWER = VanillaMaterial.CACTUS_FLOWER;
    Material CAKE = VanillaMaterial.CAKE;
    Material CALCITE = VanillaMaterial.CALCITE;
    Material CALIBRATED_SCULK_SENSOR = VanillaMaterial.CALIBRATED_SCULK_SENSOR;
    Material CAMPFIRE = VanillaMaterial.CAMPFIRE;
    Material CANDLE = VanillaMaterial.CANDLE;
    Material CANDLE_CAKE = VanillaMaterial.CANDLE_CAKE;
    Material CARROTS = VanillaMaterial.CARROTS;
    Material CARTOGRAPHY_TABLE = VanillaMaterial.CARTOGRAPHY_TABLE;
    Material CARVED_PUMPKIN = VanillaMaterial.CARVED_PUMPKIN;
    Material CAULDRON = VanillaMaterial.CAULDRON;
    Material CAVE_AIR = VanillaMaterial.CAVE_AIR;
    Material CAVE_VINES = VanillaMaterial.CAVE_VINES;
    Material CAVE_VINES_PLANT = VanillaMaterial.CAVE_VINES_PLANT;
    Material CHAIN_COMMAND_BLOCK = VanillaMaterial.CHAIN_COMMAND_BLOCK;
    Material CHERRY_BUTTON = VanillaMaterial.CHERRY_BUTTON;
    Material CHERRY_DOOR = VanillaMaterial.CHERRY_DOOR;
    Material CHERRY_FENCE = VanillaMaterial.CHERRY_FENCE;
    Material CHERRY_FENCE_GATE = VanillaMaterial.CHERRY_FENCE_GATE;
    Material CHERRY_HANGING_SIGN = VanillaMaterial.CHERRY_HANGING_SIGN;
    Material CHERRY_LEAVES = VanillaMaterial.CHERRY_LEAVES;
    Material CHERRY_LOG = VanillaMaterial.CHERRY_LOG;
    Material CHERRY_PLANKS = VanillaMaterial.CHERRY_PLANKS;
    Material CHERRY_PRESSURE_PLATE = VanillaMaterial.CHERRY_PRESSURE_PLATE;
    Material CHERRY_SAPLING = VanillaMaterial.CHERRY_SAPLING;
    Material CHERRY_SHELF = VanillaMaterial.CHERRY_SHELF;
    Material CHERRY_SIGN = VanillaMaterial.CHERRY_SIGN;
    Material CHERRY_SLAB = VanillaMaterial.CHERRY_SLAB;
    Material CHERRY_STAIRS = VanillaMaterial.CHERRY_STAIRS;
    Material CHERRY_TRAPDOOR = VanillaMaterial.CHERRY_TRAPDOOR;
    Material CHERRY_WALL_HANGING_SIGN = VanillaMaterial.CHERRY_WALL_HANGING_SIGN;
    Material CHERRY_WALL_SIGN = VanillaMaterial.CHERRY_WALL_SIGN;
    Material CHERRY_WOOD = VanillaMaterial.CHERRY_WOOD;
    Material CHEST = VanillaMaterial.CHEST;
    Material CHIPPED_ANVIL = VanillaMaterial.CHIPPED_ANVIL;
    Material CHISELED_BOOKSHELF = VanillaMaterial.CHISELED_BOOKSHELF;
    Material CHISELED_CINNABAR = VanillaMaterial.CHISELED_CINNABAR;
    Material CHISELED_COPPER = VanillaMaterial.CHISELED_COPPER;
    Material CHISELED_DEEPSLATE = VanillaMaterial.CHISELED_DEEPSLATE;
    Material CHISELED_NETHER_BRICKS = VanillaMaterial.CHISELED_NETHER_BRICKS;
    Material CHISELED_POLISHED_BLACKSTONE = VanillaMaterial.CHISELED_POLISHED_BLACKSTONE;
    Material CHISELED_QUARTZ_BLOCK = VanillaMaterial.CHISELED_QUARTZ_BLOCK;
    Material CHISELED_RED_SANDSTONE = VanillaMaterial.CHISELED_RED_SANDSTONE;
    Material CHISELED_RESIN_BRICKS = VanillaMaterial.CHISELED_RESIN_BRICKS;
    Material CHISELED_SANDSTONE = VanillaMaterial.CHISELED_SANDSTONE;
    Material CHISELED_STONE_BRICKS = VanillaMaterial.CHISELED_STONE_BRICKS;
    Material CHISELED_SULFUR = VanillaMaterial.CHISELED_SULFUR;
    Material CHISELED_TUFF = VanillaMaterial.CHISELED_TUFF;
    Material CHISELED_TUFF_BRICKS = VanillaMaterial.CHISELED_TUFF_BRICKS;
    Material CHORUS_FLOWER = VanillaMaterial.CHORUS_FLOWER;
    Material CHORUS_PLANT = VanillaMaterial.CHORUS_PLANT;
    Material CINNABAR = VanillaMaterial.CINNABAR;
    Material CINNABAR_BRICK_SLAB = VanillaMaterial.CINNABAR_BRICK_SLAB;
    Material CINNABAR_BRICK_STAIRS = VanillaMaterial.CINNABAR_BRICK_STAIRS;
    Material CINNABAR_BRICK_WALL = VanillaMaterial.CINNABAR_BRICK_WALL;
    Material CINNABAR_BRICKS = VanillaMaterial.CINNABAR_BRICKS;
    Material CINNABAR_SLAB = VanillaMaterial.CINNABAR_SLAB;
    Material CINNABAR_STAIRS = VanillaMaterial.CINNABAR_STAIRS;
    Material CINNABAR_WALL = VanillaMaterial.CINNABAR_WALL;
    Material CLAY = VanillaMaterial.CLAY;
    Material CLOSED_EYEBLOSSOM = VanillaMaterial.CLOSED_EYEBLOSSOM;
    Material COAL_BLOCK = VanillaMaterial.COAL_BLOCK;
    Material COAL_ORE = VanillaMaterial.COAL_ORE;
    Material COARSE_DIRT = VanillaMaterial.COARSE_DIRT;
    Material COBBLED_DEEPSLATE = VanillaMaterial.COBBLED_DEEPSLATE;
    Material COBBLED_DEEPSLATE_SLAB = VanillaMaterial.COBBLED_DEEPSLATE_SLAB;
    Material COBBLED_DEEPSLATE_STAIRS = VanillaMaterial.COBBLED_DEEPSLATE_STAIRS;
    Material COBBLED_DEEPSLATE_WALL = VanillaMaterial.COBBLED_DEEPSLATE_WALL;
    Material COBBLESTONE = VanillaMaterial.COBBLESTONE;
    Material COBBLESTONE_SLAB = VanillaMaterial.COBBLESTONE_SLAB;
    Material COBBLESTONE_STAIRS = VanillaMaterial.COBBLESTONE_STAIRS;
    Material COBBLESTONE_WALL = VanillaMaterial.COBBLESTONE_WALL;
    Material COBWEB = VanillaMaterial.COBWEB;
    Material COCOA = VanillaMaterial.COCOA;
    Material COMMAND_BLOCK = VanillaMaterial.COMMAND_BLOCK;
    Material COMPARATOR = VanillaMaterial.COMPARATOR;
    Material COMPOSTER = VanillaMaterial.COMPOSTER;
    Material CONDUIT = VanillaMaterial.CONDUIT;
    Material COPPER_BARS = VanillaMaterial.COPPER_BARS;
    Material COPPER_BLOCK = VanillaMaterial.COPPER_BLOCK;
    Material COPPER_BULB = VanillaMaterial.COPPER_BULB;
    Material COPPER_CHAIN = VanillaMaterial.COPPER_CHAIN;
    Material COPPER_CHEST = VanillaMaterial.COPPER_CHEST;
    Material COPPER_DOOR = VanillaMaterial.COPPER_DOOR;
    Material COPPER_GOLEM_STATUE = VanillaMaterial.COPPER_GOLEM_STATUE;
    Material COPPER_GRATE = VanillaMaterial.COPPER_GRATE;
    Material COPPER_LANTERN = VanillaMaterial.COPPER_LANTERN;
    Material COPPER_ORE = VanillaMaterial.COPPER_ORE;
    Material COPPER_TORCH = VanillaMaterial.COPPER_TORCH;
    Material COPPER_TRAPDOOR = VanillaMaterial.COPPER_TRAPDOOR;
    Material COPPER_WALL_TORCH = VanillaMaterial.COPPER_WALL_TORCH;
    Material CORNFLOWER = VanillaMaterial.CORNFLOWER;
    Material CRACKED_DEEPSLATE_BRICKS = VanillaMaterial.CRACKED_DEEPSLATE_BRICKS;
    Material CRACKED_DEEPSLATE_TILES = VanillaMaterial.CRACKED_DEEPSLATE_TILES;
    Material CRACKED_NETHER_BRICKS = VanillaMaterial.CRACKED_NETHER_BRICKS;
    Material CRACKED_POLISHED_BLACKSTONE_BRICKS = VanillaMaterial.CRACKED_POLISHED_BLACKSTONE_BRICKS;
    Material CRACKED_STONE_BRICKS = VanillaMaterial.CRACKED_STONE_BRICKS;
    Material CRAFTER = VanillaMaterial.CRAFTER;
    Material CRAFTING_TABLE = VanillaMaterial.CRAFTING_TABLE;
    Material CREAKING_HEART = VanillaMaterial.CREAKING_HEART;
    Material CREEPER_HEAD = VanillaMaterial.CREEPER_HEAD;
    Material CREEPER_WALL_HEAD = VanillaMaterial.CREEPER_WALL_HEAD;
    Material CRIMSON_BUTTON = VanillaMaterial.CRIMSON_BUTTON;
    Material CRIMSON_DOOR = VanillaMaterial.CRIMSON_DOOR;
    Material CRIMSON_FENCE = VanillaMaterial.CRIMSON_FENCE;
    Material CRIMSON_FENCE_GATE = VanillaMaterial.CRIMSON_FENCE_GATE;
    Material CRIMSON_FUNGUS = VanillaMaterial.CRIMSON_FUNGUS;
    Material CRIMSON_HANGING_SIGN = VanillaMaterial.CRIMSON_HANGING_SIGN;
    Material CRIMSON_HYPHAE = VanillaMaterial.CRIMSON_HYPHAE;
    Material CRIMSON_NYLIUM = VanillaMaterial.CRIMSON_NYLIUM;
    Material CRIMSON_PLANKS = VanillaMaterial.CRIMSON_PLANKS;
    Material CRIMSON_PRESSURE_PLATE = VanillaMaterial.CRIMSON_PRESSURE_PLATE;
    Material CRIMSON_ROOTS = VanillaMaterial.CRIMSON_ROOTS;
    Material CRIMSON_SHELF = VanillaMaterial.CRIMSON_SHELF;
    Material CRIMSON_SIGN = VanillaMaterial.CRIMSON_SIGN;
    Material CRIMSON_SLAB = VanillaMaterial.CRIMSON_SLAB;
    Material CRIMSON_STAIRS = VanillaMaterial.CRIMSON_STAIRS;
    Material CRIMSON_STEM = VanillaMaterial.CRIMSON_STEM;
    Material CRIMSON_TRAPDOOR = VanillaMaterial.CRIMSON_TRAPDOOR;
    Material CRIMSON_WALL_HANGING_SIGN = VanillaMaterial.CRIMSON_WALL_HANGING_SIGN;
    Material CRIMSON_WALL_SIGN = VanillaMaterial.CRIMSON_WALL_SIGN;
    Material CRYING_OBSIDIAN = VanillaMaterial.CRYING_OBSIDIAN;
    Material CUT_COPPER = VanillaMaterial.CUT_COPPER;
    Material CUT_COPPER_SLAB = VanillaMaterial.CUT_COPPER_SLAB;
    Material CUT_COPPER_STAIRS = VanillaMaterial.CUT_COPPER_STAIRS;
    Material CUT_RED_SANDSTONE = VanillaMaterial.CUT_RED_SANDSTONE;
    Material CUT_RED_SANDSTONE_SLAB = VanillaMaterial.CUT_RED_SANDSTONE_SLAB;
    Material CUT_SANDSTONE = VanillaMaterial.CUT_SANDSTONE;
    Material CUT_SANDSTONE_SLAB = VanillaMaterial.CUT_SANDSTONE_SLAB;
    Material CYAN_BANNER = VanillaMaterial.CYAN_BANNER;
    Material CYAN_BED = VanillaMaterial.CYAN_BED;
    Material CYAN_CANDLE = VanillaMaterial.CYAN_CANDLE;
    Material CYAN_CANDLE_CAKE = VanillaMaterial.CYAN_CANDLE_CAKE;
    Material CYAN_CARPET = VanillaMaterial.CYAN_CARPET;
    Material CYAN_CONCRETE = VanillaMaterial.CYAN_CONCRETE;
    Material CYAN_CONCRETE_POWDER = VanillaMaterial.CYAN_CONCRETE_POWDER;
    Material CYAN_GLAZED_TERRACOTTA = VanillaMaterial.CYAN_GLAZED_TERRACOTTA;
    Material CYAN_SHULKER_BOX = VanillaMaterial.CYAN_SHULKER_BOX;
    Material CYAN_STAINED_GLASS = VanillaMaterial.CYAN_STAINED_GLASS;
    Material CYAN_STAINED_GLASS_PANE = VanillaMaterial.CYAN_STAINED_GLASS_PANE;
    Material CYAN_TERRACOTTA = VanillaMaterial.CYAN_TERRACOTTA;
    Material CYAN_WALL_BANNER = VanillaMaterial.CYAN_WALL_BANNER;
    Material CYAN_WOOL = VanillaMaterial.CYAN_WOOL;
    Material DAMAGED_ANVIL = VanillaMaterial.DAMAGED_ANVIL;
    Material DANDELION = VanillaMaterial.DANDELION;
    Material DARK_OAK_BUTTON = VanillaMaterial.DARK_OAK_BUTTON;
    Material DARK_OAK_DOOR = VanillaMaterial.DARK_OAK_DOOR;
    Material DARK_OAK_FENCE = VanillaMaterial.DARK_OAK_FENCE;
    Material DARK_OAK_FENCE_GATE = VanillaMaterial.DARK_OAK_FENCE_GATE;
    Material DARK_OAK_HANGING_SIGN = VanillaMaterial.DARK_OAK_HANGING_SIGN;
    Material DARK_OAK_LEAVES = VanillaMaterial.DARK_OAK_LEAVES;
    Material DARK_OAK_LOG = VanillaMaterial.DARK_OAK_LOG;
    Material DARK_OAK_PLANKS = VanillaMaterial.DARK_OAK_PLANKS;
    Material DARK_OAK_PRESSURE_PLATE = VanillaMaterial.DARK_OAK_PRESSURE_PLATE;
    Material DARK_OAK_SAPLING = VanillaMaterial.DARK_OAK_SAPLING;
    Material DARK_OAK_SHELF = VanillaMaterial.DARK_OAK_SHELF;
    Material DARK_OAK_SIGN = VanillaMaterial.DARK_OAK_SIGN;
    Material DARK_OAK_SLAB = VanillaMaterial.DARK_OAK_SLAB;
    Material DARK_OAK_STAIRS = VanillaMaterial.DARK_OAK_STAIRS;
    Material DARK_OAK_TRAPDOOR = VanillaMaterial.DARK_OAK_TRAPDOOR;
    Material DARK_OAK_WALL_HANGING_SIGN = VanillaMaterial.DARK_OAK_WALL_HANGING_SIGN;
    Material DARK_OAK_WALL_SIGN = VanillaMaterial.DARK_OAK_WALL_SIGN;
    Material DARK_OAK_WOOD = VanillaMaterial.DARK_OAK_WOOD;
    Material DARK_PRISMARINE = VanillaMaterial.DARK_PRISMARINE;
    Material DARK_PRISMARINE_SLAB = VanillaMaterial.DARK_PRISMARINE_SLAB;
    Material DARK_PRISMARINE_STAIRS = VanillaMaterial.DARK_PRISMARINE_STAIRS;
    Material DAYLIGHT_DETECTOR = VanillaMaterial.DAYLIGHT_DETECTOR;
    Material DEAD_BRAIN_CORAL = VanillaMaterial.DEAD_BRAIN_CORAL;
    Material DEAD_BRAIN_CORAL_BLOCK = VanillaMaterial.DEAD_BRAIN_CORAL_BLOCK;
    Material DEAD_BRAIN_CORAL_FAN = VanillaMaterial.DEAD_BRAIN_CORAL_FAN;
    Material DEAD_BRAIN_CORAL_WALL_FAN = VanillaMaterial.DEAD_BRAIN_CORAL_WALL_FAN;
    Material DEAD_BUBBLE_CORAL = VanillaMaterial.DEAD_BUBBLE_CORAL;
    Material DEAD_BUBBLE_CORAL_BLOCK = VanillaMaterial.DEAD_BUBBLE_CORAL_BLOCK;
    Material DEAD_BUBBLE_CORAL_FAN = VanillaMaterial.DEAD_BUBBLE_CORAL_FAN;
    Material DEAD_BUBBLE_CORAL_WALL_FAN = VanillaMaterial.DEAD_BUBBLE_CORAL_WALL_FAN;
    Material DEAD_BUSH = VanillaMaterial.DEAD_BUSH;
    Material DEAD_FIRE_CORAL = VanillaMaterial.DEAD_FIRE_CORAL;
    Material DEAD_FIRE_CORAL_BLOCK = VanillaMaterial.DEAD_FIRE_CORAL_BLOCK;
    Material DEAD_FIRE_CORAL_FAN = VanillaMaterial.DEAD_FIRE_CORAL_FAN;
    Material DEAD_FIRE_CORAL_WALL_FAN = VanillaMaterial.DEAD_FIRE_CORAL_WALL_FAN;
    Material DEAD_HORN_CORAL = VanillaMaterial.DEAD_HORN_CORAL;
    Material DEAD_HORN_CORAL_BLOCK = VanillaMaterial.DEAD_HORN_CORAL_BLOCK;
    Material DEAD_HORN_CORAL_FAN = VanillaMaterial.DEAD_HORN_CORAL_FAN;
    Material DEAD_HORN_CORAL_WALL_FAN = VanillaMaterial.DEAD_HORN_CORAL_WALL_FAN;
    Material DEAD_TUBE_CORAL = VanillaMaterial.DEAD_TUBE_CORAL;
    Material DEAD_TUBE_CORAL_BLOCK = VanillaMaterial.DEAD_TUBE_CORAL_BLOCK;
    Material DEAD_TUBE_CORAL_FAN = VanillaMaterial.DEAD_TUBE_CORAL_FAN;
    Material DEAD_TUBE_CORAL_WALL_FAN = VanillaMaterial.DEAD_TUBE_CORAL_WALL_FAN;
    Material DECORATED_POT = VanillaMaterial.DECORATED_POT;
    Material DEEPSLATE = VanillaMaterial.DEEPSLATE;
    Material DEEPSLATE_BRICK_SLAB = VanillaMaterial.DEEPSLATE_BRICK_SLAB;
    Material DEEPSLATE_BRICK_STAIRS = VanillaMaterial.DEEPSLATE_BRICK_STAIRS;
    Material DEEPSLATE_BRICK_WALL = VanillaMaterial.DEEPSLATE_BRICK_WALL;
    Material DEEPSLATE_BRICKS = VanillaMaterial.DEEPSLATE_BRICKS;
    Material DEEPSLATE_COAL_ORE = VanillaMaterial.DEEPSLATE_COAL_ORE;
    Material DEEPSLATE_COPPER_ORE = VanillaMaterial.DEEPSLATE_COPPER_ORE;
    Material DEEPSLATE_DIAMOND_ORE = VanillaMaterial.DEEPSLATE_DIAMOND_ORE;
    Material DEEPSLATE_EMERALD_ORE = VanillaMaterial.DEEPSLATE_EMERALD_ORE;
    Material DEEPSLATE_GOLD_ORE = VanillaMaterial.DEEPSLATE_GOLD_ORE;
    Material DEEPSLATE_IRON_ORE = VanillaMaterial.DEEPSLATE_IRON_ORE;
    Material DEEPSLATE_LAPIS_ORE = VanillaMaterial.DEEPSLATE_LAPIS_ORE;
    Material DEEPSLATE_REDSTONE_ORE = VanillaMaterial.DEEPSLATE_REDSTONE_ORE;
    Material DEEPSLATE_TILE_SLAB = VanillaMaterial.DEEPSLATE_TILE_SLAB;
    Material DEEPSLATE_TILE_STAIRS = VanillaMaterial.DEEPSLATE_TILE_STAIRS;
    Material DEEPSLATE_TILE_WALL = VanillaMaterial.DEEPSLATE_TILE_WALL;
    Material DEEPSLATE_TILES = VanillaMaterial.DEEPSLATE_TILES;
    Material DETECTOR_RAIL = VanillaMaterial.DETECTOR_RAIL;
    Material DIAMOND_BLOCK = VanillaMaterial.DIAMOND_BLOCK;
    Material DIAMOND_ORE = VanillaMaterial.DIAMOND_ORE;
    Material DIORITE = VanillaMaterial.DIORITE;
    Material DIORITE_SLAB = VanillaMaterial.DIORITE_SLAB;
    Material DIORITE_STAIRS = VanillaMaterial.DIORITE_STAIRS;
    Material DIORITE_WALL = VanillaMaterial.DIORITE_WALL;
    Material DIRT = VanillaMaterial.DIRT;
    Material DIRT_PATH = VanillaMaterial.DIRT_PATH;
    Material DISPENSER = VanillaMaterial.DISPENSER;
    Material DRAGON_EGG = VanillaMaterial.DRAGON_EGG;
    Material DRAGON_HEAD = VanillaMaterial.DRAGON_HEAD;
    Material DRAGON_WALL_HEAD = VanillaMaterial.DRAGON_WALL_HEAD;
    Material DRIED_GHAST = VanillaMaterial.DRIED_GHAST;
    Material DRIED_KELP_BLOCK = VanillaMaterial.DRIED_KELP_BLOCK;
    Material DRIPSTONE_BLOCK = VanillaMaterial.DRIPSTONE_BLOCK;
    Material DROPPER = VanillaMaterial.DROPPER;
    Material EMERALD_BLOCK = VanillaMaterial.EMERALD_BLOCK;
    Material EMERALD_ORE = VanillaMaterial.EMERALD_ORE;
    Material ENCHANTING_TABLE = VanillaMaterial.ENCHANTING_TABLE;
    Material END_GATEWAY = VanillaMaterial.END_GATEWAY;
    Material END_PORTAL = VanillaMaterial.END_PORTAL;
    Material END_PORTAL_FRAME = VanillaMaterial.END_PORTAL_FRAME;
    Material END_ROD = VanillaMaterial.END_ROD;
    Material END_STONE = VanillaMaterial.END_STONE;
    Material END_STONE_BRICK_SLAB = VanillaMaterial.END_STONE_BRICK_SLAB;
    Material END_STONE_BRICK_STAIRS = VanillaMaterial.END_STONE_BRICK_STAIRS;
    Material END_STONE_BRICK_WALL = VanillaMaterial.END_STONE_BRICK_WALL;
    Material END_STONE_BRICKS = VanillaMaterial.END_STONE_BRICKS;
    Material ENDER_CHEST = VanillaMaterial.ENDER_CHEST;
    Material EXPOSED_CHISELED_COPPER = VanillaMaterial.EXPOSED_CHISELED_COPPER;
    Material EXPOSED_COPPER = VanillaMaterial.EXPOSED_COPPER;
    Material EXPOSED_COPPER_BARS = VanillaMaterial.EXPOSED_COPPER_BARS;
    Material EXPOSED_COPPER_BULB = VanillaMaterial.EXPOSED_COPPER_BULB;
    Material EXPOSED_COPPER_CHAIN = VanillaMaterial.EXPOSED_COPPER_CHAIN;
    Material EXPOSED_COPPER_CHEST = VanillaMaterial.EXPOSED_COPPER_CHEST;
    Material EXPOSED_COPPER_DOOR = VanillaMaterial.EXPOSED_COPPER_DOOR;
    Material EXPOSED_COPPER_GOLEM_STATUE = VanillaMaterial.EXPOSED_COPPER_GOLEM_STATUE;
    Material EXPOSED_COPPER_GRATE = VanillaMaterial.EXPOSED_COPPER_GRATE;
    Material EXPOSED_COPPER_LANTERN = VanillaMaterial.EXPOSED_COPPER_LANTERN;
    Material EXPOSED_COPPER_TRAPDOOR = VanillaMaterial.EXPOSED_COPPER_TRAPDOOR;
    Material EXPOSED_CUT_COPPER = VanillaMaterial.EXPOSED_CUT_COPPER;
    Material EXPOSED_CUT_COPPER_SLAB = VanillaMaterial.EXPOSED_CUT_COPPER_SLAB;
    Material EXPOSED_CUT_COPPER_STAIRS = VanillaMaterial.EXPOSED_CUT_COPPER_STAIRS;
    Material EXPOSED_LIGHTNING_ROD = VanillaMaterial.EXPOSED_LIGHTNING_ROD;
    Material FARMLAND = VanillaMaterial.FARMLAND;
    Material FERN = VanillaMaterial.FERN;
    Material FIRE = VanillaMaterial.FIRE;
    Material FIRE_CORAL = VanillaMaterial.FIRE_CORAL;
    Material FIRE_CORAL_BLOCK = VanillaMaterial.FIRE_CORAL_BLOCK;
    Material FIRE_CORAL_FAN = VanillaMaterial.FIRE_CORAL_FAN;
    Material FIRE_CORAL_WALL_FAN = VanillaMaterial.FIRE_CORAL_WALL_FAN;
    Material FIREFLY_BUSH = VanillaMaterial.FIREFLY_BUSH;
    Material FLETCHING_TABLE = VanillaMaterial.FLETCHING_TABLE;
    Material FLOWER_POT = VanillaMaterial.FLOWER_POT;
    Material FLOWERING_AZALEA = VanillaMaterial.FLOWERING_AZALEA;
    Material FLOWERING_AZALEA_LEAVES = VanillaMaterial.FLOWERING_AZALEA_LEAVES;
    Material FROGSPAWN = VanillaMaterial.FROGSPAWN;
    Material FROSTED_ICE = VanillaMaterial.FROSTED_ICE;
    Material FURNACE = VanillaMaterial.FURNACE;
    Material GILDED_BLACKSTONE = VanillaMaterial.GILDED_BLACKSTONE;
    Material GLASS = VanillaMaterial.GLASS;
    Material GLASS_PANE = VanillaMaterial.GLASS_PANE;
    Material GLOW_LICHEN = VanillaMaterial.GLOW_LICHEN;
    Material GLOWSTONE = VanillaMaterial.GLOWSTONE;
    Material GOLD_BLOCK = VanillaMaterial.GOLD_BLOCK;
    Material GOLD_ORE = VanillaMaterial.GOLD_ORE;
    Material GOLDEN_DANDELION = VanillaMaterial.GOLDEN_DANDELION;
    Material GRANITE = VanillaMaterial.GRANITE;
    Material GRANITE_SLAB = VanillaMaterial.GRANITE_SLAB;
    Material GRANITE_STAIRS = VanillaMaterial.GRANITE_STAIRS;
    Material GRANITE_WALL = VanillaMaterial.GRANITE_WALL;
    Material GRASS_BLOCK = VanillaMaterial.GRASS_BLOCK;
    Material GRAVEL = VanillaMaterial.GRAVEL;
    Material GRAY_BANNER = VanillaMaterial.GRAY_BANNER;
    Material GRAY_BED = VanillaMaterial.GRAY_BED;
    Material GRAY_CANDLE = VanillaMaterial.GRAY_CANDLE;
    Material GRAY_CANDLE_CAKE = VanillaMaterial.GRAY_CANDLE_CAKE;
    Material GRAY_CARPET = VanillaMaterial.GRAY_CARPET;
    Material GRAY_CONCRETE = VanillaMaterial.GRAY_CONCRETE;
    Material GRAY_CONCRETE_POWDER = VanillaMaterial.GRAY_CONCRETE_POWDER;
    Material GRAY_GLAZED_TERRACOTTA = VanillaMaterial.GRAY_GLAZED_TERRACOTTA;
    Material GRAY_SHULKER_BOX = VanillaMaterial.GRAY_SHULKER_BOX;
    Material GRAY_STAINED_GLASS = VanillaMaterial.GRAY_STAINED_GLASS;
    Material GRAY_STAINED_GLASS_PANE = VanillaMaterial.GRAY_STAINED_GLASS_PANE;
    Material GRAY_TERRACOTTA = VanillaMaterial.GRAY_TERRACOTTA;
    Material GRAY_WALL_BANNER = VanillaMaterial.GRAY_WALL_BANNER;
    Material GRAY_WOOL = VanillaMaterial.GRAY_WOOL;
    Material GREEN_BANNER = VanillaMaterial.GREEN_BANNER;
    Material GREEN_BED = VanillaMaterial.GREEN_BED;
    Material GREEN_CANDLE = VanillaMaterial.GREEN_CANDLE;
    Material GREEN_CANDLE_CAKE = VanillaMaterial.GREEN_CANDLE_CAKE;
    Material GREEN_CARPET = VanillaMaterial.GREEN_CARPET;
    Material GREEN_CONCRETE = VanillaMaterial.GREEN_CONCRETE;
    Material GREEN_CONCRETE_POWDER = VanillaMaterial.GREEN_CONCRETE_POWDER;
    Material GREEN_GLAZED_TERRACOTTA = VanillaMaterial.GREEN_GLAZED_TERRACOTTA;
    Material GREEN_SHULKER_BOX = VanillaMaterial.GREEN_SHULKER_BOX;
    Material GREEN_STAINED_GLASS = VanillaMaterial.GREEN_STAINED_GLASS;
    Material GREEN_STAINED_GLASS_PANE = VanillaMaterial.GREEN_STAINED_GLASS_PANE;
    Material GREEN_TERRACOTTA = VanillaMaterial.GREEN_TERRACOTTA;
    Material GREEN_WALL_BANNER = VanillaMaterial.GREEN_WALL_BANNER;
    Material GREEN_WOOL = VanillaMaterial.GREEN_WOOL;
    Material GRINDSTONE = VanillaMaterial.GRINDSTONE;
    Material HANGING_ROOTS = VanillaMaterial.HANGING_ROOTS;
    Material HAY_BLOCK = VanillaMaterial.HAY_BLOCK;
    Material HEAVY_CORE = VanillaMaterial.HEAVY_CORE;
    Material HEAVY_WEIGHTED_PRESSURE_PLATE = VanillaMaterial.HEAVY_WEIGHTED_PRESSURE_PLATE;
    Material HONEY_BLOCK = VanillaMaterial.HONEY_BLOCK;
    Material HONEYCOMB_BLOCK = VanillaMaterial.HONEYCOMB_BLOCK;
    Material HOPPER = VanillaMaterial.HOPPER;
    Material HORN_CORAL = VanillaMaterial.HORN_CORAL;
    Material HORN_CORAL_BLOCK = VanillaMaterial.HORN_CORAL_BLOCK;
    Material HORN_CORAL_FAN = VanillaMaterial.HORN_CORAL_FAN;
    Material HORN_CORAL_WALL_FAN = VanillaMaterial.HORN_CORAL_WALL_FAN;
    Material ICE = VanillaMaterial.ICE;
    Material INFESTED_CHISELED_STONE_BRICKS = VanillaMaterial.INFESTED_CHISELED_STONE_BRICKS;
    Material INFESTED_COBBLESTONE = VanillaMaterial.INFESTED_COBBLESTONE;
    Material INFESTED_CRACKED_STONE_BRICKS = VanillaMaterial.INFESTED_CRACKED_STONE_BRICKS;
    Material INFESTED_DEEPSLATE = VanillaMaterial.INFESTED_DEEPSLATE;
    Material INFESTED_MOSSY_STONE_BRICKS = VanillaMaterial.INFESTED_MOSSY_STONE_BRICKS;
    Material INFESTED_STONE = VanillaMaterial.INFESTED_STONE;
    Material INFESTED_STONE_BRICKS = VanillaMaterial.INFESTED_STONE_BRICKS;
    Material IRON_BARS = VanillaMaterial.IRON_BARS;
    Material IRON_BLOCK = VanillaMaterial.IRON_BLOCK;
    Material IRON_CHAIN = VanillaMaterial.IRON_CHAIN;
    Material IRON_DOOR = VanillaMaterial.IRON_DOOR;
    Material IRON_ORE = VanillaMaterial.IRON_ORE;
    Material IRON_TRAPDOOR = VanillaMaterial.IRON_TRAPDOOR;
    Material JACK_O_LANTERN = VanillaMaterial.JACK_O_LANTERN;
    Material JIGSAW = VanillaMaterial.JIGSAW;
    Material JUKEBOX = VanillaMaterial.JUKEBOX;
    Material JUNGLE_BUTTON = VanillaMaterial.JUNGLE_BUTTON;
    Material JUNGLE_DOOR = VanillaMaterial.JUNGLE_DOOR;
    Material JUNGLE_FENCE = VanillaMaterial.JUNGLE_FENCE;
    Material JUNGLE_FENCE_GATE = VanillaMaterial.JUNGLE_FENCE_GATE;
    Material JUNGLE_HANGING_SIGN = VanillaMaterial.JUNGLE_HANGING_SIGN;
    Material JUNGLE_LEAVES = VanillaMaterial.JUNGLE_LEAVES;
    Material JUNGLE_LOG = VanillaMaterial.JUNGLE_LOG;
    Material JUNGLE_PLANKS = VanillaMaterial.JUNGLE_PLANKS;
    Material JUNGLE_PRESSURE_PLATE = VanillaMaterial.JUNGLE_PRESSURE_PLATE;
    Material JUNGLE_SAPLING = VanillaMaterial.JUNGLE_SAPLING;
    Material JUNGLE_SHELF = VanillaMaterial.JUNGLE_SHELF;
    Material JUNGLE_SIGN = VanillaMaterial.JUNGLE_SIGN;
    Material JUNGLE_SLAB = VanillaMaterial.JUNGLE_SLAB;
    Material JUNGLE_STAIRS = VanillaMaterial.JUNGLE_STAIRS;
    Material JUNGLE_TRAPDOOR = VanillaMaterial.JUNGLE_TRAPDOOR;
    Material JUNGLE_WALL_HANGING_SIGN = VanillaMaterial.JUNGLE_WALL_HANGING_SIGN;
    Material JUNGLE_WALL_SIGN = VanillaMaterial.JUNGLE_WALL_SIGN;
    Material JUNGLE_WOOD = VanillaMaterial.JUNGLE_WOOD;
    Material KELP = VanillaMaterial.KELP;
    Material KELP_PLANT = VanillaMaterial.KELP_PLANT;
    Material LADDER = VanillaMaterial.LADDER;
    Material LANTERN = VanillaMaterial.LANTERN;
    Material LAPIS_BLOCK = VanillaMaterial.LAPIS_BLOCK;
    Material LAPIS_ORE = VanillaMaterial.LAPIS_ORE;
    Material LARGE_AMETHYST_BUD = VanillaMaterial.LARGE_AMETHYST_BUD;
    Material LARGE_FERN = VanillaMaterial.LARGE_FERN;
    Material LAVA = VanillaMaterial.LAVA;
    Material LAVA_CAULDRON = VanillaMaterial.LAVA_CAULDRON;
    Material LEAF_LITTER = VanillaMaterial.LEAF_LITTER;
    Material LECTERN = VanillaMaterial.LECTERN;
    Material LEVER = VanillaMaterial.LEVER;
    Material LIGHT = VanillaMaterial.LIGHT;
    Material LIGHT_BLUE_BANNER = VanillaMaterial.LIGHT_BLUE_BANNER;
    Material LIGHT_BLUE_BED = VanillaMaterial.LIGHT_BLUE_BED;
    Material LIGHT_BLUE_CANDLE = VanillaMaterial.LIGHT_BLUE_CANDLE;
    Material LIGHT_BLUE_CANDLE_CAKE = VanillaMaterial.LIGHT_BLUE_CANDLE_CAKE;
    Material LIGHT_BLUE_CARPET = VanillaMaterial.LIGHT_BLUE_CARPET;
    Material LIGHT_BLUE_CONCRETE = VanillaMaterial.LIGHT_BLUE_CONCRETE;
    Material LIGHT_BLUE_CONCRETE_POWDER = VanillaMaterial.LIGHT_BLUE_CONCRETE_POWDER;
    Material LIGHT_BLUE_GLAZED_TERRACOTTA = VanillaMaterial.LIGHT_BLUE_GLAZED_TERRACOTTA;
    Material LIGHT_BLUE_SHULKER_BOX = VanillaMaterial.LIGHT_BLUE_SHULKER_BOX;
    Material LIGHT_BLUE_STAINED_GLASS = VanillaMaterial.LIGHT_BLUE_STAINED_GLASS;
    Material LIGHT_BLUE_STAINED_GLASS_PANE = VanillaMaterial.LIGHT_BLUE_STAINED_GLASS_PANE;
    Material LIGHT_BLUE_TERRACOTTA = VanillaMaterial.LIGHT_BLUE_TERRACOTTA;
    Material LIGHT_BLUE_WALL_BANNER = VanillaMaterial.LIGHT_BLUE_WALL_BANNER;
    Material LIGHT_BLUE_WOOL = VanillaMaterial.LIGHT_BLUE_WOOL;
    Material LIGHT_GRAY_BANNER = VanillaMaterial.LIGHT_GRAY_BANNER;
    Material LIGHT_GRAY_BED = VanillaMaterial.LIGHT_GRAY_BED;
    Material LIGHT_GRAY_CANDLE = VanillaMaterial.LIGHT_GRAY_CANDLE;
    Material LIGHT_GRAY_CANDLE_CAKE = VanillaMaterial.LIGHT_GRAY_CANDLE_CAKE;
    Material LIGHT_GRAY_CARPET = VanillaMaterial.LIGHT_GRAY_CARPET;
    Material LIGHT_GRAY_CONCRETE = VanillaMaterial.LIGHT_GRAY_CONCRETE;
    Material LIGHT_GRAY_CONCRETE_POWDER = VanillaMaterial.LIGHT_GRAY_CONCRETE_POWDER;
    Material LIGHT_GRAY_GLAZED_TERRACOTTA = VanillaMaterial.LIGHT_GRAY_GLAZED_TERRACOTTA;
    Material LIGHT_GRAY_SHULKER_BOX = VanillaMaterial.LIGHT_GRAY_SHULKER_BOX;
    Material LIGHT_GRAY_STAINED_GLASS = VanillaMaterial.LIGHT_GRAY_STAINED_GLASS;
    Material LIGHT_GRAY_STAINED_GLASS_PANE = VanillaMaterial.LIGHT_GRAY_STAINED_GLASS_PANE;
    Material LIGHT_GRAY_TERRACOTTA = VanillaMaterial.LIGHT_GRAY_TERRACOTTA;
    Material LIGHT_GRAY_WALL_BANNER = VanillaMaterial.LIGHT_GRAY_WALL_BANNER;
    Material LIGHT_GRAY_WOOL = VanillaMaterial.LIGHT_GRAY_WOOL;
    Material LIGHT_WEIGHTED_PRESSURE_PLATE = VanillaMaterial.LIGHT_WEIGHTED_PRESSURE_PLATE;
    Material LIGHTNING_ROD = VanillaMaterial.LIGHTNING_ROD;
    Material LILAC = VanillaMaterial.LILAC;
    Material LILY_OF_THE_VALLEY = VanillaMaterial.LILY_OF_THE_VALLEY;
    Material LILY_PAD = VanillaMaterial.LILY_PAD;
    Material LIME_BANNER = VanillaMaterial.LIME_BANNER;
    Material LIME_BED = VanillaMaterial.LIME_BED;
    Material LIME_CANDLE = VanillaMaterial.LIME_CANDLE;
    Material LIME_CANDLE_CAKE = VanillaMaterial.LIME_CANDLE_CAKE;
    Material LIME_CARPET = VanillaMaterial.LIME_CARPET;
    Material LIME_CONCRETE = VanillaMaterial.LIME_CONCRETE;
    Material LIME_CONCRETE_POWDER = VanillaMaterial.LIME_CONCRETE_POWDER;
    Material LIME_GLAZED_TERRACOTTA = VanillaMaterial.LIME_GLAZED_TERRACOTTA;
    Material LIME_SHULKER_BOX = VanillaMaterial.LIME_SHULKER_BOX;
    Material LIME_STAINED_GLASS = VanillaMaterial.LIME_STAINED_GLASS;
    Material LIME_STAINED_GLASS_PANE = VanillaMaterial.LIME_STAINED_GLASS_PANE;
    Material LIME_TERRACOTTA = VanillaMaterial.LIME_TERRACOTTA;
    Material LIME_WALL_BANNER = VanillaMaterial.LIME_WALL_BANNER;
    Material LIME_WOOL = VanillaMaterial.LIME_WOOL;
    Material LODESTONE = VanillaMaterial.LODESTONE;
    Material LOOM = VanillaMaterial.LOOM;
    Material MAGENTA_BANNER = VanillaMaterial.MAGENTA_BANNER;
    Material MAGENTA_BED = VanillaMaterial.MAGENTA_BED;
    Material MAGENTA_CANDLE = VanillaMaterial.MAGENTA_CANDLE;
    Material MAGENTA_CANDLE_CAKE = VanillaMaterial.MAGENTA_CANDLE_CAKE;
    Material MAGENTA_CARPET = VanillaMaterial.MAGENTA_CARPET;
    Material MAGENTA_CONCRETE = VanillaMaterial.MAGENTA_CONCRETE;
    Material MAGENTA_CONCRETE_POWDER = VanillaMaterial.MAGENTA_CONCRETE_POWDER;
    Material MAGENTA_GLAZED_TERRACOTTA = VanillaMaterial.MAGENTA_GLAZED_TERRACOTTA;
    Material MAGENTA_SHULKER_BOX = VanillaMaterial.MAGENTA_SHULKER_BOX;
    Material MAGENTA_STAINED_GLASS = VanillaMaterial.MAGENTA_STAINED_GLASS;
    Material MAGENTA_STAINED_GLASS_PANE = VanillaMaterial.MAGENTA_STAINED_GLASS_PANE;
    Material MAGENTA_TERRACOTTA = VanillaMaterial.MAGENTA_TERRACOTTA;
    Material MAGENTA_WALL_BANNER = VanillaMaterial.MAGENTA_WALL_BANNER;
    Material MAGENTA_WOOL = VanillaMaterial.MAGENTA_WOOL;
    Material MAGMA_BLOCK = VanillaMaterial.MAGMA_BLOCK;
    Material MANGROVE_BUTTON = VanillaMaterial.MANGROVE_BUTTON;
    Material MANGROVE_DOOR = VanillaMaterial.MANGROVE_DOOR;
    Material MANGROVE_FENCE = VanillaMaterial.MANGROVE_FENCE;
    Material MANGROVE_FENCE_GATE = VanillaMaterial.MANGROVE_FENCE_GATE;
    Material MANGROVE_HANGING_SIGN = VanillaMaterial.MANGROVE_HANGING_SIGN;
    Material MANGROVE_LEAVES = VanillaMaterial.MANGROVE_LEAVES;
    Material MANGROVE_LOG = VanillaMaterial.MANGROVE_LOG;
    Material MANGROVE_PLANKS = VanillaMaterial.MANGROVE_PLANKS;
    Material MANGROVE_PRESSURE_PLATE = VanillaMaterial.MANGROVE_PRESSURE_PLATE;
    Material MANGROVE_PROPAGULE = VanillaMaterial.MANGROVE_PROPAGULE;
    Material MANGROVE_ROOTS = VanillaMaterial.MANGROVE_ROOTS;
    Material MANGROVE_SHELF = VanillaMaterial.MANGROVE_SHELF;
    Material MANGROVE_SIGN = VanillaMaterial.MANGROVE_SIGN;
    Material MANGROVE_SLAB = VanillaMaterial.MANGROVE_SLAB;
    Material MANGROVE_STAIRS = VanillaMaterial.MANGROVE_STAIRS;
    Material MANGROVE_TRAPDOOR = VanillaMaterial.MANGROVE_TRAPDOOR;
    Material MANGROVE_WALL_HANGING_SIGN = VanillaMaterial.MANGROVE_WALL_HANGING_SIGN;
    Material MANGROVE_WALL_SIGN = VanillaMaterial.MANGROVE_WALL_SIGN;
    Material MANGROVE_WOOD = VanillaMaterial.MANGROVE_WOOD;
    Material MEDIUM_AMETHYST_BUD = VanillaMaterial.MEDIUM_AMETHYST_BUD;
    Material MELON = VanillaMaterial.MELON;
    Material MELON_STEM = VanillaMaterial.MELON_STEM;
    Material MOSS_BLOCK = VanillaMaterial.MOSS_BLOCK;
    Material MOSS_CARPET = VanillaMaterial.MOSS_CARPET;
    Material MOSSY_COBBLESTONE = VanillaMaterial.MOSSY_COBBLESTONE;
    Material MOSSY_COBBLESTONE_SLAB = VanillaMaterial.MOSSY_COBBLESTONE_SLAB;
    Material MOSSY_COBBLESTONE_STAIRS = VanillaMaterial.MOSSY_COBBLESTONE_STAIRS;
    Material MOSSY_COBBLESTONE_WALL = VanillaMaterial.MOSSY_COBBLESTONE_WALL;
    Material MOSSY_STONE_BRICK_SLAB = VanillaMaterial.MOSSY_STONE_BRICK_SLAB;
    Material MOSSY_STONE_BRICK_STAIRS = VanillaMaterial.MOSSY_STONE_BRICK_STAIRS;
    Material MOSSY_STONE_BRICK_WALL = VanillaMaterial.MOSSY_STONE_BRICK_WALL;
    Material MOSSY_STONE_BRICKS = VanillaMaterial.MOSSY_STONE_BRICKS;
    Material MOVING_PISTON = VanillaMaterial.MOVING_PISTON;
    Material MUD = VanillaMaterial.MUD;
    Material MUD_BRICK_SLAB = VanillaMaterial.MUD_BRICK_SLAB;
    Material MUD_BRICK_STAIRS = VanillaMaterial.MUD_BRICK_STAIRS;
    Material MUD_BRICK_WALL = VanillaMaterial.MUD_BRICK_WALL;
    Material MUD_BRICKS = VanillaMaterial.MUD_BRICKS;
    Material MUDDY_MANGROVE_ROOTS = VanillaMaterial.MUDDY_MANGROVE_ROOTS;
    Material MUSHROOM_STEM = VanillaMaterial.MUSHROOM_STEM;
    Material MYCELIUM = VanillaMaterial.MYCELIUM;
    Material NETHER_BRICK_FENCE = VanillaMaterial.NETHER_BRICK_FENCE;
    Material NETHER_BRICK_SLAB = VanillaMaterial.NETHER_BRICK_SLAB;
    Material NETHER_BRICK_STAIRS = VanillaMaterial.NETHER_BRICK_STAIRS;
    Material NETHER_BRICK_WALL = VanillaMaterial.NETHER_BRICK_WALL;
    Material NETHER_BRICKS = VanillaMaterial.NETHER_BRICKS;
    Material NETHER_GOLD_ORE = VanillaMaterial.NETHER_GOLD_ORE;
    Material NETHER_PORTAL = VanillaMaterial.NETHER_PORTAL;
    Material NETHER_QUARTZ_ORE = VanillaMaterial.NETHER_QUARTZ_ORE;
    Material NETHER_SPROUTS = VanillaMaterial.NETHER_SPROUTS;
    Material NETHER_WART = VanillaMaterial.NETHER_WART;
    Material NETHER_WART_BLOCK = VanillaMaterial.NETHER_WART_BLOCK;
    Material NETHERITE_BLOCK = VanillaMaterial.NETHERITE_BLOCK;
    Material NETHERRACK = VanillaMaterial.NETHERRACK;
    Material NOTE_BLOCK = VanillaMaterial.NOTE_BLOCK;
    Material OAK_BUTTON = VanillaMaterial.OAK_BUTTON;
    Material OAK_DOOR = VanillaMaterial.OAK_DOOR;
    Material OAK_FENCE = VanillaMaterial.OAK_FENCE;
    Material OAK_FENCE_GATE = VanillaMaterial.OAK_FENCE_GATE;
    Material OAK_HANGING_SIGN = VanillaMaterial.OAK_HANGING_SIGN;
    Material OAK_LEAVES = VanillaMaterial.OAK_LEAVES;
    Material OAK_LOG = VanillaMaterial.OAK_LOG;
    Material OAK_PLANKS = VanillaMaterial.OAK_PLANKS;
    Material OAK_PRESSURE_PLATE = VanillaMaterial.OAK_PRESSURE_PLATE;
    Material OAK_SAPLING = VanillaMaterial.OAK_SAPLING;
    Material OAK_SHELF = VanillaMaterial.OAK_SHELF;
    Material OAK_SIGN = VanillaMaterial.OAK_SIGN;
    Material OAK_SLAB = VanillaMaterial.OAK_SLAB;
    Material OAK_STAIRS = VanillaMaterial.OAK_STAIRS;
    Material OAK_TRAPDOOR = VanillaMaterial.OAK_TRAPDOOR;
    Material OAK_WALL_HANGING_SIGN = VanillaMaterial.OAK_WALL_HANGING_SIGN;
    Material OAK_WALL_SIGN = VanillaMaterial.OAK_WALL_SIGN;
    Material OAK_WOOD = VanillaMaterial.OAK_WOOD;
    Material OBSERVER = VanillaMaterial.OBSERVER;
    Material OBSIDIAN = VanillaMaterial.OBSIDIAN;
    Material OCHRE_FROGLIGHT = VanillaMaterial.OCHRE_FROGLIGHT;
    Material OPEN_EYEBLOSSOM = VanillaMaterial.OPEN_EYEBLOSSOM;
    Material ORANGE_BANNER = VanillaMaterial.ORANGE_BANNER;
    Material ORANGE_BED = VanillaMaterial.ORANGE_BED;
    Material ORANGE_CANDLE = VanillaMaterial.ORANGE_CANDLE;
    Material ORANGE_CANDLE_CAKE = VanillaMaterial.ORANGE_CANDLE_CAKE;
    Material ORANGE_CARPET = VanillaMaterial.ORANGE_CARPET;
    Material ORANGE_CONCRETE = VanillaMaterial.ORANGE_CONCRETE;
    Material ORANGE_CONCRETE_POWDER = VanillaMaterial.ORANGE_CONCRETE_POWDER;
    Material ORANGE_GLAZED_TERRACOTTA = VanillaMaterial.ORANGE_GLAZED_TERRACOTTA;
    Material ORANGE_SHULKER_BOX = VanillaMaterial.ORANGE_SHULKER_BOX;
    Material ORANGE_STAINED_GLASS = VanillaMaterial.ORANGE_STAINED_GLASS;
    Material ORANGE_STAINED_GLASS_PANE = VanillaMaterial.ORANGE_STAINED_GLASS_PANE;
    Material ORANGE_TERRACOTTA = VanillaMaterial.ORANGE_TERRACOTTA;
    Material ORANGE_TULIP = VanillaMaterial.ORANGE_TULIP;
    Material ORANGE_WALL_BANNER = VanillaMaterial.ORANGE_WALL_BANNER;
    Material ORANGE_WOOL = VanillaMaterial.ORANGE_WOOL;
    Material OXEYE_DAISY = VanillaMaterial.OXEYE_DAISY;
    Material OXIDIZED_CHISELED_COPPER = VanillaMaterial.OXIDIZED_CHISELED_COPPER;
    Material OXIDIZED_COPPER = VanillaMaterial.OXIDIZED_COPPER;
    Material OXIDIZED_COPPER_BARS = VanillaMaterial.OXIDIZED_COPPER_BARS;
    Material OXIDIZED_COPPER_BULB = VanillaMaterial.OXIDIZED_COPPER_BULB;
    Material OXIDIZED_COPPER_CHAIN = VanillaMaterial.OXIDIZED_COPPER_CHAIN;
    Material OXIDIZED_COPPER_CHEST = VanillaMaterial.OXIDIZED_COPPER_CHEST;
    Material OXIDIZED_COPPER_DOOR = VanillaMaterial.OXIDIZED_COPPER_DOOR;
    Material OXIDIZED_COPPER_GOLEM_STATUE = VanillaMaterial.OXIDIZED_COPPER_GOLEM_STATUE;
    Material OXIDIZED_COPPER_GRATE = VanillaMaterial.OXIDIZED_COPPER_GRATE;
    Material OXIDIZED_COPPER_LANTERN = VanillaMaterial.OXIDIZED_COPPER_LANTERN;
    Material OXIDIZED_COPPER_TRAPDOOR = VanillaMaterial.OXIDIZED_COPPER_TRAPDOOR;
    Material OXIDIZED_CUT_COPPER = VanillaMaterial.OXIDIZED_CUT_COPPER;
    Material OXIDIZED_CUT_COPPER_SLAB = VanillaMaterial.OXIDIZED_CUT_COPPER_SLAB;
    Material OXIDIZED_CUT_COPPER_STAIRS = VanillaMaterial.OXIDIZED_CUT_COPPER_STAIRS;
    Material OXIDIZED_LIGHTNING_ROD = VanillaMaterial.OXIDIZED_LIGHTNING_ROD;
    Material PACKED_ICE = VanillaMaterial.PACKED_ICE;
    Material PACKED_MUD = VanillaMaterial.PACKED_MUD;
    Material PALE_HANGING_MOSS = VanillaMaterial.PALE_HANGING_MOSS;
    Material PALE_MOSS_BLOCK = VanillaMaterial.PALE_MOSS_BLOCK;
    Material PALE_MOSS_CARPET = VanillaMaterial.PALE_MOSS_CARPET;
    Material PALE_OAK_BUTTON = VanillaMaterial.PALE_OAK_BUTTON;
    Material PALE_OAK_DOOR = VanillaMaterial.PALE_OAK_DOOR;
    Material PALE_OAK_FENCE = VanillaMaterial.PALE_OAK_FENCE;
    Material PALE_OAK_FENCE_GATE = VanillaMaterial.PALE_OAK_FENCE_GATE;
    Material PALE_OAK_HANGING_SIGN = VanillaMaterial.PALE_OAK_HANGING_SIGN;
    Material PALE_OAK_LEAVES = VanillaMaterial.PALE_OAK_LEAVES;
    Material PALE_OAK_LOG = VanillaMaterial.PALE_OAK_LOG;
    Material PALE_OAK_PLANKS = VanillaMaterial.PALE_OAK_PLANKS;
    Material PALE_OAK_PRESSURE_PLATE = VanillaMaterial.PALE_OAK_PRESSURE_PLATE;
    Material PALE_OAK_SAPLING = VanillaMaterial.PALE_OAK_SAPLING;
    Material PALE_OAK_SHELF = VanillaMaterial.PALE_OAK_SHELF;
    Material PALE_OAK_SIGN = VanillaMaterial.PALE_OAK_SIGN;
    Material PALE_OAK_SLAB = VanillaMaterial.PALE_OAK_SLAB;
    Material PALE_OAK_STAIRS = VanillaMaterial.PALE_OAK_STAIRS;
    Material PALE_OAK_TRAPDOOR = VanillaMaterial.PALE_OAK_TRAPDOOR;
    Material PALE_OAK_WALL_HANGING_SIGN = VanillaMaterial.PALE_OAK_WALL_HANGING_SIGN;
    Material PALE_OAK_WALL_SIGN = VanillaMaterial.PALE_OAK_WALL_SIGN;
    Material PALE_OAK_WOOD = VanillaMaterial.PALE_OAK_WOOD;
    Material PEARLESCENT_FROGLIGHT = VanillaMaterial.PEARLESCENT_FROGLIGHT;
    Material PEONY = VanillaMaterial.PEONY;
    Material PETRIFIED_OAK_SLAB = VanillaMaterial.PETRIFIED_OAK_SLAB;
    Material PIGLIN_HEAD = VanillaMaterial.PIGLIN_HEAD;
    Material PIGLIN_WALL_HEAD = VanillaMaterial.PIGLIN_WALL_HEAD;
    Material PINK_BANNER = VanillaMaterial.PINK_BANNER;
    Material PINK_BED = VanillaMaterial.PINK_BED;
    Material PINK_CANDLE = VanillaMaterial.PINK_CANDLE;
    Material PINK_CANDLE_CAKE = VanillaMaterial.PINK_CANDLE_CAKE;
    Material PINK_CARPET = VanillaMaterial.PINK_CARPET;
    Material PINK_CONCRETE = VanillaMaterial.PINK_CONCRETE;
    Material PINK_CONCRETE_POWDER = VanillaMaterial.PINK_CONCRETE_POWDER;
    Material PINK_GLAZED_TERRACOTTA = VanillaMaterial.PINK_GLAZED_TERRACOTTA;
    Material PINK_PETALS = VanillaMaterial.PINK_PETALS;
    Material PINK_SHULKER_BOX = VanillaMaterial.PINK_SHULKER_BOX;
    Material PINK_STAINED_GLASS = VanillaMaterial.PINK_STAINED_GLASS;
    Material PINK_STAINED_GLASS_PANE = VanillaMaterial.PINK_STAINED_GLASS_PANE;
    Material PINK_TERRACOTTA = VanillaMaterial.PINK_TERRACOTTA;
    Material PINK_TULIP = VanillaMaterial.PINK_TULIP;
    Material PINK_WALL_BANNER = VanillaMaterial.PINK_WALL_BANNER;
    Material PINK_WOOL = VanillaMaterial.PINK_WOOL;
    Material PISTON = VanillaMaterial.PISTON;
    Material PISTON_HEAD = VanillaMaterial.PISTON_HEAD;
    Material PITCHER_CROP = VanillaMaterial.PITCHER_CROP;
    Material PITCHER_PLANT = VanillaMaterial.PITCHER_PLANT;
    Material PLAYER_HEAD = VanillaMaterial.PLAYER_HEAD;
    Material PLAYER_WALL_HEAD = VanillaMaterial.PLAYER_WALL_HEAD;
    Material PODZOL = VanillaMaterial.PODZOL;
    Material POINTED_DRIPSTONE = VanillaMaterial.POINTED_DRIPSTONE;
    Material POLISHED_ANDESITE = VanillaMaterial.POLISHED_ANDESITE;
    Material POLISHED_ANDESITE_SLAB = VanillaMaterial.POLISHED_ANDESITE_SLAB;
    Material POLISHED_ANDESITE_STAIRS = VanillaMaterial.POLISHED_ANDESITE_STAIRS;
    Material POLISHED_BASALT = VanillaMaterial.POLISHED_BASALT;
    Material POLISHED_BLACKSTONE = VanillaMaterial.POLISHED_BLACKSTONE;
    Material POLISHED_BLACKSTONE_BRICK_SLAB = VanillaMaterial.POLISHED_BLACKSTONE_BRICK_SLAB;
    Material POLISHED_BLACKSTONE_BRICK_STAIRS = VanillaMaterial.POLISHED_BLACKSTONE_BRICK_STAIRS;
    Material POLISHED_BLACKSTONE_BRICK_WALL = VanillaMaterial.POLISHED_BLACKSTONE_BRICK_WALL;
    Material POLISHED_BLACKSTONE_BRICKS = VanillaMaterial.POLISHED_BLACKSTONE_BRICKS;
    Material POLISHED_BLACKSTONE_BUTTON = VanillaMaterial.POLISHED_BLACKSTONE_BUTTON;
    Material POLISHED_BLACKSTONE_PRESSURE_PLATE = VanillaMaterial.POLISHED_BLACKSTONE_PRESSURE_PLATE;
    Material POLISHED_BLACKSTONE_SLAB = VanillaMaterial.POLISHED_BLACKSTONE_SLAB;
    Material POLISHED_BLACKSTONE_STAIRS = VanillaMaterial.POLISHED_BLACKSTONE_STAIRS;
    Material POLISHED_BLACKSTONE_WALL = VanillaMaterial.POLISHED_BLACKSTONE_WALL;
    Material POLISHED_CINNABAR = VanillaMaterial.POLISHED_CINNABAR;
    Material POLISHED_CINNABAR_SLAB = VanillaMaterial.POLISHED_CINNABAR_SLAB;
    Material POLISHED_CINNABAR_STAIRS = VanillaMaterial.POLISHED_CINNABAR_STAIRS;
    Material POLISHED_CINNABAR_WALL = VanillaMaterial.POLISHED_CINNABAR_WALL;
    Material POLISHED_DEEPSLATE = VanillaMaterial.POLISHED_DEEPSLATE;
    Material POLISHED_DEEPSLATE_SLAB = VanillaMaterial.POLISHED_DEEPSLATE_SLAB;
    Material POLISHED_DEEPSLATE_STAIRS = VanillaMaterial.POLISHED_DEEPSLATE_STAIRS;
    Material POLISHED_DEEPSLATE_WALL = VanillaMaterial.POLISHED_DEEPSLATE_WALL;
    Material POLISHED_DIORITE = VanillaMaterial.POLISHED_DIORITE;
    Material POLISHED_DIORITE_SLAB = VanillaMaterial.POLISHED_DIORITE_SLAB;
    Material POLISHED_DIORITE_STAIRS = VanillaMaterial.POLISHED_DIORITE_STAIRS;
    Material POLISHED_GRANITE = VanillaMaterial.POLISHED_GRANITE;
    Material POLISHED_GRANITE_SLAB = VanillaMaterial.POLISHED_GRANITE_SLAB;
    Material POLISHED_GRANITE_STAIRS = VanillaMaterial.POLISHED_GRANITE_STAIRS;
    Material POLISHED_SULFUR = VanillaMaterial.POLISHED_SULFUR;
    Material POLISHED_SULFUR_SLAB = VanillaMaterial.POLISHED_SULFUR_SLAB;
    Material POLISHED_SULFUR_STAIRS = VanillaMaterial.POLISHED_SULFUR_STAIRS;
    Material POLISHED_SULFUR_WALL = VanillaMaterial.POLISHED_SULFUR_WALL;
    Material POLISHED_TUFF = VanillaMaterial.POLISHED_TUFF;
    Material POLISHED_TUFF_SLAB = VanillaMaterial.POLISHED_TUFF_SLAB;
    Material POLISHED_TUFF_STAIRS = VanillaMaterial.POLISHED_TUFF_STAIRS;
    Material POLISHED_TUFF_WALL = VanillaMaterial.POLISHED_TUFF_WALL;
    Material POPPY = VanillaMaterial.POPPY;
    Material POTATOES = VanillaMaterial.POTATOES;
    Material POTENT_SULFUR = VanillaMaterial.POTENT_SULFUR;
    Material POTTED_ACACIA_SAPLING = VanillaMaterial.POTTED_ACACIA_SAPLING;
    Material POTTED_ALLIUM = VanillaMaterial.POTTED_ALLIUM;
    Material POTTED_AZALEA_BUSH = VanillaMaterial.POTTED_AZALEA_BUSH;
    Material POTTED_AZURE_BLUET = VanillaMaterial.POTTED_AZURE_BLUET;
    Material POTTED_BAMBOO = VanillaMaterial.POTTED_BAMBOO;
    Material POTTED_BIRCH_SAPLING = VanillaMaterial.POTTED_BIRCH_SAPLING;
    Material POTTED_BLUE_ORCHID = VanillaMaterial.POTTED_BLUE_ORCHID;
    Material POTTED_BROWN_MUSHROOM = VanillaMaterial.POTTED_BROWN_MUSHROOM;
    Material POTTED_CACTUS = VanillaMaterial.POTTED_CACTUS;
    Material POTTED_CHERRY_SAPLING = VanillaMaterial.POTTED_CHERRY_SAPLING;
    Material POTTED_CLOSED_EYEBLOSSOM = VanillaMaterial.POTTED_CLOSED_EYEBLOSSOM;
    Material POTTED_CORNFLOWER = VanillaMaterial.POTTED_CORNFLOWER;
    Material POTTED_CRIMSON_FUNGUS = VanillaMaterial.POTTED_CRIMSON_FUNGUS;
    Material POTTED_CRIMSON_ROOTS = VanillaMaterial.POTTED_CRIMSON_ROOTS;
    Material POTTED_DANDELION = VanillaMaterial.POTTED_DANDELION;
    Material POTTED_DARK_OAK_SAPLING = VanillaMaterial.POTTED_DARK_OAK_SAPLING;
    Material POTTED_DEAD_BUSH = VanillaMaterial.POTTED_DEAD_BUSH;
    Material POTTED_FERN = VanillaMaterial.POTTED_FERN;
    Material POTTED_FLOWERING_AZALEA_BUSH = VanillaMaterial.POTTED_FLOWERING_AZALEA_BUSH;
    Material POTTED_GOLDEN_DANDELION = VanillaMaterial.POTTED_GOLDEN_DANDELION;
    Material POTTED_JUNGLE_SAPLING = VanillaMaterial.POTTED_JUNGLE_SAPLING;
    Material POTTED_LILY_OF_THE_VALLEY = VanillaMaterial.POTTED_LILY_OF_THE_VALLEY;
    Material POTTED_MANGROVE_PROPAGULE = VanillaMaterial.POTTED_MANGROVE_PROPAGULE;
    Material POTTED_OAK_SAPLING = VanillaMaterial.POTTED_OAK_SAPLING;
    Material POTTED_OPEN_EYEBLOSSOM = VanillaMaterial.POTTED_OPEN_EYEBLOSSOM;
    Material POTTED_ORANGE_TULIP = VanillaMaterial.POTTED_ORANGE_TULIP;
    Material POTTED_OXEYE_DAISY = VanillaMaterial.POTTED_OXEYE_DAISY;
    Material POTTED_PALE_OAK_SAPLING = VanillaMaterial.POTTED_PALE_OAK_SAPLING;
    Material POTTED_PINK_TULIP = VanillaMaterial.POTTED_PINK_TULIP;
    Material POTTED_POPPY = VanillaMaterial.POTTED_POPPY;
    Material POTTED_RED_MUSHROOM = VanillaMaterial.POTTED_RED_MUSHROOM;
    Material POTTED_RED_TULIP = VanillaMaterial.POTTED_RED_TULIP;
    Material POTTED_SPRUCE_SAPLING = VanillaMaterial.POTTED_SPRUCE_SAPLING;
    Material POTTED_TORCHFLOWER = VanillaMaterial.POTTED_TORCHFLOWER;
    Material POTTED_WARPED_FUNGUS = VanillaMaterial.POTTED_WARPED_FUNGUS;
    Material POTTED_WARPED_ROOTS = VanillaMaterial.POTTED_WARPED_ROOTS;
    Material POTTED_WHITE_TULIP = VanillaMaterial.POTTED_WHITE_TULIP;
    Material POTTED_WITHER_ROSE = VanillaMaterial.POTTED_WITHER_ROSE;
    Material POWDER_SNOW = VanillaMaterial.POWDER_SNOW;
    Material POWDER_SNOW_CAULDRON = VanillaMaterial.POWDER_SNOW_CAULDRON;
    Material POWERED_RAIL = VanillaMaterial.POWERED_RAIL;
    Material PRISMARINE = VanillaMaterial.PRISMARINE;
    Material PRISMARINE_BRICK_SLAB = VanillaMaterial.PRISMARINE_BRICK_SLAB;
    Material PRISMARINE_BRICK_STAIRS = VanillaMaterial.PRISMARINE_BRICK_STAIRS;
    Material PRISMARINE_BRICKS = VanillaMaterial.PRISMARINE_BRICKS;
    Material PRISMARINE_SLAB = VanillaMaterial.PRISMARINE_SLAB;
    Material PRISMARINE_STAIRS = VanillaMaterial.PRISMARINE_STAIRS;
    Material PRISMARINE_WALL = VanillaMaterial.PRISMARINE_WALL;
    Material PUMPKIN = VanillaMaterial.PUMPKIN;
    Material PUMPKIN_STEM = VanillaMaterial.PUMPKIN_STEM;
    Material PURPLE_BANNER = VanillaMaterial.PURPLE_BANNER;
    Material PURPLE_BED = VanillaMaterial.PURPLE_BED;
    Material PURPLE_CANDLE = VanillaMaterial.PURPLE_CANDLE;
    Material PURPLE_CANDLE_CAKE = VanillaMaterial.PURPLE_CANDLE_CAKE;
    Material PURPLE_CARPET = VanillaMaterial.PURPLE_CARPET;
    Material PURPLE_CONCRETE = VanillaMaterial.PURPLE_CONCRETE;
    Material PURPLE_CONCRETE_POWDER = VanillaMaterial.PURPLE_CONCRETE_POWDER;
    Material PURPLE_GLAZED_TERRACOTTA = VanillaMaterial.PURPLE_GLAZED_TERRACOTTA;
    Material PURPLE_SHULKER_BOX = VanillaMaterial.PURPLE_SHULKER_BOX;
    Material PURPLE_STAINED_GLASS = VanillaMaterial.PURPLE_STAINED_GLASS;
    Material PURPLE_STAINED_GLASS_PANE = VanillaMaterial.PURPLE_STAINED_GLASS_PANE;
    Material PURPLE_TERRACOTTA = VanillaMaterial.PURPLE_TERRACOTTA;
    Material PURPLE_WALL_BANNER = VanillaMaterial.PURPLE_WALL_BANNER;
    Material PURPLE_WOOL = VanillaMaterial.PURPLE_WOOL;
    Material PURPUR_BLOCK = VanillaMaterial.PURPUR_BLOCK;
    Material PURPUR_PILLAR = VanillaMaterial.PURPUR_PILLAR;
    Material PURPUR_SLAB = VanillaMaterial.PURPUR_SLAB;
    Material PURPUR_STAIRS = VanillaMaterial.PURPUR_STAIRS;
    Material QUARTZ_BLOCK = VanillaMaterial.QUARTZ_BLOCK;
    Material QUARTZ_BRICKS = VanillaMaterial.QUARTZ_BRICKS;
    Material QUARTZ_PILLAR = VanillaMaterial.QUARTZ_PILLAR;
    Material QUARTZ_SLAB = VanillaMaterial.QUARTZ_SLAB;
    Material QUARTZ_STAIRS = VanillaMaterial.QUARTZ_STAIRS;
    Material RAIL = VanillaMaterial.RAIL;
    Material RAW_COPPER_BLOCK = VanillaMaterial.RAW_COPPER_BLOCK;
    Material RAW_GOLD_BLOCK = VanillaMaterial.RAW_GOLD_BLOCK;
    Material RAW_IRON_BLOCK = VanillaMaterial.RAW_IRON_BLOCK;
    Material RED_BANNER = VanillaMaterial.RED_BANNER;
    Material RED_BED = VanillaMaterial.RED_BED;
    Material RED_CANDLE = VanillaMaterial.RED_CANDLE;
    Material RED_CANDLE_CAKE = VanillaMaterial.RED_CANDLE_CAKE;
    Material RED_CARPET = VanillaMaterial.RED_CARPET;
    Material RED_CONCRETE = VanillaMaterial.RED_CONCRETE;
    Material RED_CONCRETE_POWDER = VanillaMaterial.RED_CONCRETE_POWDER;
    Material RED_GLAZED_TERRACOTTA = VanillaMaterial.RED_GLAZED_TERRACOTTA;
    Material RED_MUSHROOM = VanillaMaterial.RED_MUSHROOM;
    Material RED_MUSHROOM_BLOCK = VanillaMaterial.RED_MUSHROOM_BLOCK;
    Material RED_NETHER_BRICK_SLAB = VanillaMaterial.RED_NETHER_BRICK_SLAB;
    Material RED_NETHER_BRICK_STAIRS = VanillaMaterial.RED_NETHER_BRICK_STAIRS;
    Material RED_NETHER_BRICK_WALL = VanillaMaterial.RED_NETHER_BRICK_WALL;
    Material RED_NETHER_BRICKS = VanillaMaterial.RED_NETHER_BRICKS;
    Material RED_SAND = VanillaMaterial.RED_SAND;
    Material RED_SANDSTONE = VanillaMaterial.RED_SANDSTONE;
    Material RED_SANDSTONE_SLAB = VanillaMaterial.RED_SANDSTONE_SLAB;
    Material RED_SANDSTONE_STAIRS = VanillaMaterial.RED_SANDSTONE_STAIRS;
    Material RED_SANDSTONE_WALL = VanillaMaterial.RED_SANDSTONE_WALL;
    Material RED_SHULKER_BOX = VanillaMaterial.RED_SHULKER_BOX;
    Material RED_STAINED_GLASS = VanillaMaterial.RED_STAINED_GLASS;
    Material RED_STAINED_GLASS_PANE = VanillaMaterial.RED_STAINED_GLASS_PANE;
    Material RED_TERRACOTTA = VanillaMaterial.RED_TERRACOTTA;
    Material RED_TULIP = VanillaMaterial.RED_TULIP;
    Material RED_WALL_BANNER = VanillaMaterial.RED_WALL_BANNER;
    Material RED_WOOL = VanillaMaterial.RED_WOOL;
    Material REDSTONE_BLOCK = VanillaMaterial.REDSTONE_BLOCK;
    Material REDSTONE_LAMP = VanillaMaterial.REDSTONE_LAMP;
    Material REDSTONE_ORE = VanillaMaterial.REDSTONE_ORE;
    Material REDSTONE_TORCH = VanillaMaterial.REDSTONE_TORCH;
    Material REDSTONE_WALL_TORCH = VanillaMaterial.REDSTONE_WALL_TORCH;
    Material REDSTONE_WIRE = VanillaMaterial.REDSTONE_WIRE;
    Material REINFORCED_DEEPSLATE = VanillaMaterial.REINFORCED_DEEPSLATE;
    Material REPEATER = VanillaMaterial.REPEATER;
    Material REPEATING_COMMAND_BLOCK = VanillaMaterial.REPEATING_COMMAND_BLOCK;
    Material RESIN_BLOCK = VanillaMaterial.RESIN_BLOCK;
    Material RESIN_BRICK_SLAB = VanillaMaterial.RESIN_BRICK_SLAB;
    Material RESIN_BRICK_STAIRS = VanillaMaterial.RESIN_BRICK_STAIRS;
    Material RESIN_BRICK_WALL = VanillaMaterial.RESIN_BRICK_WALL;
    Material RESIN_BRICKS = VanillaMaterial.RESIN_BRICKS;
    Material RESIN_CLUMP = VanillaMaterial.RESIN_CLUMP;
    Material RESPAWN_ANCHOR = VanillaMaterial.RESPAWN_ANCHOR;
    Material ROOTED_DIRT = VanillaMaterial.ROOTED_DIRT;
    Material ROSE_BUSH = VanillaMaterial.ROSE_BUSH;
    Material SAND = VanillaMaterial.SAND;
    Material SANDSTONE = VanillaMaterial.SANDSTONE;
    Material SANDSTONE_SLAB = VanillaMaterial.SANDSTONE_SLAB;
    Material SANDSTONE_STAIRS = VanillaMaterial.SANDSTONE_STAIRS;
    Material SANDSTONE_WALL = VanillaMaterial.SANDSTONE_WALL;
    Material SCAFFOLDING = VanillaMaterial.SCAFFOLDING;
    Material SCULK = VanillaMaterial.SCULK;
    Material SCULK_CATALYST = VanillaMaterial.SCULK_CATALYST;
    Material SCULK_SENSOR = VanillaMaterial.SCULK_SENSOR;
    Material SCULK_SHRIEKER = VanillaMaterial.SCULK_SHRIEKER;
    Material SCULK_VEIN = VanillaMaterial.SCULK_VEIN;
    Material SEA_LANTERN = VanillaMaterial.SEA_LANTERN;
    Material SEA_PICKLE = VanillaMaterial.SEA_PICKLE;
    Material SEAGRASS = VanillaMaterial.SEAGRASS;
    Material SHORT_DRY_GRASS = VanillaMaterial.SHORT_DRY_GRASS;
    Material SHORT_GRASS = VanillaMaterial.SHORT_GRASS;
    Material SHROOMLIGHT = VanillaMaterial.SHROOMLIGHT;
    Material SHULKER_BOX = VanillaMaterial.SHULKER_BOX;
    Material SKELETON_SKULL = VanillaMaterial.SKELETON_SKULL;
    Material SKELETON_WALL_SKULL = VanillaMaterial.SKELETON_WALL_SKULL;
    Material SLIME_BLOCK = VanillaMaterial.SLIME_BLOCK;
    Material SMALL_AMETHYST_BUD = VanillaMaterial.SMALL_AMETHYST_BUD;
    Material SMALL_DRIPLEAF = VanillaMaterial.SMALL_DRIPLEAF;
    Material SMITHING_TABLE = VanillaMaterial.SMITHING_TABLE;
    Material SMOKER = VanillaMaterial.SMOKER;
    Material SMOOTH_BASALT = VanillaMaterial.SMOOTH_BASALT;
    Material SMOOTH_QUARTZ = VanillaMaterial.SMOOTH_QUARTZ;
    Material SMOOTH_QUARTZ_SLAB = VanillaMaterial.SMOOTH_QUARTZ_SLAB;
    Material SMOOTH_QUARTZ_STAIRS = VanillaMaterial.SMOOTH_QUARTZ_STAIRS;
    Material SMOOTH_RED_SANDSTONE = VanillaMaterial.SMOOTH_RED_SANDSTONE;
    Material SMOOTH_RED_SANDSTONE_SLAB = VanillaMaterial.SMOOTH_RED_SANDSTONE_SLAB;
    Material SMOOTH_RED_SANDSTONE_STAIRS = VanillaMaterial.SMOOTH_RED_SANDSTONE_STAIRS;
    Material SMOOTH_SANDSTONE = VanillaMaterial.SMOOTH_SANDSTONE;
    Material SMOOTH_SANDSTONE_SLAB = VanillaMaterial.SMOOTH_SANDSTONE_SLAB;
    Material SMOOTH_SANDSTONE_STAIRS = VanillaMaterial.SMOOTH_SANDSTONE_STAIRS;
    Material SMOOTH_STONE = VanillaMaterial.SMOOTH_STONE;
    Material SMOOTH_STONE_SLAB = VanillaMaterial.SMOOTH_STONE_SLAB;
    Material SNIFFER_EGG = VanillaMaterial.SNIFFER_EGG;
    Material SNOW = VanillaMaterial.SNOW;
    Material SNOW_BLOCK = VanillaMaterial.SNOW_BLOCK;
    Material SOUL_CAMPFIRE = VanillaMaterial.SOUL_CAMPFIRE;
    Material SOUL_FIRE = VanillaMaterial.SOUL_FIRE;
    Material SOUL_LANTERN = VanillaMaterial.SOUL_LANTERN;
    Material SOUL_SAND = VanillaMaterial.SOUL_SAND;
    Material SOUL_SOIL = VanillaMaterial.SOUL_SOIL;
    Material SOUL_TORCH = VanillaMaterial.SOUL_TORCH;
    Material SOUL_WALL_TORCH = VanillaMaterial.SOUL_WALL_TORCH;
    Material SPAWNER = VanillaMaterial.SPAWNER;
    Material SPONGE = VanillaMaterial.SPONGE;
    Material SPORE_BLOSSOM = VanillaMaterial.SPORE_BLOSSOM;
    Material SPRUCE_BUTTON = VanillaMaterial.SPRUCE_BUTTON;
    Material SPRUCE_DOOR = VanillaMaterial.SPRUCE_DOOR;
    Material SPRUCE_FENCE = VanillaMaterial.SPRUCE_FENCE;
    Material SPRUCE_FENCE_GATE = VanillaMaterial.SPRUCE_FENCE_GATE;
    Material SPRUCE_HANGING_SIGN = VanillaMaterial.SPRUCE_HANGING_SIGN;
    Material SPRUCE_LEAVES = VanillaMaterial.SPRUCE_LEAVES;
    Material SPRUCE_LOG = VanillaMaterial.SPRUCE_LOG;
    Material SPRUCE_PLANKS = VanillaMaterial.SPRUCE_PLANKS;
    Material SPRUCE_PRESSURE_PLATE = VanillaMaterial.SPRUCE_PRESSURE_PLATE;
    Material SPRUCE_SAPLING = VanillaMaterial.SPRUCE_SAPLING;
    Material SPRUCE_SHELF = VanillaMaterial.SPRUCE_SHELF;
    Material SPRUCE_SIGN = VanillaMaterial.SPRUCE_SIGN;
    Material SPRUCE_SLAB = VanillaMaterial.SPRUCE_SLAB;
    Material SPRUCE_STAIRS = VanillaMaterial.SPRUCE_STAIRS;
    Material SPRUCE_TRAPDOOR = VanillaMaterial.SPRUCE_TRAPDOOR;
    Material SPRUCE_WALL_HANGING_SIGN = VanillaMaterial.SPRUCE_WALL_HANGING_SIGN;
    Material SPRUCE_WALL_SIGN = VanillaMaterial.SPRUCE_WALL_SIGN;
    Material SPRUCE_WOOD = VanillaMaterial.SPRUCE_WOOD;
    Material STICKY_PISTON = VanillaMaterial.STICKY_PISTON;
    Material STONE = VanillaMaterial.STONE;
    Material STONE_BRICK_SLAB = VanillaMaterial.STONE_BRICK_SLAB;
    Material STONE_BRICK_STAIRS = VanillaMaterial.STONE_BRICK_STAIRS;
    Material STONE_BRICK_WALL = VanillaMaterial.STONE_BRICK_WALL;
    Material STONE_BRICKS = VanillaMaterial.STONE_BRICKS;
    Material STONE_BUTTON = VanillaMaterial.STONE_BUTTON;
    Material STONE_PRESSURE_PLATE = VanillaMaterial.STONE_PRESSURE_PLATE;
    Material STONE_SLAB = VanillaMaterial.STONE_SLAB;
    Material STONE_STAIRS = VanillaMaterial.STONE_STAIRS;
    Material STONECUTTER = VanillaMaterial.STONECUTTER;
    Material STRIPPED_ACACIA_LOG = VanillaMaterial.STRIPPED_ACACIA_LOG;
    Material STRIPPED_ACACIA_WOOD = VanillaMaterial.STRIPPED_ACACIA_WOOD;
    Material STRIPPED_BAMBOO_BLOCK = VanillaMaterial.STRIPPED_BAMBOO_BLOCK;
    Material STRIPPED_BIRCH_LOG = VanillaMaterial.STRIPPED_BIRCH_LOG;
    Material STRIPPED_BIRCH_WOOD = VanillaMaterial.STRIPPED_BIRCH_WOOD;
    Material STRIPPED_CHERRY_LOG = VanillaMaterial.STRIPPED_CHERRY_LOG;
    Material STRIPPED_CHERRY_WOOD = VanillaMaterial.STRIPPED_CHERRY_WOOD;
    Material STRIPPED_CRIMSON_HYPHAE = VanillaMaterial.STRIPPED_CRIMSON_HYPHAE;
    Material STRIPPED_CRIMSON_STEM = VanillaMaterial.STRIPPED_CRIMSON_STEM;
    Material STRIPPED_DARK_OAK_LOG = VanillaMaterial.STRIPPED_DARK_OAK_LOG;
    Material STRIPPED_DARK_OAK_WOOD = VanillaMaterial.STRIPPED_DARK_OAK_WOOD;
    Material STRIPPED_JUNGLE_LOG = VanillaMaterial.STRIPPED_JUNGLE_LOG;
    Material STRIPPED_JUNGLE_WOOD = VanillaMaterial.STRIPPED_JUNGLE_WOOD;
    Material STRIPPED_MANGROVE_LOG = VanillaMaterial.STRIPPED_MANGROVE_LOG;
    Material STRIPPED_MANGROVE_WOOD = VanillaMaterial.STRIPPED_MANGROVE_WOOD;
    Material STRIPPED_OAK_LOG = VanillaMaterial.STRIPPED_OAK_LOG;
    Material STRIPPED_OAK_WOOD = VanillaMaterial.STRIPPED_OAK_WOOD;
    Material STRIPPED_PALE_OAK_LOG = VanillaMaterial.STRIPPED_PALE_OAK_LOG;
    Material STRIPPED_PALE_OAK_WOOD = VanillaMaterial.STRIPPED_PALE_OAK_WOOD;
    Material STRIPPED_SPRUCE_LOG = VanillaMaterial.STRIPPED_SPRUCE_LOG;
    Material STRIPPED_SPRUCE_WOOD = VanillaMaterial.STRIPPED_SPRUCE_WOOD;
    Material STRIPPED_WARPED_HYPHAE = VanillaMaterial.STRIPPED_WARPED_HYPHAE;
    Material STRIPPED_WARPED_STEM = VanillaMaterial.STRIPPED_WARPED_STEM;
    Material STRUCTURE_BLOCK = VanillaMaterial.STRUCTURE_BLOCK;
    Material STRUCTURE_VOID = VanillaMaterial.STRUCTURE_VOID;
    Material SUGAR_CANE = VanillaMaterial.SUGAR_CANE;
    Material SULFUR = VanillaMaterial.SULFUR;
    Material SULFUR_BRICK_SLAB = VanillaMaterial.SULFUR_BRICK_SLAB;
    Material SULFUR_BRICK_STAIRS = VanillaMaterial.SULFUR_BRICK_STAIRS;
    Material SULFUR_BRICK_WALL = VanillaMaterial.SULFUR_BRICK_WALL;
    Material SULFUR_BRICKS = VanillaMaterial.SULFUR_BRICKS;
    Material SULFUR_SLAB = VanillaMaterial.SULFUR_SLAB;
    Material SULFUR_SPIKE = VanillaMaterial.SULFUR_SPIKE;
    Material SULFUR_STAIRS = VanillaMaterial.SULFUR_STAIRS;
    Material SULFUR_WALL = VanillaMaterial.SULFUR_WALL;
    Material SUNFLOWER = VanillaMaterial.SUNFLOWER;
    Material SUSPICIOUS_GRAVEL = VanillaMaterial.SUSPICIOUS_GRAVEL;
    Material SUSPICIOUS_SAND = VanillaMaterial.SUSPICIOUS_SAND;
    Material SWEET_BERRY_BUSH = VanillaMaterial.SWEET_BERRY_BUSH;
    Material TALL_DRY_GRASS = VanillaMaterial.TALL_DRY_GRASS;
    Material TALL_GRASS = VanillaMaterial.TALL_GRASS;
    Material TALL_SEAGRASS = VanillaMaterial.TALL_SEAGRASS;
    Material TARGET = VanillaMaterial.TARGET;
    Material TERRACOTTA = VanillaMaterial.TERRACOTTA;
    Material TEST_BLOCK = VanillaMaterial.TEST_BLOCK;
    Material TEST_INSTANCE_BLOCK = VanillaMaterial.TEST_INSTANCE_BLOCK;
    Material TINTED_GLASS = VanillaMaterial.TINTED_GLASS;
    Material TNT = VanillaMaterial.TNT;
    Material TORCH = VanillaMaterial.TORCH;
    Material TORCHFLOWER = VanillaMaterial.TORCHFLOWER;
    Material TORCHFLOWER_CROP = VanillaMaterial.TORCHFLOWER_CROP;
    Material TRAPPED_CHEST = VanillaMaterial.TRAPPED_CHEST;
    Material TRIAL_SPAWNER = VanillaMaterial.TRIAL_SPAWNER;
    Material TRIPWIRE = VanillaMaterial.TRIPWIRE;
    Material TRIPWIRE_HOOK = VanillaMaterial.TRIPWIRE_HOOK;
    Material TUBE_CORAL = VanillaMaterial.TUBE_CORAL;
    Material TUBE_CORAL_BLOCK = VanillaMaterial.TUBE_CORAL_BLOCK;
    Material TUBE_CORAL_FAN = VanillaMaterial.TUBE_CORAL_FAN;
    Material TUBE_CORAL_WALL_FAN = VanillaMaterial.TUBE_CORAL_WALL_FAN;
    Material TUFF = VanillaMaterial.TUFF;
    Material TUFF_BRICK_SLAB = VanillaMaterial.TUFF_BRICK_SLAB;
    Material TUFF_BRICK_STAIRS = VanillaMaterial.TUFF_BRICK_STAIRS;
    Material TUFF_BRICK_WALL = VanillaMaterial.TUFF_BRICK_WALL;
    Material TUFF_BRICKS = VanillaMaterial.TUFF_BRICKS;
    Material TUFF_SLAB = VanillaMaterial.TUFF_SLAB;
    Material TUFF_STAIRS = VanillaMaterial.TUFF_STAIRS;
    Material TUFF_WALL = VanillaMaterial.TUFF_WALL;
    Material TURTLE_EGG = VanillaMaterial.TURTLE_EGG;
    Material TWISTING_VINES = VanillaMaterial.TWISTING_VINES;
    Material TWISTING_VINES_PLANT = VanillaMaterial.TWISTING_VINES_PLANT;
    Material VAULT = VanillaMaterial.VAULT;
    Material VERDANT_FROGLIGHT = VanillaMaterial.VERDANT_FROGLIGHT;
    Material VINE = VanillaMaterial.VINE;
    Material VOID_AIR = VanillaMaterial.VOID_AIR;
    Material WALL_TORCH = VanillaMaterial.WALL_TORCH;
    Material WARPED_BUTTON = VanillaMaterial.WARPED_BUTTON;
    Material WARPED_DOOR = VanillaMaterial.WARPED_DOOR;
    Material WARPED_FENCE = VanillaMaterial.WARPED_FENCE;
    Material WARPED_FENCE_GATE = VanillaMaterial.WARPED_FENCE_GATE;
    Material WARPED_FUNGUS = VanillaMaterial.WARPED_FUNGUS;
    Material WARPED_HANGING_SIGN = VanillaMaterial.WARPED_HANGING_SIGN;
    Material WARPED_HYPHAE = VanillaMaterial.WARPED_HYPHAE;
    Material WARPED_NYLIUM = VanillaMaterial.WARPED_NYLIUM;
    Material WARPED_PLANKS = VanillaMaterial.WARPED_PLANKS;
    Material WARPED_PRESSURE_PLATE = VanillaMaterial.WARPED_PRESSURE_PLATE;
    Material WARPED_ROOTS = VanillaMaterial.WARPED_ROOTS;
    Material WARPED_SHELF = VanillaMaterial.WARPED_SHELF;
    Material WARPED_SIGN = VanillaMaterial.WARPED_SIGN;
    Material WARPED_SLAB = VanillaMaterial.WARPED_SLAB;
    Material WARPED_STAIRS = VanillaMaterial.WARPED_STAIRS;
    Material WARPED_STEM = VanillaMaterial.WARPED_STEM;
    Material WARPED_TRAPDOOR = VanillaMaterial.WARPED_TRAPDOOR;
    Material WARPED_WALL_HANGING_SIGN = VanillaMaterial.WARPED_WALL_HANGING_SIGN;
    Material WARPED_WALL_SIGN = VanillaMaterial.WARPED_WALL_SIGN;
    Material WARPED_WART_BLOCK = VanillaMaterial.WARPED_WART_BLOCK;
    Material WATER = VanillaMaterial.WATER;
    Material WATER_CAULDRON = VanillaMaterial.WATER_CAULDRON;
    Material WAXED_CHISELED_COPPER = VanillaMaterial.WAXED_CHISELED_COPPER;
    Material WAXED_COPPER_BARS = VanillaMaterial.WAXED_COPPER_BARS;
    Material WAXED_COPPER_BLOCK = VanillaMaterial.WAXED_COPPER_BLOCK;
    Material WAXED_COPPER_BULB = VanillaMaterial.WAXED_COPPER_BULB;
    Material WAXED_COPPER_CHAIN = VanillaMaterial.WAXED_COPPER_CHAIN;
    Material WAXED_COPPER_CHEST = VanillaMaterial.WAXED_COPPER_CHEST;
    Material WAXED_COPPER_DOOR = VanillaMaterial.WAXED_COPPER_DOOR;
    Material WAXED_COPPER_GOLEM_STATUE = VanillaMaterial.WAXED_COPPER_GOLEM_STATUE;
    Material WAXED_COPPER_GRATE = VanillaMaterial.WAXED_COPPER_GRATE;
    Material WAXED_COPPER_LANTERN = VanillaMaterial.WAXED_COPPER_LANTERN;
    Material WAXED_COPPER_TRAPDOOR = VanillaMaterial.WAXED_COPPER_TRAPDOOR;
    Material WAXED_CUT_COPPER = VanillaMaterial.WAXED_CUT_COPPER;
    Material WAXED_CUT_COPPER_SLAB = VanillaMaterial.WAXED_CUT_COPPER_SLAB;
    Material WAXED_CUT_COPPER_STAIRS = VanillaMaterial.WAXED_CUT_COPPER_STAIRS;
    Material WAXED_EXPOSED_CHISELED_COPPER = VanillaMaterial.WAXED_EXPOSED_CHISELED_COPPER;
    Material WAXED_EXPOSED_COPPER = VanillaMaterial.WAXED_EXPOSED_COPPER;
    Material WAXED_EXPOSED_COPPER_BARS = VanillaMaterial.WAXED_EXPOSED_COPPER_BARS;
    Material WAXED_EXPOSED_COPPER_BULB = VanillaMaterial.WAXED_EXPOSED_COPPER_BULB;
    Material WAXED_EXPOSED_COPPER_CHAIN = VanillaMaterial.WAXED_EXPOSED_COPPER_CHAIN;
    Material WAXED_EXPOSED_COPPER_CHEST = VanillaMaterial.WAXED_EXPOSED_COPPER_CHEST;
    Material WAXED_EXPOSED_COPPER_DOOR = VanillaMaterial.WAXED_EXPOSED_COPPER_DOOR;
    Material WAXED_EXPOSED_COPPER_GOLEM_STATUE = VanillaMaterial.WAXED_EXPOSED_COPPER_GOLEM_STATUE;
    Material WAXED_EXPOSED_COPPER_GRATE = VanillaMaterial.WAXED_EXPOSED_COPPER_GRATE;
    Material WAXED_EXPOSED_COPPER_LANTERN = VanillaMaterial.WAXED_EXPOSED_COPPER_LANTERN;
    Material WAXED_EXPOSED_COPPER_TRAPDOOR = VanillaMaterial.WAXED_EXPOSED_COPPER_TRAPDOOR;
    Material WAXED_EXPOSED_CUT_COPPER = VanillaMaterial.WAXED_EXPOSED_CUT_COPPER;
    Material WAXED_EXPOSED_CUT_COPPER_SLAB = VanillaMaterial.WAXED_EXPOSED_CUT_COPPER_SLAB;
    Material WAXED_EXPOSED_CUT_COPPER_STAIRS = VanillaMaterial.WAXED_EXPOSED_CUT_COPPER_STAIRS;
    Material WAXED_EXPOSED_LIGHTNING_ROD = VanillaMaterial.WAXED_EXPOSED_LIGHTNING_ROD;
    Material WAXED_LIGHTNING_ROD = VanillaMaterial.WAXED_LIGHTNING_ROD;
    Material WAXED_OXIDIZED_CHISELED_COPPER = VanillaMaterial.WAXED_OXIDIZED_CHISELED_COPPER;
    Material WAXED_OXIDIZED_COPPER = VanillaMaterial.WAXED_OXIDIZED_COPPER;
    Material WAXED_OXIDIZED_COPPER_BARS = VanillaMaterial.WAXED_OXIDIZED_COPPER_BARS;
    Material WAXED_OXIDIZED_COPPER_BULB = VanillaMaterial.WAXED_OXIDIZED_COPPER_BULB;
    Material WAXED_OXIDIZED_COPPER_CHAIN = VanillaMaterial.WAXED_OXIDIZED_COPPER_CHAIN;
    Material WAXED_OXIDIZED_COPPER_CHEST = VanillaMaterial.WAXED_OXIDIZED_COPPER_CHEST;
    Material WAXED_OXIDIZED_COPPER_DOOR = VanillaMaterial.WAXED_OXIDIZED_COPPER_DOOR;
    Material WAXED_OXIDIZED_COPPER_GOLEM_STATUE = VanillaMaterial.WAXED_OXIDIZED_COPPER_GOLEM_STATUE;
    Material WAXED_OXIDIZED_COPPER_GRATE = VanillaMaterial.WAXED_OXIDIZED_COPPER_GRATE;
    Material WAXED_OXIDIZED_COPPER_LANTERN = VanillaMaterial.WAXED_OXIDIZED_COPPER_LANTERN;
    Material WAXED_OXIDIZED_COPPER_TRAPDOOR = VanillaMaterial.WAXED_OXIDIZED_COPPER_TRAPDOOR;
    Material WAXED_OXIDIZED_CUT_COPPER = VanillaMaterial.WAXED_OXIDIZED_CUT_COPPER;
    Material WAXED_OXIDIZED_CUT_COPPER_SLAB = VanillaMaterial.WAXED_OXIDIZED_CUT_COPPER_SLAB;
    Material WAXED_OXIDIZED_CUT_COPPER_STAIRS = VanillaMaterial.WAXED_OXIDIZED_CUT_COPPER_STAIRS;
    Material WAXED_OXIDIZED_LIGHTNING_ROD = VanillaMaterial.WAXED_OXIDIZED_LIGHTNING_ROD;
    Material WAXED_WEATHERED_CHISELED_COPPER = VanillaMaterial.WAXED_WEATHERED_CHISELED_COPPER;
    Material WAXED_WEATHERED_COPPER = VanillaMaterial.WAXED_WEATHERED_COPPER;
    Material WAXED_WEATHERED_COPPER_BARS = VanillaMaterial.WAXED_WEATHERED_COPPER_BARS;
    Material WAXED_WEATHERED_COPPER_BULB = VanillaMaterial.WAXED_WEATHERED_COPPER_BULB;
    Material WAXED_WEATHERED_COPPER_CHAIN = VanillaMaterial.WAXED_WEATHERED_COPPER_CHAIN;
    Material WAXED_WEATHERED_COPPER_CHEST = VanillaMaterial.WAXED_WEATHERED_COPPER_CHEST;
    Material WAXED_WEATHERED_COPPER_DOOR = VanillaMaterial.WAXED_WEATHERED_COPPER_DOOR;
    Material WAXED_WEATHERED_COPPER_GOLEM_STATUE = VanillaMaterial.WAXED_WEATHERED_COPPER_GOLEM_STATUE;
    Material WAXED_WEATHERED_COPPER_GRATE = VanillaMaterial.WAXED_WEATHERED_COPPER_GRATE;
    Material WAXED_WEATHERED_COPPER_LANTERN = VanillaMaterial.WAXED_WEATHERED_COPPER_LANTERN;
    Material WAXED_WEATHERED_COPPER_TRAPDOOR = VanillaMaterial.WAXED_WEATHERED_COPPER_TRAPDOOR;
    Material WAXED_WEATHERED_CUT_COPPER = VanillaMaterial.WAXED_WEATHERED_CUT_COPPER;
    Material WAXED_WEATHERED_CUT_COPPER_SLAB = VanillaMaterial.WAXED_WEATHERED_CUT_COPPER_SLAB;
    Material WAXED_WEATHERED_CUT_COPPER_STAIRS = VanillaMaterial.WAXED_WEATHERED_CUT_COPPER_STAIRS;
    Material WAXED_WEATHERED_LIGHTNING_ROD = VanillaMaterial.WAXED_WEATHERED_LIGHTNING_ROD;
    Material WEATHERED_CHISELED_COPPER = VanillaMaterial.WEATHERED_CHISELED_COPPER;
    Material WEATHERED_COPPER = VanillaMaterial.WEATHERED_COPPER;
    Material WEATHERED_COPPER_BARS = VanillaMaterial.WEATHERED_COPPER_BARS;
    Material WEATHERED_COPPER_BULB = VanillaMaterial.WEATHERED_COPPER_BULB;
    Material WEATHERED_COPPER_CHAIN = VanillaMaterial.WEATHERED_COPPER_CHAIN;
    Material WEATHERED_COPPER_CHEST = VanillaMaterial.WEATHERED_COPPER_CHEST;
    Material WEATHERED_COPPER_DOOR = VanillaMaterial.WEATHERED_COPPER_DOOR;
    Material WEATHERED_COPPER_GOLEM_STATUE = VanillaMaterial.WEATHERED_COPPER_GOLEM_STATUE;
    Material WEATHERED_COPPER_GRATE = VanillaMaterial.WEATHERED_COPPER_GRATE;
    Material WEATHERED_COPPER_LANTERN = VanillaMaterial.WEATHERED_COPPER_LANTERN;
    Material WEATHERED_COPPER_TRAPDOOR = VanillaMaterial.WEATHERED_COPPER_TRAPDOOR;
    Material WEATHERED_CUT_COPPER = VanillaMaterial.WEATHERED_CUT_COPPER;
    Material WEATHERED_CUT_COPPER_SLAB = VanillaMaterial.WEATHERED_CUT_COPPER_SLAB;
    Material WEATHERED_CUT_COPPER_STAIRS = VanillaMaterial.WEATHERED_CUT_COPPER_STAIRS;
    Material WEATHERED_LIGHTNING_ROD = VanillaMaterial.WEATHERED_LIGHTNING_ROD;
    Material WEEPING_VINES = VanillaMaterial.WEEPING_VINES;
    Material WEEPING_VINES_PLANT = VanillaMaterial.WEEPING_VINES_PLANT;
    Material WET_SPONGE = VanillaMaterial.WET_SPONGE;
    Material WHEAT = VanillaMaterial.WHEAT;
    Material WHITE_BANNER = VanillaMaterial.WHITE_BANNER;
    Material WHITE_BED = VanillaMaterial.WHITE_BED;
    Material WHITE_CANDLE = VanillaMaterial.WHITE_CANDLE;
    Material WHITE_CANDLE_CAKE = VanillaMaterial.WHITE_CANDLE_CAKE;
    Material WHITE_CARPET = VanillaMaterial.WHITE_CARPET;
    Material WHITE_CONCRETE = VanillaMaterial.WHITE_CONCRETE;
    Material WHITE_CONCRETE_POWDER = VanillaMaterial.WHITE_CONCRETE_POWDER;
    Material WHITE_GLAZED_TERRACOTTA = VanillaMaterial.WHITE_GLAZED_TERRACOTTA;
    Material WHITE_SHULKER_BOX = VanillaMaterial.WHITE_SHULKER_BOX;
    Material WHITE_STAINED_GLASS = VanillaMaterial.WHITE_STAINED_GLASS;
    Material WHITE_STAINED_GLASS_PANE = VanillaMaterial.WHITE_STAINED_GLASS_PANE;
    Material WHITE_TERRACOTTA = VanillaMaterial.WHITE_TERRACOTTA;
    Material WHITE_TULIP = VanillaMaterial.WHITE_TULIP;
    Material WHITE_WALL_BANNER = VanillaMaterial.WHITE_WALL_BANNER;
    Material WHITE_WOOL = VanillaMaterial.WHITE_WOOL;
    Material WILDFLOWERS = VanillaMaterial.WILDFLOWERS;
    Material WITHER_ROSE = VanillaMaterial.WITHER_ROSE;
    Material WITHER_SKELETON_SKULL = VanillaMaterial.WITHER_SKELETON_SKULL;
    Material WITHER_SKELETON_WALL_SKULL = VanillaMaterial.WITHER_SKELETON_WALL_SKULL;
    Material YELLOW_BANNER = VanillaMaterial.YELLOW_BANNER;
    Material YELLOW_BED = VanillaMaterial.YELLOW_BED;
    Material YELLOW_CANDLE = VanillaMaterial.YELLOW_CANDLE;
    Material YELLOW_CANDLE_CAKE = VanillaMaterial.YELLOW_CANDLE_CAKE;
    Material YELLOW_CARPET = VanillaMaterial.YELLOW_CARPET;
    Material YELLOW_CONCRETE = VanillaMaterial.YELLOW_CONCRETE;
    Material YELLOW_CONCRETE_POWDER = VanillaMaterial.YELLOW_CONCRETE_POWDER;
    Material YELLOW_GLAZED_TERRACOTTA = VanillaMaterial.YELLOW_GLAZED_TERRACOTTA;
    Material YELLOW_SHULKER_BOX = VanillaMaterial.YELLOW_SHULKER_BOX;
    Material YELLOW_STAINED_GLASS = VanillaMaterial.YELLOW_STAINED_GLASS;
    Material YELLOW_STAINED_GLASS_PANE = VanillaMaterial.YELLOW_STAINED_GLASS_PANE;
    Material YELLOW_TERRACOTTA = VanillaMaterial.YELLOW_TERRACOTTA;
    Material YELLOW_WALL_BANNER = VanillaMaterial.YELLOW_WALL_BANNER;
    Material YELLOW_WOOL = VanillaMaterial.YELLOW_WOOL;
    Material ZOMBIE_HEAD = VanillaMaterial.ZOMBIE_HEAD;
    Material ZOMBIE_WALL_HEAD = VanillaMaterial.ZOMBIE_WALL_HEAD;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_AIR = VanillaMaterial.LEGACY_AIR;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STONE = VanillaMaterial.LEGACY_STONE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GRASS = VanillaMaterial.LEGACY_GRASS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DIRT = VanillaMaterial.LEGACY_DIRT;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_COBBLESTONE = VanillaMaterial.LEGACY_COBBLESTONE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WOOD = VanillaMaterial.LEGACY_WOOD;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SAPLING = VanillaMaterial.LEGACY_SAPLING;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BEDROCK = VanillaMaterial.LEGACY_BEDROCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WATER = VanillaMaterial.LEGACY_WATER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STATIONARY_WATER = VanillaMaterial.LEGACY_STATIONARY_WATER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LAVA = VanillaMaterial.LEGACY_LAVA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STATIONARY_LAVA = VanillaMaterial.LEGACY_STATIONARY_LAVA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SAND = VanillaMaterial.LEGACY_SAND;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GRAVEL = VanillaMaterial.LEGACY_GRAVEL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GOLD_ORE = VanillaMaterial.LEGACY_GOLD_ORE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_IRON_ORE = VanillaMaterial.LEGACY_IRON_ORE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_COAL_ORE = VanillaMaterial.LEGACY_COAL_ORE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LOG = VanillaMaterial.LEGACY_LOG;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LEAVES = VanillaMaterial.LEGACY_LEAVES;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SPONGE = VanillaMaterial.LEGACY_SPONGE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GLASS = VanillaMaterial.LEGACY_GLASS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LAPIS_ORE = VanillaMaterial.LEGACY_LAPIS_ORE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LAPIS_BLOCK = VanillaMaterial.LEGACY_LAPIS_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DISPENSER = VanillaMaterial.LEGACY_DISPENSER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SANDSTONE = VanillaMaterial.LEGACY_SANDSTONE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_NOTE_BLOCK = VanillaMaterial.LEGACY_NOTE_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BED_BLOCK = VanillaMaterial.LEGACY_BED_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_POWERED_RAIL = VanillaMaterial.LEGACY_POWERED_RAIL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DETECTOR_RAIL = VanillaMaterial.LEGACY_DETECTOR_RAIL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PISTON_STICKY_BASE = VanillaMaterial.LEGACY_PISTON_STICKY_BASE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WEB = VanillaMaterial.LEGACY_WEB;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LONG_GRASS = VanillaMaterial.LEGACY_LONG_GRASS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DEAD_BUSH = VanillaMaterial.LEGACY_DEAD_BUSH;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PISTON_BASE = VanillaMaterial.LEGACY_PISTON_BASE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PISTON_EXTENSION = VanillaMaterial.LEGACY_PISTON_EXTENSION;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WOOL = VanillaMaterial.LEGACY_WOOL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PISTON_MOVING_PIECE = VanillaMaterial.LEGACY_PISTON_MOVING_PIECE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_YELLOW_FLOWER = VanillaMaterial.LEGACY_YELLOW_FLOWER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RED_ROSE = VanillaMaterial.LEGACY_RED_ROSE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BROWN_MUSHROOM = VanillaMaterial.LEGACY_BROWN_MUSHROOM;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RED_MUSHROOM = VanillaMaterial.LEGACY_RED_MUSHROOM;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GOLD_BLOCK = VanillaMaterial.LEGACY_GOLD_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_IRON_BLOCK = VanillaMaterial.LEGACY_IRON_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DOUBLE_STEP = VanillaMaterial.LEGACY_DOUBLE_STEP;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STEP = VanillaMaterial.LEGACY_STEP;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BRICK = VanillaMaterial.LEGACY_BRICK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_TNT = VanillaMaterial.LEGACY_TNT;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BOOKSHELF = VanillaMaterial.LEGACY_BOOKSHELF;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_MOSSY_COBBLESTONE = VanillaMaterial.LEGACY_MOSSY_COBBLESTONE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_OBSIDIAN = VanillaMaterial.LEGACY_OBSIDIAN;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_TORCH = VanillaMaterial.LEGACY_TORCH;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_FIRE = VanillaMaterial.LEGACY_FIRE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_MOB_SPAWNER = VanillaMaterial.LEGACY_MOB_SPAWNER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WOOD_STAIRS = VanillaMaterial.LEGACY_WOOD_STAIRS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CHEST = VanillaMaterial.LEGACY_CHEST;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_REDSTONE_WIRE = VanillaMaterial.LEGACY_REDSTONE_WIRE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DIAMOND_ORE = VanillaMaterial.LEGACY_DIAMOND_ORE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DIAMOND_BLOCK = VanillaMaterial.LEGACY_DIAMOND_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WORKBENCH = VanillaMaterial.LEGACY_WORKBENCH;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CROPS = VanillaMaterial.LEGACY_CROPS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SOIL = VanillaMaterial.LEGACY_SOIL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_FURNACE = VanillaMaterial.LEGACY_FURNACE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BURNING_FURNACE = VanillaMaterial.LEGACY_BURNING_FURNACE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SIGN_POST = VanillaMaterial.LEGACY_SIGN_POST;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WOODEN_DOOR = VanillaMaterial.LEGACY_WOODEN_DOOR;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LADDER = VanillaMaterial.LEGACY_LADDER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RAILS = VanillaMaterial.LEGACY_RAILS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_COBBLESTONE_STAIRS = VanillaMaterial.LEGACY_COBBLESTONE_STAIRS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WALL_SIGN = VanillaMaterial.LEGACY_WALL_SIGN;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LEVER = VanillaMaterial.LEGACY_LEVER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STONE_PLATE = VanillaMaterial.LEGACY_STONE_PLATE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_IRON_DOOR_BLOCK = VanillaMaterial.LEGACY_IRON_DOOR_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WOOD_PLATE = VanillaMaterial.LEGACY_WOOD_PLATE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_REDSTONE_ORE = VanillaMaterial.LEGACY_REDSTONE_ORE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GLOWING_REDSTONE_ORE = VanillaMaterial.LEGACY_GLOWING_REDSTONE_ORE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_REDSTONE_TORCH_OFF = VanillaMaterial.LEGACY_REDSTONE_TORCH_OFF;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_REDSTONE_TORCH_ON = VanillaMaterial.LEGACY_REDSTONE_TORCH_ON;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STONE_BUTTON = VanillaMaterial.LEGACY_STONE_BUTTON;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SNOW = VanillaMaterial.LEGACY_SNOW;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ICE = VanillaMaterial.LEGACY_ICE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SNOW_BLOCK = VanillaMaterial.LEGACY_SNOW_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CACTUS = VanillaMaterial.LEGACY_CACTUS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CLAY = VanillaMaterial.LEGACY_CLAY;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SUGAR_CANE_BLOCK = VanillaMaterial.LEGACY_SUGAR_CANE_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_JUKEBOX = VanillaMaterial.LEGACY_JUKEBOX;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_FENCE = VanillaMaterial.LEGACY_FENCE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PUMPKIN = VanillaMaterial.LEGACY_PUMPKIN;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_NETHERRACK = VanillaMaterial.LEGACY_NETHERRACK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SOUL_SAND = VanillaMaterial.LEGACY_SOUL_SAND;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GLOWSTONE = VanillaMaterial.LEGACY_GLOWSTONE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PORTAL = VanillaMaterial.LEGACY_PORTAL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_JACK_O_LANTERN = VanillaMaterial.LEGACY_JACK_O_LANTERN;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CAKE_BLOCK = VanillaMaterial.LEGACY_CAKE_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DIODE_BLOCK_OFF = VanillaMaterial.LEGACY_DIODE_BLOCK_OFF;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DIODE_BLOCK_ON = VanillaMaterial.LEGACY_DIODE_BLOCK_ON;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STAINED_GLASS = VanillaMaterial.LEGACY_STAINED_GLASS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_TRAP_DOOR = VanillaMaterial.LEGACY_TRAP_DOOR;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_MONSTER_EGGS = VanillaMaterial.LEGACY_MONSTER_EGGS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SMOOTH_BRICK = VanillaMaterial.LEGACY_SMOOTH_BRICK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_HUGE_MUSHROOM_1 = VanillaMaterial.LEGACY_HUGE_MUSHROOM_1;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_HUGE_MUSHROOM_2 = VanillaMaterial.LEGACY_HUGE_MUSHROOM_2;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_IRON_FENCE = VanillaMaterial.LEGACY_IRON_FENCE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_THIN_GLASS = VanillaMaterial.LEGACY_THIN_GLASS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_MELON_BLOCK = VanillaMaterial.LEGACY_MELON_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PUMPKIN_STEM = VanillaMaterial.LEGACY_PUMPKIN_STEM;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_MELON_STEM = VanillaMaterial.LEGACY_MELON_STEM;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_VINE = VanillaMaterial.LEGACY_VINE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_FENCE_GATE = VanillaMaterial.LEGACY_FENCE_GATE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BRICK_STAIRS = VanillaMaterial.LEGACY_BRICK_STAIRS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SMOOTH_STAIRS = VanillaMaterial.LEGACY_SMOOTH_STAIRS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_MYCEL = VanillaMaterial.LEGACY_MYCEL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WATER_LILY = VanillaMaterial.LEGACY_WATER_LILY;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_NETHER_BRICK = VanillaMaterial.LEGACY_NETHER_BRICK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_NETHER_FENCE = VanillaMaterial.LEGACY_NETHER_FENCE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_NETHER_BRICK_STAIRS = VanillaMaterial.LEGACY_NETHER_BRICK_STAIRS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_NETHER_WARTS = VanillaMaterial.LEGACY_NETHER_WARTS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ENCHANTMENT_TABLE = VanillaMaterial.LEGACY_ENCHANTMENT_TABLE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BREWING_STAND = VanillaMaterial.LEGACY_BREWING_STAND;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CAULDRON = VanillaMaterial.LEGACY_CAULDRON;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ENDER_PORTAL = VanillaMaterial.LEGACY_ENDER_PORTAL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ENDER_PORTAL_FRAME = VanillaMaterial.LEGACY_ENDER_PORTAL_FRAME;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ENDER_STONE = VanillaMaterial.LEGACY_ENDER_STONE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DRAGON_EGG = VanillaMaterial.LEGACY_DRAGON_EGG;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_REDSTONE_LAMP_OFF = VanillaMaterial.LEGACY_REDSTONE_LAMP_OFF;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_REDSTONE_LAMP_ON = VanillaMaterial.LEGACY_REDSTONE_LAMP_ON;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WOOD_DOUBLE_STEP = VanillaMaterial.LEGACY_WOOD_DOUBLE_STEP;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WOOD_STEP = VanillaMaterial.LEGACY_WOOD_STEP;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_COCOA = VanillaMaterial.LEGACY_COCOA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SANDSTONE_STAIRS = VanillaMaterial.LEGACY_SANDSTONE_STAIRS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_EMERALD_ORE = VanillaMaterial.LEGACY_EMERALD_ORE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ENDER_CHEST = VanillaMaterial.LEGACY_ENDER_CHEST;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_TRIPWIRE_HOOK = VanillaMaterial.LEGACY_TRIPWIRE_HOOK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_TRIPWIRE = VanillaMaterial.LEGACY_TRIPWIRE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_EMERALD_BLOCK = VanillaMaterial.LEGACY_EMERALD_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SPRUCE_WOOD_STAIRS = VanillaMaterial.LEGACY_SPRUCE_WOOD_STAIRS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BIRCH_WOOD_STAIRS = VanillaMaterial.LEGACY_BIRCH_WOOD_STAIRS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_JUNGLE_WOOD_STAIRS = VanillaMaterial.LEGACY_JUNGLE_WOOD_STAIRS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_COMMAND = VanillaMaterial.LEGACY_COMMAND;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BEACON = VanillaMaterial.LEGACY_BEACON;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_COBBLE_WALL = VanillaMaterial.LEGACY_COBBLE_WALL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_FLOWER_POT = VanillaMaterial.LEGACY_FLOWER_POT;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CARROT = VanillaMaterial.LEGACY_CARROT;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_POTATO = VanillaMaterial.LEGACY_POTATO;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WOOD_BUTTON = VanillaMaterial.LEGACY_WOOD_BUTTON;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SKULL = VanillaMaterial.LEGACY_SKULL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ANVIL = VanillaMaterial.LEGACY_ANVIL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_TRAPPED_CHEST = VanillaMaterial.LEGACY_TRAPPED_CHEST;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GOLD_PLATE = VanillaMaterial.LEGACY_GOLD_PLATE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_IRON_PLATE = VanillaMaterial.LEGACY_IRON_PLATE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_REDSTONE_COMPARATOR_OFF = VanillaMaterial.LEGACY_REDSTONE_COMPARATOR_OFF;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_REDSTONE_COMPARATOR_ON = VanillaMaterial.LEGACY_REDSTONE_COMPARATOR_ON;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DAYLIGHT_DETECTOR = VanillaMaterial.LEGACY_DAYLIGHT_DETECTOR;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_REDSTONE_BLOCK = VanillaMaterial.LEGACY_REDSTONE_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_QUARTZ_ORE = VanillaMaterial.LEGACY_QUARTZ_ORE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_HOPPER = VanillaMaterial.LEGACY_HOPPER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_QUARTZ_BLOCK = VanillaMaterial.LEGACY_QUARTZ_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_QUARTZ_STAIRS = VanillaMaterial.LEGACY_QUARTZ_STAIRS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ACTIVATOR_RAIL = VanillaMaterial.LEGACY_ACTIVATOR_RAIL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DROPPER = VanillaMaterial.LEGACY_DROPPER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STAINED_CLAY = VanillaMaterial.LEGACY_STAINED_CLAY;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STAINED_GLASS_PANE = VanillaMaterial.LEGACY_STAINED_GLASS_PANE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LEAVES_2 = VanillaMaterial.LEGACY_LEAVES_2;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LOG_2 = VanillaMaterial.LEGACY_LOG_2;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ACACIA_STAIRS = VanillaMaterial.LEGACY_ACACIA_STAIRS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DARK_OAK_STAIRS = VanillaMaterial.LEGACY_DARK_OAK_STAIRS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SLIME_BLOCK = VanillaMaterial.LEGACY_SLIME_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BARRIER = VanillaMaterial.LEGACY_BARRIER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_IRON_TRAPDOOR = VanillaMaterial.LEGACY_IRON_TRAPDOOR;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PRISMARINE = VanillaMaterial.LEGACY_PRISMARINE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SEA_LANTERN = VanillaMaterial.LEGACY_SEA_LANTERN;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_HAY_BLOCK = VanillaMaterial.LEGACY_HAY_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CARPET = VanillaMaterial.LEGACY_CARPET;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_HARD_CLAY = VanillaMaterial.LEGACY_HARD_CLAY;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_COAL_BLOCK = VanillaMaterial.LEGACY_COAL_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PACKED_ICE = VanillaMaterial.LEGACY_PACKED_ICE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DOUBLE_PLANT = VanillaMaterial.LEGACY_DOUBLE_PLANT;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STANDING_BANNER = VanillaMaterial.LEGACY_STANDING_BANNER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WALL_BANNER = VanillaMaterial.LEGACY_WALL_BANNER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DAYLIGHT_DETECTOR_INVERTED = VanillaMaterial.LEGACY_DAYLIGHT_DETECTOR_INVERTED;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RED_SANDSTONE = VanillaMaterial.LEGACY_RED_SANDSTONE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RED_SANDSTONE_STAIRS = VanillaMaterial.LEGACY_RED_SANDSTONE_STAIRS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DOUBLE_STONE_SLAB2 = VanillaMaterial.LEGACY_DOUBLE_STONE_SLAB2;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STONE_SLAB2 = VanillaMaterial.LEGACY_STONE_SLAB2;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SPRUCE_FENCE_GATE = VanillaMaterial.LEGACY_SPRUCE_FENCE_GATE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BIRCH_FENCE_GATE = VanillaMaterial.LEGACY_BIRCH_FENCE_GATE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_JUNGLE_FENCE_GATE = VanillaMaterial.LEGACY_JUNGLE_FENCE_GATE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DARK_OAK_FENCE_GATE = VanillaMaterial.LEGACY_DARK_OAK_FENCE_GATE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ACACIA_FENCE_GATE = VanillaMaterial.LEGACY_ACACIA_FENCE_GATE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SPRUCE_FENCE = VanillaMaterial.LEGACY_SPRUCE_FENCE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BIRCH_FENCE = VanillaMaterial.LEGACY_BIRCH_FENCE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_JUNGLE_FENCE = VanillaMaterial.LEGACY_JUNGLE_FENCE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DARK_OAK_FENCE = VanillaMaterial.LEGACY_DARK_OAK_FENCE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ACACIA_FENCE = VanillaMaterial.LEGACY_ACACIA_FENCE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SPRUCE_DOOR = VanillaMaterial.LEGACY_SPRUCE_DOOR;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BIRCH_DOOR = VanillaMaterial.LEGACY_BIRCH_DOOR;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_JUNGLE_DOOR = VanillaMaterial.LEGACY_JUNGLE_DOOR;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ACACIA_DOOR = VanillaMaterial.LEGACY_ACACIA_DOOR;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DARK_OAK_DOOR = VanillaMaterial.LEGACY_DARK_OAK_DOOR;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_END_ROD = VanillaMaterial.LEGACY_END_ROD;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CHORUS_PLANT = VanillaMaterial.LEGACY_CHORUS_PLANT;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CHORUS_FLOWER = VanillaMaterial.LEGACY_CHORUS_FLOWER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PURPUR_BLOCK = VanillaMaterial.LEGACY_PURPUR_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PURPUR_PILLAR = VanillaMaterial.LEGACY_PURPUR_PILLAR;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PURPUR_STAIRS = VanillaMaterial.LEGACY_PURPUR_STAIRS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PURPUR_DOUBLE_SLAB = VanillaMaterial.LEGACY_PURPUR_DOUBLE_SLAB;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PURPUR_SLAB = VanillaMaterial.LEGACY_PURPUR_SLAB;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_END_BRICKS = VanillaMaterial.LEGACY_END_BRICKS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BEETROOT_BLOCK = VanillaMaterial.LEGACY_BEETROOT_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GRASS_PATH = VanillaMaterial.LEGACY_GRASS_PATH;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_END_GATEWAY = VanillaMaterial.LEGACY_END_GATEWAY;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_COMMAND_REPEATING = VanillaMaterial.LEGACY_COMMAND_REPEATING;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_COMMAND_CHAIN = VanillaMaterial.LEGACY_COMMAND_CHAIN;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_FROSTED_ICE = VanillaMaterial.LEGACY_FROSTED_ICE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_MAGMA = VanillaMaterial.LEGACY_MAGMA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_NETHER_WART_BLOCK = VanillaMaterial.LEGACY_NETHER_WART_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RED_NETHER_BRICK = VanillaMaterial.LEGACY_RED_NETHER_BRICK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BONE_BLOCK = VanillaMaterial.LEGACY_BONE_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STRUCTURE_VOID = VanillaMaterial.LEGACY_STRUCTURE_VOID;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_OBSERVER = VanillaMaterial.LEGACY_OBSERVER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WHITE_SHULKER_BOX = VanillaMaterial.LEGACY_WHITE_SHULKER_BOX;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ORANGE_SHULKER_BOX = VanillaMaterial.LEGACY_ORANGE_SHULKER_BOX;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_MAGENTA_SHULKER_BOX = VanillaMaterial.LEGACY_MAGENTA_SHULKER_BOX;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LIGHT_BLUE_SHULKER_BOX = VanillaMaterial.LEGACY_LIGHT_BLUE_SHULKER_BOX;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_YELLOW_SHULKER_BOX = VanillaMaterial.LEGACY_YELLOW_SHULKER_BOX;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LIME_SHULKER_BOX = VanillaMaterial.LEGACY_LIME_SHULKER_BOX;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PINK_SHULKER_BOX = VanillaMaterial.LEGACY_PINK_SHULKER_BOX;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GRAY_SHULKER_BOX = VanillaMaterial.LEGACY_GRAY_SHULKER_BOX;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SILVER_SHULKER_BOX = VanillaMaterial.LEGACY_SILVER_SHULKER_BOX;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CYAN_SHULKER_BOX = VanillaMaterial.LEGACY_CYAN_SHULKER_BOX;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PURPLE_SHULKER_BOX = VanillaMaterial.LEGACY_PURPLE_SHULKER_BOX;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BLUE_SHULKER_BOX = VanillaMaterial.LEGACY_BLUE_SHULKER_BOX;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BROWN_SHULKER_BOX = VanillaMaterial.LEGACY_BROWN_SHULKER_BOX;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GREEN_SHULKER_BOX = VanillaMaterial.LEGACY_GREEN_SHULKER_BOX;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RED_SHULKER_BOX = VanillaMaterial.LEGACY_RED_SHULKER_BOX;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BLACK_SHULKER_BOX = VanillaMaterial.LEGACY_BLACK_SHULKER_BOX;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WHITE_GLAZED_TERRACOTTA = VanillaMaterial.LEGACY_WHITE_GLAZED_TERRACOTTA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ORANGE_GLAZED_TERRACOTTA = VanillaMaterial.LEGACY_ORANGE_GLAZED_TERRACOTTA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_MAGENTA_GLAZED_TERRACOTTA = VanillaMaterial.LEGACY_MAGENTA_GLAZED_TERRACOTTA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LIGHT_BLUE_GLAZED_TERRACOTTA = VanillaMaterial.LEGACY_LIGHT_BLUE_GLAZED_TERRACOTTA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_YELLOW_GLAZED_TERRACOTTA = VanillaMaterial.LEGACY_YELLOW_GLAZED_TERRACOTTA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LIME_GLAZED_TERRACOTTA = VanillaMaterial.LEGACY_LIME_GLAZED_TERRACOTTA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PINK_GLAZED_TERRACOTTA = VanillaMaterial.LEGACY_PINK_GLAZED_TERRACOTTA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GRAY_GLAZED_TERRACOTTA = VanillaMaterial.LEGACY_GRAY_GLAZED_TERRACOTTA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SILVER_GLAZED_TERRACOTTA = VanillaMaterial.LEGACY_SILVER_GLAZED_TERRACOTTA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CYAN_GLAZED_TERRACOTTA = VanillaMaterial.LEGACY_CYAN_GLAZED_TERRACOTTA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PURPLE_GLAZED_TERRACOTTA = VanillaMaterial.LEGACY_PURPLE_GLAZED_TERRACOTTA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BLUE_GLAZED_TERRACOTTA = VanillaMaterial.LEGACY_BLUE_GLAZED_TERRACOTTA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BROWN_GLAZED_TERRACOTTA = VanillaMaterial.LEGACY_BROWN_GLAZED_TERRACOTTA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GREEN_GLAZED_TERRACOTTA = VanillaMaterial.LEGACY_GREEN_GLAZED_TERRACOTTA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RED_GLAZED_TERRACOTTA = VanillaMaterial.LEGACY_RED_GLAZED_TERRACOTTA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BLACK_GLAZED_TERRACOTTA = VanillaMaterial.LEGACY_BLACK_GLAZED_TERRACOTTA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CONCRETE = VanillaMaterial.LEGACY_CONCRETE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CONCRETE_POWDER = VanillaMaterial.LEGACY_CONCRETE_POWDER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STRUCTURE_BLOCK = VanillaMaterial.LEGACY_STRUCTURE_BLOCK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_IRON_SPADE = VanillaMaterial.LEGACY_IRON_SPADE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_IRON_PICKAXE = VanillaMaterial.LEGACY_IRON_PICKAXE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_IRON_AXE = VanillaMaterial.LEGACY_IRON_AXE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_FLINT_AND_STEEL = VanillaMaterial.LEGACY_FLINT_AND_STEEL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_APPLE = VanillaMaterial.LEGACY_APPLE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BOW = VanillaMaterial.LEGACY_BOW;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ARROW = VanillaMaterial.LEGACY_ARROW;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_COAL = VanillaMaterial.LEGACY_COAL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DIAMOND = VanillaMaterial.LEGACY_DIAMOND;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_IRON_INGOT = VanillaMaterial.LEGACY_IRON_INGOT;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GOLD_INGOT = VanillaMaterial.LEGACY_GOLD_INGOT;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_IRON_SWORD = VanillaMaterial.LEGACY_IRON_SWORD;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WOOD_SWORD = VanillaMaterial.LEGACY_WOOD_SWORD;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WOOD_SPADE = VanillaMaterial.LEGACY_WOOD_SPADE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WOOD_PICKAXE = VanillaMaterial.LEGACY_WOOD_PICKAXE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WOOD_AXE = VanillaMaterial.LEGACY_WOOD_AXE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STONE_SWORD = VanillaMaterial.LEGACY_STONE_SWORD;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STONE_SPADE = VanillaMaterial.LEGACY_STONE_SPADE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STONE_PICKAXE = VanillaMaterial.LEGACY_STONE_PICKAXE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STONE_AXE = VanillaMaterial.LEGACY_STONE_AXE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DIAMOND_SWORD = VanillaMaterial.LEGACY_DIAMOND_SWORD;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DIAMOND_SPADE = VanillaMaterial.LEGACY_DIAMOND_SPADE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DIAMOND_PICKAXE = VanillaMaterial.LEGACY_DIAMOND_PICKAXE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DIAMOND_AXE = VanillaMaterial.LEGACY_DIAMOND_AXE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STICK = VanillaMaterial.LEGACY_STICK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BOWL = VanillaMaterial.LEGACY_BOWL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_MUSHROOM_SOUP = VanillaMaterial.LEGACY_MUSHROOM_SOUP;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GOLD_SWORD = VanillaMaterial.LEGACY_GOLD_SWORD;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GOLD_SPADE = VanillaMaterial.LEGACY_GOLD_SPADE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GOLD_PICKAXE = VanillaMaterial.LEGACY_GOLD_PICKAXE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GOLD_AXE = VanillaMaterial.LEGACY_GOLD_AXE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STRING = VanillaMaterial.LEGACY_STRING;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_FEATHER = VanillaMaterial.LEGACY_FEATHER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SULPHUR = VanillaMaterial.LEGACY_SULPHUR;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WOOD_HOE = VanillaMaterial.LEGACY_WOOD_HOE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STONE_HOE = VanillaMaterial.LEGACY_STONE_HOE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_IRON_HOE = VanillaMaterial.LEGACY_IRON_HOE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DIAMOND_HOE = VanillaMaterial.LEGACY_DIAMOND_HOE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GOLD_HOE = VanillaMaterial.LEGACY_GOLD_HOE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SEEDS = VanillaMaterial.LEGACY_SEEDS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WHEAT = VanillaMaterial.LEGACY_WHEAT;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BREAD = VanillaMaterial.LEGACY_BREAD;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LEATHER_HELMET = VanillaMaterial.LEGACY_LEATHER_HELMET;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LEATHER_CHESTPLATE = VanillaMaterial.LEGACY_LEATHER_CHESTPLATE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LEATHER_LEGGINGS = VanillaMaterial.LEGACY_LEATHER_LEGGINGS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LEATHER_BOOTS = VanillaMaterial.LEGACY_LEATHER_BOOTS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CHAINMAIL_HELMET = VanillaMaterial.LEGACY_CHAINMAIL_HELMET;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CHAINMAIL_CHESTPLATE = VanillaMaterial.LEGACY_CHAINMAIL_CHESTPLATE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CHAINMAIL_LEGGINGS = VanillaMaterial.LEGACY_CHAINMAIL_LEGGINGS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CHAINMAIL_BOOTS = VanillaMaterial.LEGACY_CHAINMAIL_BOOTS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_IRON_HELMET = VanillaMaterial.LEGACY_IRON_HELMET;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_IRON_CHESTPLATE = VanillaMaterial.LEGACY_IRON_CHESTPLATE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_IRON_LEGGINGS = VanillaMaterial.LEGACY_IRON_LEGGINGS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_IRON_BOOTS = VanillaMaterial.LEGACY_IRON_BOOTS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DIAMOND_HELMET = VanillaMaterial.LEGACY_DIAMOND_HELMET;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DIAMOND_CHESTPLATE = VanillaMaterial.LEGACY_DIAMOND_CHESTPLATE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DIAMOND_LEGGINGS = VanillaMaterial.LEGACY_DIAMOND_LEGGINGS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DIAMOND_BOOTS = VanillaMaterial.LEGACY_DIAMOND_BOOTS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GOLD_HELMET = VanillaMaterial.LEGACY_GOLD_HELMET;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GOLD_CHESTPLATE = VanillaMaterial.LEGACY_GOLD_CHESTPLATE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GOLD_LEGGINGS = VanillaMaterial.LEGACY_GOLD_LEGGINGS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GOLD_BOOTS = VanillaMaterial.LEGACY_GOLD_BOOTS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_FLINT = VanillaMaterial.LEGACY_FLINT;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PORK = VanillaMaterial.LEGACY_PORK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GRILLED_PORK = VanillaMaterial.LEGACY_GRILLED_PORK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PAINTING = VanillaMaterial.LEGACY_PAINTING;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GOLDEN_APPLE = VanillaMaterial.LEGACY_GOLDEN_APPLE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SIGN = VanillaMaterial.LEGACY_SIGN;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WOOD_DOOR = VanillaMaterial.LEGACY_WOOD_DOOR;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BUCKET = VanillaMaterial.LEGACY_BUCKET;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WATER_BUCKET = VanillaMaterial.LEGACY_WATER_BUCKET;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LAVA_BUCKET = VanillaMaterial.LEGACY_LAVA_BUCKET;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_MINECART = VanillaMaterial.LEGACY_MINECART;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SADDLE = VanillaMaterial.LEGACY_SADDLE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_IRON_DOOR = VanillaMaterial.LEGACY_IRON_DOOR;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_REDSTONE = VanillaMaterial.LEGACY_REDSTONE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SNOW_BALL = VanillaMaterial.LEGACY_SNOW_BALL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BOAT = VanillaMaterial.LEGACY_BOAT;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LEATHER = VanillaMaterial.LEGACY_LEATHER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_MILK_BUCKET = VanillaMaterial.LEGACY_MILK_BUCKET;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CLAY_BRICK = VanillaMaterial.LEGACY_CLAY_BRICK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CLAY_BALL = VanillaMaterial.LEGACY_CLAY_BALL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SUGAR_CANE = VanillaMaterial.LEGACY_SUGAR_CANE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PAPER = VanillaMaterial.LEGACY_PAPER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BOOK = VanillaMaterial.LEGACY_BOOK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SLIME_BALL = VanillaMaterial.LEGACY_SLIME_BALL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_STORAGE_MINECART = VanillaMaterial.LEGACY_STORAGE_MINECART;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_POWERED_MINECART = VanillaMaterial.LEGACY_POWERED_MINECART;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_EGG = VanillaMaterial.LEGACY_EGG;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_COMPASS = VanillaMaterial.LEGACY_COMPASS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_FISHING_ROD = VanillaMaterial.LEGACY_FISHING_ROD;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WATCH = VanillaMaterial.LEGACY_WATCH;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GLOWSTONE_DUST = VanillaMaterial.LEGACY_GLOWSTONE_DUST;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RAW_FISH = VanillaMaterial.LEGACY_RAW_FISH;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_COOKED_FISH = VanillaMaterial.LEGACY_COOKED_FISH;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_INK_SACK = VanillaMaterial.LEGACY_INK_SACK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BONE = VanillaMaterial.LEGACY_BONE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SUGAR = VanillaMaterial.LEGACY_SUGAR;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CAKE = VanillaMaterial.LEGACY_CAKE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BED = VanillaMaterial.LEGACY_BED;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DIODE = VanillaMaterial.LEGACY_DIODE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_COOKIE = VanillaMaterial.LEGACY_COOKIE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_MAP = VanillaMaterial.LEGACY_MAP;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SHEARS = VanillaMaterial.LEGACY_SHEARS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_MELON = VanillaMaterial.LEGACY_MELON;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PUMPKIN_SEEDS = VanillaMaterial.LEGACY_PUMPKIN_SEEDS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_MELON_SEEDS = VanillaMaterial.LEGACY_MELON_SEEDS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RAW_BEEF = VanillaMaterial.LEGACY_RAW_BEEF;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_COOKED_BEEF = VanillaMaterial.LEGACY_COOKED_BEEF;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RAW_CHICKEN = VanillaMaterial.LEGACY_RAW_CHICKEN;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_COOKED_CHICKEN = VanillaMaterial.LEGACY_COOKED_CHICKEN;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ROTTEN_FLESH = VanillaMaterial.LEGACY_ROTTEN_FLESH;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ENDER_PEARL = VanillaMaterial.LEGACY_ENDER_PEARL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BLAZE_ROD = VanillaMaterial.LEGACY_BLAZE_ROD;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GHAST_TEAR = VanillaMaterial.LEGACY_GHAST_TEAR;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GOLD_NUGGET = VanillaMaterial.LEGACY_GOLD_NUGGET;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_NETHER_STALK = VanillaMaterial.LEGACY_NETHER_STALK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_POTION = VanillaMaterial.LEGACY_POTION;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GLASS_BOTTLE = VanillaMaterial.LEGACY_GLASS_BOTTLE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SPIDER_EYE = VanillaMaterial.LEGACY_SPIDER_EYE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_FERMENTED_SPIDER_EYE = VanillaMaterial.LEGACY_FERMENTED_SPIDER_EYE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BLAZE_POWDER = VanillaMaterial.LEGACY_BLAZE_POWDER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_MAGMA_CREAM = VanillaMaterial.LEGACY_MAGMA_CREAM;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BREWING_STAND_ITEM = VanillaMaterial.LEGACY_BREWING_STAND_ITEM;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CAULDRON_ITEM = VanillaMaterial.LEGACY_CAULDRON_ITEM;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_EYE_OF_ENDER = VanillaMaterial.LEGACY_EYE_OF_ENDER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SPECKLED_MELON = VanillaMaterial.LEGACY_SPECKLED_MELON;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_MONSTER_EGG = VanillaMaterial.LEGACY_MONSTER_EGG;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_EXP_BOTTLE = VanillaMaterial.LEGACY_EXP_BOTTLE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_FIREBALL = VanillaMaterial.LEGACY_FIREBALL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BOOK_AND_QUILL = VanillaMaterial.LEGACY_BOOK_AND_QUILL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_WRITTEN_BOOK = VanillaMaterial.LEGACY_WRITTEN_BOOK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_EMERALD = VanillaMaterial.LEGACY_EMERALD;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ITEM_FRAME = VanillaMaterial.LEGACY_ITEM_FRAME;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_FLOWER_POT_ITEM = VanillaMaterial.LEGACY_FLOWER_POT_ITEM;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CARROT_ITEM = VanillaMaterial.LEGACY_CARROT_ITEM;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_POTATO_ITEM = VanillaMaterial.LEGACY_POTATO_ITEM;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BAKED_POTATO = VanillaMaterial.LEGACY_BAKED_POTATO;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_POISONOUS_POTATO = VanillaMaterial.LEGACY_POISONOUS_POTATO;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_EMPTY_MAP = VanillaMaterial.LEGACY_EMPTY_MAP;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GOLDEN_CARROT = VanillaMaterial.LEGACY_GOLDEN_CARROT;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SKULL_ITEM = VanillaMaterial.LEGACY_SKULL_ITEM;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CARROT_STICK = VanillaMaterial.LEGACY_CARROT_STICK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_NETHER_STAR = VanillaMaterial.LEGACY_NETHER_STAR;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PUMPKIN_PIE = VanillaMaterial.LEGACY_PUMPKIN_PIE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_FIREWORK = VanillaMaterial.LEGACY_FIREWORK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_FIREWORK_CHARGE = VanillaMaterial.LEGACY_FIREWORK_CHARGE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ENCHANTED_BOOK = VanillaMaterial.LEGACY_ENCHANTED_BOOK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_REDSTONE_COMPARATOR = VanillaMaterial.LEGACY_REDSTONE_COMPARATOR;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_NETHER_BRICK_ITEM = VanillaMaterial.LEGACY_NETHER_BRICK_ITEM;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_QUARTZ = VanillaMaterial.LEGACY_QUARTZ;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_EXPLOSIVE_MINECART = VanillaMaterial.LEGACY_EXPLOSIVE_MINECART;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_HOPPER_MINECART = VanillaMaterial.LEGACY_HOPPER_MINECART;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PRISMARINE_SHARD = VanillaMaterial.LEGACY_PRISMARINE_SHARD;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_PRISMARINE_CRYSTALS = VanillaMaterial.LEGACY_PRISMARINE_CRYSTALS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RABBIT = VanillaMaterial.LEGACY_RABBIT;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_COOKED_RABBIT = VanillaMaterial.LEGACY_COOKED_RABBIT;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RABBIT_STEW = VanillaMaterial.LEGACY_RABBIT_STEW;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RABBIT_FOOT = VanillaMaterial.LEGACY_RABBIT_FOOT;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RABBIT_HIDE = VanillaMaterial.LEGACY_RABBIT_HIDE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ARMOR_STAND = VanillaMaterial.LEGACY_ARMOR_STAND;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_IRON_BARDING = VanillaMaterial.LEGACY_IRON_BARDING;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GOLD_BARDING = VanillaMaterial.LEGACY_GOLD_BARDING;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DIAMOND_BARDING = VanillaMaterial.LEGACY_DIAMOND_BARDING;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LEASH = VanillaMaterial.LEGACY_LEASH;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_NAME_TAG = VanillaMaterial.LEGACY_NAME_TAG;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_COMMAND_MINECART = VanillaMaterial.LEGACY_COMMAND_MINECART;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_MUTTON = VanillaMaterial.LEGACY_MUTTON;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_COOKED_MUTTON = VanillaMaterial.LEGACY_COOKED_MUTTON;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BANNER = VanillaMaterial.LEGACY_BANNER;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_END_CRYSTAL = VanillaMaterial.LEGACY_END_CRYSTAL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SPRUCE_DOOR_ITEM = VanillaMaterial.LEGACY_SPRUCE_DOOR_ITEM;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BIRCH_DOOR_ITEM = VanillaMaterial.LEGACY_BIRCH_DOOR_ITEM;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_JUNGLE_DOOR_ITEM = VanillaMaterial.LEGACY_JUNGLE_DOOR_ITEM;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ACACIA_DOOR_ITEM = VanillaMaterial.LEGACY_ACACIA_DOOR_ITEM;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DARK_OAK_DOOR_ITEM = VanillaMaterial.LEGACY_DARK_OAK_DOOR_ITEM;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CHORUS_FRUIT = VanillaMaterial.LEGACY_CHORUS_FRUIT;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_CHORUS_FRUIT_POPPED = VanillaMaterial.LEGACY_CHORUS_FRUIT_POPPED;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BEETROOT = VanillaMaterial.LEGACY_BEETROOT;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BEETROOT_SEEDS = VanillaMaterial.LEGACY_BEETROOT_SEEDS;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BEETROOT_SOUP = VanillaMaterial.LEGACY_BEETROOT_SOUP;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_DRAGONS_BREATH = VanillaMaterial.LEGACY_DRAGONS_BREATH;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SPLASH_POTION = VanillaMaterial.LEGACY_SPLASH_POTION;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SPECTRAL_ARROW = VanillaMaterial.LEGACY_SPECTRAL_ARROW;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_TIPPED_ARROW = VanillaMaterial.LEGACY_TIPPED_ARROW;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_LINGERING_POTION = VanillaMaterial.LEGACY_LINGERING_POTION;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SHIELD = VanillaMaterial.LEGACY_SHIELD;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_ELYTRA = VanillaMaterial.LEGACY_ELYTRA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BOAT_SPRUCE = VanillaMaterial.LEGACY_BOAT_SPRUCE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BOAT_BIRCH = VanillaMaterial.LEGACY_BOAT_BIRCH;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BOAT_JUNGLE = VanillaMaterial.LEGACY_BOAT_JUNGLE;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BOAT_ACACIA = VanillaMaterial.LEGACY_BOAT_ACACIA;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_BOAT_DARK_OAK = VanillaMaterial.LEGACY_BOAT_DARK_OAK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_TOTEM = VanillaMaterial.LEGACY_TOTEM;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_SHULKER_SHELL = VanillaMaterial.LEGACY_SHULKER_SHELL;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_IRON_NUGGET = VanillaMaterial.LEGACY_IRON_NUGGET;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_KNOWLEDGE_BOOK = VanillaMaterial.LEGACY_KNOWLEDGE_BOOK;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GOLD_RECORD = VanillaMaterial.LEGACY_GOLD_RECORD;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_GREEN_RECORD = VanillaMaterial.LEGACY_GREEN_RECORD;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RECORD_3 = VanillaMaterial.LEGACY_RECORD_3;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RECORD_4 = VanillaMaterial.LEGACY_RECORD_4;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RECORD_5 = VanillaMaterial.LEGACY_RECORD_5;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RECORD_6 = VanillaMaterial.LEGACY_RECORD_6;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RECORD_7 = VanillaMaterial.LEGACY_RECORD_7;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RECORD_8 = VanillaMaterial.LEGACY_RECORD_8;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RECORD_9 = VanillaMaterial.LEGACY_RECORD_9;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RECORD_10 = VanillaMaterial.LEGACY_RECORD_10;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RECORD_11 = VanillaMaterial.LEGACY_RECORD_11;
    @Deprecated(since = "1.13", forRemoval = true)
    Material LEGACY_RECORD_12 = VanillaMaterial.LEGACY_RECORD_12;

    @Deprecated(since = "1.13", forRemoval = true)
    String LEGACY_PREFIX = "LEGACY_";

    // ---- instance API ----

    // Paper start - add Translatable
    @Override
    @NotNull String translationKey();


    // Paper end - add Translatable

    // Paper start - item rarity API
    /**
     * Returns the item rarity for the item. The Material <b>MUST</b> be an Item not a block.
     * Use {@link #isItem()} before this.
     *
     * @return the item rarity
     * @deprecated use {@link org.bukkit.inventory.meta.ItemMeta#hasRarity()} and {@link org.bukkit.inventory.meta.ItemMeta#getRarity()}
     */
    @NotNull
    @Deprecated(forRemoval = true, since = "1.20.5")
    io.papermc.paper.inventory.ItemRarity getItemRarity();


    // Paper end - item rarity API

    // Paper start - item default attributes API
    /**
     * Returns an immutable multimap of attributes for the slot.
     * {@link #isItem()} must be true for this material.
     *
     * @param equipmentSlot the slot to get the attributes for
     * @throws IllegalArgumentException if {@link #isItem()} is false
     * @return an immutable multimap of attributes
     * @deprecated use {@link #getDefaultAttributeModifiers(EquipmentSlot)}
     */
    @NotNull
    @Deprecated(forRemoval = true, since = "1.20.5")
    Multimap<Attribute, AttributeModifier> getItemAttributes(@NotNull EquipmentSlot equipmentSlot);


    // Paper end - item default attributes API

    // Paper start - isCollidable API
    /**
     * Checks if this material is collidable.
     *
     * @return true if collidable
     * @throws IllegalArgumentException if {@link #isBlock()} is false
     */
    boolean isCollidable();


    // Paper end - isCollidable API

    /**
     * Do not use for any reason.
     *
     * @return ID of this material
     * @apiNote Internal Use Only
     */
    @ApiStatus.Internal // Paper
    int getId();



    /**
     * Checks if this constant is a legacy material.
     *
     * @return legacy status
     */
    // @Deprecated(since = "1.13", forRemoval = true) // Paper - this is useful, don't deprecate
    boolean isLegacy();



    @NotNull
    @Override
    NamespacedKey getKey();



    /**
     * Gets the maximum amount of this material that can be held in a stack.
     * <p>
     * Note that this is the <strong>default</strong> maximum size for this Material.
     * {@link ItemStack ItemStacks} are able to change their maximum stack size per
     * stack with {@link ItemMeta#setMaxStackSize(Integer)}. If an ItemStack instance
     * is available, {@link ItemStack#getMaxStackSize()} may be preferred.
     *
     * @return Maximum stack size for this material
     */
    int getMaxStackSize();



    /**
     * Gets the maximum durability of this material
     *
     * @return Maximum durability for this material
     */
    short getMaxDurability();



    /**
     * Creates a new {@link BlockData} instance for this Material, with all
     * properties initialized to unspecified defaults.
     *
     * @return new data instance
     */
    @NotNull
    BlockData createBlockData();



    /**
     * Creates a new {@link BlockData} instance for this Material, with
     * all properties initialized to unspecified defaults.
     *
     * @param consumer consumer to run on new instance before returning
     * @return new data instance
     */
    @NotNull
    BlockData createBlockData(@Nullable Consumer<? super BlockData> consumer);



    /**
     * Creates a new {@link BlockData} instance for this Material, with all
     * properties initialized to unspecified defaults, except for those provided
     * in data.
     *
     * @param data data string
     * @return new data instance
     * @throws IllegalArgumentException if the specified data is not valid
     */
    @NotNull
    BlockData createBlockData(@Nullable String data) throws IllegalArgumentException;



    /**
     * Gets the MaterialData class associated with this Material
     *
     * @return MaterialData associated with this Material
     * @deprecated use {@link #createBlockData()}
     */
    @NotNull
    @Deprecated // Paper
    Class<? extends MaterialData> getData();



    /**
     * Constructs a new MaterialData relevant for this Material, with the
     * given initial data
     *
     * @param raw Initial data to construct the MaterialData with
     * @return New MaterialData with the given data
     * @deprecated Magic value
     */
    @Deprecated(since = "1.6.2")
    @NotNull
    MaterialData getNewData(final byte raw);



    /**
     * Checks if this Material is a placable block
     *
     * @return true if this material is a block
     */
    boolean isBlock();



    /**
     * Checks if this Material provides the {@link io.papermc.paper.datacomponent.DataComponentTypes#FOOD} and
     * {@link io.papermc.paper.datacomponent.DataComponentTypes#CONSUMABLE} and, thereby, is edible by a player.
     *
     * @return true if this Material is edible.
     */
    boolean isEdible();



    /**
     * @return True if this material represents a playable music disk.
     */
    boolean isRecord();



    /**
     * Check if the material is a block and solid (can be built upon)
     *
     * @return True if this material is a block and solid
     */
    boolean isSolid();



    /**
     * Check if the material is an air block.
     *
     * @return True if this material is an air block.
     */
    boolean isAir();



    /**
     * @return If the type is either AIR, CAVE_AIR or VOID_AIR
     * @deprecated use {@link #isAir()}
     */
    @Deprecated(since = "1.21.5")
    boolean isEmpty();



    /**
     * Check if the material is a block and does not block any light
     *
     * @return True if this material is a block and does not block any light
     * @deprecated currently does not have an implementation which is well
     * linked to the underlying server. Contributions welcome.
     */
    @Deprecated(since = "1.13", forRemoval = true)
    boolean isTransparent();



    /**
     * Check if the material is a block and can catch fire
     *
     * @return True if this material is a block and can catch fire
     */
    boolean isFlammable();



    /**
     * Check if the material is a block and can burn away
     *
     * @return True if this material is a block and can burn away
     */
    boolean isBurnable();



    /**
     * Checks if this Material can be used as fuel in a Furnace
     *
     * @return true if this Material can be used as fuel.
     */
    boolean isFuel();



    /**
     * Check if the material is a block and occludes light in the lighting engine.
     * <p>
     * Generally speaking, most full blocks will occlude light. Non-full blocks are
     * not occluding (e.g. anvils, chests, tall grass, stairs, etc.), nor are specific
     * full blocks such as barriers or spawners which block light despite their texture.
     * <p>
     * An occluding block will have the following effects:
     * <ul>
     *   <li>Chests cannot be opened if an occluding block is above it.
     *   <li>Mobs cannot spawn inside of occluding blocks.
     *   <li>Only occluding blocks can be "powered" ({@link Block#isBlockPowered()}).
     * </ul>
     * This list may be inconclusive. For a full list of the side effects of an occluding
     * block, see the <a href="https://minecraft.wiki/w/Opacity">Minecraft Wiki</a>.
     *
     * @return True if this material is a block and occludes light
     */
    boolean isOccluding();



    /**
     * @return True if this material is affected by gravity.
     */
    boolean hasGravity();



    /**
     * Checks if this Material is an obtainable item.
     *
     * @return true if this material is an item
     */
    boolean isItem();



    /**
     * Checks if this Material can be interacted with.
     *
     * Interactable materials include those with functionality when they are
     * interacted with by a player such as chests, furnaces, etc.
     *
     * Some blocks such as piston heads and stairs are considered interactable
     * though may not perform any additional functionality.
     *
     * Note that the interactability of some materials may be dependant on their
     * state as well. This method will return true if there is at least one
     * state in which additional interact handling is performed for the
     * material.
     *
     * @return true if this material can be interacted with.
     * @deprecated This method is not comprehensive and does not accurately reflect what block types are
     * interactable. Many "interactions" are defined on the item not block, and many are conditional on some other world state
     * checks being true.
     */
    @Deprecated // Paper
    boolean isInteractable();



    /**
     * Obtains the block's hardness level (also known as "strength").
     * <br>
     * This number is used to calculate the time required to break each block.
     * <br>
     * Only available when {@link #isBlock()} is true.
     *
     * @return the hardness of that material.
     */
    float getHardness();



    /**
     * Obtains the blast resistance value (also known as block "durability").
     * <br>
     * This value is used in explosions to calculate whether a block should be
     * broken or not.
     * <br>
     * Only available when {@link #isBlock()} is true.
     *
     * @return the blast resistance of that material.
     */
    float getBlastResistance();



    /**
     * Returns a value that represents how 'slippery' the block is.
     *
     * Blocks with higher slipperiness, like {@link Material#ICE} can be slid on
     * further by the player and other entities.
     *
     * Most blocks have a default slipperiness of {@code 0.6f}.
     *
     * Only available when {@link #isBlock()} is true.
     *
     * @return the slipperiness of this block
     */
    float getSlipperiness();



    /**
     * Determines the remaining item in a crafting grid after crafting with this
     * ingredient.
     * <br>
     * Only available when {@link #isItem()} is true.
     *
     * @return the item left behind when crafting, or null if nothing is.
     */
    @Nullable
    Material getCraftingRemainingItem();



    /**
     * Get the best suitable slot for this Material.
     *
     * For most items this will be {@link EquipmentSlot#HAND}.
     *
     * @return the best EquipmentSlot for this Material
     */
    @NotNull
    EquipmentSlot getEquipmentSlot();



    // Paper start - improve default item attribute API
    /**
     * Return an immutable copy of all default {@link Attribute}s and their {@link AttributeModifier}s.
     * <p>
     * Default attributes are those that are always preset on some items, unless
     * they are specifically overridden on that {@link ItemStack}. Examples include
     * the attack damage on weapons or the armor value on armor.
     * <p>
     * Only available when {@link #isItem()} is true.
     *
     * @return the immutable {@link Multimap} with the respective default
     * Attributes and modifiers, or an empty map if no attributes are set.
     */
    @NotNull @org.jetbrains.annotations.Unmodifiable Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers();


    // Paper end - improve default item attribute API

    /**
     * Return an immutable copy of all default {@link Attribute}s and their
     * {@link AttributeModifier}s for a given {@link EquipmentSlot}.
     * <p>
     * Default attributes are those that are always preset on some items, unless
     * they are specifically overridden on that {@link ItemStack}. Examples include
     * the attack damage on weapons or the armor value on armor.
     * <p>
     * Only available when {@link #isItem()} is true.
     *
     * @param slot the {@link EquipmentSlot} to check
     * @return the immutable {@link Multimap} with the respective default
     * Attributes and modifiers, or an empty map if no attributes are set.
     */
    @NotNull
    Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(@NotNull EquipmentSlot slot);



    /**
     * Get the {@link CreativeCategory} to which this material belongs.
     *
     * @return the creative category. null if it does not belong to a category
     * @deprecated items can belong to multiple creative categories and this is no
     * longer implemented, will always be {@link CreativeCategory#BUILDING_BLOCKS} if not null
     */
    @Deprecated(since = "1.20.6", forRemoval = true)
    @Nullable CreativeCategory getCreativeCategory();



    /**
     * Get the translation key of the item or block associated with this
     * material.
     *
     * If this material has both an item and a block form, the item form is
     * used.
     *
     * @return the translation key of the item or block associated with this
     * material
     * @see #getBlockTranslationKey()
     * @see #getItemTranslationKey()
     * @deprecated use {@link #translationKey()}
     */
    @Override
    @NotNull
    @Deprecated(forRemoval = true) // Paper
    String getTranslationKey();



    /**
     * Get the translation key of the block associated with this material, or
     * null if this material does not have an associated block.
     *
     * @return the translation key of the block associated with this material,
     * or null if this material does not have an associated block
     */
    @Nullable
    String getBlockTranslationKey();



    /**
     * Get the translation key of the item associated with this material, or
     * null if this material does not have an associated item.
     *
     * @return the translation key of the item associated with this material, or
     * null if this material does not have an associated item.
     */
    @Nullable
    String getItemTranslationKey();



    /**
     * Checks whether this material is compostable (can be inserted into a
     * composter).
     *
     * @return true if this material is compostable
     * @see #getCompostChance()
     */
    boolean isCompostable();



    /**
     * Get the chance that this material will successfully compost. The returned
     * value is between 0 and 1 (inclusive).
     *
     * Materials with a compost chance of 1 will always raise the composter's
     * level, while materials with a compost chance of 0 will never raise it.
     *
     * Plugins should check that {@link #isCompostable} returns true before
     * calling this method.
     *
     * @return the chance that this material will successfully compost
     * @throws IllegalArgumentException if the material is not compostable
     * @see #isCompostable()
     */
    float getCompostChance();



    /**
     * Tries to convert this Material to an item type
     *
     * @return the converted item type or null
     */
    @Nullable
    @org.jetbrains.annotations.Contract(pure = true) // Paper
    ItemType asItemType();



    /**
     * Tries to convert this Material to a block type
     *
     * @return the converted block type or null
     */
    @Nullable
    @org.jetbrains.annotations.Contract(pure = true) // Paper
    BlockType asBlockType();



    // Paper start - data component API
    /**
     * Gets the default value of the data component type for this item type.
     *
     * @param type the data component type
     * @param <T> the value type
     * @return the default value or {@code null} if there is none
     * @see #hasDefaultData(io.papermc.paper.datacomponent.DataComponentType) for DataComponentType.NonValued
     * @throws IllegalArgumentException if {@link #isItem()} is {@code false}
     */
    @Nullable <T> T getDefaultData(final io.papermc.paper.datacomponent.DataComponentType.@NotNull Valued<T> type);



    /**
     * Checks if the data component type has a default value for this item type.
     *
     * @param type the data component type
     * @return {@code true} if there is a default value
     * @throws IllegalArgumentException if {@link #isItem()} is {@code false}
     */
    boolean hasDefaultData(final io.papermc.paper.datacomponent.@NotNull DataComponentType type);



    /**
     * Gets the default data component types for this item type.
     *
     * @return an immutable set of data component types
     * @throws IllegalArgumentException if {@link #isItem()} is {@code false}
     */
    java.util.@org.jetbrains.annotations.Unmodifiable @NotNull Set<io.papermc.paper.datacomponent.DataComponentType> getDefaultDataTypes();

    /**
     * Enum-style constant name for vanilla types (e.g. {@code "STONE"}).
     * Custom types return {@link #getKey()} as a string.
     *
     * <p>Source-compatible with former {@code Enum#name()}.
     */
    default @NotNull String name() {
        if (this instanceof Enum<?> e) {
            return e.name();
        }
        return getKey().toString();
    }

    /**
     * {@code true} when this is a vanilla Minecraft material constant.
     */
    default boolean isVanilla() {
        return this instanceof VanillaMaterial;
    }

    /**
     * {@code true} when this is a registered custom material (not a vanilla constant).
     */
    default boolean isCustom() {
        return !isVanilla();
    }

    // ---- static lookup (compat with former enum statics) ----

    /**
     * All <em>vanilla</em> material constants (not custom registrations).
     */
    @NotNull
    static Material[] values() {
        final VanillaMaterial[] vanilla = VanillaMaterial.values();
        final Material[] out = new Material[vanilla.length];
        System.arraycopy(vanilla, 0, out, 0, vanilla.length);
        return out;
    }

    /**
     * Looks up a <em>vanilla</em> material by its enum constant name (e.g. {@code "STONE"}).
     * Does not resolve custom material keys — use {@link #getByKey(NamespacedKey)} for that.
     */
    @NotNull
    static Material valueOf(@NotNull final String name) {
        return VanillaMaterial.valueOf(name);
    }

    /**
     * Attempts to get the Material with the given name.
     * <p>
     * This is a normal lookup, names must be the precise name they are given
     * in the enum.
     *
     * @param name Name of the material to get
     * @return Material if found, or null
     */
    @Nullable
    static Material getMaterial(@NotNull final String name) {
        return getMaterial(name, false);
    }

    /**
     * Attempts to get the Material with the given name.
     * <p>
     * This is a normal lookup, names must be the precise name they are given in
     * the enum (but optionally including the LEGACY_PREFIX if legacyName is
     * true).
     * <p>
     * If legacyName is true, then the lookup will be against legacy materials,
     * but the returned Material will be a modern material (ie this method is
     * useful for updating stored data).
     *
     * @param name Name of the material to get
     * @param legacyName whether this is a legacy name lookup
     * @return Material if found, or null
     */
    @Nullable
    static Material getMaterial(@NotNull String name, boolean legacyName) {
        if (legacyName) {
            if (!name.startsWith(LEGACY_PREFIX)) {
                name = LEGACY_PREFIX + name;
            }
            final VanillaMaterial match = VanillaMaterial.byName(name);
            if (match == null) {
                return null;
            }
            return Bukkit.getUnsafe().fromLegacy(match);
        }
        return VanillaMaterial.byName(name);
    }

    /**
     * Attempts to match the Material with the given name.
     * <p>
     * This is a match lookup; names will be stripped of the "minecraft:"
     * namespace, converted to uppercase, then stripped of special characters in
     * an attempt to format it like the enum.
     *
     * @param name Name of the material to get
     * @return Material if found, or null
     */
    @Nullable
    static Material matchMaterial(@NotNull final String name) {
        return matchMaterial(name, false);
    }

    /**
     * Attempts to match the Material with the given name.
     * <p>
     * This is a match lookup; names will be stripped of the "minecraft:"
     * namespace, converted to uppercase, then stripped of special characters in
     * an attempt to format it like the enum.
     *
     * @param name Name of the material to get
     * @param legacyName whether this is a legacy name (see
     * {@link #getMaterial(java.lang.String, boolean)}
     * @return Material if found, or null
     */
    @Nullable
    static Material matchMaterial(@NotNull final String name, boolean legacyName) {
        Preconditions.checkArgument(name != null, "Name cannot be null");

        // Custom / namespaced key path (Task 5 will fully wire catalog; stub safe for now)
        if (name.indexOf(':') >= 0) {
            final NamespacedKey key = NamespacedKey.fromString(name);
            if (key != null) {
                final Material byKey = getByKey(key).orElse(null);
                if (byKey != null) {
                    return byKey;
                }
            }
        }

        String filtered = name;
        if (filtered.startsWith(NamespacedKey.MINECRAFT + ":")) {
            filtered = filtered.substring((NamespacedKey.MINECRAFT + ":").length());
        }
        filtered = filtered.toUpperCase(Locale.ROOT);
        filtered = filtered.replaceAll("\\s+", "_").replaceAll("\\W", "");
        return getMaterial(filtered, legacyName);
    }

    /**
     * Resolve any material (vanilla or mintychochip custom catalog) by key.
     * Vanilla-only until Task 5 wires CustomBlocks; implement empty custom branch first.
     */
    @NotNull
    static Optional<Material> getByKey(@Nullable final NamespacedKey key) {
        if (key == null) {
            return Optional.empty();
        }
        // Task 5: CustomBlocks.get(key)
        try {
            final Material reg = Registry.MATERIAL.get(key);
            if (reg != null) {
                return Optional.of(reg);
            }
        } catch (final Throwable ignored) {
            // bootstrap
        }
        final VanillaMaterial byName = VanillaMaterial.byName(key.getKey().toUpperCase(Locale.ROOT));
        return Optional.ofNullable(byName);
    }
}
