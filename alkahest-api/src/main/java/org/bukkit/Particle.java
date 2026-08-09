package org.bukkit;

import com.google.common.base.Preconditions;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.index.qual.Positive;
import org.jetbrains.annotations.NotNull;

import static io.papermc.paper.util.BoundChecker.requirePositive;
import static io.papermc.paper.util.BoundChecker.requireRange;

/**
 * A particle identity accepted by Bukkit particle APIs.
 *
 * <p>Vanilla types are the constants on this interface (e.g. {@link #POOF}); they are instances of
 * {@link VanillaParticle}. Custom types (registered through {@link ParticleRegistry}) implement this
 * interface so they can be used anywhere a {@code Particle} is accepted, including
 * {@link com.destroystokyo.paper.ParticleBuilder}.
 *
 * <p>This was formerly an enum. The generated constants now live on {@link VanillaParticle} and are
 * re-exported here as fields; {@link #values()} and {@link #valueOf(String)} remain vanilla-only
 * compatibility helpers. Enum-only external {@code switch} bytecode cannot be preserved.
 */
public interface Particle extends Keyed {

    // ---- vanilla constants (source-compatible with former enum constants) ----

    Particle POOF = VanillaParticle.POOF;
    Particle EXPLOSION = VanillaParticle.EXPLOSION;
    Particle EXPLOSION_EMITTER = VanillaParticle.EXPLOSION_EMITTER;
    Particle FIREWORK = VanillaParticle.FIREWORK;
    Particle BUBBLE = VanillaParticle.BUBBLE;
    Particle SPLASH = VanillaParticle.SPLASH;
    Particle FISHING = VanillaParticle.FISHING;
    Particle UNDERWATER = VanillaParticle.UNDERWATER;
    Particle CRIT = VanillaParticle.CRIT;
    Particle ENCHANTED_HIT = VanillaParticle.ENCHANTED_HIT;
    Particle SMOKE = VanillaParticle.SMOKE;
    Particle LARGE_SMOKE = VanillaParticle.LARGE_SMOKE;
    /**
     * Uses {@link Spell} as DataType.
     */
    Particle EFFECT = VanillaParticle.EFFECT;
    /**
     * Uses {@link Spell} as DataType.
     */
    Particle INSTANT_EFFECT = VanillaParticle.INSTANT_EFFECT;
    /**
     * Uses {@link Color} as DataType (with alpha support).
     */
    Particle ENTITY_EFFECT = VanillaParticle.ENTITY_EFFECT;
    Particle WITCH = VanillaParticle.WITCH;
    Particle DRIPPING_WATER = VanillaParticle.DRIPPING_WATER;
    Particle DRIPPING_LAVA = VanillaParticle.DRIPPING_LAVA;
    Particle ANGRY_VILLAGER = VanillaParticle.ANGRY_VILLAGER;
    Particle HAPPY_VILLAGER = VanillaParticle.HAPPY_VILLAGER;
    Particle MYCELIUM = VanillaParticle.MYCELIUM;
    Particle NOTE = VanillaParticle.NOTE;
    Particle PORTAL = VanillaParticle.PORTAL;
    Particle ENCHANT = VanillaParticle.ENCHANT;
    Particle FLAME = VanillaParticle.FLAME;
    Particle LAVA = VanillaParticle.LAVA;
    Particle CLOUD = VanillaParticle.CLOUD;
    /**
     * Uses {@link DustOptions} as DataType.
     */
    Particle DUST = VanillaParticle.DUST;
    Particle ITEM_SNOWBALL = VanillaParticle.ITEM_SNOWBALL;
    Particle ITEM_SLIME = VanillaParticle.ITEM_SLIME;
    Particle HEART = VanillaParticle.HEART;
    /**
     * Uses {@link ItemStack} as DataType.
     */
    Particle ITEM = VanillaParticle.ITEM;
    /**
     * Uses {@link BlockData} as DataType.
     */
    Particle BLOCK = VanillaParticle.BLOCK;
    Particle RAIN = VanillaParticle.RAIN;
    Particle ELDER_GUARDIAN = VanillaParticle.ELDER_GUARDIAN;
    /**
     * Uses {@link Float} as DataType, for the power of the breath.
     */
    Particle DRAGON_BREATH = VanillaParticle.DRAGON_BREATH;
    Particle END_ROD = VanillaParticle.END_ROD;
    Particle DAMAGE_INDICATOR = VanillaParticle.DAMAGE_INDICATOR;
    Particle SWEEP_ATTACK = VanillaParticle.SWEEP_ATTACK;
    /**
     * Uses {@link BlockData} as DataType.
     */
    Particle FALLING_DUST = VanillaParticle.FALLING_DUST;
    Particle TOTEM_OF_UNDYING = VanillaParticle.TOTEM_OF_UNDYING;
    Particle SPIT = VanillaParticle.SPIT;
    Particle SQUID_INK = VanillaParticle.SQUID_INK;
    Particle BUBBLE_POP = VanillaParticle.BUBBLE_POP;
    Particle CURRENT_DOWN = VanillaParticle.CURRENT_DOWN;
    Particle BUBBLE_COLUMN_UP = VanillaParticle.BUBBLE_COLUMN_UP;
    Particle NAUTILUS = VanillaParticle.NAUTILUS;
    Particle DOLPHIN = VanillaParticle.DOLPHIN;
    Particle SNEEZE = VanillaParticle.SNEEZE;
    Particle CAMPFIRE_COSY_SMOKE = VanillaParticle.CAMPFIRE_COSY_SMOKE;
    Particle CAMPFIRE_SIGNAL_SMOKE = VanillaParticle.CAMPFIRE_SIGNAL_SMOKE;
    Particle COMPOSTER = VanillaParticle.COMPOSTER;
    /**
     * Uses {@link Color} as DataType.
     */
    Particle FLASH = VanillaParticle.FLASH;
    Particle FALLING_LAVA = VanillaParticle.FALLING_LAVA;
    Particle LANDING_LAVA = VanillaParticle.LANDING_LAVA;
    Particle FALLING_WATER = VanillaParticle.FALLING_WATER;
    Particle DRIPPING_HONEY = VanillaParticle.DRIPPING_HONEY;
    Particle FALLING_HONEY = VanillaParticle.FALLING_HONEY;
    Particle LANDING_HONEY = VanillaParticle.LANDING_HONEY;
    Particle FALLING_NECTAR = VanillaParticle.FALLING_NECTAR;
    Particle SOUL_FIRE_FLAME = VanillaParticle.SOUL_FIRE_FLAME;
    Particle ASH = VanillaParticle.ASH;
    Particle CRIMSON_SPORE = VanillaParticle.CRIMSON_SPORE;
    Particle WARPED_SPORE = VanillaParticle.WARPED_SPORE;
    Particle SOUL = VanillaParticle.SOUL;
    Particle DRIPPING_OBSIDIAN_TEAR = VanillaParticle.DRIPPING_OBSIDIAN_TEAR;
    Particle FALLING_OBSIDIAN_TEAR = VanillaParticle.FALLING_OBSIDIAN_TEAR;
    Particle LANDING_OBSIDIAN_TEAR = VanillaParticle.LANDING_OBSIDIAN_TEAR;
    Particle REVERSE_PORTAL = VanillaParticle.REVERSE_PORTAL;
    Particle WHITE_ASH = VanillaParticle.WHITE_ASH;
    /**
     * Uses {@link DustTransition} as DataType.
     */
    Particle DUST_COLOR_TRANSITION = VanillaParticle.DUST_COLOR_TRANSITION;
    /**
     * Uses {@link Vibration} as DataType.
     */
    Particle VIBRATION = VanillaParticle.VIBRATION;
    Particle FALLING_SPORE_BLOSSOM = VanillaParticle.FALLING_SPORE_BLOSSOM;
    Particle SPORE_BLOSSOM_AIR = VanillaParticle.SPORE_BLOSSOM_AIR;
    Particle SMALL_FLAME = VanillaParticle.SMALL_FLAME;
    Particle SNOWFLAKE = VanillaParticle.SNOWFLAKE;
    Particle DRIPPING_DRIPSTONE_LAVA = VanillaParticle.DRIPPING_DRIPSTONE_LAVA;
    Particle FALLING_DRIPSTONE_LAVA = VanillaParticle.FALLING_DRIPSTONE_LAVA;
    Particle DRIPPING_DRIPSTONE_WATER = VanillaParticle.DRIPPING_DRIPSTONE_WATER;
    Particle FALLING_DRIPSTONE_WATER = VanillaParticle.FALLING_DRIPSTONE_WATER;
    Particle GLOW_SQUID_INK = VanillaParticle.GLOW_SQUID_INK;
    Particle GLOW = VanillaParticle.GLOW;
    Particle WAX_ON = VanillaParticle.WAX_ON;
    Particle WAX_OFF = VanillaParticle.WAX_OFF;
    Particle ELECTRIC_SPARK = VanillaParticle.ELECTRIC_SPARK;
    Particle SCRAPE = VanillaParticle.SCRAPE;
    Particle SONIC_BOOM = VanillaParticle.SONIC_BOOM;
    Particle SCULK_SOUL = VanillaParticle.SCULK_SOUL;
    /**
     * Uses {@link Float} as DataType, the angle in radians.
     */
    Particle SCULK_CHARGE = VanillaParticle.SCULK_CHARGE;
    Particle SCULK_CHARGE_POP = VanillaParticle.SCULK_CHARGE_POP;
    /**
     * Uses {@link Integer} as DataType.
     */
    Particle SHRIEK = VanillaParticle.SHRIEK;
    Particle CHERRY_LEAVES = VanillaParticle.CHERRY_LEAVES;
    Particle PALE_OAK_LEAVES = VanillaParticle.PALE_OAK_LEAVES;
    /**
     * Uses {@link Color} as DataType.
     */
    Particle TINTED_LEAVES = VanillaParticle.TINTED_LEAVES;
    Particle EGG_CRACK = VanillaParticle.EGG_CRACK;
    Particle DUST_PLUME = VanillaParticle.DUST_PLUME;
    Particle WHITE_SMOKE = VanillaParticle.WHITE_SMOKE;
    Particle GUST = VanillaParticle.GUST;
    Particle SMALL_GUST = VanillaParticle.SMALL_GUST;
    Particle GUST_EMITTER_LARGE = VanillaParticle.GUST_EMITTER_LARGE;
    Particle GUST_EMITTER_SMALL = VanillaParticle.GUST_EMITTER_SMALL;
    Particle TRIAL_SPAWNER_DETECTION = VanillaParticle.TRIAL_SPAWNER_DETECTION;
    Particle TRIAL_SPAWNER_DETECTION_OMINOUS = VanillaParticle.TRIAL_SPAWNER_DETECTION_OMINOUS;
    Particle VAULT_CONNECTION = VanillaParticle.VAULT_CONNECTION;
    Particle INFESTED = VanillaParticle.INFESTED;
    Particle ITEM_COBWEB = VanillaParticle.ITEM_COBWEB;
    /**
     * Uses {@link BlockData} as DataType.
     */
    Particle DUST_PILLAR = VanillaParticle.DUST_PILLAR;
    /**
     * Uses {@link BlockData} as DataType.
     */
    Particle BLOCK_CRUMBLE = VanillaParticle.BLOCK_CRUMBLE;
    Particle FIREFLY = VanillaParticle.FIREFLY;
    /**
     * Uses {@link Trail} as DataType.
     */
    Particle TRAIL = VanillaParticle.TRAIL;
    Particle OMINOUS_SPAWNING = VanillaParticle.OMINOUS_SPAWNING;
    Particle RAID_OMEN = VanillaParticle.RAID_OMEN;
    Particle TRIAL_OMEN = VanillaParticle.TRIAL_OMEN;
    /**
     * Uses {@link BlockData} as DataType.
     */
    Particle BLOCK_MARKER = VanillaParticle.BLOCK_MARKER;
    Particle COPPER_FIRE_FLAME = VanillaParticle.COPPER_FIRE_FLAME;
    Particle PAUSE_MOB_GROWTH = VanillaParticle.PAUSE_MOB_GROWTH;
    Particle RESET_MOB_GROWTH = VanillaParticle.RESET_MOB_GROWTH;
    Particle NOXIOUS_GAS = VanillaParticle.NOXIOUS_GAS;
    Particle NOXIOUS_GAS_CLOUD = VanillaParticle.NOXIOUS_GAS_CLOUD;
    Particle SULFUR_CUBE_GOO = VanillaParticle.SULFUR_CUBE_GOO;
    Particle SULFUR_BUBBLES = VanillaParticle.SULFUR_BUBBLES;
    /**
     * Uses {@link Geyser} as DataType.
     */
    Particle GEYSER = VanillaParticle.GEYSER;
    /**
     * Uses {@link GeyserBase} as DataType.
     */
    Particle GEYSER_BASE = VanillaParticle.GEYSER_BASE;
    /**
     * Uses {@link Geyser} as DataType.
     */
    Particle GEYSER_PLUME = VanillaParticle.GEYSER_PLUME;
    /**
     * Uses {@link GeyserBase} as DataType.
     */
    Particle GEYSER_POOF = VanillaParticle.GEYSER_POOF;

