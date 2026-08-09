package io.papermc.paper.registry;

import io.papermc.paper.registry.entry.RegistryEntryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.bukkit.Keyed;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistryEntryBuilderBackendTest {

    @Test
    void apiOnlyRejectsNullBackend() {
        final ResourceKey<Registry<Object>> mcKey = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("bukkit", "test-registry"));
        final RegistryKey<Keyed> apiKey = RegistryKeyImpl.<Keyed>createInternal("test");
        final RegistryEntryBuilder<Object, Keyed> builder = RegistryEntryBuilder.start(mcKey, apiKey);
        final NullPointerException exception = assertThrows(NullPointerException.class, () -> builder.apiOnly(null, () -> null));
        assertEquals("Registry backend must not be null", exception.getMessage());
    }
}