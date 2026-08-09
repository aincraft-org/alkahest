package dev.mintychochip.genetics.model;

import dev.mintychochip.genetics.dna.DnaSequence;
import dev.mintychochip.genetics.dna.MutationSettings;
import java.util.Objects;
import java.util.random.RandomGenerator;
import org.jspecify.annotations.Nullable;

/**
 * One allele: a DNA sequence, optionally tagged with a stable label for display
 * (e.g. {@code "O"} / {@code "o"} for orange coat).
 */
public final class Allele {

    private final DnaSequence sequence;
    private final @Nullable String label;

    private Allele(final DnaSequence sequence, final @Nullable String label) {
        this.sequence = Objects.requireNonNull(sequence, "sequence");
        this.label = label;
    }

    public static Allele of(final DnaSequence sequence) {
        return new Allele(sequence, null);
    }

    public static Allele of(final DnaSequence sequence, final @Nullable String label) {
        return new Allele(sequence, label);
    }

    public static Allele of(final String bases) {
        return of(DnaSequence.of(bases));
    }

    public static Allele of(final String bases, final @Nullable String label) {
        return of(DnaSequence.of(bases), label);
    }

    public DnaSequence sequence() {
        return this.sequence;
    }

    public @Nullable String label() {
        return this.label;
    }

    public boolean isFunctional() {
        return this.sequence.isFunctional();
    }

    /**
     * Display key: explicit label, else "FUNC"/"NULL" from translation, else sequence.
     */
    public String displayKey() {
        if (this.label != null && !this.label.isEmpty()) {
            return this.label;
        }
        return this.isFunctional() ? "FUNC" : "NULL";
    }

    public Allele mutate(final RandomGenerator random, final MutationSettings settings) {
        final DnaSequence mutated = this.sequence.mutate(random, settings);
        if (mutated.equals(this.sequence)) {
            return this;
        }
        // Labels only stick when the sequence is unchanged; mutation drops the tag
        // so phenotype re-derives from DNA (or stays NULL/FUNC).
        return new Allele(mutated, null);
    }

    public Allele withLabel(final @Nullable String label) {
        return new Allele(this.sequence, label);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof Allele other
            && this.sequence.equals(other.sequence)
            && Objects.equals(this.label, other.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.sequence, this.label);
    }

    @Override
    public String toString() {
        return this.label != null ? this.label + ":" + this.sequence : this.sequence.toString();
    }
}
