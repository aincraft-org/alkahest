package io.papermc.paper.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jspecify.annotations.Nullable;
import org.bukkit.support.environment.AllFeatures;
import org.junit.jupiter.api.Test;

/**
 * Contract for the generic {@link PaperCatalogRegistry} merged native-plus-catalog view.
 */
@AllFeatures
class CatalogRegistryTest {

    private record TestValue(String rawKey) implements org.bukkit.Keyed {
        @Override
        public NamespacedKey getKey() {
            return NamespacedKey.fromString(this.rawKey());
        }
    }

    /** Minimal native registry that also supports tags. */
    private static final class FakeNative implements Registry<TestValue> {
        private final Map<NamespacedKey, TestValue> values = new LinkedHashMap<>();
        private final Map<TagKey<TestValue>, Tag<TestValue>> tags = new LinkedHashMap<>();

        FakeNative(final TestValue... entries) {
            for (final TestValue value : entries) {
                this.values.put(value.getKey(), value);
            }
        }

        @Override
        public @Nullable TestValue get(final NamespacedKey key) {
            return this.values.get(key);
        }

        @Override
        public Iterator<TestValue> iterator() {
            return this.values.values().iterator();
        }
        @Override
        public int size() {
            return this.values.size();
        }

        @Override
        public java.util.stream.Stream<TestValue> stream() {
            return this.values.values().stream();
        }

        @Override
        public Stream<NamespacedKey> keyStream() {
            return this.values.keySet().stream();
        }

        @Override
        public NamespacedKey getKey(final TestValue value) {
            return value.getKey();
        }

        @Override
        public boolean hasTag(final TagKey<TestValue> key) {
            return this.tags.containsKey(key);
        }

        @Override
        public Tag<TestValue> getTag(final TagKey<TestValue> key) {
            return this.tags.get(key);
        }

        @Override
        public Collection<Tag<TestValue>> getTags() {
            return this.tags.values();
        }
    }

    private static final TestValue NATIVE_A = new TestValue("minecraft:native_a");
    private static final TestValue NATIVE_B = new TestValue("minecraft:native_b");
    private static final TestValue CUSTOM_A = new TestValue("mintychochip:custom_a");
    private static final TestValue CUSTOM_B = new TestValue("mintychochip:custom_b");

    private static PaperCatalogRegistry<TestValue> merged(
        final FakeNative nativeReg,
        final Map<NamespacedKey, TestValue> catalog
    ) {
        return new PaperCatalogRegistry<>(() -> nativeReg, () -> catalog);
    }

    @Test
    void nativeWinsOnGetAndKeyCollision() {
        final FakeNative nativeReg = new FakeNative(NATIVE_A, NATIVE_B);
        final Map<NamespacedKey, TestValue> catalog = new LinkedHashMap<>();
        catalog.put(CUSTOM_A.getKey(), CUSTOM_A);

        final PaperCatalogRegistry<TestValue> merged = merged(nativeReg, catalog);
        assertSame(NATIVE_B, merged.get(NATIVE_B.getKey()));
        assertSame(CUSTOM_A, merged.get(CUSTOM_A.getKey()));
        assertNull(merged.get(new NamespacedKey("minecraft", "missing")));
    }

    @Test
    void iterationIsNativeFirstThenCatalog() {
        final FakeNative nativeReg = new FakeNative(NATIVE_A, NATIVE_B);
        final Map<NamespacedKey, TestValue> catalog = new LinkedHashMap<>();
        catalog.put(CUSTOM_B.getKey(), CUSTOM_B);
        catalog.put(CUSTOM_A.getKey(), CUSTOM_A);

        final PaperCatalogRegistry<TestValue> merged = merged(nativeReg, catalog);
        assertEquals(List.of(NATIVE_A, NATIVE_B, CUSTOM_A, CUSTOM_B),
            StreamSupport.stream(merged.spliterator(), false).toList());
    }

    @Test
    void sizeAndKeyStreamCombine() {
        final FakeNative nativeReg = new FakeNative(NATIVE_A, NATIVE_B);
        final Map<NamespacedKey, TestValue> catalog = new LinkedHashMap<>();
        catalog.put(CUSTOM_A.getKey(), CUSTOM_A);

        final PaperCatalogRegistry<TestValue> merged = merged(nativeReg, catalog);
        assertEquals(3, merged.size());
        assertEquals(
            List.of(NATIVE_A.getKey(), NATIVE_B.getKey(), CUSTOM_A.getKey()),
            merged.keyStream().toList()
        );
    }

    @Test
    void identityPredicatesDistinguishNativeFromCatalog() {
        final FakeNative nativeReg = new FakeNative(NATIVE_A);
        final Map<NamespacedKey, TestValue> catalog = new LinkedHashMap<>();
        catalog.put(CUSTOM_A.getKey(), CUSTOM_A);

        final PaperCatalogRegistry<TestValue> merged = merged(nativeReg, catalog);
        assertTrue(merged.isNative(NATIVE_A));
        assertFalse(merged.isNative(CUSTOM_A));
        assertTrue(merged.isCatalog(CUSTOM_A));
        assertFalse(merged.isCatalog(NATIVE_A));
    }

    @Test
    void tagsDelegateOnlyToNative() {
        final FakeNative nativeReg = new FakeNative(NATIVE_A);
        final Map<NamespacedKey, TestValue> catalog = new LinkedHashMap<>();
        catalog.put(CUSTOM_A.getKey(), CUSTOM_A);
        final PaperCatalogRegistry<TestValue> merged = merged(nativeReg, catalog);
        assertFalse(merged.hasTag(null));
        assertTrue(merged.getTags().isEmpty());
    }
}