package dev.mintychochip.provenance;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Concrete place a stack instance can live in.
 *
 * <p>Two independent {@link ItemStack} objects carrying the same UUID at two
 * different concrete locations is the dupe signal. {@link LocationKind#UNKNOWN} means the
 * location has not been observed yet; moving from UNKNOWN (or between
 * locations) is a transfer, never a collision by itself.
 */
public final class StackLocation {

    public enum LocationKind {
        /** Player inventory / equipment slot ({@code playerUuid} + {@code slot}). */
        PLAYER_SLOT,
        /** A dropped item entity ({@code entityUuid}). */
        ITEM_ENTITY,
        /** Machine/workstation/builtin label with no owning inventory (e.g. {@code furnace:0,64,0}). */
        TRANSIENT,
        /** Not yet observed. */
        UNKNOWN
    }

    private final @NotNull LocationKind kind;
    private final @Nullable UUID playerUuid;
    private final @Nullable UUID entityUuid;
    private final int slot;
    private final @Nullable String label;

    private StackLocation(
        final @NotNull LocationKind kind,
        final @Nullable UUID playerUuid,
        final @Nullable UUID entityUuid,
        final int slot,
        final @Nullable String label
    ) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.playerUuid = playerUuid;
        this.entityUuid = entityUuid;
        this.slot = slot;
        this.label = label;
    }

    public static @NotNull StackLocation playerSlot(final @NotNull UUID player, final int slot) {
        return new StackLocation(LocationKind.PLAYER_SLOT, Objects.requireNonNull(player, "player"), null, slot, null);
    }

    public static @NotNull StackLocation itemEntity(final @NotNull UUID entity) {
        return new StackLocation(LocationKind.ITEM_ENTITY, null, Objects.requireNonNull(entity, "entity"), 0, null);
    }

    public static @NotNull StackLocation labeled(final @NotNull String label) {
        return new StackLocation(LocationKind.TRANSIENT, null, null, 0, Objects.requireNonNull(label, "label"));
    }

    public static @NotNull StackLocation unknown() {
        return new StackLocation(LocationKind.UNKNOWN, null, null, 0, null);
    }

    public @NotNull LocationKind kind() {
        return this.kind;
    }

    public @Nullable UUID playerUuid() {
        return this.playerUuid;
    }

    public @Nullable UUID entityUuid() {
        return this.entityUuid;
    }

    public int slot() {
        return this.slot;
    }

    public @Nullable String label() {
        return this.label;
    }

    /** Whether this location is concrete enough to collide with a different one. */
    public boolean isConcrete() {
        return this.kind != LocationKind.UNKNOWN;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StackLocation other)) {
            return false;
        }
        return this.kind == other.kind
            && this.slot == other.slot
            && Objects.equals(this.playerUuid, other.playerUuid)
            && Objects.equals(this.entityUuid, other.entityUuid)
            && Objects.equals(this.label, other.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.kind, this.playerUuid, this.entityUuid, this.slot, this.label);
    }

    /** Compact human-readable form for the admin command. */
    public @NotNull String display() {
        return switch (this.kind) {
            case PLAYER_SLOT -> "player:" + this.playerUuid + ":" + this.slot;
            case ITEM_ENTITY -> "item_entity:" + this.entityUuid;
            case TRANSIENT -> this.label == null ? "transient" : this.label;
            case UNKNOWN -> "unknown";
        };
    }

    @Override
    public String toString() {
        return this.display();
    }
}
