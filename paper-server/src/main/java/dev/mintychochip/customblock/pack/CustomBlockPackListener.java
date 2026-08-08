package dev.mintychochip.customblock.pack;

import dev.mintychochip.MintyInternalPlugin;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Auto-sends the custom-block resource pack on join.
 */
public final class CustomBlockPackListener implements Listener {

    private final CustomBlockPackService service;

    public CustomBlockPackListener(@NotNull final CustomBlockPackService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        final int delay = this.service.settings().joinDelayTicks();
        final var player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(
            MintyInternalPlugin.get(),
            () -> {
                if (player.isOnline()) {
                    this.service.sendTo(player);
                }
            },
            delay
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPackStatus(final PlayerResourcePackStatusEvent event) {
        // Optional diagnostics — avoid spam; only log failures / declines.
        switch (event.getStatus()) {
            case DECLINED, FAILED_DOWNLOAD, FAILED_RELOAD, INVALID_URL, DISCARDED ->
                Bukkit.getLogger().warning(
                    "[mintychochip] resource pack " + event.getStatus()
                        + " for " + event.getPlayer().getName()
                );
            default -> {
            }
        }
    }
}
