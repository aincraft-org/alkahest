package io.papermc.paper.registry;

import io.papermc.paper.registry.entry.RegistryEntry;
import io.papermc.paper.registry.entry.RegistryEntryMeta;
import java.util.stream.Stream;
import org.bukkit.Keyed;
import org.bukkit.support.environment.AllFeatures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
