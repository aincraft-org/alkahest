package dev.mintychochip.provenance;

/**
 * Audit log event kinds for stack identity.
 */
public enum ProvenanceEventType {
    BIRTH,
    DEATH,
    SPLIT,
    MERGE,
    TRANSFORM,
    REHYDRATE,
    CLAIM,
    COLLISION,
    ZOMBIE
}
