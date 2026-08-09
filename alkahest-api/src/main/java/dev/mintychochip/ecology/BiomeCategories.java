package dev.mintychochip.ecology;

import java.util.Locale;
import java.util.Map;

/** Maps vanilla biome ids → climate categories from ecology.json (or defaults). */
public final class BiomeCategories {
    private BiomeCategories() {
    }

    public static String category(final String biomeKey) {
        final Map<String, String> mapping = EcologyConfig.get().biomeCategories();
        final String mapped = mapping.get(biomeKey);
        if (mapped != null) {
            return mapped;
        }
        final int colon = biomeKey.indexOf(':');
        final String path = colon >= 0 ? biomeKey.substring(colon + 1) : biomeKey;
        return path.toLowerCase(Locale.ROOT);
    }
}