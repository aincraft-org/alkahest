package io.papermc.paper.registry;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.VanillaEntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

/**
 * Server tag-aware view of entity types: delegates get/iteration to the API
 * {@link org.bukkit.EntityTypeRegistry} (vanilla + {@code CustomEntities}), and tags to the
 * NMS-backed vanilla simple registry.
 *
 * <p>mintychochip — used by {@link PaperSimpleRegistry#entityType()} for
 * {@link RegistryKey#ENTITY_TYPE} / RegistryAccess. {@link Registry#ENTITY_TYPE} uses the
 * API registry directly (same merge, no tags).
 */
@NullMarked
final class EntityTypeRegistry extends Registry.NotARegistry<EntityType> {

    private final org.bukkit.EntityTypeRegistry merged;
    private final Registry<VanillaEntityType> vanillaForTags;

    EntityTypeRegistry(
        final org.bukkit.EntityTypeRegistry merged,
        final Registry<VanillaEntityType> vanillaForTags
    ) {
        this.merged = merged;
        this.vanillaForTags = vanillaForTags;
    }

    @Override
    public @Nullable EntityType get(final NamespacedKey key) {
        return this.merged.get(key);
    }

    @Override
    public @NotNull Iterator<EntityType> iterator() {
        return this.merged.iterator();
    }

    @Override
    public int size() {
        return this.merged.size();
    }

    @Override
    public Stream<NamespacedKey> keyStream() {
        return this.merged.keyStream();
    }

    @Override
    public boolean hasTag(final io.papermc.paper.registry.tag.TagKey<EntityType> key) {
        return this.vanillaForTags.hasTag((io.papermc.paper.registry.tag.TagKey) key);
    }

    @Override
    public io.papermc.paper.registry.tag.Tag<EntityType> getTag(final io.papermc.paper.registry.tag.TagKey<EntityType> key) {
        // Tags are vanilla-only for now
        return (io.papermc.paper.registry.tag.Tag<EntityType>) (io.papermc.paper.registry.tag.Tag<?>) this.vanillaForTags.getTag((io.papermc.paper.registry.tag.TagKey) key);
    }

    @Override
    public Collection<io.papermc.paper.registry.tag.Tag<EntityType>> getTags() {
        return (Collection) this.vanillaForTags.getTags();
    }
}
