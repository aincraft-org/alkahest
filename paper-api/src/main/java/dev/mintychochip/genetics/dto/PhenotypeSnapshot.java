package dev.mintychochip.genetics.dto;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Decoded traits for gameplay / UI.
 */
public final class PhenotypeSnapshot {

    private final List<PhenotypeTrait> traits;

    public PhenotypeSnapshot(final List<PhenotypeTrait> traits) {
        this.traits = List.copyOf(traits);
    }

    public List<PhenotypeTrait> traits() {
        return this.traits;
    }

    public Optional<String> get(final String key) {
        for (final PhenotypeTrait trait : this.traits) {
            if (trait.key().equals(key)) {
                return Optional.of(trait.value());
            }
        }
        return Optional.empty();
    }

    public @Nullable String getOrNull(final String key) {
        return this.get(key).orElse(null);
    }
}
