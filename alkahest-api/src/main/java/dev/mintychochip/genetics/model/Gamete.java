package dev.mintychochip.genetics.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Haploid contribution from one parent: at most one allele per locus.
 * For males, X-linked loci are omitted when the gamete is a Y-bearing sperm.
 */
public final class Gamete {

    public enum SexChromosome {
        /** Egg, or X-bearing sperm. */
        X,
        /** Y-bearing sperm (no X-linked alleles). */
        Y
    }

    private final SexChromosome sexChromosome;
    private final Map<LocusId, Allele> alleles;

    private Gamete(final SexChromosome sexChromosome, final Map<LocusId, Allele> alleles) {
        this.sexChromosome = sexChromosome;
        this.alleles = Collections.unmodifiableMap(alleles);
    }

    public static Builder builder(final SexChromosome sexChromosome) {
        return new Builder(sexChromosome);
    }

    public SexChromosome sexChromosome() {
        return this.sexChromosome;
    }

    public Map<LocusId, Allele> alleles() {
        return this.alleles;
    }

    public @Nullable Allele get(final LocusId id) {
        return this.alleles.get(id);
    }

    public static final class Builder {
        private final SexChromosome sexChromosome;
        private final Map<LocusId, Allele> alleles = new LinkedHashMap<>();

        private Builder(final SexChromosome sexChromosome) {
            this.sexChromosome = Objects.requireNonNull(sexChromosome);
        }

        public Builder put(final LocusId id, final Allele allele) {
            this.alleles.put(Objects.requireNonNull(id), Objects.requireNonNull(allele));
            return this;
        }

        public Gamete build() {
            return new Gamete(this.sexChromosome, new LinkedHashMap<>(this.alleles));
        }
    }
}
