package org.bukkit.event.entity;

import com.google.common.base.Preconditions;
import dev.mintychochip.genetics.dto.BreedGenetics;
import dev.mintychochip.genetics.model.Sex;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when one Entity breeds with another Entity.
 *
 * <p>When mintychochip animal genetics ran for this breed, {@link #getGenetics()}
 * is non-null and {@link #getMother()} / {@link #getFather()} are the genetic
 * dam (female) and sire (male). Plugins can cancel using that metadata
 * (e.g. reject a phenotype) without needing a separate genetics event.
 */
public class EntityBreedEvent extends EntityEvent implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final LivingEntity mother;
    private final LivingEntity father;
    private final LivingEntity breeder;
    private final ItemStack bredWith;
    private int experience;
    private final @Nullable BreedGenetics genetics;

    private boolean cancelled;

    @ApiStatus.Internal
    public EntityBreedEvent(@NotNull LivingEntity child, @NotNull LivingEntity mother, @NotNull LivingEntity father, @Nullable LivingEntity breeder, @Nullable ItemStack bredWith, int experience) {
        this(child, mother, father, breeder, bredWith, experience, null);
    }

    @ApiStatus.Internal
    public EntityBreedEvent(@NotNull LivingEntity child, @NotNull LivingEntity mother, @NotNull LivingEntity father, @Nullable LivingEntity breeder, @Nullable ItemStack bredWith, int experience, @Nullable BreedGenetics genetics) {
        super(child);

        this.mother = mother;
        this.father = father;
        this.breeder = breeder; // Breeder can be null in the case of spontaneous conception
        this.bredWith = bredWith;
        this.experience = experience;
        this.genetics = genetics;
    }

    @NotNull
    @Override
    public LivingEntity getEntity() {
        return (LivingEntity) this.entity;
    }

    /**
     * Gets the mother parent.
     *
     * <p>With genetics present, this is the chromosomal female (dam).
     * Without genetics, this is the historical CraftBukkit "birth" parent
     * (entity that initiated the breed call), not a real sex role.
     *
     * @return The mother / birth parent
     */
    @NotNull
    public LivingEntity getMother() {
        return this.mother;
    }

    /**
     * Gets the father parent.
     *
     * <p>With genetics present, this is the chromosomal male (sire).
     * Without genetics, this is the other parent entity from the breed call.
     *
     * @return the father / other parent
     */
    @NotNull
    public LivingEntity getFather() {
        return this.father;
    }

    /**
     * mintychochip genetics snapshot for this breed, if animal genetics produced it.
     *
     * <p>Null for villagers, genetics-disabled animals, or other non-genetic paths.
     * When non-null, use genotype/phenotype/sex here to decide whether to
     * {@link #setCancelled(boolean) cancel} the birth.
     *
     * @return genetics metadata, or null
     */
    @Nullable
    public BreedGenetics getGenetics() {
        return this.genetics;
    }

    /**
     * Whether {@link #getGenetics()} is present.
     */
    public boolean hasGenetics() {
        return this.genetics != null;
    }

    /**
     * Child chromosomal sex when genetics is present.
     *
     * @return child sex, or null if no genetics payload
     */
    @Nullable
    public Sex getChildSex() {
        return this.genetics != null ? this.genetics.childSex() : null;
    }

    /**
     * Gets the Entity responsible for breeding. Breeder is {@code null} for spontaneous
     * conception.
     *
     * @return The Entity who initiated breeding.
     */
    @Nullable
    public LivingEntity getBreeder() {
        return this.breeder;
    }

    /**
     * The ItemStack that was used to initiate breeding, if present.
     *
     * @return ItemStack used to initiate breeding.
     */
    @Nullable
    public ItemStack getBredWith() {
        return this.bredWith;
    }

    /**
     * Get the amount of experience granted by breeding.
     *
     * @return experience amount
     */
    public int getExperience() {
        return this.experience;
    }

    /**
     * Set the amount of experience granted by breeding.
     *
     * @param experience experience amount
     */
    public void setExperience(int experience) {
        Preconditions.checkArgument(experience >= 0, "Experience cannot be negative");
        this.experience = experience;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
