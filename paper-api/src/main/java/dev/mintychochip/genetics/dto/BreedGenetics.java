package dev.mintychochip.genetics.dto;

import dev.mintychochip.genetics.model.Sex;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.NamespacedKey;
import org.jspecify.annotations.Nullable;

/**
 * Genetics payload attached to a successful animal breed (plugin-facing).
 *
 * <p>{@link #mother()} / {@link #father()} are chromosomal dam/sire roles
 * (female / male), not the vanilla entity call order.
 *
 * <p>{@link #childVariant()} is the resolved Minecraft registry key the server
 * will (or did) apply for the child's look — not a substitute for genotype.
 */
public final class BreedGenetics {

    private final GenotypeSnapshot mother;
    private final GenotypeSnapshot father;
    private final GenotypeSnapshot child;
    private final PhenotypeSnapshot motherPhenotype;
    private final PhenotypeSnapshot fatherPhenotype;
    private final PhenotypeSnapshot childPhenotype;
    private final @Nullable NamespacedKey childVariant;

    public BreedGenetics(
        final GenotypeSnapshot mother,
        final GenotypeSnapshot father,
        final GenotypeSnapshot child,
        final PhenotypeSnapshot motherPhenotype,
        final PhenotypeSnapshot fatherPhenotype,
        final PhenotypeSnapshot childPhenotype
    ) {
        this(mother, father, child, motherPhenotype, fatherPhenotype, childPhenotype, null);
    }

    public BreedGenetics(
        final GenotypeSnapshot mother,
        final GenotypeSnapshot father,
        final GenotypeSnapshot child,
        final PhenotypeSnapshot motherPhenotype,
        final PhenotypeSnapshot fatherPhenotype,
        final PhenotypeSnapshot childPhenotype,
        final @Nullable NamespacedKey childVariant
    ) {
        this.mother = Objects.requireNonNull(mother, "mother");
        this.father = Objects.requireNonNull(father, "father");
        this.child = Objects.requireNonNull(child, "child");
        this.motherPhenotype = Objects.requireNonNull(motherPhenotype, "motherPhenotype");
        this.fatherPhenotype = Objects.requireNonNull(fatherPhenotype, "fatherPhenotype");
        this.childPhenotype = Objects.requireNonNull(childPhenotype, "childPhenotype");
        this.childVariant = childVariant;
        if (mother.sex() != Sex.FEMALE) {
            throw new IllegalArgumentException("mother genotype must be FEMALE, got " + mother.sex());
        }
        if (father.sex() != Sex.MALE) {
            throw new IllegalArgumentException("father genotype must be MALE, got " + father.sex());
        }
    }

    public GenotypeSnapshot mother() {
        return this.mother;
    }

    public GenotypeSnapshot father() {
        return this.father;
    }

    public GenotypeSnapshot child() {
        return this.child;
    }

    public PhenotypeSnapshot motherPhenotype() {
        return this.motherPhenotype;
    }

    public PhenotypeSnapshot fatherPhenotype() {
        return this.fatherPhenotype;
    }

    public PhenotypeSnapshot childPhenotype() {
        return this.childPhenotype;
    }

    /**
     * Resolved primary registry variant for the child (e.g. {@code minecraft:calico}),
     * if a mapping exists for this species/phenotype. Empty means "leave vanilla look".
     */
    public Optional<NamespacedKey> childVariant() {
        return Optional.ofNullable(this.childVariant);
    }

    public @Nullable NamespacedKey childVariantOrNull() {
        return this.childVariant;
    }

    public Sex motherSex() {
        return this.mother.sex();
    }

    public Sex fatherSex() {
        return this.father.sex();
    }

    public Sex childSex() {
        return this.child.sex();
    }
}
