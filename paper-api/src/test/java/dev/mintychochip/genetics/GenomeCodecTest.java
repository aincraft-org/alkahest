package dev.mintychochip.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.genetics.catalog.DefaultGeneticsCatalog;
import dev.mintychochip.genetics.dna.MutationSettings;
import dev.mintychochip.genetics.dto.PhenotypeDecoder;
import dev.mintychochip.genetics.engine.BreedingEngine;
import dev.mintychochip.genetics.engine.GeneticMatePolicy;
import dev.mintychochip.genetics.engine.RecombinationSettings;
import dev.mintychochip.genetics.io.GenomeCodec;
import dev.mintychochip.genetics.model.Allele;
import dev.mintychochip.genetics.model.GeneCopy;
import dev.mintychochip.genetics.model.Genome;
import dev.mintychochip.genetics.model.GenomeGenerator;
import dev.mintychochip.genetics.model.LocusCatalog;
import dev.mintychochip.genetics.model.Sex;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Codec round-trip and pure-API exercise of default catalog + façade entry points.
 */
public class GenomeCodecTest {

    @Test
    public void encodeDecodePreservesGenome() {
        final LocusCatalog catalog = DefaultGeneticsCatalog.get();
        final Genome original = new GenomeGenerator(catalog, new Random(99L)).generate(Sex.FEMALE);
        final String json = GenomeCodec.encode(original);
        final Genome decoded = GenomeCodec.decode(json);
        assertTrue(GenomeCodec.deepEquals(original, decoded), "round-trip must preserve alleles");
        assertEquals(original.sex(), decoded.sex());
        assertEquals(original.genes().size(), decoded.genes().size());
    }

    @Test
    public void hemizygousMaleRoundTrips() {
        final Genome male = Genome.builder(Sex.MALE)
            .put(DefaultGeneticsCatalog.COAT, GeneCopy.hemizygous(Allele.of("ATGAAACCC", "O")))
            .put(DefaultGeneticsCatalog.VITALITY, GeneCopy.diploid(Allele.of("ATGCCCCCC"), Allele.of("ATGGGGGGG")))
            .put(DefaultGeneticsCatalog.MT_VIGOR, GeneCopy.diploid(Allele.of("ATGTTTTTT"), Allele.of("ATGTTTTTT")))
            .build();
        final Genome back = GenomeCodec.decode(GenomeCodec.encode(male));
        assertTrue(GenomeCodec.deepEquals(male, back));
        assertTrue(back.getOrNull(DefaultGeneticsCatalog.COAT.id()).isHemizygous());
    }

    @Test
    public void pureApiCrossWithMutationAndPhenotypeDecode() {
        final LocusCatalog catalog = DefaultGeneticsCatalog.get();
        final Allele orange = Allele.of("ATGAAACCC", "O");
        final Allele black = Allele.of("ATGCCCGGG", "o");
        final Allele vit = Allele.of("ATGAAAAAA");
        final Allele mt = Allele.of("ATGTTTTTT");

        final Genome father = Genome.builder(Sex.MALE)
            .put(DefaultGeneticsCatalog.COAT, GeneCopy.hemizygous(orange))
            .put(DefaultGeneticsCatalog.VITALITY, GeneCopy.diploid(vit, vit))
            .put(DefaultGeneticsCatalog.MT_VIGOR, GeneCopy.diploid(mt, mt))
            .build();
        final Genome mother = Genome.builder(Sex.FEMALE)
            .put(DefaultGeneticsCatalog.COAT, GeneCopy.diploid(black, black))
            .put(DefaultGeneticsCatalog.VITALITY, GeneCopy.diploid(vit, vit))
            .put(DefaultGeneticsCatalog.MT_VIGOR, GeneCopy.diploid(mt, mt))
            .build();

        assertTrue(GeneticMatePolicy.allowsMate(father, mother));
        assertTrue(!GeneticMatePolicy.allowsMate(father, father));

        final BreedingEngine engine = Genetics.breedingEngine(
            catalog,
            RecombinationSettings.NONE,
            MutationSettings.DEFAULT,
            new Random(12345L)
        );
        final Genome child = engine.cross(father, mother).orElseThrow().child();
        final var genotype = Genetics.genotype(child, catalog);
        final var phenotype = Genetics.phenotype(child, catalog);

        assertEquals(child.sex(), genotype.sex());
        assertTrue(child.get(DefaultGeneticsCatalog.COAT.id()).isPresent());
        // Maternal mt always from dam sequences (may be mutated at bases)
        assertTrue(child.get(DefaultGeneticsCatalog.MT_VIGOR.id()).isPresent());

        if (child.sex() == Sex.FEMALE) {
            final String coat = phenotype.getOrNull("coat");
            // With no mut on labels path, hetero O/o → calico; mutation may drop labels → FUNC+FUNC
            assertTrue(coat != null && !coat.isEmpty());
        } else {
            assertTrue(child.getOrNull(DefaultGeneticsCatalog.COAT.id()).isHemizygous());
            // Son never calico
            assertTrue(!"calico".equals(phenotype.getOrNull("coat")));
        }

        // Forced hetero female still calico via decoder
        final Genome calico = Genome.builder(Sex.FEMALE)
            .put(DefaultGeneticsCatalog.COAT, GeneCopy.diploid(orange, black))
            .put(DefaultGeneticsCatalog.VITALITY, GeneCopy.diploid(vit, vit))
            .put(DefaultGeneticsCatalog.MT_VIGOR, GeneCopy.diploid(mt, mt))
            .build();
        assertEquals("calico", new PhenotypeDecoder(catalog).decode(calico).getOrNull("coat"));
    }
}
