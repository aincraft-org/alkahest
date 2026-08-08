package dev.mintychochip.customblock;

/**
 * Strategy used to host a custom block on a vanilla client.
 *
 * <p>Vanilla clients cannot receive real new block registry IDs. Each custom block is therefore
 * either <strong>baked</strong> onto a finite vanilla block-state space, or rendered via
 * <strong>client packets</strong> (PacketBlocks-style item displays).
 */
public enum BlockHostType {
    /**
     * Resource-pack retexture of chorus plant block states.
     * Finite capacity; persists as real world block data.
     */
    CHORUS,

    /**
     * Resource-pack retexture of huge mushroom block states (brown or red family).
     * Finite capacity; persists as real world block data.
     */
    MUSHROOM,

    /**
     * Resource-pack retexture of tripwire string block states.
     * Finite capacity; persists as real world block data.
     */
    TRIPWIRE,

    /**
     * Client-only item-display packets plus a fake solid block change (default glass),
     * matching the PacketBlocks approach. Near-unlimited variants; identity stored
     * separately from vanilla block states.
     */
    PACKET;

    /** {@code true} when the host encodes identity in vanilla block states + a resource pack. */
    public boolean isBaked() {
        return this != PACKET;
    }

    /** {@code true} when visuals are pushed as client packets rather than baked block models. */
    public boolean isPacket() {
        return this == PACKET;
    }
}
