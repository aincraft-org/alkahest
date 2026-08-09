package org.bukkit.potion;

import java.util.List;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A potion type identified by a namespaced key.
 *
 * <p>Vanilla constants are re-exported from this interface and owned by {@link VanillaPotionType}.
 * Custom values registered through {@link PotionTypeRegistry} have catalog identity only and are
 * not native potion holders. The static enum-style helpers are vanilla-only compatibility methods.
 */
public interface PotionType extends Keyed, io.papermc.paper.world.flag.FeatureDependant {

    // Start generate - PotionType
    PotionType AWKWARD = VanillaPotionType.AWKWARD;
    PotionType FIRE_RESISTANCE = VanillaPotionType.FIRE_RESISTANCE;
    PotionType HARMING = VanillaPotionType.HARMING;
    PotionType HEALING = VanillaPotionType.HEALING;
    PotionType INFESTED = VanillaPotionType.INFESTED;
    PotionType INVISIBILITY = VanillaPotionType.INVISIBILITY;
    PotionType LEAPING = VanillaPotionType.LEAPING;
    PotionType LONG_FIRE_RESISTANCE = VanillaPotionType.LONG_FIRE_RESISTANCE;
    PotionType LONG_INVISIBILITY = VanillaPotionType.LONG_INVISIBILITY;
    PotionType LONG_LEAPING = VanillaPotionType.LONG_LEAPING;
    PotionType LONG_NIGHT_VISION = VanillaPotionType.LONG_NIGHT_VISION;
    PotionType LONG_POISON = VanillaPotionType.LONG_POISON;
    PotionType LONG_REGENERATION = VanillaPotionType.LONG_REGENERATION;
    PotionType LONG_SLOW_FALLING = VanillaPotionType.LONG_SLOW_FALLING;
    PotionType LONG_SLOWNESS = VanillaPotionType.LONG_SLOWNESS;
    PotionType LONG_STRENGTH = VanillaPotionType.LONG_STRENGTH;
    PotionType LONG_SWIFTNESS = VanillaPotionType.LONG_SWIFTNESS;
    PotionType LONG_TURTLE_MASTER = VanillaPotionType.LONG_TURTLE_MASTER;
    PotionType LONG_WATER_BREATHING = VanillaPotionType.LONG_WATER_BREATHING;
    PotionType LONG_WEAKNESS = VanillaPotionType.LONG_WEAKNESS;
    PotionType LUCK = VanillaPotionType.LUCK;
    PotionType MUNDANE = VanillaPotionType.MUNDANE;
    PotionType NIGHT_VISION = VanillaPotionType.NIGHT_VISION;
    PotionType OOZING = VanillaPotionType.OOZING;
    PotionType POISON = VanillaPotionType.POISON;
    PotionType REGENERATION = VanillaPotionType.REGENERATION;
    PotionType SLOW_FALLING = VanillaPotionType.SLOW_FALLING;
    PotionType SLOWNESS = VanillaPotionType.SLOWNESS;
    PotionType STRENGTH = VanillaPotionType.STRENGTH;
    PotionType STRONG_HARMING = VanillaPotionType.STRONG_HARMING;
    PotionType STRONG_HEALING = VanillaPotionType.STRONG_HEALING;
    PotionType STRONG_LEAPING = VanillaPotionType.STRONG_LEAPING;
    PotionType STRONG_POISON = VanillaPotionType.STRONG_POISON;
    PotionType STRONG_REGENERATION = VanillaPotionType.STRONG_REGENERATION;
    PotionType STRONG_SLOWNESS = VanillaPotionType.STRONG_SLOWNESS;
    PotionType STRONG_STRENGTH = VanillaPotionType.STRONG_STRENGTH;
    PotionType STRONG_SWIFTNESS = VanillaPotionType.STRONG_SWIFTNESS;
    PotionType STRONG_TURTLE_MASTER = VanillaPotionType.STRONG_TURTLE_MASTER;
    PotionType SWIFTNESS = VanillaPotionType.SWIFTNESS;
    PotionType THICK = VanillaPotionType.THICK;
    PotionType TURTLE_MASTER = VanillaPotionType.TURTLE_MASTER;
    PotionType WATER = VanillaPotionType.WATER;
    PotionType WATER_BREATHING = VanillaPotionType.WATER_BREATHING;
    PotionType WEAKNESS = VanillaPotionType.WEAKNESS;
    PotionType WEAVING = VanillaPotionType.WEAVING;
    PotionType WIND_CHARGED = VanillaPotionType.WIND_CHARGED;
    // End generate - PotionType

    /** @return the potion effect type of this potion type */
    @Nullable
    @Deprecated(since = "1.20.2")
    PotionEffectType getEffectType();

    /** @return a list of all effects this potion type has */
    @NotNull
    List<PotionEffect> getPotionEffects();

    /** @return whether this potion type is instant */
    @Deprecated(since = "1.20.2")
    boolean isInstant();

    /** @return whether this potion type has an upgraded state */
    boolean isUpgradeable();

    /** @return whether this potion type has an extended state */
    boolean isExtendable();

    /** @return the maximum supported level */
    int getMaxLevel();

    @Override
    @NotNull
    NamespacedKey getKey();

    /** Returns whether this is one of the vanilla enum constants. */
    boolean isVanilla();

    /** Returns whether this is a catalog-backed custom potion type. */
    boolean isCustom();

    /** All vanilla potion constants; custom catalog values are excluded. */
    @NotNull
    static PotionType[] values() {
        final VanillaPotionType[] vanilla = VanillaPotionType.values();
        final PotionType[] result = new PotionType[vanilla.length];
        System.arraycopy(vanilla, 0, result, 0, vanilla.length);
        return result;
    }

    /** Looks up a vanilla potion constant by its enum name. */
    @NotNull
    static PotionType valueOf(@NotNull final String name) {
        return VanillaPotionType.valueOf(name);
    }

    /**
     * @param effectType the effect to get by
     * @return the matching vanilla potion type
     * @deprecated Misleading; potions can have multiple effects.
     */
    @Deprecated(since = "1.9")
    @Nullable
    static PotionType getByEffect(@Nullable final PotionEffectType effectType) {
        if (effectType == null) {
            return WATER;
        }
        for (final VanillaPotionType type : VanillaPotionType.values()) {
            if (effectType.equals(type.getEffectType())) {
                return type;
            }
        }
        return null;
    }

    /**
     * @deprecated Do not use; this interface will be removed in a future major version.
     */
    @Deprecated(since = "1.20.2", forRemoval = true)
    @ApiStatus.Internal
    interface InternalPotionData {
        PotionEffectType getEffectType();
        List<PotionEffect> getPotionEffects();
        boolean isInstant();
        boolean isUpgradeable();
        boolean isExtendable();
        int getMaxLevel();
    }
}
