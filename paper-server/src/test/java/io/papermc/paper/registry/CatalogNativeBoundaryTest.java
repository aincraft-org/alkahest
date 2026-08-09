package io.papermc.paper.registry;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.customblock.CustomBlockDefinition;
import dev.mintychochip.customblock.CustomBlocks;
import dev.mintychochip.customblock.PacketHostSpec;
import java.util.List;
import java.util.Set;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.ParticleRegistry;
import org.bukkit.Registry;
import org.bukkit.craftbukkit.CraftParticle;
import org.bukkit.craftbukkit.block.CraftBlockType;
import org.bukkit.craftbukkit.entity.memory.CraftMemoryKey;
import org.bukkit.craftbukkit.inventory.CraftItemType;
import org.bukkit.craftbukkit.potion.CraftPotionType;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.entity.memory.MemoryKeyRegistry;
import org.bukkit.potion.PotionType;
import org.bukkit.potion.PotionTypeRegistry;
import org.bukkit.potion.VanillaPotionType;
import org.bukkit.support.environment.AllFeatures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@AllFeatures
class CatalogNativeBoundaryTest {

    @AfterEach
    void clearCatalogs() {
        ParticleRegistry.reset();
        PotionTypeRegistry.reset();
        MemoryKeyRegistry.clear();
        CustomBlocks.reset();
    }

    @Test
    void catalogValuesAreLiveThroughServerRegistryViews() {
        final Particle particle = ParticleRegistry.register(
            new NamespacedKey("mintychochip", "live_particle"), Void.class);
        final PotionType potion = PotionTypeRegistry.register(
            new NamespacedKey("mintychochip", "live_potion"), List.of(), false, false, 1, Set.of());
        final MemoryKey<String> memory = MemoryKeyRegistry.create(
            new NamespacedKey("mintychochip", "live_memory"), String.class);

        final Registry<Particle> particles = RegistryAccess.registryAccess().getRegistry(RegistryKey.PARTICLE_TYPE);
        final Registry<PotionType> potions = RegistryAccess.registryAccess().getRegistry(RegistryKey.POTION);
        final Registry<MemoryKey<?>> memories = RegistryAccess.registryAccess().getRegistry(RegistryKey.MEMORY_MODULE_TYPE);

        assertSame(particle, particles.get(particle.getKey()));
        assertSame(PotionType.WATER, potions.get(VanillaPotionType.WATER.getKey()));
        assertSame(potion, potions.get(potion.getKey()));
        assertSame(memory, memories.get(memory.getKey()));
        assertTrue(particles instanceof PaperCatalogRegistry<?>);
        assertTrue(potions instanceof PaperCatalogRegistry<?>);
    }

    @Test
    void customParticleIsRejectedByNativeConverter() {
        final Particle custom = ParticleRegistry.register(
            new NamespacedKey("mintychochip", "native_reject"), Void.class);
        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> CraftParticle.bukkitToMinecraft(custom));
        assertTrue(exception.getMessage().contains(custom.getKey().toString()));
    }

    @Test
    void customPotionIsRejectedByNativeConverter() {
        final PotionType custom = PotionTypeRegistry.register(
            new NamespacedKey("mintychochip", "native_reject"), List.of(), false, false, 1, Set.of());
        final IllegalArgumentException holderException = assertThrows(
            IllegalArgumentException.class, () -> CraftPotionType.bukkitToMinecraft(custom));
        final IllegalArgumentException serializationException = assertThrows(
            IllegalArgumentException.class, () -> CraftPotionType.bukkitToString(custom));
        assertTrue(holderException.getMessage().contains(custom.getKey().toString()));
        assertTrue(serializationException.getMessage().contains(custom.getKey().toString()));
    }

    @Test
    void customMemoryKeyIsRejectedByNativeConverter() {
        final MemoryKey<String> custom = MemoryKeyRegistry.create(
            new NamespacedKey("mintychochip", "native_reject"), String.class);
        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> CraftMemoryKey.bukkitToMinecraft(custom));
        assertTrue(exception.getMessage().contains(custom.getKey().toString()));
    }

    @Test
    void customMaterialIsRejectedByNativeConverters() {
        final CustomBlockDefinition custom = CustomBlockDefinition.builder("mintychochip:native_reject")
            .host(PacketHostSpec.defaults())
            .build();
        CustomBlocks.register(custom);

        final IllegalArgumentException blockException = assertThrows(
            IllegalArgumentException.class, () -> CraftBlockType.bukkitToMinecraft(custom));
        final IllegalArgumentException itemException = assertThrows(
            IllegalArgumentException.class, () -> CraftItemType.bukkitToMinecraft(custom));
        assertTrue(blockException.getMessage().contains(custom.getKey().toString()));
        assertTrue(itemException.getMessage().contains(custom.getKey().toString()));
    }
}
