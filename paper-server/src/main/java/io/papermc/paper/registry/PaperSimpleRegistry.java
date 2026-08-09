package io.papermc.paper.registry;

import io.papermc.paper.registry.set.NamedRegistryKeySetImpl;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import java.util.Collection;
import java.util.function.Predicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import org.bukkit.Keyed;
import org.bukkit.Particle;
import org.bukkit.ParticleRegistry;
import org.bukkit.Registry;
import org.bukkit.VanillaParticle;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionType;
import org.bukkit.potion.PotionTypeRegistry;
import org.bukkit.potion.VanillaPotionType;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PaperSimpleRegistry<T extends Enum<T> & Keyed, M> extends Registry.SimpleRegistry<T> {

    static Registry<EntityType> entityType() {
        // mintychochip - EntityType is an interface; vanilla constants live on VanillaEntityType.
        // The shared adapter merges the atomic CustomEntities snapshot and delegates tags to vanilla.
        final PaperSimpleRegistry<org.bukkit.entity.VanillaEntityType, net.minecraft.world.entity.EntityType<?>> vanilla =
            new PaperSimpleRegistry<>(org.bukkit.entity.VanillaEntityType.class, entity -> entity != org.bukkit.entity.VanillaEntityType.UNKNOWN, BuiltInRegistries.ENTITY_TYPE);
        return new EntityTypeRegistry(vanilla);
    }

    static Registry<Particle> particleType() {
        final PaperSimpleRegistry<VanillaParticle, net.minecraft.core.particles.ParticleType<?>> vanilla =
            new PaperSimpleRegistry<>(VanillaParticle.class, BuiltInRegistries.PARTICLE_TYPE);
        return new PaperCatalogRegistry<>(() -> vanilla, ParticleRegistry::asMap);
    }

    static Registry<PotionType> potion() {
        final PaperSimpleRegistry<VanillaPotionType, net.minecraft.world.item.alchemy.Potion> vanilla =
            new PaperSimpleRegistry<>(VanillaPotionType.class, BuiltInRegistries.POTION);
        return new PaperCatalogRegistry<>(() -> vanilla, PotionTypeRegistry::asMap);
    }

    private final net.minecraft.core.Registry<M> nmsRegistry;

    protected PaperSimpleRegistry(final Class<T> type, final net.minecraft.core.Registry<M> nmsRegistry) {
        super(type);
        this.nmsRegistry = nmsRegistry;
    }

    public PaperSimpleRegistry(final Class<T> type, final Predicate<T> predicate, final net.minecraft.core.Registry<M> nmsRegistry) {
        super(type, predicate);
        this.nmsRegistry = nmsRegistry;
    }

    @Override
    public boolean hasTag(final TagKey<T> key) {
        final net.minecraft.tags.TagKey<M> nmsKey = PaperRegistries.toNms(key);
        return this.nmsRegistry.get(nmsKey).isPresent();
    }

    @Override
    public Tag<T> getTag(final TagKey<T> key) {
        final HolderSet.Named<M> namedHolderSet = this.nmsRegistry.get(PaperRegistries.toNms(key)).orElseThrow();
        return new NamedRegistryKeySetImpl<>(key, namedHolderSet);
    }

    @Override
    public Collection<Tag<T>> getTags() {
        return this.nmsRegistry.getTags().<Tag<T>>map(NamedRegistryKeySetImpl::new).toList();
    }
}
