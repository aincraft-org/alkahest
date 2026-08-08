package dev.mintychochip;

import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginBase;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoadOrder;
import org.bukkit.plugin.PluginLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Minimal always-enabled plugin handle so mintychochip systems can register Bukkit listeners
 * without shipping a separate plugin jar.
 */
public final class MintyInternalPlugin extends PluginBase {

    private static final MintyInternalPlugin INSTANCE = new MintyInternalPlugin();

    private final PluginMeta meta = new Meta();
    private final Logger logger = Logger.getLogger("mintychochip");
    private Server server;
    private boolean enabled;
    private boolean naggable = true;

    private MintyInternalPlugin() {
    }

    public static MintyInternalPlugin get() {
        return INSTANCE;
    }

    public void attach(@NotNull final Server server) {
        this.server = server;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public @NotNull File getDataFolder() {
        final File folder = new File(this.server.getWorldContainer(), "mintychochip");
        // noinspection ResultOfMethodCallIgnored
        folder.mkdirs();
        return folder;
    }

    @Override
    @Deprecated
    public @NotNull PluginDescriptionFile getDescription() {
        return new PluginDescriptionFile("mintychochip", "1.0", "dev.mintychochip.MintyInternalPlugin");
    }

    @Override
    public @NotNull PluginMeta getPluginMeta() {
        return this.meta;
    }

    @Override
    public @NotNull FileConfiguration getConfig() {
        throw new UnsupportedOperationException("mintychochip internal plugin has no config.yml");
    }

    @Override
    public @Nullable InputStream getResource(@NotNull final String filename) {
        return null;
    }

    @Override
    public void saveConfig() {
    }

    @Override
    public void saveDefaultConfig() {
    }

    @Override
    public void saveResource(@NotNull final String resourcePath, final boolean replace) {
    }

    @Override
    public void reloadConfig() {
    }

    @Override
    @Deprecated(forRemoval = true)
    public @NotNull PluginLoader getPluginLoader() {
        throw new UnsupportedOperationException("mintychochip is not a jar plugin");
    }

    @Override
    public @NotNull Server getServer() {
        return this.server;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void onDisable() {
        this.enabled = false;
    }

    @Override
    public void onLoad() {
    }

    @Override
    public void onEnable() {
        this.enabled = true;
    }

    @Override
    public boolean isNaggable() {
        return this.naggable;
    }

    @Override
    public void setNaggable(final boolean canNag) {
        this.naggable = canNag;
    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull final String worldName, @Nullable final String id) {
        return null;
    }

    @Override
    public @Nullable BiomeProvider getDefaultBiomeProvider(@NotNull final String worldName, @Nullable final String id) {
        return null;
    }

    @Override
    public @NotNull Logger getLogger() {
        return this.logger;
    }

    @Override
    public boolean onCommand(
        @NotNull final CommandSender sender,
        @NotNull final Command command,
        @NotNull final String label,
        @NotNull final String[] args
    ) {
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(
        @NotNull final CommandSender sender,
        @NotNull final Command command,
        @NotNull final String alias,
        @NotNull final String[] args
    ) {
        return null;
    }

    @Override
    public @NotNull LifecycleEventManager<org.bukkit.plugin.Plugin> getLifecycleManager() {
        throw new UnsupportedOperationException("mintychochip internal plugin has no lifecycle manager");
    }

    private static final class Meta implements PluginMeta {
        @Override
        public @NotNull String getName() {
            return "mintychochip";
        }

        @Override
        public @NotNull String getMainClass() {
            return MintyInternalPlugin.class.getName();
        }

        @Override
        public @NotNull PluginLoadOrder getLoadOrder() {
            return PluginLoadOrder.POSTWORLD;
        }

        @Override
        public @NotNull String getVersion() {
            return "1.0";
        }

        @Override
        public @Nullable String getLoggerPrefix() {
            return "mintychochip";
        }

        @Override
        public @NotNull List<String> getPluginDependencies() {
            return List.of();
        }

        @Override
        public @NotNull List<String> getPluginSoftDependencies() {
            return List.of();
        }

        @Override
        public @NotNull List<String> getLoadBeforePlugins() {
            return List.of();
        }

        @Override
        public @NotNull List<String> getProvidedPlugins() {
            return List.of();
        }

        @Override
        public @NotNull List<String> getAuthors() {
            return List.of("mintychochip");
        }

        @Override
        public @NotNull List<String> getContributors() {
            return List.of();
        }

        @Override
        public @Nullable String getDescription() {
            return "Internal mintychochip fork systems";
        }

        @Override
        public @Nullable String getWebsite() {
            return null;
        }

        @Override
        public @NotNull List<Permission> getPermissions() {
            return List.of();
        }

        @Override
        public @NotNull PermissionDefault getPermissionDefault() {
            return PermissionDefault.OP;
        }

        @Override
        public @NotNull String getAPIVersion() {
            return "1.21";
        }

        @Override
        public @NotNull String namespace() {
            return "mintychochip";
        }
    }
}
