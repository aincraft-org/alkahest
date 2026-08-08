package dev.mintychochip.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.genetics.dna.DnaSequence;
import dev.mintychochip.genetics.dna.MutationSettings;
import dev.mintychochip.genetics.dto.PhenotypeDecoder;
import dev.mintychochip.genetics.dto.PhenotypeSnapshot;
import dev.mintychochip.genetics.dto.Zygosity;
import dev.mintychochip.genetics.engine.BreedingEngine;
import dev.mintychochip.genetics.engine.BreedingResult;
import dev.mintychochip.genetics.engine.RecombinationSettings;
import dev.mintychochip.genetics.model.Allele;
import dev.mintychochip.genetics.model.DominanceMode;
import dev.mintychochip.genetics.model.GeneCopy;
import dev.mintychochip.genetics.model.Genome;
import dev.mintychochip.genetics.model.LocusCatalog;
import dev.mintychochip.genetics.model.LocusDefinition;
import dev.mintychochip.genetics.model.Sex;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class BreedingEngineTest {

    private static final LocusDefinition COAT = LocusDefinition.xLinked("coat", 10, DominanceMode.CODOMINANT);
    private static final LocusDefinition SPEED = LocusDefinition.autosomal("speed", 1, 50, DominanceMode.COMPLETE);
    private static final LocusDefinition STAMINA = LocusDefinition.autosomal("stamina", 1, 200, DominanceMode.COMPLETE);
    private static final LocusDefinition MT = LocusDefinition.maternal("mt-vigor");

    private static final Allele ORANGE = Allele.of("ATGAAACCC", "O");
    private static final Allele BLACK = Allele.of("ATGCCCGGG", "o");
    private static final Allele SPEED_A = Allele.of("ATGAAAAAA", "SA");
    private static final Allele SPEED_B = Allele.of("ATGCCCCCC", "SB");
    private static final Allele STAM_A = Allele.of("ATGGGAAAA", "TA");
    private static final Allele STAM_B = Allele.of("ATGTCCCCC", "TB");
    private static final Allele MT_A = Allele.of("ATGTTTTT", "M1");
    private static final Allele MT_B = Allele.of("ATGGGGGG", "M2");

    private static LocusCatalog catalog() {
        return LocusCatalog.builder()
            .add(COAT)
            .add(SPEED)
            .add(STAMINA)
            .add(MT)
            .build();
    }

    @Test
    public void sameSexCannotBreed() {
        final LocusCatalog catalog = catalog();
        final BreedingEngine engine = new BreedingEngine(
            catalog, RecombinationSettings.NONE, MutationSettings.NONE, new Random(1L)
        );
        final Genome maleA = male(ORANGE, SPEED_A, STAM_A, MT_A);
        final Genome maleB = male(BLACK, SPEED_B, STAM_B, MT_B);
        assertTrue(engine.cross(maleA, maleB).isEmpty());
    }

    @Test
    public void sonsGetXOnlyFromDam() {
        final LocusCatalog catalog = catalog();
        // Force child sex male: try many seeds until we get a son
        boolean sawSon = false;
        for (long seed = 0; seed < 200; seed++) {
            final BreedingEngine engine = new BreedingEngine(
                catalog, RecombinationSettings.NONE, MutationSettings.NONE, new Random(seed)
            );
            final Genome father = male(ORANGE, SPEED_A, STAM_A, MT_A);
            final Genome mother = Genome.builder(Sex.FEMALE)
                .put(COAT, GeneCopy.diploid(BLACK, BLACK))
                .put(SPEED, GeneCopy.diploid(SPEED_B, SPEED_B))
                .put(STAMINA, GeneCopy.diploid(STAM_B, STAM_B))
                .put(MT, GeneCopy.diploid(MT_B, MT_B))
                .build();
            final Optional<BreedingResult> result = engine.cross(father, mother);
            assertTrue(result.isPresent());
            final Genome child = result.get().child();
            if (child.sex() != Sex.MALE) {
                continue;
            }
            sawSon = true;
            final GeneCopy coat = child.getOrNull(COAT.id());
            assertNotNull(coat);
            assertTrue(coat.isHemizygous());
            // Must be dam's black, never father's orange
            assertEquals(BLACK.sequence(), coat.alleleA().sequence());
            break;
        }
        assertTrue(sawSon, "expected at least one male child in seed scan");
    }

    @Test
    public void heterozygousFemaleCoatDecodesAsCalico() {
        final LocusCatalog catalog = catalog();
        final Genome female = Genome.builder(Sex.FEMALE)
            .put(COAT, GeneCopy.diploid(ORANGE, BLACK))
            .put(SPEED, GeneCopy.diploid(SPEED_A, SPEED_A))
            .put(STAMINA, GeneCopy.diploid(STAM_A, STAM_A))
            .put(MT, GeneCopy.diploid(MT_A, MT_A))
            .build();
        final PhenotypeSnapshot pheno = new PhenotypeDecoder(catalog).decode(female);
        assertEquals("calico", pheno.getOrNull("coat"));
    }

    @Test
    public void daughtersCanBeCalicoFromOrangeSireBlackDam() {
        final LocusCatalog catalog = catalog();
        boolean sawCalicoDaughter = false;
        boolean sawDaughter = false;
        for (long seed = 0; seed < 300; seed++) {
            final BreedingEngine engine = new BreedingEngine(
                catalog, RecombinationSettings.NONE, MutationSettings.NONE, new Random(seed)
            );
            final Genome father = male(ORANGE, SPEED_A, STAM_A, MT_A);
            final Genome mother = Genome.builder(Sex.FEMALE)
                .put(COAT, GeneCopy.diploid(BLACK, BLACK))
                .put(SPEED, GeneCopy.diploid(SPEED_A, SPEED_A))
                .put(STAMINA, GeneCopy.diploid(STAM_A, STAM_A))
                .put(MT, GeneCopy.diploid(MT_A, MT_A))
                .build();
            final Genome child = engine.cross(father, mother).orElseThrow().child();
            if (child.sex() != Sex.FEMALE) {
                continue;
            }
            sawDaughter = true;
            final PhenotypeSnapshot pheno = new PhenotypeDecoder(catalog).decode(child);
            if ("calico".equals(pheno.getOrNull("coat"))) {
                sawCalicoDaughter = true;
                final GeneCopy coat = child.getOrNull(COAT.id());
                assertNotNull(coat);
                assertTrue(coat.isDiploid());
                assertTrue(coat.isHeterozygous());
                break;
            }
        }
        assertTrue(sawDaughter, "expected at least one female child in seed scan");
        assertTrue(sawCalicoDaughter, "expected O/o daughter phenotype calico");
    }

    @Test
    public void malesNeverDecodeAsCalico() {
        final LocusCatalog catalog = catalog();
        final PhenotypeDecoder decoder = new PhenotypeDecoder(catalog);
        final Genome male = male(ORANGE, SPEED_A, STAM_A, MT_A);
        final PhenotypeSnapshot pheno = decoder.decode(male);
        assertEquals("O", pheno.getOrNull("coat"));
        assertFalse("calico".equals(pheno.getOrNull("coat")));
    }

    @Test
    public void maternalLocusIgnoresSire() {
        final LocusCatalog catalog = catalog();
        for (long seed = 0; seed < 50; seed++) {
            final BreedingEngine engine = new BreedingEngine(
                catalog, RecombinationSettings.NONE, MutationSettings.NONE, new Random(seed)
            );
            final Genome father = male(ORANGE, SPEED_A, STAM_A, MT_A);
            final Genome mother = Genome.builder(Sex.FEMALE)
                .put(COAT, GeneCopy.diploid(BLACK, BLACK))
                .put(SPEED, GeneCopy.diploid(SPEED_A, SPEED_A))
                .put(STAMINA, GeneCopy.diploid(STAM_A, STAM_A))
                .put(MT, GeneCopy.diploid(MT_B, MT_B))
                .build();
            final Genome child = engine.cross(father, mother).orElseThrow().child();
            final GeneCopy mt = child.getOrNull(MT.id());
            assertNotNull(mt);
            assertEquals(MT_B.sequence(), mt.alleleA().sequence());
            assertEquals(MT_B.sequence(), mt.alleleB().sequence());
        }
    }

    @Test
    public void zeroCrossoverKeepsLinkedPhase() {
        // Father homozygous SA/TA vs SB/TB on same chromosome — gametes should be
        // pure parental combinations when recombination is off... actually for
        // diploid hetero father SA/SB at speed and TA/TB at stamina in coupling
        // SA-TA / SB-TB, no crossover means child gets SA+TA or SB+TB from father.
        final LocusCatalog catalog = catalog();
        final Genome father = Genome.builder(Sex.MALE)
            .put(COAT, GeneCopy.hemizygous(ORANGE))
            .put(SPEED, GeneCopy.diploid(SPEED_A, SPEED_B))
            .put(STAMINA, GeneCopy.diploid(STAM_A, STAM_B))
            .put(MT, GeneCopy.diploid(MT_A, MT_A))
            .build();
        final Genome mother = Genome.builder(Sex.FEMALE)
            .put(COAT, GeneCopy.diploid(BLACK, BLACK))
            .put(SPEED, GeneCopy.diploid(SPEED_A, SPEED_A))
            .put(STAMINA, GeneCopy.diploid(STAM_A, STAM_A))
            .put(MT, GeneCopy.diploid(MT_A, MT_A))
            .build();

        for (long seed = 0; seed < 100; seed++) {
            final BreedingEngine engine = new BreedingEngine(
                catalog, RecombinationSettings.NONE, MutationSettings.NONE, new Random(seed)
            );
            final Genome child = engine.cross(father, mother).orElseThrow().child();
            // Mother always contributes SA and TA. Child = fatherPick + mother allele.
            final GeneCopy speed = child.getOrNull(SPEED.id());
            final GeneCopy stamina = child.getOrNull(STAMINA.id());
            assertNotNull(speed);
            assertNotNull(stamina);
            final Allele fatherSpeed = speed.isHomozygous() ? SPEED_A : SPEED_B;
            final Allele fatherStam = stamina.isHomozygous() ? STAM_A : STAM_B;
            // Coupling phase SA-TA and SB-TB: fatherSpeed A iff fatherStam A
            if (fatherSpeed.sequence().equals(SPEED_A.sequence())) {
                assertEquals(STAM_A.sequence(), fatherStam.sequence());
            } else {
                assertEquals(STAM_B.sequence(), fatherStam.sequence());
            }
        }
    }

    @Test
    public void breedingAppliesPointMutationsWhenEnabled() {
        final LocusCatalog catalog = catalog();
        final MutationSettings heavy = new MutationSettings(0.3, 0.0, 0.0, 1);
        boolean mutated = false;
        for (long seed = 0; seed < 100; seed++) {
            final BreedingEngine engine = new BreedingEngine(
                catalog, RecombinationSettings.NONE, heavy, new Random(seed)
            );
            final Genome father = male(ORANGE, SPEED_A, STAM_A, MT_A);
            final Genome mother = Genome.builder(Sex.FEMALE)
                .put(COAT, GeneCopy.diploid(BLACK, BLACK))
                .put(SPEED, GeneCopy.diploid(SPEED_A, SPEED_A))
                .put(STAMINA, GeneCopy.diploid(STAM_A, STAM_A))
                .put(MT, GeneCopy.diploid(MT_A, MT_A))
                .build();
            final Genome child = engine.cross(father, mother).orElseThrow().child();
            for (final var entry : child.genes().entrySet()) {
                final GeneCopy copy = entry.getValue();
                if (!isParentalSequence(copy.alleleA())
                    || (copy.alleleB() != null && !isParentalSequence(copy.alleleB()))) {
                    mutated = true;
                    break;
                }
            }
            if (mutated) {
                break;
            }
        }
        assertTrue(mutated, "expected germline point mutation under high rate");
    }

    @Test
    public void genotypeSnapshotReportsZygosity() {
        final LocusCatalog catalog = catalog();
        final Genome female = Genome.builder(Sex.FEMALE)
            .put(COAT, GeneCopy.diploid(ORANGE, BLACK))
            .put(SPEED, GeneCopy.diploid(SPEED_A, SPEED_A))
            .put(STAMINA, GeneCopy.diploid(STAM_A, STAM_B))
            .put(MT, GeneCopy.diploid(MT_A, MT_A))
            .build();
        final var snap = Genetics.genotype(female, catalog);
        assertEquals(Sex.FEMALE, snap.sex());
        assertTrue(snap.loci().stream().anyMatch(l ->
            l.locus().equals(COAT.id()) && l.zygosity() == Zygosity.HETEROZYGOUS));
        assertTrue(snap.loci().stream().anyMatch(l ->
            l.locus().equals(SPEED.id()) && l.zygosity() == Zygosity.HOMOZYGOUS));
    }

    private static Genome male(final Allele coat, final Allele speed, final Allele stam, final Allele mt) {
        return Genome.builder(Sex.MALE)
            .put(COAT, GeneCopy.hemizygous(coat))
            .put(SPEED, GeneCopy.diploid(speed, speed))
            .put(STAMINA, GeneCopy.diploid(stam, stam))
            .put(MT, GeneCopy.diploid(mt, mt))
            .build();
    }

    private static boolean isParentalSequence(final Allele allele) {
        final DnaSequence s = allele.sequence();
        return s.equals(ORANGE.sequence())
            || s.equals(BLACK.sequence())
            || s.equals(SPEED_A.sequence())
            || s.equals(SPEED_B.sequence())
            || s.equals(STAM_A.sequence())
            || s.equals(STAM_B.sequence())
            || s.equals(MT_A.sequence())
            || s.equals(MT_B.sequence());
    }
}
