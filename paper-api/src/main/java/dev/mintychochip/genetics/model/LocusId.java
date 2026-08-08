package dev.mintychochip.genetics.model;

import java.util.Objects;

/**
 * Stable identifier for a locus in a catalog (e.g. {@code "coat.orange"}, {@code "stamina.1"}).
 */
public final class LocusId implements Comparable<LocusId> {

    private final String key;

    private LocusId(final String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("LocusId key cannot be blank");
        }
        this.key = key;
    }

    public static LocusId of(final String key) {
        return new LocusId(key);
    }

    public String key() {
        return this.key;
    }

    @Override
    public int compareTo(final LocusId other) {
        return this.key.compareTo(other.key);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof LocusId other && this.key.equals(other.key);
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public String toString() {
        return this.key;
    }
}
