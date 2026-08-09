package dev.mintychochip.customblock;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Packet host: client-only item display + fake collision block (PacketBlocks-style).
 *
 * <p>Defaults mirror PacketBlocks:
 * translation {@code (0.5, 0.5, 0.5)}, scale slightly above 1 so models cover the cube,
 * identity rotations, collision material key {@code minecraft:glass}.
 */
public final class PacketHostSpec implements HostSpec {

    private static final Vector3f DEFAULT_TRANSLATION = new Vector3f(0.5f, 0.5f, 0.5f);
    private static final Vector3f DEFAULT_SCALE = new Vector3f(1.001f, 1.001f, 1.001f);
    private static final Quaternionf DEFAULT_ROTATION = new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f);
    private static final String DEFAULT_COLLISION = "minecraft:glass";

    private final Vector3f translation;
    private final Vector3f scale;
    private final Quaternionf leftRotation;
    private final Quaternionf rightRotation;
    /** Namespaced material id for {@code sendBlockChange} collision (e.g. {@code minecraft:glass}). */
    private final String collisionMaterialKey;

    private PacketHostSpec(
        final Vector3f translation,
        final Vector3f scale,
        final Quaternionf leftRotation,
        final Quaternionf rightRotation,
        final String collisionMaterialKey
    ) {
        this.translation = new Vector3f(Objects.requireNonNull(translation, "translation"));
        this.scale = new Vector3f(Objects.requireNonNull(scale, "scale"));
        this.leftRotation = new Quaternionf(Objects.requireNonNull(leftRotation, "leftRotation"));
        this.rightRotation = new Quaternionf(Objects.requireNonNull(rightRotation, "rightRotation"));
        if (collisionMaterialKey == null || collisionMaterialKey.isBlank()) {
            throw new IllegalArgumentException("collisionMaterialKey required");
        }
        this.collisionMaterialKey = collisionMaterialKey;
    }

    public static PacketHostSpec defaults() {
        return new PacketHostSpec(
            DEFAULT_TRANSLATION,
            DEFAULT_SCALE,
            DEFAULT_ROTATION,
            DEFAULT_ROTATION,
            DEFAULT_COLLISION
        );
    }

    public static Builder builder() {
        return new Builder();
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

    public String collisionMaterialKey() {
        return this.collisionMaterialKey;
    }

    @Override
    public @NotNull BlockHostType type() {
        return BlockHostType.PACKET;
    }

    public static final class Builder {
        private Vector3f translation = new Vector3f(DEFAULT_TRANSLATION);
        private Vector3f scale = new Vector3f(DEFAULT_SCALE);
        private Quaternionf leftRotation = new Quaternionf(DEFAULT_ROTATION);
        private Quaternionf rightRotation = new Quaternionf(DEFAULT_ROTATION);
        private String collisionMaterialKey = DEFAULT_COLLISION;

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

        public Builder collisionMaterialKey(final String collisionMaterialKey) {
            this.collisionMaterialKey = Objects.requireNonNull(collisionMaterialKey, "collisionMaterialKey");
            return this;
        }

        public PacketHostSpec build() {
            return new PacketHostSpec(
                this.translation,
                this.scale,
                this.leftRotation,
                this.rightRotation,
                this.collisionMaterialKey
            );
        }
    }
}
