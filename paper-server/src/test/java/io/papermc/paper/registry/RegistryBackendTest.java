package io.papermc.paper.registry;

import io.papermc.paper.registry.entry.RegistryEntry;
import io.papermc.paper.registry.entry.RegistryEntryMeta;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.support.environment.AllFeatures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AllFeatures
class RegistryBackendTest {

    @BeforeAll
    static void before() throws ClassNotFoundException {
        Class.forName(RegistryKey.class.getName()); // load all keys so they are found for the test
    }

    static Stream<RegistryKey<?>> allKeys() {
        return RegistryKeyImpl.REGISTRY_KEYS.stream();
    }

    static Stream<Arguments> apiOnlyKeys() {
        return Stream.of(
            Arguments.of((RegistryKey<?>) RegistryKey.PARTICLE_TYPE, RegistryBackendKind.CATALOG),
            Arguments.of((RegistryKey<?>) RegistryKey.POTION, RegistryBackendKind.CATALOG),
            Arguments.of((RegistryKey<?>) RegistryKey.MEMORY_MODULE_TYPE, RegistryBackendKind.CATALOG),
            Arguments.of((RegistryKey<?>) RegistryKey.ENTITY_TYPE, RegistryBackendKind.MERGED)
        );
    }

    @ParameterizedTest
    @MethodSource("allKeys")
    void everyKeyHasExplicitBackend(final RegistryKey<?> key) {
        assertNotNull(PaperRegistries.getEntry(key).meta().backend(), key::toString);
    }

    @ParameterizedTest
    @MethodSource("apiOnlyKeys")
    void testApiOnlyBackends(final RegistryKey<?> key, final RegistryBackendKind expectedBackend) {
        final RegistryEntry<?, ?> entry = PaperRegistries.getEntry(key);
        // the API-only registries must be exactly ApiOnly backend metadata
        final RegistryEntryMeta.ApiOnly<?, ?> apiOnly = assertInstanceOf(RegistryEntryMeta.ApiOnly.class, entry.meta(), key::toString);
        assertEquals(expectedBackend, apiOnly.backend(), key::toString);
    }

    @Test
    void backendScopeIsExplicitAndNotReloadable() {
        final Set<RegistryKey<?>> catalogKeys = Set.of(
            RegistryKey.PARTICLE_TYPE,
            RegistryKey.POTION,
            RegistryKey.MEMORY_MODULE_TYPE
        );
        for (final RegistryKey<?> key : RegistryKeyImpl.REGISTRY_KEYS) {
            final RegistryBackendKind backend = PaperRegistries.backend(key);
            assertNotEquals(RegistryBackendKind.NATIVE_RELOADABLE, backend, key::toString);
            if (backend == RegistryBackendKind.CATALOG) {
                assertTrue(catalogKeys.contains(key), key::toString);
            }
            if (backend == RegistryBackendKind.MERGED) {
                assertEquals(RegistryKey.ENTITY_TYPE, key);
            }
        }
        assertEquals(
            catalogKeys,
            RegistryKeyImpl.REGISTRY_KEYS.stream()
                .filter(key -> PaperRegistries.backend(key) == RegistryBackendKind.CATALOG)
                .collect(Collectors.toUnmodifiableSet())
        );
    }

    @Test
    void catalogBackendsRejectWritableNativeRegistryAccess() {
        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PaperRegistryAccess.instance().getWritableRegistry(RegistryKey.PARTICLE_TYPE)
        );
        assertTrue(exception.getMessage().contains("CATALOG"));
        assertTrue(exception.getMessage().contains("native"));
    }
}