    /**
     * Returns the required data type for the particle.
     *
     * @return the required data type
     */
    @NotNull
    Class<?> getDataType();

    @NotNull
    @Override
    NamespacedKey getKey();

    /**
     * Creates a {@link com.destroystokyo.paper.ParticleBuilder}.
     *
     * @return a {@link com.destroystokyo.paper.ParticleBuilder} for the particle
     */
    @NotNull
    default com.destroystokyo.paper.ParticleBuilder builder() {
        return new com.destroystokyo.paper.ParticleBuilder(this);
    }

    /**
     * {@code true} when this is a vanilla Minecraft particle constant.
     */
    default boolean isVanilla() {
        return this instanceof VanillaParticle;
    }

    /**
     * {@code true} when this is a registered custom particle (not a vanilla constant).
     */
    default boolean isCustom() {
        return !isVanilla();
    }

    // ---- static lookup (compat with former enum statics; vanilla-only) ----

    /**
     * All <em>vanilla</em> particle constants (not custom registrations). Prefer iterating
     * {@link ParticleRegistry#values()} when custom values should be included.
     *
     * @return an array of all vanilla particle constants
     */
    @NotNull
    static Particle[] values() {
        final VanillaParticle[] vanilla = VanillaParticle.values();
        final Particle[] out = new Particle[vanilla.length];
        System.arraycopy(vanilla, 0, out, 0, vanilla.length);
        return out;
    }

