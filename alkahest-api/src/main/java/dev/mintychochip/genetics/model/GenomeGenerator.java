package dev.mintychochip.genetics.model;

import dev.mintychochip.genetics.dna.DnaSequence;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Builds a random founder genome for a catalog (wild spawn / first attach).
 */
public final class GenomeGenerator {

    private final LocusCatalog catalog;
    private final RandomGenerator random;
    private final int codonCount;

    public GenomeGenerator(final LocusCatalog catalog, final RandomGenerator random) {
        this(catalog, random, 3);
    }

    public GenomeGenerator(final LocusCatalog catalog, final RandomGenerator random, final int codonCount) {
        this.catalog = Objects.requireNonNull(catalog);
        this.random = Objects.requireNonNull(random);
        this.codonCount = codonCount;
    }

    public Genome generate(final Sex sex) {
        final Genome.Builder builder = Genome.builder(sex);
        for (final LocusDefinition locus : this.catalog.all()) {
            final GeneCopy copy = switch (locus.inheritance()) {
                case X_LINKED -> sex == Sex.MALE
                    ? GeneCopy.hemizygous(this.randomAllele(locus))
                    : GeneCopy.diploid(this.randomAllele(locus), this.randomAllele(locus));
                case Y_LINKED -> sex == Sex.MALE
                    ? GeneCopy.hemizygous(this.randomAllele(locus))
                    : null;
                case MATERNAL, AUTOSOMAL -> GeneCopy.diploid(this.randomAllele(locus), this.randomAllele(locus));
            };
            if (copy != null) {
                builder.put(locus.id(), copy);
            }
        }
        return builder.build();
    }

    private Allele randomAllele(final LocusDefinition locus) {
        // Coat-style codominant X locus: 50/50 O vs o labels for discovery play.
        if (locus.dominance() == DominanceMode.CODOMINANT
            && locus.inheritance() == InheritanceMode.X_LINKED) {
            if (this.random.nextBoolean()) {
                return Allele.of(DnaSequence.functionalOrf(this.random, this.codonCount), "O");
            }
            return Allele.of(DnaSequence.functionalOrf(this.random, this.codonCount), "o");
        }
        return Allele.of(DnaSequence.functionalOrf(this.random, this.codonCount));
    }
}
