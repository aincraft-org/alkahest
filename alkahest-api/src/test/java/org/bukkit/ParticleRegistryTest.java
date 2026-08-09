package org.bukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.destroystokyo.paper.ParticleBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the atomic custom {@link Particle} catalog and the vanilla interface migration.
 */
public class ParticleRegistryTest {

    @BeforeEach
    public void setUp() {
        ParticleRegistry.reset();
    }

    @AfterEach
    public void tearDown() {
        ParticleRegistry.reset();
    }

    @Test
    public void vanillaConstantsPreserved() {
        // The full constant set is re-exported from the interface as vanilla instances.
        assertSame(VanillaParticle.POOF, Particle.POOF);
        assertSame(VanillaParticle.DUST, Particle.DUST);
        assertSame(VanillaParticle.GEYSER_PLUME, Particle.GEYSER_PLUME);
        assertTrue(Particle.POOF.isVanilla());
        assertFalse(Particle.POOF.isCustom());
        assertTrue(Particle.values().length > 0);
        assertSame(Particle.POOF, Particle.valueOf("POOF"));
    }

    @Test
    public void customParticleExposesKeyAndDataType() {
        final Particle custom = ParticleRegistry.register(
            new NamespacedKey("mintychochip", "custom"), Integer.class);
        assertFalse(custom.isVanilla());
        assertTrue(custom.isCustom());
        assertEquals("custom", custom.getKey().getKey());
        assertEquals("mintychochip", custom.getKey().getNamespace());
        assertEquals(Integer.class, custom.getDataType());
    }

    @Test
    public void customParticleWorksWithBuilder() {
        final Particle custom = ParticleRegistry.register(
            new NamespacedKey("mintychochip", "builder"), Particle.DustOptions.class);
        final ParticleBuilder builder = custom.builder();
        assertNotNull(builder);
        assertSame(custom, builder.particle());
    }

    @Test
    public void customValuesSnapshotIsDeterministic() {
        final Particle a = ParticleRegistry.register(new NamespacedKey("mintychochip", "zeta"), Void.class);
        final Particle b = ParticleRegistry.register(new NamespacedKey("mintychochip", "alpha"), Void.class);
        final Particle c = ParticleRegistry.register(new NamespacedKey("z", "mid"), Void.class);

        final List<Particle> first = new ArrayList<>(ParticleRegistry.values());
        final List<Particle> second = new ArrayList<>(ParticleRegistry.values());
        assertEquals(first, second, "custom snapshot must be deterministic");
        assertEquals(List.of(b, c, a), first, "custom values must be sorted by full namespaced key");
        assertEquals(3, ParticleRegistry.size());
    }

    @Test
    public void asMapReflectsAtomicSnapshot() {
        final Particle custom = ParticleRegistry.register(new NamespacedKey("mintychochip", "asmap"), Void.class);
        assertSame(custom, ParticleRegistry.asMap().get(custom.getKey()));
        assertEquals(1, ParticleRegistry.asMap().size());
        assertThrows(UnsupportedOperationException.class,
            () -> ParticleRegistry.asMap().put(new NamespacedKey("x", "y"), custom));
    }

    @Test
    public void duplicateKeyRejectedBeforePublication() {
        final NamespacedKey key = new NamespacedKey("mintychochip", "dup");
        final Particle first = ParticleRegistry.register(key, Void.class);
        assertThrows(IllegalArgumentException.class, () -> ParticleRegistry.register(key, Void.class));
        assertEquals(1, ParticleRegistry.size(), "failed registration must not mutate the snapshot");
        assertSame(first, ParticleRegistry.get(key));
    }

    @Test
    public void minecraftNamespaceRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> ParticleRegistry.register(NamespacedKey.minecraft("not_allowed"), Void.class));
        assertEquals(0, ParticleRegistry.size());
    }

    @Test
    public void vanillaKeyRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> ParticleRegistry.register(NamespacedKey.minecraft("poof"), Void.class));
        assertEquals(0, ParticleRegistry.size(), "vanilla collision must not be published");
    }

    @Test
    public void nullArgumentsRejected() {
        assertThrows(NullPointerException.class, () -> ParticleRegistry.register(null, Void.class));
        assertThrows(NullPointerException.class,
            () -> ParticleRegistry.register(new NamespacedKey("mintychochip", "x"), null));
        assertEquals(0, ParticleRegistry.size());
    }

    @Test
    public void getResolvesCustomAndMissing() {
        final Particle custom = ParticleRegistry.register(new NamespacedKey("mintychochip", "getme"), Void.class);
        assertSame(custom, ParticleRegistry.get(custom.getKey()));
        assertNull(ParticleRegistry.get(new NamespacedKey("mintychochip", UUID.randomUUID().toString())));
    }
}