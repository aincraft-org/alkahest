package dev.mintychochip.customentity;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.VanillaMaterial;

/**
 * Built-in sample custom entities registered at bootstrap.
 */
public final class DefaultCustomEntities {

    public static final NamespacedKey GLOW_CUBE = new NamespacedKey("mintychochip", "glow_cube");

    private DefaultCustomEntities() {
    }

    /** Register all default definitions if not already present. */
    public static void registerAll() {
        if (!CustomEntities.contains(GLOW_CUBE)) {
            CustomEntities.register(glowCube());
        }
    }

    public static CustomEntityDefinition glowCube() {
        // VanillaMaterial: Material interface statics can be null during bootstrap clinit races.
        final Material glowstone = Material.GLOWSTONE != null ? Material.GLOWSTONE : VanillaMaterial.GLOWSTONE;
        return CustomEntityDefinition.builder(GLOW_CUBE)
            .host(BlockModelHostSpec.of(glowstone))
            .displayName(Component.text("Glow Cube", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false))
            .build();
    }
}
