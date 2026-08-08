package dev.mintychochip.genetics;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.support.environment.Normal;
import org.junit.jupiter.api.Test;

/**
 * Structural proof that the Animal breed path calls the genetics façade.
 */
@Normal
public class AnimalGeneticsHooksPresentTest {

    @Test
    public void animalSourceWiresGeneticsFacade() throws Exception {
        final String src = readProjectFile(
            "src/minecraft/java/net/minecraft/world/entity/animal/Animal.java",
            "paper-server/src/minecraft/java/net/minecraft/world/entity/animal/Animal.java"
        );
        assertTrue(src.contains("dev.mintychochip.genetics.AnimalGenetics.allowsMate"), "canMate must call AnimalGenetics.allowsMate");
        assertTrue(src.contains("dev.mintychochip.genetics.AnimalGenetics.prepareBreed"), "spawnChildFromBreeding must call AnimalGenetics.prepareBreed");
        assertTrue(src.contains("dev.mintychochip.genetics.AnimalGenetics.discardBreed"), "cancel path must discard child genome");
        assertTrue(src.contains("dev.mintychochip.genetics.AnimalGenetics.applyChildAppearance"), "successful breed must apply phenotype to registry variants");
        assertTrue(src.contains("breedGenetics"), "EntityBreedEvent must receive genetics metadata");
        assertTrue(src.contains("dev.mintychochip.genetics.AnimalGenetics.save"), "save path must persist genome");
        assertTrue(src.contains("dev.mintychochip.genetics.AnimalGenetics.load"), "load path must restore genome");
    }

    @Test
    public void animalPatchDocumentsGeneticsHooks() throws Exception {
        final String text = readProjectFile(
            "patches/sources/net/minecraft/world/entity/animal/Animal.java.patch",
            "paper-server/patches/sources/net/minecraft/world/entity/animal/Animal.java.patch"
        );
        assertTrue(text.contains("AnimalGenetics.allowsMate"), "patch must include mate gate");
        assertTrue(text.contains("AnimalGenetics.prepareBreed") || text.contains("AnimalGenetics.onBreed"), "patch must include breed hook");
        assertTrue(text.contains("AnimalGenetics.save"), "patch must include save hook");
    }

    private static String readProjectFile(final String... relativeCandidates) throws Exception {
        Path cwd = Path.of("").toAbsolutePath();
        for (int i = 0; i < 6; i++) {
            for (final String rel : relativeCandidates) {
                final Path candidate = cwd.resolve(rel);
                if (Files.isRegularFile(candidate)) {
                    return Files.readString(candidate);
                }
            }
            final Path parent = cwd.getParent();
            if (parent == null) {
                break;
            }
            cwd = parent;
        }
        throw new java.nio.file.NoSuchFileException(String.join(" | ", relativeCandidates));
    }
}
