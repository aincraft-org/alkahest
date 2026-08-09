package dev.mintychochip.customblock;

import java.util.Objects;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * How a custom block should feel when mined / exploded — typically by emulating a vanilla block.
 *
 * <p>Example: electrum ore can {@link #emulate(Material) emulate} {@link Material#IRON_ORE} so dig
 * speed, preferred tool, and blast resistance match iron ore even though the world carrier is glass.
 */
public final class BlockFeel {

    /** Soft default when nothing is configured (glass-like). */
    public static final BlockFeel DEFAULT = of(0.3F, 0.3F, false, null);

    private final @Nullable Float hardness;
    private final @Nullable Float blastResistance;
    private final boolean requiresCorrectToolForDrops;
    /**
     * Vanilla block used as the tool/mining template (tags, tier, destroy speed vs item).
     * When set without explicit hardness/blast, those are taken from this material at runtime.
     */
    private final @Nullable Material emulate;

    private BlockFeel(
        final @Nullable Float hardness,
        final @Nullable Float blastResistance,
        final boolean requiresCorrectToolForDrops,
        final @Nullable Material emulate
    ) {
        if (hardness != null && hardness < -1.0F) {
            throw new IllegalArgumentException("hardness must be >= -1");
        }
        if (blastResistance != null && blastResistance < 0.0F) {
            throw new IllegalArgumentException("blastResistance must be >= 0");
        }
        this.hardness = hardness;
        this.blastResistance = blastResistance;
        this.requiresCorrectToolForDrops = requiresCorrectToolForDrops;
        this.emulate = emulate;
    }

    /**
     * Fully emulate a vanilla block's hardness, blast resistance, tool rules, and dig speed tags.
     *
     * <p>Does not call {@link Material#isBlock()} (needs full registry). Prefer real block materials.
     */
    public static @NotNull BlockFeel emulate(@NotNull final Material blockMaterial) {
        Objects.requireNonNull(blockMaterial, "blockMaterial");
        if (blockMaterial.isCustom()) {
            throw new IllegalArgumentException("emulate target must be vanilla Material");
        }
        // Hardness/blast resolved lazily from the material at runtime (needs registry).
        return new BlockFeel(null, null, true, blockMaterial);
    }

    /**
     * Explicit numbers; optional tool template for preferred-tool / dig-speed tags.
     */
    public static @NotNull BlockFeel of(
        final float hardness,
        final float blastResistance,
        final boolean requiresCorrectToolForDrops,
        final @Nullable Material toolTemplate
    ) {
        return new BlockFeel(hardness, blastResistance, requiresCorrectToolForDrops, toolTemplate);
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Destroy time (hardness). {@code -1} = unbreakable (bedrock-style).
     */
    public float hardness() {
        if (this.hardness != null) {
            return this.hardness;
        }
        if (this.emulate != null) {
            return this.emulate.getHardness();
        }
        return DEFAULT.hardness != null ? DEFAULT.hardness : 0.3F;
    }

    public float blastResistance() {
        if (this.blastResistance != null) {
            return this.blastResistance;
        }
        if (this.emulate != null) {
            return this.emulate.getBlastResistance();
        }
        return DEFAULT.blastResistance != null ? DEFAULT.blastResistance : 0.3F;
    }

    public boolean requiresCorrectToolForDrops() {
        return this.requiresCorrectToolForDrops;
    }

    /**
     * Vanilla material used for tool speed / correct-tool checks.
     * When only explicit strength is set with no template, returns {@code null}.
     */
    public @Nullable Material toolTemplate() {
        return this.emulate;
    }

    public @Nullable Material emulate() {
        return this.emulate;
    }

    public boolean isUnbreakable() {
        return this.hardness() < 0.0F;
    }

    public static final class Builder {
        private @Nullable Float hardness;
        private @Nullable Float blastResistance;
        private boolean requiresCorrectToolForDrops;
        private @Nullable Material emulate;

        public Builder hardness(final float hardness) {
            this.hardness = hardness;
            return this;
        }

        public Builder blastResistance(final float blastResistance) {
            this.blastResistance = blastResistance;
            return this;
        }

        /** Both hardness and blast (vanilla {@code strength(x)}). */
        public Builder strength(final float value) {
            this.hardness = value;
            this.blastResistance = value;
            return this;
        }

        public Builder strength(final float hardness, final float blastResistance) {
            this.hardness = hardness;
            this.blastResistance = blastResistance;
            return this;
        }

        public Builder requiresCorrectToolForDrops(final boolean value) {
            this.requiresCorrectToolForDrops = value;
            return this;
        }

        public Builder requiresCorrectToolForDrops() {
            this.requiresCorrectToolForDrops = true;
            return this;
        }

        /**
         * Emulate this vanilla block for tool tags / dig speed (and for hardness/blast if not set).
         */
        public Builder emulate(final Material blockMaterial) {
            this.emulate = Objects.requireNonNull(blockMaterial, "blockMaterial");
            if (blockMaterial.isCustom()) {
                throw new IllegalArgumentException("emulate target must be vanilla Material");
            }
            return this;
        }

        public BlockFeel build() {
            return new BlockFeel(
                this.hardness,
                this.blastResistance,
                this.requiresCorrectToolForDrops,
                this.emulate
            );
        }
    }
}
