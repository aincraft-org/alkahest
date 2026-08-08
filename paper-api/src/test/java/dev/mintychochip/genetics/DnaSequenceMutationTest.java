package dev.mintychochip.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.genetics.dna.DnaSequence;
import dev.mintychochip.genetics.dna.MutationSettings;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class DnaSequenceMutationTest {

    @Test
    public void pointSubstitutionChangesOneBase() {
        final DnaSequence original = DnaSequence.of("ATGAAACCC");
        final DnaSequence mutated = original.substituteAt(3, 'T');
        assertEquals('T', mutated.baseAt(3));
        assertEquals(1, original.hammingDistance(mutated));
        assertEquals(original.length(), mutated.length());
    }

    @Test
    public void stopCodonIsNotFunctional() {
        // TAA is stop
        assertFalse(DnaSequence.of("ATGTAA").isFunctional());
        assertTrue(DnaSequence.of("ATGAAACCC").isFunctional());
    }

    @Test
    public void heavySubstitutionRateEventuallyMutates() {
        final DnaSequence original = DnaSequence.of("ATGAAACCCGGG");
        final MutationSettings heavy = new MutationSettings(0.5, 0.0, 0.0, 1);
        final Random random = new Random(42L);
        boolean changed = false;
        for (int i = 0; i < 20; i++) {
            final DnaSequence next = original.mutate(random, heavy);
            if (!next.equals(original)) {
                changed = true;
                break;
            }
        }
        assertTrue(changed, "expected at least one point mutation under high substitution rate");
    }

    @Test
    public void noneSettingsPreservesSequence() {
        final DnaSequence original = DnaSequence.of("ATGAAACCC");
        assertEquals(original, original.mutate(new Random(1L), MutationSettings.NONE));
    }

    @Test
    public void functionalOrfHasNoStops() {
        final DnaSequence orf = DnaSequence.functionalOrf(new Random(7L), 4);
        assertEquals(12, orf.length());
        assertTrue(orf.isFunctional());
    }
}
