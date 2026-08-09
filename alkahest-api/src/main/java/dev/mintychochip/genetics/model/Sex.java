package dev.mintychochip.genetics.model;

/**
 * Chromosomal sex for XY mammals (calico-style X linkage).
 */
public enum Sex {
    FEMALE,
    MALE;

    public boolean isFemale() {
        return this == FEMALE;
    }

    public boolean isMale() {
        return this == MALE;
    }
}
