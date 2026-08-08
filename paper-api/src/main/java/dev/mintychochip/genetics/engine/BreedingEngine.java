package dev.mintychochip.genetics.engine;

import dev.mintychochip.genetics.dna.MutationSettings;
import dev.mintychochip.genetics.model.Allele;
import dev.mintychochip.genetics.model.Gamete;
import dev.mintychochip.genetics.model.GeneCopy;
import dev.mintychochip.genetics.model.Genome;
import dev.mintychochip.genetics.model.InheritanceMode;
import dev.mintychochip.genetics.model.LocusCatalog;
import dev.mintychochip.genetics.model.LocusDefinition;
import dev.mintychochip.genetics.model.Sex;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.random.RandomGenerator;
import org.jspecify.annotations.Nullable;

/**
 * Recombination-first breeding: meiosis → gametes → fertilization → point mutation.
 * Requires opposite-sex parents.
 */
public final class BreedingEngine {

    private final LocusCatalog catalog;
    private final RecombinationSettings recombination;
    private final MutationSettings mutation;
    private final RandomGenerator random;
    private final MeiosisEngine meiosis;

    public BreedingEngine(
        final LocusCatalog catalog,
        final RecombinationSettings recombination,
        final MutationSettings mutation,
        final RandomGenerator random
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.recombination = Objects.requireNonNull(recombination, "recombination");
        this.mutation = Objects.requireNonNull(mutation, "mutation");
        this.random = Objects.requireNonNull(random, "random");
        this.meiosis = new MeiosisEngine(random, recombination);
    }

    public static BreedingEngine create(
        final LocusCatalog catalog,
        final RandomGenerator random
    ) {
        return new BreedingEngine(catalog, RecombinationSettings.DEFAULT, MutationSettings.DEFAULT, random);
    }

    /**
     * @return empty if parents are the same sex or otherwise incompatible
     */
    public Optional<BreedingResult> cross(final Genome sire, final Genome dam) {
        Objects.requireNonNull(sire, "sire");
        Objects.requireNonNull(dam, "dam");
        if (sire.sex() == dam.sex()) {
            return Optional.empty();
        }
        // Normalize roles: sire = male, dam = female for sex-linked rules.
        final Genome father = sire.sex() == Sex.MALE ? sire : dam;
        final Genome mother = sire.sex() == Sex.FEMALE ? sire : dam;
        if (father.sex() != Sex.MALE || mother.sex() != Sex.FEMALE) {
            return Optional.empty();
        }

        // Prefer nextLong bit over nextBoolean(): java.util.Random's first
        // nextBoolean/nextInt(2)/nextDouble are heavily biased for sequential seeds.
        final Sex childSex = (this.random.nextLong() & 1L) == 0L ? Sex.MALE : Sex.FEMALE;
        final Gamete fatherGamete = this.produceGamete(father, childSex == Sex.FEMALE
            ? Gamete.SexChromosome.X
            : Gamete.SexChromosome.Y);
        final Gamete motherGamete = this.produceGamete(mother, Gamete.SexChromosome.X);

        final Genome.Builder child = Genome.builder(childSex);
        for (final LocusDefinition locus : this.catalog.all()) {
            final GeneCopy copy = this.fuse(locus, fatherGamete, motherGamete, childSex);
            if (copy != null) {
                child.put(locus.id(), this.mutateCopy(copy));
            }
        }
        return Optional.of(new BreedingResult(child.build()));
    }

    private GeneCopy mutateCopy(final GeneCopy copy) {
        final Allele a = copy.alleleA().mutate(this.random, this.mutation);
        if (copy.isHemizygous()) {
            return GeneCopy.hemizygous(a);
        }
        return GeneCopy.diploid(a, copy.alleleB().mutate(this.random, this.mutation));
    }

    private @Nullable GeneCopy fuse(
        final LocusDefinition locus,
        final Gamete fatherGamete,
        final Gamete motherGamete,
        final Sex childSex
    ) {
        return switch (locus.inheritance()) {
            case AUTOSOMAL -> {
                final Allele fa = fatherGamete.get(locus.id());
                final Allele ma = motherGamete.get(locus.id());
                if (fa == null || ma == null) {
                    yield null;
                }
                yield GeneCopy.diploid(fa, ma);
            }
            case X_LINKED -> {
                final Allele ma = motherGamete.get(locus.id());
                if (ma == null) {
                    yield null;
                }
                if (childSex == Sex.MALE) {
                    // Son: single X from dam.
                    yield GeneCopy.hemizygous(ma);
                }
                final Allele fa = fatherGamete.get(locus.id());
                if (fa == null) {
                    // Father contributed Y; should not happen for daughters.
                    yield GeneCopy.hemizygous(ma);
                }
                yield GeneCopy.diploid(fa, ma);
            }
            case Y_LINKED -> {
                if (childSex != Sex.MALE) {
                    yield null;
                }
                final Allele fa = fatherGamete.get(locus.id());
                yield fa == null ? null : GeneCopy.hemizygous(fa);
            }
            case MATERNAL -> {
                final Allele ma = motherGamete.get(locus.id());
                // Stored as homozygous diploid for a haploid organelle genome.
                yield ma == null ? null : GeneCopy.diploid(ma, ma);
            }
        };
    }

