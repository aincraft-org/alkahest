package dev.mintychochip.genetics.engine;

import dev.mintychochip.genetics.model.Genome;
import dev.mintychochip.genetics.model.Sex;
import java.util.Objects;

/**
 * Pure mate gate used by server hooks and tests.
 */
public final class GeneticMatePolicy {

    private GeneticMatePolicy() {
    }

    /**
     * Opposite-sex only. Same-sex genomes cannot breed.
     */
    public static boolean allowsMate(final Genome a, final Genome b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        return a.sex() != b.sex();
    }

    public static boolean isMale(final Genome genome) {
        return genome.sex() == Sex.MALE;
    }

    public static boolean isFemale(final Genome genome) {
        return genome.sex() == Sex.FEMALE;
    }
}
