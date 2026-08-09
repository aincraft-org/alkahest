package dev.mintychochip.provenance;

/**
 * Why a stack instance entered the economy (or how it was derived).
 */
public enum ProvenanceSource {
    /** Block break / {@code popResource} style drops (natural / unstamped world). */
    BLOCK_DROP,
    /**
     * Drop recovered from a player-placed block that still had placement memory.
     * Parents point at the stack UUID that was placed (anti-wash).
     */
    BLOCK_RECOVER,
    /** Mob / entity death loot. */
    ENTITY_DROP,
    /** Loot table (chest, fishing, archaeology, …). */
    LOOT,
    /** Crafting table / inventory craft result. */
    CRAFT,
    /** Furnace / smoker / blast / campfire output. */
    SMELT,
    /** Stonecutter, smithing, loom, etc. */
    SPECIAL_RECIPE,
    /** A container merge derived a new stack identity from target and source. */
    MERGE,
    /** Split from a parent stack (partial pick). */
    SPLIT,
    /** Admin /give, creative, or explicit mint API. */
    GIVE,
    /** Villager / custom merchant trade result. */
    TRADE,
    /** Loaded from disk without prior census entry. */
    REHYDRATE,
    /** Pre-system or unstamped stack first seen. */
    LEGACY,
    /** Catch-all when the creation path is not classified. */
    UNKNOWN
}
