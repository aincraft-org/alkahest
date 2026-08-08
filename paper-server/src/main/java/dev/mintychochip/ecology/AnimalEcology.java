package dev.mintychochip.ecology;

import dev.mintychochip.season.Season;
import dev.mintychochip.season.Seasons;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import org.jspecify.annotations.Nullable;

/**
 * Server façade for ecology animal selection. Samples climate + season and
 * picks a catalog species for CREATURE natural spawn attempts.
 */
public final class AnimalEcology {
    private static final Random RNG = new Random();

    private AnimalEcology() {
    }

    public static boolean isEnabled(final ServerLevel level) {
        EcologyConfig.ensureLoaded(level.getServer().getServerDirectory());
        return EcologyConfig.get().animalsEnabled();
    }

    /**
     * If animals ecology is enabled and a catalog candidate scores &gt; 0 for
     * this position, returns that entity type; otherwise empty (caller may
     * keep vanilla type or skip).
     */
    public static Optional<EntityType<? extends Mob>> pickReplacement(
        final ServerLevel level,
        final BlockPos pos
    ) {
        if (!isEnabled(level)) {
            return Optional.empty();
        }
        final ClimateSample climate = CropEcology.sampleClimate(level, pos);
        final Season season = Seasons.current();
        final Optional<AnimalProfile> profile = AnimalSelector.select(
            EcologyConfig.get().animalCatalog(),
            climate,
            season,
            RNG
        );
        if (profile.isEmpty()) {
            return Optional.empty();
        }
        return resolveMobType(profile.get().target());
    }

    /**
     * For a vanilla CREATURE natural attempt: if ecology selects a species,
     * return it; if ecology is enabled but no candidate fits, return empty so
     * the attempt can be skipped (do not invent density). If ecology is
     * disabled, return empty and leave vanilla type alone (caller checks enabled).
     */
    public static @Nullable EntityType<? extends Mob> replaceCreatureType(
        final ServerLevel level,
        final BlockPos pos,
        final EntityType<?> vanillaType
    ) {
        if (!isEnabled(level)) {
            return null;
        }
        // Only manage passive land animals; hostiles never go through CREATURE.
        final Optional<EntityType<? extends Mob>> pick = pickReplacement(level, pos);
        return pick.orElse(null);
    }

    public static boolean shouldManageCategory(final MobCategory category) {
        return category == MobCategory.CREATURE;
    }

    @SuppressWarnings("unchecked")
    private static Optional<EntityType<? extends Mob>> resolveMobType(final String target) {
        final Identifier id = Identifier.tryParse(target);
        if (id == null) {
            return Optional.empty();
        }
        final Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
        if (type.isEmpty()) {
            return Optional.empty();
        }
        final EntityType<?> t = type.get();
        if (t.getCategory() != MobCategory.CREATURE && t.getCategory() != MobCategory.AMBIENT) {
            // Still allow catalog types that register as CREATURE-like; polar bear etc. are CREATURE
        }
        try {
            return Optional.of((EntityType<? extends Mob>) t);
        } catch (final ClassCastException ex) {
            return Optional.empty();
        }
    }

    /** Entity registry key string for tests / debug. */
    public static String keyOf(final EntityType<?> type) {
        final Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id == null ? type.toString().toLowerCase(Locale.ROOT) : id.toString();
    }
}
