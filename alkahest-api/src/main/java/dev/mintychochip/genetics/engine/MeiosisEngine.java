package dev.mintychochip.genetics.engine;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Meiosis: for loci ordered along one chromosome, produce a haploid chromatid
 * (one allele pick per locus) with optional crossing-over between homologs.
 */
public final class MeiosisEngine {

    private final RandomGenerator random;
    private final RecombinationSettings settings;

    public MeiosisEngine(final RandomGenerator random, final RecombinationSettings settings) {
        this.random = random;
        this.settings = settings;
    }

    /**
     * @param positions sorted ascending locus positions on one chromosome
     * @return {@code false} = homolog A, {@code true} = homolog B, one entry per position
     */
    public boolean[] produceGameteChromatid(final int[] positions) {
        final int locusCount = positions.length;
        final boolean[] picks = new boolean[locusCount];
        if (locusCount == 0) {
            return picks;
        }
        if (locusCount == 1) {
            picks[0] = this.random.nextBoolean();
            return picks;
        }
        final int crossovers = this.settings.sampleCrossoverCount(this.random);
        final int minPos = positions[0];
        final int maxPos = positions[locusCount - 1];
        final int span = Math.max(1, maxPos - minPos + 1);
        for (int c = 0; c < crossovers; c++) {
            final int cut = minPos + this.random.nextInt(span);
            for (int i = 0; i < locusCount; i++) {
                if (positions[i] > cut) {
                    picks[i] = !picks[i];
                }
            }
        }
        // Gamete receives one of the two recombinant chromatids at random.
        if (this.random.nextBoolean()) {
            for (int i = 0; i < locusCount; i++) {
                picks[i] = !picks[i];
            }
        }
        return picks;
    }

    public boolean[] produceGameteChromatid(final List<Integer> positions) {
        final int[] arr = new int[positions.size()];
        for (int i = 0; i < positions.size(); i++) {
            arr[i] = positions.get(i);
        }
        return this.produceGameteChromatid(arr);
    }
}
