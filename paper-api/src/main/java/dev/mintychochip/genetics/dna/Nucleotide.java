package dev.mintychochip.genetics.dna;

import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * The four DNA bases.
 */
public enum Nucleotide {
    A('A'),
    T('T'),
    C('C'),
    G('G');

    private final char symbol;

    Nucleotide(final char symbol) {
        this.symbol = symbol;
    }

    public char symbol() {
        return this.symbol;
    }

    public static Nucleotide fromSymbol(final char symbol) {
        return switch (symbol) {
            case 'A', 'a' -> A;
            case 'T', 't' -> T;
            case 'C', 'c' -> C;
            case 'G', 'g' -> G;
            default -> throw new IllegalArgumentException("Not a DNA base: " + symbol);
        };
    }

    public static Nucleotide random(final RandomGenerator random) {
        final Nucleotide[] values = values();
        return values[random.nextInt(values.length)];
    }

    public static Nucleotide random() {
        return random(ThreadLocalRandom.current());
    }
}
