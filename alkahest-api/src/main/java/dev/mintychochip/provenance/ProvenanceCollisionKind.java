package dev.mintychochip.provenance;

/**
 * Why a stack UUID was flagged as colliding.
 */
public enum ProvenanceCollisionKind {
    /** Two independent stacks with the same UUID observed at two different concrete locations. */
    DUPLICATE_LOCATION,
    /** Two independent stacks with the same UUID merged into one — duplicate quantity laundering. */
    DUPLICATE_MERGE
}
