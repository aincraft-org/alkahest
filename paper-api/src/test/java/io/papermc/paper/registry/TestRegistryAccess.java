package io.papermc.paper.registry;

import java.util.Collections;
import java.util.Iterator;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Minimal {@link RegistryAccess} for paper-api unit tests.
 *
 * <p>Returns empty registries so {@link Registry} interface clinit can finish
 * (legacy / keyed fields call into RegistryAccess). Real server tests use the
 * CraftBukkit implementation under {@code @Normal} bootstrap.
 */
public class TestRegistryAccess implements RegistryAccess {

    @Override
    @Deprecated(since = "1.20.6", forRemoval = true)
    public @Nullable <T extends Keyed> Registry<T> getRegistry(final @NotNull Class<T> type) {
        return emptyRegistry();
    }

    @Override
    public @NotNull <T extends Keyed> Registry<T> getRegistry(final @NotNull RegistryKey<T> registryKey) {
        return emptyRegistry();
    }

    private static <T extends Keyed> Registry<T> emptyRegistry() {
        return new Registry.NotARegistry<>() {
            @Override
            public @Nullable T get(final NamespacedKey key) {
                return null;
            }

            @Override
            public @NotNull Iterator<T> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 0;
            }
        };
    }
}
