package dev.mintychochip.genetics.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Ordered registry of locus definitions used for meiosis and phenotype decode.
 */
public final class LocusCatalog {

    private final Map<LocusId, LocusDefinition> byId;

    private LocusCatalog(final Map<LocusId, LocusDefinition> byId) {
        this.byId = Collections.unmodifiableMap(byId);
    }

    public static LocusCatalog of(final Collection<LocusDefinition> loci) {
        final Map<LocusId, LocusDefinition> map = new LinkedHashMap<>();
        for (final LocusDefinition locus : loci) {
            if (map.put(locus.id(), locus) != null) {
                throw new IllegalArgumentException("Duplicate locus: " + locus.id());
            }
        }
        return new LocusCatalog(map);
    }

    public static Builder builder() {
        return new Builder();
    }

    public @Nullable LocusDefinition get(final LocusId id) {
        return this.byId.get(id);
    }

    public LocusDefinition require(final LocusId id) {
        final LocusDefinition def = this.byId.get(id);
        if (def == null) {
            throw new IllegalArgumentException("Unknown locus: " + id);
        }
        return def;
    }

    public Collection<LocusDefinition> all() {
        return this.byId.values();
    }

    public int size() {
        return this.byId.size();
    }

    public static final class Builder {
        private final Map<LocusId, LocusDefinition> byId = new LinkedHashMap<>();

        public Builder add(final LocusDefinition definition) {
            Objects.requireNonNull(definition, "definition");
            if (this.byId.put(definition.id(), definition) != null) {
                throw new IllegalArgumentException("Duplicate locus: " + definition.id());
            }
            return this;
        }

        public LocusCatalog build() {
            return new LocusCatalog(new LinkedHashMap<>(this.byId));
        }
    }
}
