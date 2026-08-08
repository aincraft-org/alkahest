package dev.mintychochip.genetics.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Alleles present at one locus for one individual.
 * Females (and autosomes) are diploid; males are hemizygous on X/Y.
 */
public final class GeneCopy {

    private final Allele alleleA;
    private final @Nullable Allele alleleB;

    private GeneCopy(final Allele alleleA, final @Nullable Allele alleleB) {
        this.alleleA = Objects.requireNonNull(alleleA, "alleleA");
        this.alleleB = alleleB;
    }

    public static GeneCopy diploid(final Allele alleleA, final Allele alleleB) {
        return new GeneCopy(alleleA, Objects.requireNonNull(alleleB, "alleleB"));
    }

    public static GeneCopy hemizygous(final Allele allele) {
        return new GeneCopy(allele, null);
    }

    public Allele alleleA() {
        return this.alleleA;
    }

    public @Nullable Allele alleleB() {
        return this.alleleB;
    }

    public boolean isHemizygous() {
        return this.alleleB == null;
    }

    public boolean isDiploid() {
        return this.alleleB != null;
    }

    public boolean isHomozygous() {
        return this.alleleB != null && this.alleleA.sequence().equals(this.alleleB.sequence());
    }

    public boolean isHeterozygous() {
        return this.alleleB != null && !this.alleleA.sequence().equals(this.alleleB.sequence());
    }

    /**
     * Picks one allele for a gamete (diploid: 50/50; hemizygous: the only copy).
     */
    public Allele pickGameteAllele(final boolean pickB) {
        if (this.alleleB == null) {
            return this.alleleA;
        }
        return pickB ? this.alleleB : this.alleleA;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof GeneCopy other
            && this.alleleA.equals(other.alleleA)
            && Objects.equals(this.alleleB, other.alleleB);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.alleleA, this.alleleB);
    }

    @Override
    public String toString() {
        return this.alleleB == null
            ? this.alleleA + " (hemi)"
            : this.alleleA + "/" + this.alleleB;
    }
}
