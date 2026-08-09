package org.bukkit;

import dev.mintychochip.customblock.CustomBlocks;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

/**
 * Bukkit {@link Registry} for {@link Material}: non-legacy vanilla constants plus registered
 * custom materials ({@link dev.mintychochip.customblock.CustomBlockDefinition}).
 *
 * <p>mintychochip — backs {@link Registry#MATERIAL} after Material became an interface.
 * Mirrors {@link EntityTypeRegistry}: vanilla from a {@link SimpleRegistry} of
 * {@link VanillaMaterial}, customs from {@link CustomBlocks}.
 *
 * <p>Tags are unsupported (same as the former vanilla-only {@link SimpleRegistry} for materials).
 */
@ApiStatus.Internal
@NullMarked
public final class MaterialRegistry extends Registry.NotARegistry<Material> {

    private final Registry<VanillaMaterial> vanilla;

    public MaterialRegistry(final Registry<VanillaMaterial> vanilla) {
        this.vanilla = vanilla;
    }

    @Override
    public @Nullable Material get(final NamespacedKey key) {
        final VanillaMaterial v = this.vanilla.get(key);
        if (v != null) {
            return v;
        }
        return CustomBlocks.get(key).orElse(null);
    }

    @Override
    public @NotNull Iterator<Material> iterator() {
        final Collection<dev.mintychochip.customblock.CustomBlockDefinition> custom = CustomBlocks.all();
        final List<Material> all = new ArrayList<>(this.vanilla.size() + custom.size());
        for (final VanillaMaterial v : this.vanilla) {
            all.add(v);
        }
        all.addAll(custom);
        return all.iterator();
    }

    @Override
    public int size() {
        return this.vanilla.size() + CustomBlocks.all().size();
    }

    @Override
    public Stream<NamespacedKey> keyStream() {
        return StreamSupport.stream(this.spliterator(), false).map(Keyed::getKey);
    }

    /** Returns whether the value is the exact object in the native material registry. */
    public boolean isNative(final Material value) {
        return value != null && this.vanilla.get(value.getKey()) == value;
    }

    /** Returns whether the value is the exact object in the current custom-block catalog. */
    public boolean isCatalog(final Material value) {
        return value != null && CustomBlocks.get(value.getKey()).orElse(null) == value;
    }
}
