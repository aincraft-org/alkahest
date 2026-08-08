package io.papermc.testplugin.genetics;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.AQUA;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

import dev.mintychochip.genetics.Genetics;
import dev.mintychochip.genetics.catalog.DefaultGeneticsCatalog;
import dev.mintychochip.genetics.dna.MutationSettings;
import dev.mintychochip.genetics.dto.BreedGenetics;
import dev.mintychochip.genetics.dto.GenotypeSnapshot;
import dev.mintychochip.genetics.dto.PhenotypeSnapshot;
import dev.mintychochip.genetics.dto.PhenotypeTrait;
import dev.mintychochip.genetics.dto.PhenotypeVariantResolver;
import dev.mintychochip.genetics.engine.BreedingEngine;
import dev.mintychochip.genetics.engine.BreedingResult;
import dev.mintychochip.genetics.engine.RecombinationSettings;
import dev.mintychochip.genetics.model.Allele;
import dev.mintychochip.genetics.model.GeneCopy;
import dev.mintychochip.genetics.model.Genome;
import dev.mintychochip.genetics.model.LocusCatalog;
import dev.mintychochip.genetics.model.Sex;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Offline pure-API demo (Paper {@link BasicCommand} — not plugin.yml).
 *
 * <pre>
 *   /geneticsdemo          — opposite-sex orange×black cross
 *   /geneticsdemo samesex  — show same-sex rejection
 * </pre>
 */
public final class GeneticsDemoCommand implements BasicCommand {

    @Override
    public void execute(@NotNull final CommandSourceStack stack, @NotNull final String[] args) {
        final CommandSender sender = stack.getSender();
        final boolean sameSex = args.length > 0 && args[0].equalsIgnoreCase("samesex");

        final LocusCatalog catalog = DefaultGeneticsCatalog.get();
        final Genome father = Genome.builder(Sex.MALE)
            .put(DefaultGeneticsCatalog.COAT, GeneCopy.hemizygous(Allele.of("ATGAAACCC", "O")))
            .put(DefaultGeneticsCatalog.VITALITY, GeneCopy.diploid(Allele.of("ATGAAAAAA"), Allele.of("ATGAAAAAA")))
            .put(DefaultGeneticsCatalog.MT_VIGOR, GeneCopy.diploid(Allele.of("ATGCCCCCC"), Allele.of("ATGCCCCCC")))
            .build();
        final Genome mother = sameSex
            ? Genome.builder(Sex.MALE)
                .put(DefaultGeneticsCatalog.COAT, GeneCopy.hemizygous(Allele.of("ATGCCCGGG", "o")))
                .put(DefaultGeneticsCatalog.VITALITY, GeneCopy.diploid(Allele.of("ATGGGGGGG"), Allele.of("ATGGGGGGG")))
                .put(DefaultGeneticsCatalog.MT_VIGOR, GeneCopy.diploid(Allele.of("ATGTTTTTT"), Allele.of("ATGTTTTTT")))
                .build()
            : Genome.builder(Sex.FEMALE)
                .put(DefaultGeneticsCatalog.COAT, GeneCopy.diploid(Allele.of("ATGCCCGGG", "o"), Allele.of("ATGCCCGGG", "o")))
                .put(DefaultGeneticsCatalog.VITALITY, GeneCopy.diploid(Allele.of("ATGGGGGGG"), Allele.of("ATGGGGGGG")))
                .put(DefaultGeneticsCatalog.MT_VIGOR, GeneCopy.diploid(Allele.of("ATGTTTTTT"), Allele.of("ATGTTTTTT")))
                .build();

        sender.sendMessage(text("=== genetics pure API demo ===", GOLD));
        sender.sendMessage(text("father: ", GRAY).append(text(describe(father, catalog), YELLOW)));
        sender.sendMessage(text("mother: ", GRAY).append(text(describe(mother, catalog), YELLOW)));

        final BreedingEngine engine = Genetics.breedingEngine(
            catalog,
            RecombinationSettings.NONE,
            MutationSettings.NONE,
            new Random(42L)
        );
        final Optional<BreedingResult> result = Genetics.cross(engine, father, mother);

        if (result.isEmpty()) {
            sender.sendMessage(text("cross rejected (same sex or incompatible).", RED));
            return;
        }

        final Genome child = result.get().child();
        final GenotypeSnapshot childGt = Genetics.genotype(child, catalog);
        final PhenotypeSnapshot childPh = Genetics.phenotype(child, catalog);

        final BreedGenetics payload = new BreedGenetics(
            Genetics.genotype(mother, catalog),
            Genetics.genotype(father, catalog),
            childGt,
            Genetics.phenotype(mother, catalog),
            Genetics.phenotype(father, catalog),
            childPh,
            PhenotypeVariantResolver.resolve(EntityType.CAT, childPh).orElse(null)
        );

        sender.sendMessage(text("child sex: ", GRAY).append(text(payload.childSex().name(), GREEN)));
        final String coat = payload.childPhenotype().getOrNull("coat");
        sender.sendMessage(text("child coat: ", GRAY).append(text(coat != null ? coat : "?", AQUA)));
        sender.sendMessage(text("child traits: ", GRAY).append(text(formatTraits(childPh), AQUA)));
        sender.sendMessage(text("resolved cat variant: ", GRAY).append(text(
            payload.childVariant().map(k -> k.asString()).orElse("(none)"),
            GOLD
        )));
        sender.sendMessage(text(
            "tip: breed animals in-world; EntityBreedEvent carries the same BreedGenetics.",
            GRAY
        ));
    }

    @Override
    public @NotNull Collection<String> suggest(
        @NotNull final CommandSourceStack commandSourceStack,
        @NotNull final String[] args
    ) {
        if (args.length <= 1) {
            return List.of("samesex");
        }
        return List.of();
    }

    @Override
    public @Nullable String permission() {
        return "papertest.geneticsdemo";
    }

    private static String describe(final Genome genome, final LocusCatalog catalog) {
        final PhenotypeSnapshot ph = Genetics.phenotype(genome, catalog);
        final String coat = ph.getOrNull("coat");
        return genome.sex().name() + " coat=" + (coat != null ? coat : "?");
    }

    private static String formatTraits(final PhenotypeSnapshot snapshot) {
        return snapshot.traits().stream()
            .map(PhenotypeTrait::toString)
            .collect(Collectors.joining(", "));
    }
}
