package dev.mintychochip.customentity;

import org.bukkit.Bukkit;
import org.bukkit.Server;

/**
 * Server-side bootstrap for custom entities: catalog defaults.
 *
 * <p>Identity lives on entity PDC (no separate placement store). Spawn/apply is on-demand
 * via {@link CustomEntityLifecycle}.
 */
public final class CustomEntityBootstrap {

    private static volatile boolean installed;

    private CustomEntityBootstrap() {
    }

    /**
     * Register default custom entity definitions.
     * Safe to call repeatedly. Intended from {@code CraftServer.enablePlugins(POSTWORLD)}.
     */
    public static synchronized void ensureInstalled(final Server server) {
        if (installed) {
            return;
        }
        DefaultCustomEntities.registerAll();
        installed = true;
        Bukkit.getLogger().info(
            "[mintychochip] custom entities: block-model hosts ("
                + CustomEntities.catalog().size() + " registered)"
        );
    }

    /** Install using the global Bukkit server (when already available). */
    public static void ensureInstalled() {
        ensureInstalled(Bukkit.getServer());
    }

    public static boolean isInstalled() {
        return installed;
    }

    /** Tests only. */
    public static synchronized void reset() {
        installed = false;
        CustomEntities.reset();
    }
}
