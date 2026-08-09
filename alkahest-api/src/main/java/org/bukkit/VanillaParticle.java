package org.bukkit;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Vanilla Minecraft particles. The constants are re-exported by {@link Particle} for source
 * compatibility; custom particles are supplied by {@link ParticleRegistry}.
 */
public enum VanillaParticle implements Particle {
    // Start generate - Particle
    POOF("poof"),
    EXPLOSION("explosion"),
    EXPLOSION_EMITTER("explosion_emitter"),
    FIREWORK("firework"),
    BUBBLE("bubble"),
    SPLASH("splash"),
    FISHING("fishing"),
    UNDERWATER("underwater"),
    CRIT("crit"),
    ENCHANTED_HIT("enchanted_hit"),
    SMOKE("smoke"),
    LARGE_SMOKE("large_smoke"),
    /** Uses {@link Particle.Spell} as DataType. */
    EFFECT("effect", Particle.Spell.class),
    /** Uses {@link Particle.Spell} as DataType. */
    INSTANT_EFFECT("instant_effect", Particle.Spell.class),
    /** Uses {@link Color} as DataType (with alpha support). */
    ENTITY_EFFECT("entity_effect", Color.class),
    WITCH("witch"),
    DRIPPING_WATER("dripping_water"),
    DRIPPING_LAVA("dripping_lava"),
    ANGRY_VILLAGER("angry_villager"),
    HAPPY_VILLAGER("happy_villager"),
    MYCELIUM("mycelium"),
    NOTE("note"),
    PORTAL("portal"),
    ENCHANT("enchant"),
    FLAME("flame"),
    LAVA("lava"),
    CLOUD("cloud"),
    /** Uses {@link Particle.DustOptions} as DataType. */
    DUST("dust", Particle.DustOptions.class),
    ITEM_SNOWBALL("item_snowball"),
    ITEM_SLIME("item_slime"),
    HEART("heart"),
    /** Uses {@link org.bukkit.inventory.ItemStack} as DataType. */
    ITEM("item", org.bukkit.inventory.ItemStack.class),
    /** Uses {@link org.bukkit.block.data.BlockData} as DataType. */
    BLOCK("block", org.bukkit.block.data.BlockData.class),
    RAIN("rain"),
    ELDER_GUARDIAN("elder_guardian"),
    /** Uses {@link Float} as DataType, for the power of the breath. */
    DRAGON_BREATH("dragon_breath", Float.class),
    END_ROD("end_rod"),
    DAMAGE_INDICATOR("damage_indicator"),
    SWEEP_ATTACK("sweep_attack"),
    /** Uses {@link org.bukkit.block.data.BlockData} as DataType. */
    FALLING_DUST("falling_dust", org.bukkit.block.data.BlockData.class),
    TOTEM_OF_UNDYING("totem_of_undying"),
    SPIT("spit"),
    SQUID_INK("squid_ink"),
    BUBBLE_POP("bubble_pop"),
    CURRENT_DOWN("current_down"),
    BUBBLE_COLUMN_UP("bubble_column_up"),
    NAUTILUS("nautilus"),
    DOLPHIN("dolphin"),
    SNEEZE("sneeze"),
    CAMPFIRE_COSY_SMOKE("campfire_cosy_smoke"),
    CAMPFIRE_SIGNAL_SMOKE("campfire_signal_smoke"),
    COMPOSTER("composter"),
    /** Uses {@link Color} as DataType. */
    FLASH("flash", Color.class),
    FALLING_LAVA("falling_lava"),
    LANDING_LAVA("landing_lava"),
    FALLING_WATER("falling_water"),
    DRIPPING_HONEY("dripping_honey"),
    FALLING_HONEY("falling_honey"),
    LANDING_HONEY("landing_honey"),
    FALLING_NECTAR("falling_nectar"),
    SOUL_FIRE_FLAME("soul_fire_flame"),
    ASH("ash"),
    CRIMSON_SPORE("crimson_spore"),
    WARPED_SPORE("warped_spore"),
    SOUL("soul"),
    DRIPPING_OBSIDIAN_TEAR("dripping_obsidian_tear"),
    FALLING_OBSIDIAN_TEAR("falling_obsidian_tear"),
    LANDING_OBSIDIAN_TEAR("landing_obsidian_tear"),
    REVERSE_PORTAL("reverse_portal"),
    WHITE_ASH("white_ash"),
    /** Uses {@link Particle.DustTransition} as DataType. */
    DUST_COLOR_TRANSITION("dust_color_transition", Particle.DustTransition.class),
    /** Uses {@link Vibration} as DataType. */
    VIBRATION("vibration", Vibration.class),
    FALLING_SPORE_BLOSSOM("falling_spore_blossom"),
    SPORE_BLOSSOM_AIR("spore_blossom_air"),
    SMALL_FLAME("small_flame"),
    SNOWFLAKE("snowflake"),
    DRIPPING_DRIPSTONE_LAVA("dripping_dripstone_lava"),
    FALLING_DRIPSTONE_LAVA("falling_dripstone_lava"),
    DRIPPING_DRIPSTONE_WATER("dripping_dripstone_water"),
    FALLING_DRIPSTONE_WATER("falling_dripstone_water"),
    GLOW_SQUID_INK("glow_squid_ink"),
    GLOW("glow"),
    WAX_ON("wax_on"),
    WAX_OFF("wax_off"),
    ELECTRIC_SPARK("electric_spark"),
    SCRAPE("scrape"),
    SONIC_BOOM("sonic_boom"),
    SCULK_SOUL("sculk_soul"),
    /** Uses {@link Float} as DataType, the angle in radians. */
    SCULK_CHARGE("sculk_charge", Float.class),
    SCULK_CHARGE_POP("sculk_charge_pop"),
    /** Uses {@link Integer} as DataType. */
    SHRIEK("shriek", Integer.class),
    CHERRY_LEAVES("cherry_leaves"),
    PALE_OAK_LEAVES("pale_oak_leaves"),
    /** Uses {@link Color} as DataType. */
    TINTED_LEAVES("tinted_leaves", Color.class),
    EGG_CRACK("egg_crack"),
    DUST_PLUME("dust_plume"),
    WHITE_SMOKE("white_smoke"),
    GUST("gust"),
    SMALL_GUST("small_gust"),
    GUST_EMITTER_LARGE("gust_emitter_large"),
    GUST_EMITTER_SMALL("gust_emitter_small"),
    TRIAL_SPAWNER_DETECTION("trial_spawner_detection"),
    TRIAL_SPAWNER_DETECTION_OMINOUS("trial_spawner_detection_ominous"),
    VAULT_CONNECTION("vault_connection"),
    INFESTED("infested"),
    ITEM_COBWEB("item_cobweb"),
    /** Uses {@link org.bukkit.block.data.BlockData} as DataType. */
    DUST_PILLAR("dust_pillar", org.bukkit.block.data.BlockData.class),
    /** Uses {@link org.bukkit.block.data.BlockData} as DataType. */
    BLOCK_CRUMBLE("block_crumble", org.bukkit.block.data.BlockData.class),
    FIREFLY("firefly"),
    /** Uses {@link Particle.Trail} as DataType. */
    TRAIL("trail", Particle.Trail.class),
    OMINOUS_SPAWNING("ominous_spawning"),
    RAID_OMEN("raid_omen"),
    TRIAL_OMEN("trial_omen"),
    /** Uses {@link org.bukkit.block.data.BlockData} as DataType. */
    BLOCK_MARKER("block_marker", org.bukkit.block.data.BlockData.class),
    COPPER_FIRE_FLAME("copper_fire_flame"),
    PAUSE_MOB_GROWTH("pause_mob_growth"),
    RESET_MOB_GROWTH("reset_mob_growth"),
    NOXIOUS_GAS("noxious_gas"),
    NOXIOUS_GAS_CLOUD("noxious_gas_cloud"),
    SULFUR_CUBE_GOO("sulfur_cube_goo"),
    SULFUR_BUBBLES("sulfur_bubbles"),
    /** Uses {@link Particle.Geyser} as DataType. */
    GEYSER("geyser", Particle.Geyser.class),
    /** Uses {@link Particle.GeyserBase} as DataType. */
    GEYSER_BASE("geyser_base", Particle.GeyserBase.class),
    /** Uses {@link Particle.Geyser} as DataType. */
    GEYSER_PLUME("geyser_plume", Particle.Geyser.class),
    /** Uses {@link Particle.GeyserBase} as DataType. */
    GEYSER_POOF("geyser_poof", Particle.GeyserBase.class);
    // End generate - Particle

