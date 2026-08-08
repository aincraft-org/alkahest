package dev.mintychochip.customblock;

import dev.mintychochip.MintyInternalPlugin;
import dev.mintychochip.customblock.pack.CustomBlockPackListener;
import dev.mintychochip.customblock.pack.CustomBlockPackService;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;

/**
 * Server-side bootstrap for custom blocks: placement, listeners, commands, resource pack host.
 */
public final class CustomBlockBootstrap {

    private static volatile boolean installed;
    private static volatile MemoryCustomBlockLookup memoryLookup;
    private static volatile CustomBlockListener listener;
    private static volatile CustomBlockDigFeel digFeelListener;
    private static volatile CustomBlockPackListener packListener;
    private static volatile CustomBlockPackService packService;

    private CustomBlockBootstrap() {
    }

    /**
     * Install lookup + event listeners + defaults + commands + pack host.
     * Safe to call repeatedly. Intended from {@code CraftServer.enablePlugins(POSTWORLD)}.
     */
    public static synchronized void ensureInstalled(final Server server) {
        if (installed) {
            return;
        }
        memoryLookup = new MemoryCustomBlockLookup();
        CustomBlocks.setLookup(memoryLookup);
        DefaultCustomBlocks.registerAll();

        final MintyInternalPlugin plugin = MintyInternalPlugin.get();
        plugin.attach(server);
        plugin.setEnabled(true);

        listener = new CustomBlockListener();
        digFeelListener = new CustomBlockDigFeel();
        final var packetDisplayListener = new dev.mintychochip.customblock.display.PacketDisplayListener();
        final PluginManager pm = server.getPluginManager();
        pm.registerEvents(listener, plugin);
        pm.registerEvents(digFeelListener, plugin);
        pm.registerEvents(packetDisplayListener, plugin);

        registerCommands();

        // Auto-serve resource pack (HTTP + join delivery)
        final Path serverRoot = server.getWorldContainer().toPath().toAbsolutePath().normalize();
        packService = CustomBlockPackService.start(serverRoot, Bukkit.getLogger());
        if (packService != null) {
            packListener = new CustomBlockPackListener(packService);
            pm.registerEvents(packListener, plugin);
        }

        installed = true;
        Bukkit.getLogger().info(
            "[mintychochip] custom blocks: place/break + dig-feel + /customblock give ("
                + CustomBlocks.catalog().size() + " registered)"
                + (packService != null ? " + resource pack auto-serve" : "")
        );
    }

    private static void registerCommands() {
        // Bukkit command map — works during POSTWORLD without brigadier lifecycle context.
        try {
            final CustomBlockBukkitCommand cmd = new CustomBlockBukkitCommand();
            Bukkit.getCommandMap().register("mintychochip", cmd);
            Bukkit.getLogger().info("[mintychochip] registered /customblock (aliases: /cb, /cblock)");
        } catch (final Throwable t) {
            Bukkit.getLogger().warning(
                "[mintychochip] failed to register /customblock: " + t.getMessage()
            );
        }
    }

    /** Install using the global Bukkit server (when already available). */
    public static void ensureInstalled() {
        ensureInstalled(Bukkit.getServer());
    }

    public static MemoryCustomBlockLookup placements() {
        if (memoryLookup == null) {
            ensureInstalled();
        }
        return memoryLookup;
    }

    public static boolean isInstalled() {
        return installed;
    }

    public static CustomBlockPackService packService() {
        return packService;
    }

    /** Tests only. */
    public static synchronized void reset() {
        if (listener != null && MintyInternalPlugin.get().isEnabled()) {
            org.bukkit.event.HandlerList.unregisterAll(listener);
        }
        if (digFeelListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(digFeelListener);
        }
        if (packListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(packListener);
        }
        if (packService != null) {
            packService.close();
        }
        if (memoryLookup != null) {
            memoryLookup.clear();
        }
        memoryLookup = null;
        listener = null;
        digFeelListener = null;
        packListener = null;
        packService = null;
        installed = false;
        MintyInternalPlugin.get().setEnabled(false);
        CustomBlocks.reset();
    }
}
