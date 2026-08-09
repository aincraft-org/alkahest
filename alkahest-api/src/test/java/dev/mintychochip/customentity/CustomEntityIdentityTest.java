package dev.mintychochip.customentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Identity / façade tests that do not require a live Craft entity.
 */
public class CustomEntityIdentityTest {

    @AfterEach
    public void tearDown() {
        CustomEntities.reset();
    }

    @Test
    public void parseEntityTag() {
        assertTrue(CustomEntityTags.parse(null).isEmpty());
        assertTrue(CustomEntityTags.parse("").isEmpty());
        assertTrue(CustomEntityTags.parse("not a key").isEmpty());

        final Optional<NamespacedKey> key = CustomEntityTags.parse("mintychochip:glow_cube");
        assertTrue(key.isPresent());
        assertEquals("mintychochip", key.get().getNamespace());
        assertEquals("glow_cube", key.get().getKey());
    }

    @Test
    public void stampAndReadViaPdcRoundTrip() {
        final NamespacedKey id = NamespacedKey.fromString("mintychochip:glow_cube");
        final CustomEntityDefinition def = CustomEntityDefinition.builder(id)
            .host(BlockModelHostSpec.of(Material.GLOWSTONE))
            .build();
        CustomEntities.register(def);

        final MapPersistentDataContainer pdc = new MapPersistentDataContainer();
        CustomEntityTags.write(pdc, def);

        assertEquals(id, CustomEntityTags.read(pdc).orElseThrow());
        assertTrue(CustomEntityTags.has(pdc));

        CustomEntityTags.clear(pdc);
        assertTrue(CustomEntityTags.read(pdc).isEmpty());
    }

    @Test
    public void keyOfEntityUsesPdcStamp() {
        final NamespacedKey id = NamespacedKey.fromString("mintychochip:glow_cube");
        final CustomEntityDefinition def = CustomEntityDefinition.builder(id)
            .host(BlockModelHostSpec.of(Material.GLOWSTONE))
            .build();
        CustomEntities.register(def);

        final MapPersistentDataContainer pdc = new MapPersistentDataContainer();
        final Entity entity = mock(Entity.class);
        when(entity.getPersistentDataContainer()).thenReturn(pdc);
        when(entity.getType()).thenReturn(EntityType.BLOCK_DISPLAY);

        assertTrue(CustomEntities.keyOf(entity).isEmpty());
        assertFalse(CustomEntities.isCustomEntity(entity));

        CustomEntities.stamp(entity, def);

        assertEquals(id, CustomEntities.keyOf(entity).orElseThrow());
        assertEquals(def, CustomEntities.of(entity).orElseThrow());
        assertTrue(CustomEntities.isCustomEntity(entity));
        // Carrier type stays vanilla
        assertEquals(EntityType.BLOCK_DISPLAY, entity.getType());
    }

    @Test
    public void plainEntityReportsAbsent() {
        final MapPersistentDataContainer pdc = new MapPersistentDataContainer();
        final Entity entity = mock(Entity.class);
        when(entity.getPersistentDataContainer()).thenReturn(pdc);
        when(entity.getType()).thenReturn(EntityType.PIG);

        assertTrue(CustomEntities.keyOf(entity).isEmpty());
        assertTrue(CustomEntities.of(entity).isEmpty());
        assertFalse(CustomEntities.isCustomEntity(entity));
        assertFalse(entity.isCustomEntity());
        assertEquals(EntityType.PIG, entity.getType());
    }

    @Test
    public void nullEntityReportsAbsent() {
        assertTrue(CustomEntities.keyOf(null).isEmpty());
        assertTrue(CustomEntities.of(null).isEmpty());
        assertFalse(CustomEntities.isCustomEntity(null));
    }

    @Test
    public void keyWithoutCatalogEntryStillReportsKey() {
        final NamespacedKey id = NamespacedKey.fromString("mintychochip:unknown_orb");
        final MapPersistentDataContainer pdc = new MapPersistentDataContainer();
        CustomEntityTags.write(pdc, id);

        final Entity entity = mock(Entity.class);
        when(entity.getPersistentDataContainer()).thenReturn(pdc);

        assertEquals(id, CustomEntities.keyOf(entity).orElseThrow());
        // key present but not registered → of() empty, isCustomEntity still true
        assertTrue(CustomEntities.of(entity).isEmpty());
        assertTrue(CustomEntities.isCustomEntity(entity));
    }

    @Test
    public void stampIdWritesOnlyKey() {
        final NamespacedKey id = NamespacedKey.fromString("mintychochip:marker");
        final MapPersistentDataContainer pdc = new MapPersistentDataContainer();
        final Entity entity = mock(Entity.class);
        when(entity.getPersistentDataContainer()).thenReturn(pdc);

        CustomEntities.stampId(entity, id);
        assertEquals(id, CustomEntities.keyOf(entity).orElseThrow());
    }

    @Test
    public void clearStampRemovesIdentity() {
        final NamespacedKey id = NamespacedKey.fromString("mintychochip:glow_cube");
        final MapPersistentDataContainer pdc = new MapPersistentDataContainer();
        final Entity entity = mock(Entity.class);
        when(entity.getPersistentDataContainer()).thenReturn(pdc);

        CustomEntities.stampId(entity, id);
        assertTrue(CustomEntities.isCustomEntity(entity));
        CustomEntities.clearStamp(entity);
        assertFalse(CustomEntities.isCustomEntity(entity));
    }

    /**
     * Minimal in-memory PDC so stamp/read tests exercise the real {@link CustomEntityTags}
     * code path without a Craft entity.
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
