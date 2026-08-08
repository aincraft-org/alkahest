package dev.mintychochip.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.genetics.catalog.DefaultGeneticsCatalog;
import dev.mintychochip.genetics.dto.PhenotypeDecoder;
import dev.mintychochip.genetics.dto.PhenotypeSnapshot;
import dev.mintychochip.genetics.dto.PhenotypeTrait;
import dev.mintychochip.genetics.dto.PhenotypeVariantResolver;
import dev.mintychochip.genetics.model.Allele;
import dev.mintychochip.genetics.model.GeneCopy;
import dev.mintychochip.genetics.model.Genome;
import dev.mintychochip.genetics.model.Sex;
import java.util.List;
import java.util.Optional;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

public class PhenotypeVariantResolverTest {

    @Test
    public void classicOrangeMapsToRedCat() {
        assertEquals(
            NamespacedKey.minecraft("red"),
            PhenotypeVariantResolver.coatToCatVariant("O").orElseThrow()
        );
    }

    @Test
    public void classicNonOrangeMapsToBlackCat() {
        assertEquals(
            NamespacedKey.minecraft("black"),
            PhenotypeVariantResolver.coatToCatVariant("o").orElseThrow()
        );
    }

    @Test
    public void calicoMapsToCalicoCat() {
        assertEquals(
            NamespacedKey.minecraft("calico"),
            PhenotypeVariantResolver.coatToCatVariant("calico").orElseThrow()
        );
    }

    @Test
    public void heteroFemaleCoatDecodesToCalicoThenCatVariant() {
        final Genome female = Genome.builder(Sex.FEMALE)
            .put(DefaultGeneticsCatalog.COAT, GeneCopy.diploid(
                Allele.of("ATGAAACCC", "O"),
                Allele.of("ATGCCCGGG", "o")
            ))
            .build();
        final PhenotypeSnapshot phenotype = new PhenotypeDecoder(DefaultGeneticsCatalog.get()).decode(female);
        assertEquals(Optional.of("calico"), phenotype.get("coat"));
        assertEquals(
            Optional.of(NamespacedKey.minecraft("calico")),
            PhenotypeVariantResolver.resolve(EntityType.CAT, phenotype)
        );
    }

    @Test
    public void maleOrangeNeverCalico() {
        final Genome male = Genome.builder(Sex.MALE)
            .put(DefaultGeneticsCatalog.COAT, GeneCopy.hemizygous(Allele.of("ATGAAACCC", "O")))
            .build();
        final PhenotypeSnapshot phenotype = new PhenotypeDecoder(DefaultGeneticsCatalog.get()).decode(male);
        assertEquals(Optional.of("O"), phenotype.get("coat"));
        assertEquals(
            Optional.of(NamespacedKey.minecraft("red")),
            PhenotypeVariantResolver.resolve(EntityType.CAT, phenotype)
        );
    }

    @Test
    public void cowOrangeSystemDoesNotForceClimateVariant() {
        final PhenotypeSnapshot phenotype = new PhenotypeSnapshot(List.of(new PhenotypeTrait("coat", "O")));
        assertTrue(PhenotypeVariantResolver.resolve(EntityType.COW, phenotype).isEmpty());
    }

    @Test
    public void cowExplicitClimateLabelMaps() {
        final PhenotypeSnapshot phenotype = new PhenotypeSnapshot(List.of(new PhenotypeTrait("coat", "warm")));
        assertEquals(
            Optional.of(NamespacedKey.minecraft("warm")),
            PhenotypeVariantResolver.resolve(EntityType.COW, phenotype)
        );
    }

    @Test
    public void wolfOrangeMapsToRusty() {
        assertEquals(
            NamespacedKey.minecraft("rusty"),
            PhenotypeVariantResolver.coatToWolfVariant("O").orElseThrow()
        );
    }
}
