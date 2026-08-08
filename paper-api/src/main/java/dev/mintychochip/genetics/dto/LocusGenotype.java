package dev.mintychochip.genetics.dto;

import dev.mintychochip.genetics.model.Allele;
import dev.mintychochip.genetics.model.LocusId;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Public genotype view of one locus.
 */
public final class LocusGenotype {

    private final LocusId locus;
    private final Allele alleleA;
    private final @Nullable Allele alleleB;
    private final Zygosity zygosity;

    public LocusGenotype(
        final LocusId locus,
        final Allele alleleA,
        final @Nullable Allele alleleB,
        final Zygosity zygosity
    ) {
        this.locus = Objects.requireNonNull(locus);
        this.alleleA = Objects.requireNonNull(alleleA);
        this.alleleB = alleleB;
        this.zygosity = Objects.requireNonNull(zygosity);
    }

    public LocusId locus() {
        return this.locus;
    }

    public Allele alleleA() {
        return this.alleleA;
    }

    public @Nullable Allele alleleB() {
        return this.alleleB;
    }

    public Zygosity zygosity() {
        return this.zygosity;
    }
}
