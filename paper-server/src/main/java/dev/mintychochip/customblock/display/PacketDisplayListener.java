package dev.mintychochip.customblock.display;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Chunk show/hide for packet item displays.
 */
public final class PacketDisplayListener implements Listener {

    private final PacketDisplayService service = PacketDisplayService.get();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(final PlayerChunkLoadEvent event) {
        this.service.showChunk(event.getPlayer(), event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(final PlayerChunkUnloadEvent event) {
        this.service.hideChunk(event.getPlayer(), event.getChunk());
    }
}
