package org.bukkit.potion;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.FeatureFlag;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

/**
 * Atomic catalog for logical custom {@link PotionType} values.
 *
 * <p>Custom potions are never inserted into Minecraft's native potion registry and therefore
 * cannot be converted to native potion holders or serialized as native potion contents.
 */
@NullMarked
public final class PotionTypeRegistry {

    private static final Comparator<PotionType> KEY_ORDER =
        Comparator.comparing(value -> value.getKey().toString());
    private static final AtomicReference<Snapshot> SNAPSHOT = new AtomicReference<>(Snapshot.empty());

    private PotionTypeRegistry() {
    }

    /** Starts a typed custom potion registration. */
    public static @NotNull Builder builder(@NotNull final NamespacedKey key) {
        return new Builder(key);
    }

    /** Registers a custom potion with immutable effect and feature metadata. */
    public static @NotNull PotionType register(
        @NotNull final NamespacedKey key,
        @NotNull final List<PotionEffect> effects,
        final boolean upgradeable,
        final boolean extendable,
        final int maxLevel,
        @NotNull final Set<FeatureFlag> requiredFeatures
    ) {
        return builder(key)
            .effects(effects)
            .upgradeable(upgradeable)
            .extendable(extendable)
            .maxLevel(maxLevel)
            .requiredFeatures(requiredFeatures)
            .build();
    }

    public static @Nullable PotionType get(@Nullable final NamespacedKey key) {
        return key == null ? null : SNAPSHOT.get().byKey().get(key);
    }

    public static @NotNull List<PotionType> values() {
        return SNAPSHOT.get().values();
    }

    public static @NotNull List<PotionType> all() {
        return values();
    }

    public static @NotNull Map<NamespacedKey, PotionType> asMap() {
        return SNAPSHOT.get().byKey();
    }

    public static int size() {
        return SNAPSHOT.get().values().size();
    }

    public static void reset() {
        SNAPSHOT.set(Snapshot.empty());
    }

    public static void clear() {
        reset();
    }

    public static final class Builder {
        private final NamespacedKey key;
        private List<PotionEffect> effects = List.of();
        private boolean upgradeable;
        private boolean extendable;
        private int maxLevel = 1;
        private Set<FeatureFlag> requiredFeatures = Set.of();

        private Builder(final NamespacedKey key) {
            this.key = Objects.requireNonNull(key, "key");
        }

        public @NotNull Builder effects(@NotNull final List<PotionEffect> effects) {
            this.effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
            return this;
        }

        public @NotNull Builder upgradeable(final boolean upgradeable) {
            this.upgradeable = upgradeable;
            return this;
        }

        public @NotNull Builder extendable(final boolean extendable) {
            this.extendable = extendable;
            return this;
        }

        public @NotNull Builder maxLevel(final int maxLevel) {
            this.maxLevel = maxLevel;
            return this;
        }

        public @NotNull Builder requiredFeatures(@NotNull final Set<FeatureFlag> requiredFeatures) {
            this.requiredFeatures = Set.copyOf(Objects.requireNonNull(requiredFeatures, "requiredFeatures"));
            return this;
        }

        public @NotNull PotionType build() {
            if (NamespacedKey.MINECRAFT.equals(this.key.getNamespace())) {
                throw new IllegalArgumentException("custom potion key must not use the minecraft namespace: " + this.key);
            }
            if (VanillaPotionType.fromKey(this.key) != null) {
                throw new IllegalStateException("potion key collides with a vanilla key: " + this.key);
            }
            if (this.maxLevel < 1) {
                throw new IllegalArgumentException("maxLevel must be at least 1");
            }
            for (final PotionEffect effect : this.effects) {
                Objects.requireNonNull(effect, "effects cannot contain null");
            }
            final PotionType value = new RegisteredPotionType(
                this.key,
                this.effects,
                this.upgradeable,
                this.extendable,
                this.maxLevel,
                this.requiredFeatures
            );
            SNAPSHOT.updateAndGet(current -> {
                if (current.byKey().containsKey(this.key)) {
                    throw new IllegalStateException("potion type already registered: " + this.key);
                }
                final Map<NamespacedKey, PotionType> next = new TreeMap<>(Comparator.comparing(NamespacedKey::toString));
                next.putAll(current.byKey());
                next.put(this.key, value);
                return Snapshot.of(next);
            });
            return value;
        }
    }

    private static final class RegisteredPotionType implements PotionType {
        private final NamespacedKey key;
        private final List<PotionEffect> effects;
        private final boolean upgradeable;
        private final boolean extendable;
        private final int maxLevel;
        private final Set<FeatureFlag> requiredFeatures;

        private RegisteredPotionType(
            final NamespacedKey key,
            final List<PotionEffect> effects,
            final boolean upgradeable,
            final boolean extendable,
            final int maxLevel,
            final Set<FeatureFlag> requiredFeatures
        ) {
            this.key = key;
            this.effects = List.copyOf(effects);
            this.upgradeable = upgradeable;
            this.extendable = extendable;
            this.maxLevel = maxLevel;
            this.requiredFeatures = Set.copyOf(requiredFeatures);
        }

        @Override
        public @Nullable PotionEffectType getEffectType() {
            return this.effects.isEmpty() ? null : this.effects.get(0).getType();
        }

        @Override
        public @NotNull List<PotionEffect> getPotionEffects() {
            return this.effects;
        }

        @Override
        public boolean isInstant() {
            return this.effects.stream().anyMatch(effect -> effect.getType().isInstant());
        }

        @Override
        public boolean isUpgradeable() {
            return this.upgradeable;
        }

        @Override
        public boolean isExtendable() {
            return this.extendable;
        }

        @Override
        public int getMaxLevel() {
            return this.maxLevel;
        }

        @Override
        public @NotNull NamespacedKey getKey() {
            return this.key;
        }

        @Override
        public @NotNull Set<FeatureFlag> requiredFeatures() {
            return this.requiredFeatures;
        }

        @Override
        public boolean isVanilla() {
            return false;
        }

        @Override
        public boolean isCustom() {
            return true;
        }
    }

    private record Snapshot(List<PotionType> values, Map<NamespacedKey, PotionType> byKey) {
        private static Snapshot empty() {
            return new Snapshot(List.of(), Map.of());
        }

        private static Snapshot of(final Map<NamespacedKey, PotionType> values) {
            final LinkedHashMap<NamespacedKey, PotionType> ordered = new LinkedHashMap<>(values);
            return new Snapshot(
                List.copyOf(ordered.values()),
                Collections.unmodifiableMap(ordered)
            );
        }
    }
}
