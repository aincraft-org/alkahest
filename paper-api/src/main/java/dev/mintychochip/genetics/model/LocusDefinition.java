package dev.mintychochip.genetics.model;

import java.util.Objects;

/**
 * Catalog entry for a locus: where it lives on the chromosome map and how it inherits.
 */
public final class LocusDefinition {

    private final LocusId id;
    private final int chromosome;
    private final int position;
    private final InheritanceMode inheritance;
    private final DominanceMode dominance;
    private final String phenotypeKey;

    public LocusDefinition(
        final LocusId id,
        final int chromosome,
        final int position,
        final InheritanceMode inheritance,
        final DominanceMode dominance,
        final String phenotypeKey
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.chromosome = chromosome;
        this.position = position;
        this.inheritance = Objects.requireNonNull(inheritance, "inheritance");
        this.dominance = Objects.requireNonNull(dominance, "dominance");
        this.phenotypeKey = Objects.requireNonNull(phenotypeKey, "phenotypeKey");
    }

    public static LocusDefinition autosomal(
        final String key,
        final int chromosome,
        final int position,
        final DominanceMode dominance
    ) {
        final LocusId id = LocusId.of(key);
        return new LocusDefinition(id, chromosome, position, InheritanceMode.AUTOSOMAL, dominance, key);
    }

    public static LocusDefinition xLinked(
        final String key,
        final int position,
        final DominanceMode dominance
    ) {
        final LocusId id = LocusId.of(key);
        // X is modeled as chromosome 0 by convention for sex-linked maps.
        return new LocusDefinition(id, 0, position, InheritanceMode.X_LINKED, dominance, key);
    }

    public static LocusDefinition maternal(final String key) {
        final LocusId id = LocusId.of(key);
        return new LocusDefinition(id, -1, 0, InheritanceMode.MATERNAL, DominanceMode.COMPLETE, key);
    }

    public LocusId id() {
        return this.id;
    }

    public int chromosome() {
        return this.chromosome;
    }

    public int position() {
        return this.position;
    }

    public InheritanceMode inheritance() {
        return this.inheritance;
    }

    public DominanceMode dominance() {
        return this.dominance;
    }

    public String phenotypeKey() {
        return this.phenotypeKey;
    }
}
