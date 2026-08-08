package dev.mintychochip.genetics.dto;

import java.util.Objects;

/**
 * One decoded phenotypic trait (e.g. key {@code "coat"}, value {@code "calico"}).
 */
public final class PhenotypeTrait {

    private final String key;
    private final String value;

    public PhenotypeTrait(final String key, final String value) {
        this.key = Objects.requireNonNull(key, "key");
        this.value = Objects.requireNonNull(value, "value");
    }

    public String key() {
        return this.key;
    }

    public String value() {
        return this.value;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof PhenotypeTrait other
            && this.key.equals(other.key)
            && this.value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.key, this.value);
    }

    @Override
    public String toString() {
        return this.key + "=" + this.value;
    }
}
