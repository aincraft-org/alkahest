package dev.mintychochip.customentity;

/**
 * Strategy used to host a custom entity on a vanilla client.
 *
 * <p>Vanilla clients cannot receive real new entity registry IDs. Each custom entity is therefore
 * presented via a vanilla carrier entity (e.g. {@code BLOCK_DISPLAY}) plus separate logical identity.
 */
public enum EntityHostType {
    /**
     * Vanilla {@code BlockDisplay} carrier showing a block model (material + optional transform).
     * Primary first-class host for “block model entities.”
     */
    BLOCK_MODEL;

    public boolean isBlockModel() {
        return this == BLOCK_MODEL;
    }
}
