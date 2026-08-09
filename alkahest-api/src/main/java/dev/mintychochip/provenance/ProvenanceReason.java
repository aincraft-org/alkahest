package dev.mintychochip.provenance;

/**
 * Why a stack UUID left the live census.
 */
public enum ProvenanceReason {
    /** Fully consumed (eaten, used up, craft ingredient emptied). */
    CONSUMED,
    /** Absorbed into another stack of the same type. */
    MERGED,
    /** Burned, voided, cactus, despawned as item entity, etc. */
    DESTROYED,
    /** Admin clear / creative destroy. */
    CLEARED,
    /** Explicit test or API kill. */
    MANUAL
}
