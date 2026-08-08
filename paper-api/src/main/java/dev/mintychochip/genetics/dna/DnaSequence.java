package dev.mintychochip.genetics.dna;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Immutable DNA base string. Alleles are sequences; point mutations edit bases.
 */
public final class DnaSequence {

    private final String bases;

    private DnaSequence(final String bases) {
        this.bases = bases;
    }

    public static DnaSequence of(final String bases) {
        if (bases == null || bases.isEmpty()) {
            throw new IllegalArgumentException("DNA sequence cannot be empty");
        }
        final String upper = bases.toUpperCase();
        for (int i = 0; i < upper.length(); i++) {
            Nucleotide.fromSymbol(upper.charAt(i));
        }
        return new DnaSequence(upper);
    }

    public static DnaSequence random(final RandomGenerator random, final int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Sequence length must be positive");
        }
        final StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(Nucleotide.random(random).symbol());
        }
        return new DnaSequence(sb.toString());
    }

    /**
     * Builds a functional open reading frame of the given codon count (no stops),
     * useful for test alleles.
     */
    public static DnaSequence functionalOrf(final RandomGenerator random, final int codonCount) {
        if (codonCount <= 0) {
            throw new IllegalArgumentException("codonCount must be positive");
        }
        final StringBuilder sb = new StringBuilder(codonCount * 3);
        for (int c = 0; c < codonCount; c++) {
            String codon;
            do {
                codon = "" + Nucleotide.random(random).symbol()
                    + Nucleotide.random(random).symbol()
                    + Nucleotide.random(random).symbol();
            } while (CodonTable.isStopCodon(codon));
            sb.append(codon);
        }
        return new DnaSequence(sb.toString());
    }

    public int length() {
        return this.bases.length();
    }

    public char baseAt(final int index) {
        return this.bases.charAt(index);
    }

    public String asString() {
        return this.bases;
    }

    public List<String> codons() {
        final List<String> codons = new ArrayList<>(this.bases.length() / 3);
        for (int i = 0; i + 3 <= this.bases.length(); i += 3) {
            codons.add(this.bases.substring(i, i + 3));
        }
        return codons;
    }

    /**
     * Loss-of-function when any complete codon is a stop (premature or full-frame stop).
     */
    public boolean isFunctional() {
        for (final String codon : this.codons()) {
            if (CodonTable.isStopCodon(codon)) {
                return false;
            }
        }
        return !this.codons().isEmpty();
    }

    public String translate() {
        final StringBuilder protein = new StringBuilder();
        for (final String codon : this.codons()) {
            final char aa = CodonTable.translate(codon);
            if (aa == CodonTable.STOP) {
                break;
            }
            protein.append(aa);
        }
        return protein.toString();
    }

    /**
     * Germline mutation: per-base substitutions (point mutations) plus rare indels.
     */
    public DnaSequence mutate(final RandomGenerator random, final MutationSettings settings) {
        if (!settings.isEnabled()) {
            return this;
        }
        final StringBuilder sb = new StringBuilder(this.bases);
        for (int i = 0; i < sb.length(); i++) {
            if (random.nextDouble() < settings.substitutionRate()) {
                sb.setCharAt(i, randomDifferentBase(random, sb.charAt(i)));
            }
        }
        for (int i = 0; i <= sb.length() && sb.length() > 1; i++) {
            if (random.nextDouble() < settings.insertionRate()) {
                final int run = 1 + random.nextInt(settings.maxIndelLength());
                for (int j = 0; j < run; j++) {
                    sb.insert(i, Nucleotide.random(random).symbol());
                }
                i += run;
            } else if (random.nextDouble() < settings.deletionRate()) {
                final int run = 1 + random.nextInt(Math.min(settings.maxIndelLength(), sb.length() - 1));
                sb.delete(i, Math.min(i + run, sb.length()));
                if (sb.isEmpty()) {
                    break;
                }
            }
        }
        if (sb.isEmpty()) {
            return this;
        }
        return new DnaSequence(sb.toString());
    }

    /**
     * Forces a point mutation at {@code index} (for tests and tools).
     */
    public DnaSequence substituteAt(final int index, final char newBase) {
        if (index < 0 || index >= this.bases.length()) {
            throw new IndexOutOfBoundsException("index " + index);
        }
        final char replacement = Nucleotide.fromSymbol(newBase).symbol();
        if (replacement == this.bases.charAt(index)) {
            return this;
        }
        final StringBuilder sb = new StringBuilder(this.bases);
        sb.setCharAt(index, replacement);
        return new DnaSequence(sb.toString());
    }

    public int hammingDistance(final DnaSequence other) {
        final int max = Math.min(this.bases.length(), other.bases.length());
        int distance = Math.abs(this.bases.length() - other.bases.length());
        for (int i = 0; i < max; i++) {
            if (this.bases.charAt(i) != other.bases.charAt(i)) {
                distance++;
            }
        }
        return distance;
    }

    private static char randomDifferentBase(final RandomGenerator random, final char original) {
        char replacement;
        do {
            replacement = Nucleotide.random(random).symbol();
        } while (replacement == original);
        return replacement;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof DnaSequence other && this.bases.equals(other.bases);
    }

    @Override
    public int hashCode() {
        return this.bases.hashCode();
    }

    @Override
    public String toString() {
        return this.bases;
    }
}
