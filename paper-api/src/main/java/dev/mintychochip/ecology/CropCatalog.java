package dev.mintychochip.ecology;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Crop id → profile map (built-in defaults or loaded from ecology.json). */
public final class CropCatalog {
    private final Map<String, CropProfile> byId;

    public CropCatalog(final Map<String, CropProfile> profiles) {
        this.byId = Collections.unmodifiableMap(new LinkedHashMap<>(profiles));
    }

    public static CropCatalog builtin() {
        final Map<String, CropProfile> m = new LinkedHashMap<>();
        put(m, "minecraft:wheat", Set.of("plains", "forest"), Set.of("savanna", "mountain"), 0.20, 0.85);
        put(m, "minecraft:carrots", Set.of("plains", "forest", "taiga"), Set.of("mountain"), 0.20, 0.85);
        put(m, "minecraft:potatoes", Set.of("plains", "forest", "taiga"), Set.of("mountain"), 0.20, 0.85);
        put(m, "minecraft:beetroots", Set.of("plains", "forest"), Set.of("savanna"), 0.25, 0.90);
        put(m, "minecraft:torchflower_crop", Set.of("plains", "forest"), Set.of("savanna"), 0.25, 0.90);
        put(m, "minecraft:pumpkin_stem", Set.of("plains", "forest"), Set.of("jungle", "swamp"), 0.30, 0.90);
        put(m, "minecraft:attached_pumpkin_stem", Set.of("plains", "forest"), Set.of("jungle", "swamp"), 0.30, 0.90);
        put(m, "minecraft:melon_stem", Set.of("jungle", "swamp"), Set.of("plains"), 0.40, 0.95);
        put(m, "minecraft:attached_melon_stem", Set.of("jungle", "swamp"), Set.of("plains"), 0.40, 0.95);
        put(m, "minecraft:pitcher_crop", Set.of("plains", "forest", "swamp"), Set.of("jungle"), 0.30, 0.95);
        put(m, "minecraft:sugar_cane", Set.of("jungle", "swamp", "beach"), Set.of(), 0.55, 1.00);
        put(m, "minecraft:cactus", Set.of("desert", "badlands"), Set.of(), 0.00, 0.30);
        put(m, "minecraft:cocoa", Set.of("jungle"), Set.of(), 0.55, 1.00);
        put(m, "minecraft:sweet_berry_bush", Set.of("taiga", "snowy"), Set.of(), 0.20, 0.80);
        put(m, "minecraft:nether_wart", Set.of(), Set.of(), 0.00, 1.00);
        put(m, "minecraft:oak_sapling", Set.of("forest", "plains"), Set.of("savanna", "mountain"), 0.25, 0.85);
        put(m, "minecraft:birch_sapling", Set.of("forest"), Set.of("plains", "taiga"), 0.25, 0.85);
        put(m, "minecraft:spruce_sapling", Set.of("taiga", "snowy"), Set.of("forest", "mountain"), 0.15, 0.90);
        put(m, "minecraft:jungle_sapling", Set.of("jungle", "swamp"), Set.of("forest"), 0.55, 1.00);
        put(m, "minecraft:acacia_sapling", Set.of("savanna"), Set.of("plains", "badlands"), 0.05, 0.50);
        put(m, "minecraft:dark_oak_sapling", Set.of("forest", "swamp"), Set.of("taiga"), 0.35, 0.90);
        put(m, "minecraft:cherry_sapling", Set.of("forest"), Set.of("plains", "mountain"), 0.30, 0.85);
        put(m, "minecraft:mangrove_propagule", Set.of("swamp", "beach"), Set.of("jungle"), 0.50, 1.00);
        put(m, "minecraft:pale_oak_sapling", Set.of("forest"), Set.of("plains", "swamp"), 0.30, 0.90);
        return new CropCatalog(m);
    }

    public CropProfile forBlock(final String blockKey) {
        return this.byId.get(blockKey);
    }

    public Map<String, CropProfile> all() {
        return this.byId;
    }

    private static void put(
        final Map<String, CropProfile> m,
        final String id,
        final Set<String> nativeCats,
        final Set<String> tolerantCats,
        final double minH,
        final double maxH
    ) {
        m.put(id, new CropProfile(id, nativeCats, tolerantCats, minH, maxH));
    }
}