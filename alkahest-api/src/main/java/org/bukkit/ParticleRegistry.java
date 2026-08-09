package org.bukkit;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

/**
 * Static, immutable, atomically published catalog of custom {@link Particle} implementations.
 *
 * <p>Custom particles are registered with a non-{@code minecraft} {@link NamespacedKey} and a data
 * type. Registration publishes a complete new snapshot atomically: readers observe either the
 * previous snapshot or the new one, never a partially built catalog. A failed registration
 * (null key/type, {@code minecraft} namespace, or a key already in use by a vanilla or custom
 * particle) throws and leaves the previous snapshot unchanged.
 *
 * <p>Custom particles have <em>catalog identity only</em>. They are not inserted into any native
 * Minecraft registry and cannot be passed to an NMS particle packet or converted to a native holder;
 * they are intended for API-side use such as {@link com.destroystokyo.paper.ParticleBuilder}.
 */
@NullMarked
public final class ParticleRegistry {

    private static final class Snapshot {
        static final Snapshot EMPTY = new Snapshot(Collections.emptyMap());

        private final Map<NamespacedKey, Particle> byKey;

        Snapshot(final Map<NamespacedKey, Particle> byKey) {
            this.byKey = Collections.unmodifiableMap(byKey);
        }
    }

    private static final AtomicReference<Snapshot> SNAPSHOT = new AtomicReference<>(Snapshot.EMPTY);

    private ParticleRegistry() {
    }

    /**
     * Registers a new custom particle under {@code key} carrying {@code dataType}.
     *
     * <p>The key must be non-null and must not use the {@code minecraft} namespace. The key must not
     * already be registered (custom or vanilla). The data type must be non-null. Publication is
     * atomic; on any validation failure the previously published snapshot is unchanged.
     *
     * @param key the namespaced key for the custom particle
     * @param dataType the data type consumed by the particle
     * @return the immutable custom particle
     * @throws NullPointerException if {@code key} or {@code dataType} is null
     * @throws IllegalArgumentException if the key uses the {@code minecraft} namespace or is already registered
     */
    public static @NotNull Particle register(final @NotNull NamespacedKey key, final @NotNull Class<?> dataType) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(dataType, "dataType");
        Preconditions.checkArgument(!key.getNamespace().equals(NamespacedKey.MINECRAFT),
            "Cannot register a custom particle in the 'minecraft' namespace: " + key);

        final Particle particle = new RegisteredParticle(key, dataType);
        SNAPSHOT.getAndUpdate(snapshot -> {
            Preconditions.checkArgument(!snapshot.byKey.containsKey(particle.getKey()),
                "Particle '" + particle.getKey() + "' is already registered");
            Preconditions.checkArgument(VanillaParticle.fromKey(particle.getKey()) == null,
                "Particle '" + particle.getKey() + "' is a vanilla particle");
            Preconditions.checkArgument(!snapshot.byKey.containsValue(particle),
                "Particle instance is already registered");
            final Map<NamespacedKey, Particle> next = new TreeMap<>(Comparator.comparing(NamespacedKey::toString));
            next.putAll(snapshot.byKey);
            next.put(particle.getKey(), particle);
            return new Snapshot(next);
        });
        return particle;
    }

    /**
     * Gets the registered custom particle for {@code key}, or {@code null} if none.
     *
     * @param key the namespaced key
     * @return the registered custom particle, or {@code null}
     */
    public static @Nullable Particle get(final @Nullable NamespacedKey key) {
        if (key == null) {
            return null;
        }
        return SNAPSHOT.get().byKey.get(key);
    }

    /**
     * Gets the registered custom particle for {@code key}.
     *
     * @param key the namespaced key
     * @return an {@link Optional} containing the registered custom particle, or empty
     */
    public static @NotNull Optional<Particle> getOptional(final @NotNull NamespacedKey key) {
        return Optional.ofNullable(get(key));
    }

    /**
     * All registered custom particles in deterministic order (sorted by full namespaced key).
     * The returned collection is a snapshot and is unmodifiable.
     *
     * @return an unmodifiable snapshot of all custom particles
     */
    public static @NotNull Collection<Particle> values() {
        return SNAPSHOT.get().byKey.values();
    }

    /**
     * Alias for {@link #values()}.
     *
     * @return an unmodifiable snapshot of all custom particles
     */
    public static @NotNull Collection<Particle> all() {
        return values();
    }

    /**
     * Immutable snapshot of all registered custom particles keyed by their {@link NamespacedKey}.
     * The returned map is unmodifiable and reflects a single atomic publication; it is safe for
     * server registry adapters to consume as a live, complete key-to-value view.
     *
     * @return an unmodifiable snapshot map of custom particles
     */
    public static @NotNull Map<NamespacedKey, Particle> asMap() {
        return SNAPSHOT.get().byKey;
    }

    /**
     * The number of registered custom particles.
     *
     * @return current custom particle count
     */
    public static int size() {
        return SNAPSHOT.get().byKey.size();
    }

    /**
     * Clears all registered custom particles, returning the catalog to its empty snapshot.
     */
    public static void reset() {
        SNAPSHOT.set(Snapshot.EMPTY);
    }

    /**
     * Alias for {@link #reset()}.
     */
    public static void clear() {
        reset();
    }

    private static final class RegisteredParticle implements Particle {
        private final NamespacedKey key;
        private final Class<?> dataType;

        RegisteredParticle(final NamespacedKey key, final Class<?> dataType) {
            this.key = key;
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
        public boolean isVanilla() {
            return false;
        }

        @Override
        public boolean isCustom() {
            return true;
        }
    }
}