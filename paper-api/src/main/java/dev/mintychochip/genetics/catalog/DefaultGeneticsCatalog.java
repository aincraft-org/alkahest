package dev.mintychochip.genetics.catalog;

import dev.mintychochip.genetics.model.DominanceMode;
import dev.mintychochip.genetics.model.LocusCatalog;
import dev.mintychochip.genetics.model.LocusDefinition;

/**
 * Minimal mammal catalog used until species-specific catalogs exist.
 * Includes sex-linked coat (calico-ready), one autosomal, one maternal locus.
 */
public final class DefaultGeneticsCatalog {

    public static final LocusDefinition COAT = LocusDefinition.xLinked("coat", 10, DominanceMode.CODOMINANT);
    public static final LocusDefinition VITALITY = LocusDefinition.autosomal("vitality", 1, 40, DominanceMode.COMPLETE);
    public static final LocusDefinition MT_VIGOR = LocusDefinition.maternal("mt-vigor");

    private static final LocusCatalog INSTANCE = LocusCatalog.builder()
        .add(COAT)
        .add(VITALITY)
        .add(MT_VIGOR)
        .build();

    private DefaultGeneticsCatalog() {
    }

    public static LocusCatalog get() {
        return INSTANCE;
    }
}
