package io.papermc.testplugin.genetics;

import dev.mintychochip.genetics.dto.BreedGenetics;
import dev.mintychochip.genetics.dto.PhenotypeTrait;
import dev.mintychochip.genetics.model.Sex;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.plugin.Plugin;

/**
 * Demo: react to mintychochip genetics payload on {@link EntityBreedEvent}.
 *
 * <p>Jar owns simulation (sex gate, recombination, variant apply). This plugin
 * only observes and announces — cancel with metadata if you want policy.
 */
public final class GeneticsBreedListener implements Listener {

    private final Plugin plugin;

    public GeneticsBreedListener(final Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(final EntityBreedEvent event) {
        if (!event.hasGenetics()) {
            // Villagers / genetics-disabled paths: no payload.
            return;
        }

        final BreedGenetics genetics = event.getGenetics();
        final Sex childSex = genetics.childSex();
        final String coat = genetics.childPhenotype().getOrNull("coat");
        final String variant = genetics.childVariant()
            .map(key -> key.asString())
            .orElse("(no registry mapping)");

        final Component message = Component.text()
            .append(Component.text("[genetics] ", NamedTextColor.DARK_AQUA))
            .append(Component.text(event.getEntity().getType().name(), NamedTextColor.YELLOW))
            .append(Component.text(" born ", NamedTextColor.GRAY))
            .append(Component.text(childSex.name(), sexColor(childSex)))
            .append(Component.text(" · coat=", NamedTextColor.GRAY))
            .append(Component.text(coat != null ? coat : "?", NamedTextColor.GOLD))
            .append(Component.text(" · variant=", NamedTextColor.GRAY))
            .append(Component.text(variant, NamedTextColor.AQUA))
            .build();

        // Prefer the player who fed them; else nearby ops-style broadcast to breeders only.
        final LivingEntity breeder = event.getBreeder();
        if (breeder instanceof Player player) {
            player.sendMessage(message);
            // Applied registry look is already on the live entity after the event.
            if (event.getEntity() instanceof Cat cat) {
                player.sendMessage(Component.text(
                    "  live cat type: " + cat.getCatType().getKey().asString(),
                    NamedTextColor.DARK_GRAY
                ));
            }
        } else {
            this.plugin.getComponentLogger().info(message);
        }

        // Example policy (disabled): reject a phenotype with full context in hand.
        // if ("calico".equals(coat)) {
        //     event.setCancelled(true);
        // }
    }

    /**
     * Example of cancellable policy using genetics metadata (not used by default).
     * Register a separate NORMAL-priority handler if you enable this.
     */
    @SuppressWarnings("unused")
    public void exampleCancelPolicy(final EntityBreedEvent event) {
        if (!event.hasGenetics()) {
            return;
        }
        final BreedGenetics g = event.getGenetics();
        // e.g. only allow female calves, or ban a coat:
        if (g.childSex() == Sex.MALE && "O".equals(g.childPhenotype().getOrNull("coat"))) {
            event.setCancelled(true);
            final Entity breeder = event.getBreeder();
            if (breeder instanceof Player player) {
                player.sendMessage(Component.text(
                    "Breed cancelled by plugin policy (orange male).",
                    NamedTextColor.RED
                ));
            }
        }
    }

    private static NamedTextColor sexColor(final Sex sex) {
        return sex == Sex.FEMALE ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.BLUE;
    }
}
