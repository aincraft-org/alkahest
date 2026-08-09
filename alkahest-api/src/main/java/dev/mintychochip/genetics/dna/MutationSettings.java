package dev.mintychochip.genetics.dna;

/**
 * Germline mutation rates applied to allele sequences during breeding.
 * Rates are per base per generation unless noted.
 */
public final class MutationSettings {

    /** Probability a single base is substituted by a different base. */
    private final double substitutionRate;
    /** Probability of starting an insertion at a given position. */
    private final double insertionRate;
    /** Probability of starting a deletion at a given position. */
    private final double deletionRate;
    /** Maximum bases inserted or deleted in one indel event. */
    private final int maxIndelLength;

    public static final MutationSettings NONE = new MutationSettings(0.0, 0.0, 0.0, 1);

    /** Defaults tuned for playable drift without melting every breed. */
    public static final MutationSettings DEFAULT = new MutationSettings(0.004, 0.0005, 0.0005, 2);

    public MutationSettings(
        final double substitutionRate,
        final double insertionRate,
        final double deletionRate,
        final int maxIndelLength
    ) {
        if (substitutionRate < 0.0 || insertionRate < 0.0 || deletionRate < 0.0) {
            throw new IllegalArgumentException("Mutation rates must be non-negative");
        }
        if (maxIndelLength < 1) {
            throw new IllegalArgumentException("maxIndelLength must be >= 1");
        }
        this.substitutionRate = substitutionRate;
        this.insertionRate = insertionRate;
        this.deletionRate = deletionRate;
        this.maxIndelLength = maxIndelLength;
    }

    public double substitutionRate() {
        return this.substitutionRate;
    }

    public double insertionRate() {
        return this.insertionRate;
    }

    public double deletionRate() {
        return this.deletionRate;
    }

    public int maxIndelLength() {
        return this.maxIndelLength;
    }

    public boolean isEnabled() {
        return this.substitutionRate > 0.0 || this.insertionRate > 0.0 || this.deletionRate > 0.0;
    }
}
