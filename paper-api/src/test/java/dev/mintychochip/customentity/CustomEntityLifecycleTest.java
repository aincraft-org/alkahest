package dev.mintychochip.customentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Spawn/apply unit tests calling the shipped {@link CustomEntityLifecycle} path
 * with mocks (no full server world).
 */
public class CustomEntityLifecycleTest {

    private CustomEntityDefinition glowCube;

    @BeforeEach
    public void setUp() {
        CustomEntities.reset();
        this.glowCube = CustomEntityDefinition.builder("mintychochip:glow_cube")
            .host(BlockModelHostSpec.builder(Material.GLOWSTONE)
                .scale(new Vector3f(0.75f, 0.75f, 0.75f))
                .build())
            .build();
        CustomEntities.register(this.glowCube);
    }

    @AfterEach
    public void tearDown() {
        CustomEntities.reset();
    }

    @Test
    public void applySetsBlockTransformAndIdentity() {
        final MapPersistentDataContainer pdc = new MapPersistentDataContainer();
        final BlockDisplay display = mock(BlockDisplay.class);
        when(display.getPersistentDataContainer()).thenReturn(pdc);
        when(display.getType()).thenReturn(EntityType.BLOCK_DISPLAY);

        final BlockData blockData = mock(BlockData.class);
        when(blockData.getMaterial()).thenReturn(Material.GLOWSTONE);

        // Drive the real shipped apply path (injected BlockData avoids Bukkit registry).
        CustomEntityLifecycle.apply(display, this.glowCube, blockData);

        verify(display).setBlock(blockData);
        final Transformation expected = ((BlockModelHostSpec) this.glowCube.host()).toTransformation();
        verify(display).setTransformation(expected);

        assertEquals(this.glowCube.namespacedKey(), CustomEntities.keyOf(display).orElseThrow());
        assertEquals(this.glowCube, CustomEntities.of(display).orElseThrow());
        assertTrue(CustomEntities.isCustomEntity(display));
        // Carrier type remains vanilla BlockDisplay
        assertEquals(EntityType.BLOCK_DISPLAY, display.getType());
        assertEquals(Material.GLOWSTONE, this.glowCube.blockMaterial());
        assertEquals(Material.GLOWSTONE, blockData.getMaterial());
    }

    @Test
    public void applyRejectsNulls() {
        final BlockDisplay display = mock(BlockDisplay.class);
        final BlockData data = mock(BlockData.class);
        assertThrows(NullPointerException.class, () -> CustomEntityLifecycle.apply(null, this.glowCube, data));
        assertThrows(NullPointerException.class, () -> CustomEntityLifecycle.apply(display, null, data));
        assertThrows(NullPointerException.class, () -> CustomEntityLifecycle.apply(display, this.glowCube, null));
    }

    @Test
    public void isBlockModelCarrier() {
        final BlockDisplay display = mock(BlockDisplay.class);
        when(display.getType()).thenReturn(EntityType.BLOCK_DISPLAY);
        assertTrue(CustomEntityLifecycle.isBlockModelCarrier(display));

        final Entity pig = mock(Entity.class);
        when(pig.getType()).thenReturn(EntityType.PIG);
        assertFalse(CustomEntityLifecycle.isBlockModelCarrier(pig));
    }

    @Test
    public void spawnByKeyRequiresRegistration() {
        final Location loc = mock(Location.class);
        when(loc.getWorld()).thenReturn(mock(World.class));
        final NamespacedKey missing = NamespacedKey.fromString("mintychochip:missing");
        assertThrows(IllegalArgumentException.class, () -> CustomEntityLifecycle.spawn(loc, missing));
    }

    @Test
    public void sampleGlowCubeDefinitionShape() {
        // Mirrors server DefaultCustomEntities.glowCube() so the sample shape is covered
        // without requiring paper-server compile for this unit suite.
        final NamespacedKey key = new NamespacedKey("mintychochip", "glow_cube");
        final CustomEntityDefinition sample = CustomEntityDefinition.builder(key)
            .host(BlockModelHostSpec.of(Material.GLOWSTONE))
            .build();
        CustomEntities.reset();
        CustomEntities.register(sample);
        assertTrue(CustomEntities.contains(key));
        assertEquals(Material.GLOWSTONE, CustomEntities.get(key).orElseThrow().blockMaterial());
        assertEquals(EntityHostType.BLOCK_MODEL, sample.hostType());
        assertThrows(IllegalStateException.class, () -> CustomEntities.register(sample));
    }

    /**
     * Minimal in-memory PDC so apply tests exercise real stamp code.
     */
    static final class MapPersistentDataContainer implements PersistentDataContainer {
        private final Map<NamespacedKey, Object> values = new HashMap<>();

        @Override
        public <P, C> void set(
            @NotNull final NamespacedKey key,
            @NotNull final PersistentDataType<P, C> type,
            @NotNull final C value
        ) {
            this.values.put(key, value);
        }

        @Override
        public <P, C> boolean has(@NotNull final NamespacedKey key, @NotNull final PersistentDataType<P, C> type) {
            return this.values.containsKey(key);
        }

        @Override
        public boolean has(@NotNull final NamespacedKey key) {
            return this.values.containsKey(key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <P, C> @Nullable C get(
            @NotNull final NamespacedKey key,
            @NotNull final PersistentDataType<P, C> type
        ) {
            return (C) this.values.get(key);
        }

        @Override
        public <P, C> @NotNull C getOrDefault(
            @NotNull final NamespacedKey key,
            @NotNull final PersistentDataType<P, C> type,
            @NotNull final C defaultValue
        ) {
            final C value = get(key, type);
            return value != null ? value : defaultValue;
        }

        @Override
        public @NotNull Set<NamespacedKey> getKeys() {
            return Set.copyOf(this.values.keySet());
        }

        @Override
        public void remove(@NotNull final NamespacedKey key) {
            this.values.remove(key);
        }

        @Override
        public boolean isEmpty() {
            return this.values.isEmpty();
        }

        @Override
        public void copyTo(@NotNull final PersistentDataContainer other, final boolean replace) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull PersistentDataAdapterContext getAdapterContext() {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte @NotNull [] serializeToBytes() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void readFromBytes(final byte @NotNull [] bytes, final boolean clear) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getSize() {
            return this.values.size();
        }
    }
}
