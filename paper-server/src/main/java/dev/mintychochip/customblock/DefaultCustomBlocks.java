package dev.mintychochip.customblock;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

/**
 * Built-in sample custom blocks registered at bootstrap.
 */
public final class DefaultCustomBlocks {

    public static final NamespacedKey ELECTRUM_ORE = new NamespacedKey("mintychochip", "electrum_ore");

    private DefaultCustomBlocks() {
    }

    /** Register all default definitions if not already present. */
    public static void registerAll() {
        if (!CustomBlocks.contains(ELECTRUM_ORE)) {
            CustomBlocks.register(electrumOre());
        }
    }

    public static CustomBlockDefinition electrumOre() {
        // Use a placeable block base (GLASS) so the client plays the normal block-place
        // animation, not the paper "use item" animation. Packet host carrier is also glass;
        // presentation is the resource-pack item model + world visuals.
        return CustomBlockDefinition.builder(ELECTRUM_ORE)
            .host(PacketHostSpec.defaults())
            .itemMaterial(Material.GLASS)
            .itemModel(Key.key("mintychochip", "electrum_ore"))
            .displayName(Component.text("Electrum Ore", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false))
            // Dig speed, pickaxe preference, and blast resistance match iron ore.
            .emulate(Material.IRON_ORE)
            .build();
    }
}