    /**
     * Looks up a <em>vanilla</em> particle by its enum constant name (e.g. {@code "POOF"}).
     * Does not resolve custom particle keys — use {@link ParticleRegistry#get(NamespacedKey)}.
     *
     * @param name the name of the vanilla particle constant
     * @return the matching vanilla particle
     * @throws IllegalArgumentException if no vanilla particle has that name
     */
    @NotNull
    static Particle valueOf(@NotNull final String name) {
        return VanillaParticle.valueOf(name);
    }

    /**
     * Options which can be applied to dust particles - a particle
     * color and size.
     */
    class DustOptions {
        private final Color color;
        private final float size;

        public DustOptions(@NotNull Color color, float size) {
            Preconditions.checkArgument(color != null, "color");
            this.color = color;
            this.size = requireRange(size, "size", 0.01F, 4.0F);
        }

        /**
         * The color of the particles to be displayed.
         *
         * @return particle color
         */
        @NotNull
        public Color getColor() {
            return color;
        }

        /**
         * Relative size of the particle.
         *
         * @return relative particle size
         */
        public float getSize() {
            return size;
        }
    }

    /**
     * Options which can be applied to a color transitioning dust particles.
     */
    class DustTransition extends DustOptions {
        private final Color toColor;

        public DustTransition(@NotNull Color fromColor, @NotNull Color toColor, float size) {
            super(fromColor, size);

            Preconditions.checkArgument(toColor != null, "toColor");
            this.toColor = toColor;
        }

