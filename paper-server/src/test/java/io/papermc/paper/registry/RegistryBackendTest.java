package io.papermc.paper.registry;

import io.papermc.paper.registry.entry.RegistryEntry;
import io.papermc.paper.registry.entry.RegistryEntryMeta;
import java.util.stream.Stream;
import org.bukkit.Keyed;
import org.bukkit.support.environment.AllFeatures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
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

    @ParameterizedTest
    @MethodSource("allKeys")
    void everyKeyHasExplicitBackend(final RegistryKey<?> key) {
        assertNotNull(PaperRegistries.getEntry(key).meta().backend(), key::toString);
    }

    @ParameterizedTest
    @MethodSource("allKeys")
    <M, A extends Keyed> void testApiOnlyBackends(final RegistryKey<A> key) {
        final RegistryEntry<M, A> entry = PaperRegistries.getEntry(key);
        if (entry.meta() instanceof RegistryEntryMeta.ApiOnly<M, A> apiOnly) {
            final RegistryBackendKind backend = apiOnly.backend();
            if (key == RegistryKey.PARTICLE_TYPE || key == RegistryKey.POTION || key == RegistryKey.MEMORY_MODULE_TYPE) {
                assertEquals(RegistryBackendKind.CATALOG, backend, key::toString);
            } else if (key == RegistryKey.ENTITY_TYPE) {
                assertInstanceOf(RegistryEntryMeta.ApiOnly.class, entry.meta());
                assertEquals(RegistryBackendKind.MERGED, backend, key::toString);
            }
        }
    }
}