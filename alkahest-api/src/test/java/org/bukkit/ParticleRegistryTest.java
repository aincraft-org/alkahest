package org.bukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ParticleRegistryTest {

    @AfterEach
    void clearCatalog() {
        ParticleRegistry.reset();
    }

    @Test
    void vanillaCompatibilityHelpersRemainVanillaOnly() {
        assertSame(VanillaParticle.POOF, Particle.POOF);
        assertSame(VanillaParticle.POOF, Particle.valueOf("POOF"));
        assertEquals(VanillaParticle.values().length, Particle.values().length);
        assertTrue(Particle.POOF.isVanilla());
        assertFalse(Particle.POOF.isCustom());
    }

    @Test
    void customParticlePublishesSortedAndWorksWithBuilder() {
        final Particle zebra = ParticleRegistry.register(
            new NamespacedKey("mintychochip", "zebra"), String.class);
        final Particle apple = ParticleRegistry.register(
            new NamespacedKey("mintychochip", "apple"), Integer.class);

        assertTrue(zebra.isCustom());
        assertEquals(String.class, zebra.getDataType());
        assertSame(zebra, ParticleRegistry.get(zebra.getKey()));
        assertSame(zebra, zebra.builder().particle());
        assertEquals(
            List.of("mintychochip:apple", "mintychochip:zebra"),
            ParticleRegistry.values().stream().map(value -> value.getKey().toString()).toList()
        );
        assertEquals(apple, ParticleRegistry.values().stream().toList().get(0));
    }


    @Test
    void customNamespaceMayReuseVanillaPath() {
        final Particle custom = ParticleRegistry.register(
            new NamespacedKey("mintychochip", "poof"), Void.class);

        assertSame(custom, ParticleRegistry.get(custom.getKey()));
        assertFalse(custom.isVanilla());
    }
    @Test
    void failedRegistrationLeavesPublishedSnapshot() {
        final Particle first = ParticleRegistry.register(
            new NamespacedKey("mintychochip", "first"), Void.class);
        assertThrows(IllegalArgumentException.class,
            () -> ParticleRegistry.register(NamespacedKey.minecraft("bad"), Void.class));
        assertThrows(IllegalArgumentException.class,
            () -> ParticleRegistry.register(first.getKey(), Void.class));
        assertEquals(List.of(first), new ArrayList<>(ParticleRegistry.values()));
    }
}
