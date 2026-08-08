package dev.mintychochip.provenance;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Read/write mintychochip stack provenance inside {@link DataComponents#CUSTOM_DATA}.
 *
 * <p>On-stack payload is the portable identity; the server census/lineage store is authoritative
 * for live/dead and full history walks.
 */
public final class StackStamp {

    /** Root compound under custom_data. */
    public static final String ROOT = "MintyProvenance";
    public static final String KEY_ID = "id";
    public static final String KEY_SOURCE = "source";
    public static final String KEY_BORN = "born";
    public static final String KEY_PARENTS = "parents";

    private StackStamp() {
    }

    public static @NotNull Optional<StackProvenance> read(final @Nullable ItemStack stack) {
        if (stack == null || stack == ItemStack.EMPTY) {
            return Optional.empty();
        }
        // count<=0 makes ItemStack.isEmpty() true and hides components; temporarily
        // revive count so post-consume merge/death can still read the stamp.
        final int count = stack.getCount();
        final boolean revived = count <= 0;
        if (revived) {
            stack.setCount(1);
        }
        try {
            if (stack.getItem() == net.minecraft.world.item.Items.AIR) {
                return Optional.empty();
            }
            final CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
            if (custom == null || custom.isEmpty()) {
                return Optional.empty();
            }
            final CompoundTag root = custom.getUnsafe().getCompoundOrEmpty(ROOT);
            if (root.isEmpty()) {
                return Optional.empty();
            }
            final String idRaw = root.getStringOr(KEY_ID, "");
            if (idRaw.isEmpty()) {
                return Optional.empty();
            }
            final UUID id;
            try {
                id = UUID.fromString(idRaw);
            } catch (final IllegalArgumentException ex) {
                return Optional.empty();
            }
            final ProvenanceSource source = parseSource(root.getStringOr(KEY_SOURCE, ProvenanceSource.UNKNOWN.name()));
            final long born = root.getLongOr(KEY_BORN, 0L);
            final List<UUID> parents = readParents(root);
            return Optional.of(new StackProvenance(id, source, parents, born));
        } finally {
            if (revived) {
                stack.setCount(count);
            }
        }
    }

    public static @NotNull Optional<UUID> readId(final @Nullable ItemStack stack) {
        return read(stack).map(StackProvenance::id);
    }

    public static void write(final @NotNull ItemStack stack, final @NotNull StackProvenance stamp) {
        if (stack.isEmpty()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            final CompoundTag root = new CompoundTag();
            root.putString(KEY_ID, stamp.id().toString());
            root.putString(KEY_SOURCE, stamp.source().name());
            root.putLong(KEY_BORN, stamp.bornEpochMs());
            if (!stamp.parents().isEmpty()) {
                final ListTag parents = new ListTag();
                for (final UUID parent : stamp.parents()) {
                    parents.add(StringTag.valueOf(parent.toString()));
                }
                root.put(KEY_PARENTS, parents);
            }
            tag.put(ROOT, root);
        });
    }

    public static void clear(final @NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(ROOT));
    }

    /**
     * Whether two stacks match for stacking/merging if we ignore the provenance stamp.
     * Without this, every distinct UUID would refuse to stack (breaking vanilla).
     */
    public static boolean sameItemSameComponentsIgnoringProvenance(
        final @NotNull ItemStack a,
        final @NotNull ItemStack b
    ) {
        if (!ItemStack.isSameItem(a, b)) {
            return false;
        }
        if (a.isEmpty() && b.isEmpty()) {
            return true;
        }
        if (java.util.Objects.equals(a.getComponents(), b.getComponents())) {
            return true;
        }
        // Compare component-by-component; CUSTOM_DATA compared with ROOT stripped.
        final var types = new java.util.HashSet<net.minecraft.core.component.DataComponentType<?>>();
        a.getComponents().keySet().forEach(types::add);
        b.getComponents().keySet().forEach(types::add);
        for (final net.minecraft.core.component.DataComponentType<?> type : types) {
            if (type == DataComponents.CUSTOM_DATA) {
                if (!customDataEqualIgnoringProvenance(a.get(DataComponents.CUSTOM_DATA), b.get(DataComponents.CUSTOM_DATA))) {
                    return false;
                }
                continue;
            }
            if (!java.util.Objects.equals(a.get(type), b.get(type))) {
                return false;
            }
        }
        return true;
    }

    private static boolean customDataEqualIgnoringProvenance(
        final @Nullable CustomData a,
        final @Nullable CustomData b
    ) {
        final CompoundTag tagA = stripProvenance(a);
        final CompoundTag tagB = stripProvenance(b);
        if (tagA.isEmpty() && tagB.isEmpty()) {
            return true;
        }
        return tagA.equals(tagB);
    }

    private static @NotNull CompoundTag stripProvenance(final @Nullable CustomData data) {
        if (data == null || data.isEmpty()) {
            return new CompoundTag();
        }
        final CompoundTag copy = data.copyTag();
        copy.remove(ROOT);
        return copy;
    }

    private static @NotNull List<UUID> readParents(final CompoundTag root) {
        final ListTag list = root.getListOrEmpty(KEY_PARENTS);
        if (list.isEmpty()) {
            return List.of();
        }
        final List<UUID> out = new ArrayList<>(list.size());
        for (final Tag tag : list) {
            if (tag instanceof StringTag stringTag) {
                try {
                    out.add(UUID.fromString(stringTag.value()));
                } catch (final IllegalArgumentException ignored) {
                    // skip bad parent entries
                }
            }
        }
        return List.copyOf(out);
    }

    private static @NotNull ProvenanceSource parseSource(final String raw) {
        try {
            return ProvenanceSource.valueOf(raw);
        } catch (final IllegalArgumentException ex) {
            return ProvenanceSource.UNKNOWN;
        }
    }
}
