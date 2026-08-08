package dev.mintychochip.genetics.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Full diploid (or sex-aware) genotype for one animal.
 */
public final class Genome {

    private final Sex sex;
    private final Map<LocusId, GeneCopy> genes;

    private Genome(final Sex sex, final Map<LocusId, GeneCopy> genes) {
        this.sex = Objects.requireNonNull(sex, "sex");
        this.genes = Collections.unmodifiableMap(genes);
    }

    public static Builder builder(final Sex sex) {
        return new Builder(sex);
    }

    public Sex sex() {
        return this.sex;
    }

    public Map<LocusId, GeneCopy> genes() {
        return this.genes;
    }

    public Optional<GeneCopy> get(final LocusId id) {
        return Optional.ofNullable(this.genes.get(id));
    }

    public @Nullable GeneCopy getOrNull(final LocusId id) {
        return this.genes.get(id);
    }

    public static final class Builder {
        private final Sex sex;
        private final Map<LocusId, GeneCopy> genes = new LinkedHashMap<>();

        private Builder(final Sex sex) {
            this.sex = sex;
        }

        public Builder put(final LocusId id, final GeneCopy copy) {
            this.genes.put(Objects.requireNonNull(id), Objects.requireNonNull(copy));
            return this;
        }

        public Builder put(final LocusDefinition locus, final GeneCopy copy) {
            return this.put(locus.id(), copy);
        }

        public Genome build() {
            return new Genome(this.sex, new LinkedHashMap<>(this.genes));
        }
    }
}
