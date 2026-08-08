package dev.mintychochip.provenance;

import org.bukkit.Bukkit;
import org.bukkit.Server;

/**
 * Installs the provenance admin command and the durable writer. Safe to call
 * repeatedly; the writer is installed once per server process.
 */
public final class ProvenanceBootstrap {

    private static volatile boolean installed;

    private ProvenanceBootstrap() {
    }

    public static synchronized void ensureInstalled(final Server server) {
        if (installed) {
            return;
        }
        try {
            Bukkit.getCommandMap().register("mintychochip", new ProvenanceBukkitCommand());
            try {
                final java.io.File worldContainer = server.getWorldContainer();
                if (worldContainer != null) {
                    java.nio.file.Path auditRoot = worldContainer.toPath();
                    if (!server.getWorlds().isEmpty()) {
                        final org.bukkit.World primary = server.getWorlds().getFirst();
                        if (primary != null && primary.getWorldFolder() != null) {
                            auditRoot = primary.getWorldFolder().toPath();
                        }
                    }
                    ProvenanceWriter.install(auditRoot, message -> Bukkit.getLogger().info(message));
                }
            } catch (final Throwable t) {
                Bukkit.getLogger().warning("[mintychochip] provenance writer not installed: " + t.getMessage());
            }
            Bukkit.getLogger().info("[mintychochip] item provenance tracking enabled (/provenance)");
            Runtime.getRuntime().addShutdownHook(new Thread(ProvenanceWriter::flushAndClose, "mintychochip-provenance-flush"));        } catch (final Throwable t) {
            Bukkit.getLogger().warning("[mintychochip] failed to register /provenance: " + t.getMessage());
        }
        installed = true;
    }
}
