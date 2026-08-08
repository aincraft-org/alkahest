package dev.mintychochip.customentity;

import org.bukkit.NamespacedKey;

/**
 * Stable namespaced keys used to stamp custom-entity identity on entities (PDC).
 */
public final class CustomEntityKeys {

    /**
     * PDC key on {@link org.bukkit.entity.Entity}: string value is the custom entity id
     * (e.g. {@code mintychochip:glow_cube}).
     */
    public static final NamespacedKey ENTITY_ID = new NamespacedKey("mintychochip", "custom_entity");

    private CustomEntityKeys() {
    }
}
