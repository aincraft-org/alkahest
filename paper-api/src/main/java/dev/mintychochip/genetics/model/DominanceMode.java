package dev.mintychochip.genetics.model;

/**
 * How two alleles at a locus resolve to a phenotypic label.
 */
public enum DominanceMode {
    /**
     * Functional allele masks null; if both functional and sequences differ,
     * the first (allele A) label wins for simple display.
     */
    COMPLETE,
    /**
     * Heterozygotes express both (e.g. calico when orange/black on X).
     */
    CODOMINANT
}
