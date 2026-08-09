package dev.mintychochip.genetics;

import dev.mintychochip.genetics.dna.MutationSettings;
import dev.mintychochip.genetics.dto.GenotypeSnapshot;
import dev.mintychochip.genetics.dto.PhenotypeDecoder;
import dev.mintychochip.genetics.dto.PhenotypeSnapshot;
import dev.mintychochip.genetics.engine.BreedingEngine;
import dev.mintychochip.genetics.engine.BreedingResult;
import dev.mintychochip.genetics.engine.RecombinationSettings;
import dev.mintychochip.genetics.model.Genome;
import dev.mintychochip.genetics.model.LocusCatalog;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Public entry points for pure genetics (no NMS).
 */
public final class Genetics {

    private Genetics() {
    }

    public static BreedingEngine breedingEngine(
        final LocusCatalog catalog,
        final RecombinationSettings recombination,
        final MutationSettings mutation,
        final RandomGenerator random
    ) {
        return new BreedingEngine(catalog, recombination, mutation, random);
    }

    public static BreedingEngine breedingEngine(final LocusCatalog catalog, final RandomGenerator random) {
        return BreedingEngine.create(catalog, random);
    }

    public static Optional<BreedingResult> cross(
        final BreedingEngine engine,
        final Genome parentA,
        final Genome parentB
    ) {
        return engine.cross(parentA, parentB);
    }

    public static GenotypeSnapshot genotype(final Genome genome, final LocusCatalog catalog) {
        return GenotypeSnapshot.from(genome, catalog);
    }

    public static PhenotypeSnapshot phenotype(final Genome genome, final LocusCatalog catalog) {
        return new PhenotypeDecoder(catalog).decode(genome);
    }
}
