package dev.mintychochip.genetics.model;

/**
 * How a locus is transmitted across generations.
 */
public enum InheritanceMode {
    /** Both parents contribute; recombines on autosomes. */
    AUTOSOMAL,
    /**
     * On X. Females recombine two Xs; males are hemizygous.
     * Sons get X only from the dam; daughters get one X from each parent.
     */
    X_LINKED,
    /** Father → sons only. */
    Y_LINKED,
    /** Mother only (mitochondrial-style); no recombination with sire. */
    MATERNAL
}
