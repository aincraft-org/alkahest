package dev.mintychochip.genetics.dto;

import dev.mintychochip.genetics.model.Allele;
import dev.mintychochip.genetics.model.DominanceMode;
import dev.mintychochip.genetics.model.GeneCopy;
import dev.mintychochip.genetics.model.Genome;
import dev.mintychochip.genetics.model.LocusCatalog;
import dev.mintychochip.genetics.model.LocusDefinition;
import dev.mintychochip.genetics.model.Sex;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Maps genotype → phenotype labels. Default rules support complete dominance
 * (functional vs null) and codominance (e.g. calico on heterozygous X).
 */
public final class PhenotypeDecoder {

    private final LocusCatalog catalog;

    public PhenotypeDecoder(final LocusCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog);
    }

    public PhenotypeSnapshot decode(final Genome genome) {
        final List<PhenotypeTrait> traits = new ArrayList<>();
        for (final LocusDefinition locus : this.catalog.all()) {
            final GeneCopy copy = genome.getOrNull(locus.id());
            if (copy == null) {
                continue;
            }
            final String value = this.decodeLocus(locus, copy, genome.sex());
            if (value != null) {
                traits.add(new PhenotypeTrait(locus.phenotypeKey(), value));
            }
        }
        return new PhenotypeSnapshot(traits);
    }

    public PhenotypeSnapshot decode(final GenotypeSnapshot genotype) {
        // Rebuild minimal views via genome is not needed; decode from snapshot fields.
        final List<PhenotypeTrait> traits = new ArrayList<>();
        for (final LocusGenotype locusGt : genotype.loci()) {
            final LocusDefinition def = this.catalog.get(locusGt.locus());
            if (def == null) {
                continue;
            }
            final GeneCopy copy = locusGt.alleleB() == null
                ? GeneCopy.hemizygous(locusGt.alleleA())
                : GeneCopy.diploid(locusGt.alleleA(), locusGt.alleleB());
            final String value = this.decodeLocus(def, copy, genotype.sex());
            if (value != null) {
                traits.add(new PhenotypeTrait(def.phenotypeKey(), value));
            }
        }
        return new PhenotypeSnapshot(traits);
    }

    private String decodeLocus(final LocusDefinition locus, final GeneCopy copy, final Sex sex) {
        if (locus.dominance() == DominanceMode.CODOMINANT) {
            return decodeCodominant(copy, sex);
        }
        return decodeComplete(copy);
    }

    private static String decodeComplete(final GeneCopy copy) {
        if (copy.isHemizygous()) {
            return copy.alleleA().displayKey();
        }
        final Allele a = copy.alleleA();
        final Allele b = copy.alleleB();
        final boolean aFunc = a.isFunctional();
        final boolean bFunc = b.isFunctional();
        if (aFunc && bFunc) {
            // Both functional: prefer label match or allele A.
            if (Objects.equals(a.displayKey(), b.displayKey())) {
                return a.displayKey();
            }
            return a.displayKey();
        }
        if (aFunc) {
            return a.displayKey();
        }
        if (bFunc) {
            return b.displayKey();
        }
        return "NULL";
    }

    /**
     * Codominant: heterozygotes show both labels joined, or "calico" when
     * classic O/o orange labels differ. Males (hemizygous) never calico.
     */
    private static String decodeCodominant(final GeneCopy copy, final Sex sex) {
        if (copy.isHemizygous() || sex == Sex.MALE) {
            return copy.alleleA().displayKey();
        }
        final String a = copy.alleleA().displayKey();
        final String b = copy.alleleB().displayKey();
        if (a.equals(b)) {
            return a;
        }
        // Named orange system: O + o (or ORANGE + BLACK) → calico in females.
        if (isOrangePair(a, b)) {
            return "calico";
        }
        // Lexicographic join for stable combined labels.
        if (a.compareTo(b) < 0) {
            return a + "+" + b;
        }
        return b + "+" + a;
    }

    private static boolean isOrangePair(final String a, final String b) {
        return (isOrange(a) && isNonOrange(b)) || (isOrange(b) && isNonOrange(a));
    }

    private static boolean isOrange(final String key) {
        // Single-letter O/o is case-sensitive (classic cat genetics notation).
        return key.equals("O") || key.equalsIgnoreCase("ORANGE");
    }

    private static boolean isNonOrange(final String key) {
        return key.equals("o")
            || key.equalsIgnoreCase("BLACK")
            || key.equalsIgnoreCase("B")
            || key.equalsIgnoreCase("NON_ORANGE");
    }
}