        /**
         * The final of the particles to be displayed.
         *
         * @return final particle color
         */
        @NotNull
        public Color getToColor() {
            return toColor;
        }
    }

    /**
     * Options which can be applied to trail particles - a location, color and duration.
     */
    class Trail {
        private final Location target;
        private final Color color;
        private final int duration;

        public Trail(@NotNull Location target, @NotNull Color color, @Positive int duration) {
            this.target = target;
            this.color = color;
            this.duration = requirePositive(duration, "duration");
        }

        /**
         * The target of the particles to be displayed.
         *
         * @return particle target
         */
        @NotNull
        public Location getTarget() {
            return target;
        }

        /**
         * The color of the particles to be displayed.
         *
         * @return particle color
         */
        @NotNull
        public Color getColor() {
            return color;
        }

        /**
         * The duration of the trail to be displayed.
         *
         * @return trail duration
         */
        public @Positive int getDuration() {
            return duration;
        }
    }

    /**
     * Options which can be applied to effect particles.
     */
    class Spell {
        private final Color color;
        private final float power;

        public Spell(@NotNull Color color, float power) {
            this.color = color;
            this.power = power;
        }

        /**
         * The color of the particles to be displayed.
         *
         * @return particle color
         */
        public @NotNull Color getColor() {
            return color;
        }

        /**
         * The power of the particles to be displayed.
         *
         * @return particle power
         */
        public float getPower() {
            return power;
        }
    }

    /**
     * Options which can be applied to geyser base particles.
     */
    class GeyserBase extends AbstractGeyser {
        private final float burstImpulse;

        public GeyserBase(final int waterBlocks, final float burstImpulse) {
            super(waterBlocks);
            this.burstImpulse = burstImpulse;
        }

        /**
         * {@return the burst impulse}
         */
        public float getBurstImpulse() {
            return this.burstImpulse;
        }
    }

    /**
     * Options which can be applied to geyser particles.
     */
    class Geyser extends AbstractGeyser {
        public Geyser(final int waterBlocks) {
            super(waterBlocks);
        }
    }

    /**
     * Shared base for geyser particle options.
     */
    abstract class AbstractGeyser {
        private final int waterBlocks;

        protected AbstractGeyser(final @Positive int waterBlocks) {
            this.waterBlocks = requirePositive(waterBlocks, "waterBlocks");
        }

        /**
         * The number of water blocks below the geyser
         * which scale the particle size and its burst impulse.
         *
         * @return the number of water blocks
         */
        public @Positive int getWaterBlocks() {
            return waterBlocks;
        }
    }
}