package dev.mintychochip.genetics.engine;

import java.util.random.RandomGenerator;

/**
 * How often crossing-over happens during meiosis (0, 1, or 2 chiasmata per chromosome).
 */
public final class RecombinationSettings {

    private final double noCrossoverChance;
    private final double singleCrossoverChance;
    private final double doubleCrossoverChance;

    public static final RecombinationSettings DEFAULT = new RecombinationSettings(0.35, 0.50, 0.15);

    /** No crossovers: whole chromatid inherited as a block (tests / strong linkage). */
    public static final RecombinationSettings NONE = new RecombinationSettings(1.0, 0.0, 0.0);

    public RecombinationSettings(
        final double noCrossoverChance,
        final double singleCrossoverChance,
        final double doubleCrossoverChance
    ) {
        final double sum = noCrossoverChance + singleCrossoverChance + doubleCrossoverChance;
        if (sum <= 0.0) {
            throw new IllegalArgumentException("Crossover probabilities must sum to a positive value");
        }
        this.noCrossoverChance = noCrossoverChance / sum;
        this.singleCrossoverChance = singleCrossoverChance / sum;
        this.doubleCrossoverChance = doubleCrossoverChance / sum;
    }

    public int sampleCrossoverCount(final RandomGenerator random) {
        final double roll = random.nextDouble();
        if (roll < this.noCrossoverChance) {
            return 0;
        }
        if (roll < this.noCrossoverChance + this.singleCrossoverChance) {
            return 1;
        }
        return this.doubleCrossoverChance > 0.0 ? 2 : 0;
    }

    public double noCrossoverChance() {
        return this.noCrossoverChance;
    }

    public double singleCrossoverChance() {
        return this.singleCrossoverChance;
    }

    public double doubleCrossoverChance() {
        return this.doubleCrossoverChance;
    }
}