    private Gamete produceGamete(final Genome parent, final Gamete.SexChromosome sexChromosome) {
        final Gamete.Builder builder = Gamete.builder(sexChromosome);

        // Autosomal: group by chromosome and recombine.
        final Map<Integer, List<LocusDefinition>> autosomalByChr = new TreeMap<>();
        for (final LocusDefinition locus : this.catalog.all()) {
            if (locus.inheritance() != InheritanceMode.AUTOSOMAL) {
                continue;
            }
            if (parent.getOrNull(locus.id()) == null) {
                continue;
            }
            autosomalByChr.computeIfAbsent(locus.chromosome(), unused -> new ArrayList<>()).add(locus);
        }
        for (final List<LocusDefinition> loci : autosomalByChr.values()) {
            loci.sort(Comparator.comparingInt(LocusDefinition::position));
            final int[] positions = new int[loci.size()];
            for (int i = 0; i < loci.size(); i++) {
                positions[i] = loci.get(i).position();
            }
            final boolean[] picks = this.meiosis.produceGameteChromatid(positions);
            for (int i = 0; i < loci.size(); i++) {
                final GeneCopy copy = parent.getOrNull(loci.get(i).id());
                if (copy != null) {
                    builder.put(loci.get(i).id(), copy.pickGameteAllele(picks[i]));
                }
            }
        }

        // X-linked
        if (sexChromosome == Gamete.SexChromosome.X) {
            final List<LocusDefinition> xLoci = new ArrayList<>();
            for (final LocusDefinition locus : this.catalog.all()) {
                if (locus.inheritance() == InheritanceMode.X_LINKED && parent.getOrNull(locus.id()) != null) {
                    xLoci.add(locus);
                }
            }
            xLoci.sort(Comparator.comparingInt(LocusDefinition::position));
            if (parent.sex() == Sex.MALE) {
                // Hemizygous: pass the single X as a block.
                for (final LocusDefinition locus : xLoci) {
                    final GeneCopy copy = parent.getOrNull(locus.id());
                    if (copy != null) {
                        builder.put(locus.id(), copy.alleleA());
                    }
                }
            } else if (!xLoci.isEmpty()) {
                final int[] positions = new int[xLoci.size()];
                for (int i = 0; i < xLoci.size(); i++) {
                    positions[i] = xLoci.get(i).position();
                }
                final boolean[] picks = this.meiosis.produceGameteChromatid(positions);
                for (int i = 0; i < xLoci.size(); i++) {
                    final GeneCopy copy = parent.getOrNull(xLoci.get(i).id());
                    if (copy != null) {
                        builder.put(xLoci.get(i).id(), copy.pickGameteAllele(picks[i]));
                    }
                }
            }
        }

        // Y-linked: only on Y sperm
        if (sexChromosome == Gamete.SexChromosome.Y && parent.sex() == Sex.MALE) {
            for (final LocusDefinition locus : this.catalog.all()) {
                if (locus.inheritance() != InheritanceMode.Y_LINKED) {
                    continue;
                }
                final GeneCopy copy = parent.getOrNull(locus.id());
                if (copy != null) {
                    builder.put(locus.id(), copy.alleleA());
                }
            }
        }

        // Maternal: eggs only (mother always produces X eggs in this model)
        if (parent.sex() == Sex.FEMALE) {
            for (final LocusDefinition locus : this.catalog.all()) {
                if (locus.inheritance() != InheritanceMode.MATERNAL) {
                    continue;
                }
                final GeneCopy copy = parent.getOrNull(locus.id());
                if (copy != null) {
                    // One random maternal allele if diploid storage.
                    builder.put(locus.id(), copy.pickGameteAllele(this.random.nextBoolean()));
                }
            }
        }

        return builder.build();
    }

    public LocusCatalog catalog() {
        return this.catalog;
    }
}
