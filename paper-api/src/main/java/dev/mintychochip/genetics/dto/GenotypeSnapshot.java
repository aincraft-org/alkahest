package dev.mintychochip.genetics.dto;

import dev.mintychochip.genetics.model.GeneCopy;
import dev.mintychochip.genetics.model.Genome;
import dev.mintychochip.genetics.model.LocusCatalog;
import dev.mintychochip.genetics.model.LocusDefinition;
import dev.mintychochip.genetics.model.Sex;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Plugin-friendly genotype DTO derived from a {@link Genome}.
 */
public final class GenotypeSnapshot {

    private final Sex sex;
    private final List<LocusGenotype> loci;

    public GenotypeSnapshot(final Sex sex, final List<LocusGenotype> loci) {
        this.sex = Objects.requireNonNull(sex);
        this.loci = List.copyOf(loci);
    }

    public static GenotypeSnapshot from(final Genome genome, final LocusCatalog catalog) {
        final List<LocusGenotype> loci = new ArrayList<>();
        for (final LocusDefinition def : catalog.all()) {
            final GeneCopy copy = genome.getOrNull(def.id());
            if (copy == null) {
                continue;
            }
            final Zygosity zygosity;
            if (copy.isHemizygous()) {
                zygosity = Zygosity.HEMIZYGOUS;
            } else if (copy.isHomozygous()) {
                zygosity = Zygosity.HOMOZYGOUS;
            } else {
                zygosity = Zygosity.HETEROZYGOUS;
            }
            loci.add(new LocusGenotype(def.id(), copy.alleleA(), copy.alleleB(), zygosity));
        }
        return new GenotypeSnapshot(genome.sex(), loci);
    }

    public Sex sex() {
        return this.sex;
    }

    public List<LocusGenotype> loci() {
        return this.loci;
    }

    public List<LocusGenotype> lociView() {
        return Collections.unmodifiableList(this.loci);
    }
}
