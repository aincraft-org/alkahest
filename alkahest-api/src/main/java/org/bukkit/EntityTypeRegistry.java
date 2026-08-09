package org.bukkit;

import dev.mintychochip.customentity.CustomEntities;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
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
 * Mirrors {@link MaterialRegistry}: vanilla from a {@link SimpleRegistry} of
 * {@link VanillaEntityType}, customs from {@link CustomEntities}.
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
        final VanillaEntityType v = this.vanilla.get(key);
        if (v != null) {
            return v;
        }
        return CustomEntities.get(key).orElse(null);
    }

    @Override
    public @NotNull Iterator<EntityType> iterator() {
        final Set<EntityType> all = new LinkedHashSet<>();
        for (final VanillaEntityType v : this.vanilla) {
            all.add(v);
        }
        all.addAll(CustomEntities.all());
        return all.iterator();
    }

    @Override
    public int size() {
        return this.vanilla.size() + CustomEntities.catalog().size();
    }

    @Override
    public Stream<NamespacedKey> keyStream() {
        return StreamSupport.stream(this.spliterator(), false).map(Keyed::getKey);
    }
}
