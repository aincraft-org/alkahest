package dev.mintychochip.customblock;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Baked host: brown or red huge mushroom block states remapped by a resource pack.
 *
 * @param variant    brown vs red mushroom block family
 * @param stateIndex optional pack-allocated state slot; {@code null} until assigned
 */
public record MushroomHostSpec(
    @NotNull MushroomVariant variant,
    @Nullable Integer stateIndex
) implements HostSpec {

    public MushroomHostSpec {
        Objects.requireNonNull(variant, "variant");
        if (stateIndex != null && stateIndex < 0) {
            throw new IllegalArgumentException("stateIndex must be >= 0, got " + stateIndex);
        }
    }

    public static MushroomHostSpec brown() {
        return new MushroomHostSpec(MushroomVariant.BROWN, null);
    }

    public static MushroomHostSpec red() {
        return new MushroomHostSpec(MushroomVariant.RED, null);
    }

    public static MushroomHostSpec of(final MushroomVariant variant, final int stateIndex) {
        return new MushroomHostSpec(variant, stateIndex);
    }

    @Override
    public @NotNull BlockHostType type() {
        return BlockHostType.MUSHROOM;
    }
}
