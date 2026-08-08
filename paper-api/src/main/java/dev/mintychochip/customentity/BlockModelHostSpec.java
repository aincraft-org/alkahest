package dev.mintychochip.customentity;

import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Block-model host: vanilla {@code BlockDisplay} showing {@link #blockMaterial()} with optional transform.
 *
 * <p>Defaults: identity translation/rotation, scale {@code (1,1,1)} — a full cube at the entity origin.
 */
public final class BlockModelHostSpec implements EntityHostSpec {

    private static final Vector3f DEFAULT_TRANSLATION = new Vector3f(0.0f, 0.0f, 0.0f);
    private static final Vector3f DEFAULT_SCALE = new Vector3f(1.0f, 1.0f, 1.0f);
    private static final Quaternionf DEFAULT_ROTATION = new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f);

    private final Material blockMaterial;
    private final Vector3f translation;
    private final Vector3f scale;
    private final Quaternionf leftRotation;
    private final Quaternionf rightRotation;

    private BlockModelHostSpec(
        final Material blockMaterial,
        final Vector3f translation,
        final Vector3f scale,
        final Quaternionf leftRotation,
        final Quaternionf rightRotation
    ) {
        this.blockMaterial = Objects.requireNonNull(blockMaterial, "blockMaterial");
        // Avoid Material#isAir() / isBlock() — those touch registries (need full bootstrap).
        if (blockMaterial == Material.AIR
            || blockMaterial == Material.CAVE_AIR
            || blockMaterial == Material.VOID_AIR) {
            throw new IllegalArgumentException("blockMaterial must not be air");
        }
        this.translation = new Vector3f(Objects.requireNonNull(translation, "translation"));
        this.scale = new Vector3f(Objects.requireNonNull(scale, "scale"));
        this.leftRotation = new Quaternionf(Objects.requireNonNull(leftRotation, "leftRotation"));
        this.rightRotation = new Quaternionf(Objects.requireNonNull(rightRotation, "rightRotation"));
    }

    /** Default transform (identity) with the given block model. */
    public static BlockModelHostSpec of(final Material blockMaterial) {
        return new BlockModelHostSpec(
            blockMaterial,
            DEFAULT_TRANSLATION,
            DEFAULT_SCALE,
            DEFAULT_ROTATION,
            DEFAULT_ROTATION
        );
    }

    public static Builder builder(final Material blockMaterial) {
        return new Builder(blockMaterial);
    }

    public Material blockMaterial() {
        return this.blockMaterial;
    }

    public Vector3f translation() {
        return new Vector3f(this.translation);
    }

    public Vector3f scale() {
        return new Vector3f(this.scale);
    }

    public Quaternionf leftRotation() {
        return new Quaternionf(this.leftRotation);
    }

    public Quaternionf rightRotation() {
        return new Quaternionf(this.rightRotation);
    }

    /** Bukkit {@link Transformation} matching this host (for apply-to-carrier). */
    public @NotNull Transformation toTransformation() {
        return new Transformation(
            this.translation(),
            this.leftRotation(),
            this.scale(),
            this.rightRotation()
        );
    }

    @Override
    public @NotNull EntityHostType type() {
        return EntityHostType.BLOCK_MODEL;
    }

    public static final class Builder {
        private final Material blockMaterial;
        private Vector3f translation = new Vector3f(DEFAULT_TRANSLATION);
        private Vector3f scale = new Vector3f(DEFAULT_SCALE);
        private Quaternionf leftRotation = new Quaternionf(DEFAULT_ROTATION);
        private Quaternionf rightRotation = new Quaternionf(DEFAULT_ROTATION);

        private Builder(final Material blockMaterial) {
            this.blockMaterial = Objects.requireNonNull(blockMaterial, "blockMaterial");
        }

        public Builder translation(final Vector3f translation) {
            this.translation = new Vector3f(Objects.requireNonNull(translation, "translation"));
            return this;
        }

        public Builder scale(final Vector3f scale) {
            this.scale = new Vector3f(Objects.requireNonNull(scale, "scale"));
            return this;
        }

        public Builder leftRotation(final Quaternionf leftRotation) {
            this.leftRotation = new Quaternionf(Objects.requireNonNull(leftRotation, "leftRotation"));
            return this;
        }

        public Builder rightRotation(final Quaternionf rightRotation) {
            this.rightRotation = new Quaternionf(Objects.requireNonNull(rightRotation, "rightRotation"));
            return this;
        }

        public BlockModelHostSpec build() {
            return new BlockModelHostSpec(
                this.blockMaterial,
                this.translation,
                this.scale,
                this.leftRotation,
                this.rightRotation
            );
        }
    }
}
