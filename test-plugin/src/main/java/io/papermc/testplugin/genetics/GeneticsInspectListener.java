package io.papermc.testplugin.genetics;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.AQUA;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_AQUA;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Shift + right-click an animal to show what plugins can see without NMS:
 * applied registry variant (phenotype apply target). Chromosomal sex is on
 * {@code EntityBreedEvent#getGenetics()} / server NBT — not yet a public entity query.
 */
public final class GeneticsInspectListener implements Listener {

    @EventHandler
    public void onInspect(final PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        final Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        final Entity target = event.getRightClicked();
        if (!(target instanceof Animals)) {
            return;
        }

        final Component line = text()
            .append(text("[inspect] ", DARK_AQUA))
            .append(text(target.getType().name(), YELLOW))
            .append(text(" · ", GRAY))
            .append(text(describeAppearance(target), AQUA))
            .build();
        player.sendActionBar(line);
        player.sendMessage(line);
        player.sendMessage(text(
            "  (sex/genotype: listen to EntityBreedEvent#getGenetics() or pure Genetics API)",
            GRAY
        ));
    }

    private static String describeAppearance(final Entity entity) {
        if (entity instanceof Cat cat) {
            return "cat type=" + cat.getCatType().getKey().asString();
        }
        if (entity instanceof Wolf wolf) {
            return "wolf variant=" + wolf.getVariant().getKey().asString();
        }
        if (entity instanceof Cow cow) {
            return "cow variant=" + cow.getVariant().getKey().asString();
        }
        return "no mapped variant API for this species";
    }
}
