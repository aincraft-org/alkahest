package dev.mintychochip.customblock;

import org.bukkit.NamespacedKey;

/**
 * Stable namespaced keys used to stamp custom-block identity on items and (later) world data.
 */
public final class CustomBlockKeys {

    /**
     * PDC key on {@link org.bukkit.inventory.ItemStack}: string value is the custom block id
     * (e.g. {@code mintychochip:electrum_ore}).
     */
    public static final NamespacedKey ITEM_ID = new NamespacedKey("mintychochip", "custom_block");

    private CustomBlockKeys() {
    }
}