    private static final Map<String, VanillaParticle> BY_PARTICLE_NAME = new HashMap<>();

    static {
        for (final VanillaParticle particle : values()) {
            BY_PARTICLE_NAME.put(particle.key.getKey(), particle);
        }
    }

    private final NamespacedKey key;
    private final Class<?> dataType;

    VanillaParticle(final String key) {
        this(key, Void.class);
    }

    VanillaParticle(final String key, final Class<?> dataType) {
        this.key = NamespacedKey.minecraft(key);
        this.dataType = dataType;
    }

    @Override
    public @NotNull Class<?> getDataType() {
        return this.dataType;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return this.key;
    }

    @Override
    public @NotNull com.destroystokyo.paper.ParticleBuilder builder() {
        return new com.destroystokyo.paper.ParticleBuilder(this);
    }

    @Override
    public boolean isVanilla() {
        return true;
    }

    @Override
    public boolean isCustom() {
        return false;
    }

    /**
     * Resolves a vanilla particle by its {@link NamespacedKey}, or {@code null} if none matches.
     *
     * @param key the namespaced key
     * @return the matching vanilla particle, or {@code null}
     */
    public static @Nullable VanillaParticle fromKey(final @Nullable NamespacedKey key) {
        if (key == null || !NamespacedKey.MINECRAFT.equals(key.getNamespace())) {
            return null;
        }
        return BY_PARTICLE_NAME.get(key.getKey());
    }
}
