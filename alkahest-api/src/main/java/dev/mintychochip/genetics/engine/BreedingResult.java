package dev.mintychochip.genetics.engine;

import dev.mintychochip.genetics.model.Genome;
import java.util.Objects;

/**
 * Outcome of a successful genetic cross.
 */
public final class BreedingResult {

    private final Genome child;

    public BreedingResult(final Genome child) {
        this.child = Objects.requireNonNull(child, "child");
    }

    public Genome child() {
        return this.child;
    }
}
