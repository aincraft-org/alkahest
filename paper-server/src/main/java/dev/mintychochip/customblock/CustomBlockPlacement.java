package dev.mintychochip.customblock;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.Tripwire;
import org.jetbrains.annotations.NotNull;

/**
 * Applies the world carrier block for a custom-block definition (host-specific).
 *
 * <p>Identity is stored separately in {@link MemoryCustomBlockLookup}; this only sets
 * the vanilla block the client (and physics) see as a stand-in.
 */
public final class CustomBlockPlacement {

    private CustomBlockPlacement() {
    }

    /**
     * Set the carrier block at {@code block} for {@code definition}.
     * Does not register identity — call {@link MemoryCustomBlockLookup#put} separately.
     */
    public static void applyCarrier(@NotNull final Block block, @NotNull final CustomBlockDefinition definition) {
        block.setBlockData(carrierData(definition), false);
    }

    /** Vanilla carrier material for this host (for manual placement / checks). */
    public static @NotNull Material carrierMaterial(@NotNull final CustomBlockDefinition definition) {
        return definition.carrierMaterial();
    }

    public static @NotNull BlockData carrierData(@NotNull final CustomBlockDefinition definition) {
        final Material material = carrierMaterial(definition);
        final BlockData data = Bukkit.createBlockData(material);

        // Optional state index is reserved for pack allocation later.
        // For now, use stable default states that look/behave acceptably as carriers.
        if (data instanceof MultipleFacing facing && definition.hostType() == BlockHostType.CHORUS) {
            // Full cross so chorus plant has a solid footprint for interaction.
            for (final BlockFace face : facing.getAllowedFaces()) {
                facing.setFace(face, true);
            }
            return facing;
        }
        if (data instanceof MultipleFacing facing && definition.hostType() == BlockHostType.MUSHROOM) {
            // All faces true → "all stem" style cube; pack will retexture by state later.
            for (final BlockFace face : facing.getAllowedFaces()) {
                facing.setFace(face, true);
            }
            return facing;
        }
        if (data instanceof Tripwire tripwire) {
            tripwire.setAttached(true);
            tripwire.setDisarmed(true);
            return tripwire;
        }
        return data;
    }
}
