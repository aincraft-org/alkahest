package org.bukkit;

import dev.mintychochip.customentity.CustomEntities;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.VanillaEntityType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

/**
 * Bukkit {@link Registry} for {@link EntityType}: non-{@link VanillaEntityType#UNKNOWN}
 * vanilla constants plus registered custom types
 * ({@link dev.mintychochip.customentity.CustomEntityDefinition}).
 *
 * <p>mintychochip — backs {@link Registry#ENTITY_TYPE} after EntityType became an interface.
 * Vanilla values come from a {@link SimpleRegistry} of {@link VanillaEntityType}; custom values
 * come from {@link CustomEntities} and are published by its atomic catalog.
 *
 * <p>Tags are unsupported on this API-side registry. The server may install a tag-aware
 * façade for {@link io.papermc.paper.registry.RegistryKey#ENTITY_TYPE} via RegistryAccess.
 */
@ApiStatus.Internal
@NullMarked
public class EntityTypeRegistry extends Registry.NotARegistry<EntityType> {

    private final Registry<VanillaEntityType> vanilla;

    public EntityTypeRegistry(final Registry<VanillaEntityType> vanilla) {
        this.vanilla = vanilla;
    }

    /** Vanilla-only view (excludes customs). Used by tag-aware server wrappers. */
    public final Registry<VanillaEntityType> vanilla() {
        return this.vanilla;
    }

    @Override
    public @Nullable EntityType get(final NamespacedKey key) {
        final VanillaEntityType value = this.vanilla.get(key);
        if (value != null) {
            return value;
        }
        return CustomEntities.get(key).orElse(null);
    }

    @Override
    public @NotNull Iterator<EntityType> iterator() {
        final Collection<dev.mintychochip.customentity.CustomEntityDefinition> custom = CustomEntities.all();
        final List<EntityType> all = new ArrayList<>(this.vanilla.size() + custom.size());
        for (final VanillaEntityType value : this.vanilla) {
            all.add(value);
        }
        all.addAll(custom);
        return all.iterator();
    }

    @Override
    public int size() {
        return this.vanilla.size() + CustomEntities.all().size();
    }

    @Override
    public Stream<NamespacedKey> keyStream() {
        return StreamSupport.stream(this.spliterator(), false).map(Keyed::getKey);
    }

    /** Returns whether the value is the exact object in the native entity registry view. */
    public boolean isNative(final EntityType value) {
        return value != null && this.vanilla.get(value.getKey()) == value;
    }

    /** Returns whether the value is the exact object in the current custom entity catalog. */
    public boolean isCatalog(final EntityType value) {
        return value != null && CustomEntities.get(value.getKey()).orElse(null) == value;
    }
}
