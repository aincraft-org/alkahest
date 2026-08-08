package dev.mintychochip.provenance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Server-side item stack provenance engine.
 *
 * <p><b>Rules</b>
 * <ul>
 *   <li>Every non-empty stack that enters the economy gets a UUID stamp.</li>
 *   <li>{@code ItemStack.copy()} preserves UUID — two independent stacks with the same
 *       UUID at two different concrete locations is the dupe signal.</li>
 *   <li>{@code split} / {@code consumeAndReturn} mint a new UUID on the extracted
 *       portion with parent linkage; a full extraction moves the identity.</li>
 *   <li>Craft/smelt/trade birth a new UUID whose parents are the ingredient UUIDs.</li>
 *   <li>A {@code transfer} moves one tracked instance to a new location and never
 *       collides; an {@code observe} at a second concrete location collides once.</li>
 *   <li>Merging two independent stacks that share a UUID is recorded as
 *       {@link ProvenanceCollisionKind#DUPLICATE_MERGE}, never silently normalized.</li>
 *   <li>Player-placed blocks keep placement memory; break re-emits
 *       {@link ProvenanceSource#BLOCK_RECOVER}.</li>
 *   <li>Death removes from the live census; lineage nodes remain for history walks
 *       and are persisted to the provenance repository.</li>
 * </ul>
 */
public final class ItemProvenance {

    private static final int AUDIT_CAPACITY = 16_384;
    private static final int COLLISION_CAPACITY = 4_096;

    private static volatile boolean enabled = true;
    private static final LiveIndex LIVE = new LiveIndex();
    private static final LineageStore LINEAGE = new LineageStore();
    private static final AuditLog AUDIT = new AuditLog(AUDIT_CAPACITY);
    private static final PlacementStore PLACEMENTS = new PlacementStore();
    /** Placement records held by entities (falling blocks, endermen) while off-world. */
    private static final ConcurrentHashMap<UUID, PlacementRecord> CARRIED_BY_ENTITY = new ConcurrentHashMap<>();
    /** Bounded collision trail; keyed dedupe map prevents repeating the same pair. */
    private static final java.util.ArrayDeque<CollisionRecord> COLLISIONS = new java.util.ArrayDeque<>();
    private static final ConcurrentHashMap<UUID, String> COLLISION_SEEN = new ConcurrentHashMap<>();

    private ItemProvenance() {
    }

    public static void setEnabled(final boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** Wipe runtime state only. Durable lineage/collision records survive in the repository. */
    public static void clearAll() {
        LIVE.clear();
        LINEAGE.clearCache();
        AUDIT.clear();
        synchronized (COLLISIONS) {
            COLLISIONS.clear();
        }
        COLLISION_SEEN.clear();
        PLACEMENTS.clear();
        CARRIED_BY_ENTITY.clear();
    }

    public static @NotNull LiveIndex live() {
        return LIVE;
    }

    public static @NotNull LineageStore lineage() {
        return LINEAGE;
    }

    public static @NotNull AuditLog audit() {
        return AUDIT;
    }

    public static @NotNull PlacementStore placements() {
        return PLACEMENTS;
    }

    public static @NotNull List<CollisionRecord> collisions() {
        synchronized (COLLISIONS) {
            return List.copyOf(COLLISIONS);
        }
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    public static @NotNull Optional<StackProvenance> of(final @Nullable ItemStack stack) {
        return StackStamp.read(stack);
    }

    public static @NotNull Optional<UUID> idOf(final @Nullable ItemStack stack) {
        return StackStamp.readId(stack);
    }

    public static @NotNull List<LineageNode> explain(final @NotNull UUID id) {
        return LINEAGE.walkAncestors(id);
    }

    public static @NotNull List<LineageNode> explain(final @Nullable ItemStack stack) {
        return idOf(stack).map(ItemProvenance::explain).orElse(List.of());
    }

    public static @NotNull String explainText(final @NotNull UUID id) {
        final Optional<LineageNode> root = LINEAGE.get(id);
        if (root.isEmpty()) {
            return "unknown id=" + id;
        }
        final StringBuilder sb = new StringBuilder();
        final java.util.HashSet<UUID> seen = new java.util.HashSet<>();
        appendTreeLine(sb, root.get(), "", true, true, seen, 0, 64);
        return sb.toString().stripTrailing();
    }

    private static void appendTreeLine(
        final StringBuilder sb,
        final LineageNode node,
        final String prefix,
        final boolean isRoot,
        final boolean isLast,
        final java.util.Set<UUID> seen,
        final int depth,
        final int maxDepth
    ) {
        if (!isRoot) {
            sb.append(prefix);
            sb.append(isLast ? "└─ " : "├─ ");
        }
        sb.append(shortItem(node.itemId()))
            .append("  ")
            .append(node.source())
            .append("  ")
            .append(shortUuid(node.id()));
        if (node.dead()) {
            sb.append("  DEAD[").append(node.deathReason()).append(']');
        }
        sb.append('\n');

        if (depth >= maxDepth || !seen.add(node.id())) {
            return;
        }
        final List<UUID> parents = node.parents();
        final String childPrefix = isRoot ? "" : prefix + (isLast ? "   " : "│  ");
        for (int i = 0; i < parents.size(); i++) {
            final boolean last = i == parents.size() - 1;
            final Optional<LineageNode> parent = LINEAGE.get(parents.get(i));
            if (parent.isEmpty()) {
                sb.append(childPrefix).append(last ? "└─ " : "├─ ")
                    .append('?').append("  ").append(shortUuid(parents.get(i))).append('\n');
                continue;
            }
            appendTreeLine(sb, parent.get(), childPrefix, false, last, seen, depth + 1, maxDepth);
        }
    }

    static @NotNull String shortItem(final @NotNull String itemId) {
        if (itemId.startsWith("minecraft:")) {
            return itemId.substring("minecraft:".length());
        }
        return itemId;
    }

    static @NotNull String shortUuid(final @NotNull UUID id) {
        final String s = id.toString();
        return s.length() >= 8 ? s.substring(0, 8) : s;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public static @NotNull Optional<UUID> birth(
        final @NotNull ItemStack stack,
        final @NotNull ProvenanceSource source,
        final @NotNull StackLocation location,
        final @NotNull List<UUID> parents
    ) {
        if (!enabled || stack.isEmpty()) {
            return Optional.empty();
        }
        final long now = System.currentTimeMillis();
        final UUID id = UUID.randomUUID();
        final String itemId = itemId(stack);
        final List<UUID> parentList = dedupe(parents);
        StackStamp.write(stack, new StackProvenance(id, source, parentList, now));

        final LineageNode node = new LineageNode(id, itemId, source, parentList, now, location.display());
        LINEAGE.put(node);
        final LiveEntry liveEntry = new LiveEntry(id, itemId, location, stack.getCount(), now);
        LIVE.put(liveEntry);
        persistLive(liveEntry, false);

        final ProvenanceEventType eventType =
            source == ProvenanceSource.CRAFT
                || source == ProvenanceSource.SMELT
                || source == ProvenanceSource.SPECIAL_RECIPE
                || source == ProvenanceSource.TRADE
                || source == ProvenanceSource.BLOCK_RECOVER
                ? ProvenanceEventType.TRANSFORM
                : ProvenanceEventType.BIRTH;

        AUDIT.append(new ProvenanceEvent(
            now,
            eventType,
            id,
            itemId,
            source,
            null,
            parentList,
            location.display(),
            null
        ));
        return Optional.of(id);
    }

    public static @NotNull Optional<UUID> birth(
        final @NotNull ItemStack stack,
        final @NotNull ProvenanceSource source,
        final @NotNull StackLocation location
    ) {
        return birth(stack, source, location, List.of());
    }

    public static @NotNull Optional<UUID> birthIfAbsent(
        final @NotNull ItemStack stack,
        final @NotNull ProvenanceSource source,
        final @NotNull StackLocation location,
        final @NotNull List<UUID> parents
    ) {
        if (!enabled || stack.isEmpty()) {
            return Optional.empty();
        }
        final Optional<UUID> existing = StackStamp.readId(stack);
        if (existing.isPresent()) {
            rehydrateIfNeeded(stack, existing.get(), location);
            return existing;
        }
        return birth(stack, source, location, parents);
    }

    public static @NotNull Optional<UUID> birthIfAbsent(
        final @NotNull ItemStack stack,
        final @NotNull ProvenanceSource source,
        final @NotNull StackLocation location
    ) {
        return birthIfAbsent(stack, source, location, List.of());
    }

    /**
     * Ensure the stack has an identity, observed at {@code location}.
     * Unstamped stacks get a {@link ProvenanceSource#LEGACY} birth.
     */
    public static @NotNull Optional<UUID> ensure(
        final @NotNull ItemStack stack,
        final @NotNull StackLocation location
    ) {
        if (!enabled || stack.isEmpty()) {
            return Optional.empty();
        }
        final Optional<UUID> existing = StackStamp.readId(stack);
        if (existing.isPresent()) {
            rehydrateIfNeeded(stack, existing.get(), location);
            return existing;
        }
        return birth(stack, ProvenanceSource.LEGACY, location);
    }

    /**
     * Record that this stack instance is now at {@code location}. A second concrete
     * location for the same UUID is the dupe signal; the accepted census location is
     * never overwritten by the colliding observation.
     *
     * @return true when a collision was recorded
     */
    public static boolean observe(final @NotNull ItemStack stack, final @NotNull StackLocation location) {
        if (!enabled || stack.isEmpty()) {
            return false;
        }
        final Optional<UUID> id = StackStamp.readId(stack);
        if (id.isEmpty()) {
            birth(stack, ProvenanceSource.LEGACY, location);
            return false;
        }
        return observe(id.get(), stack, location);
    }

    private static boolean observe(final UUID id, final ItemStack stack, final StackLocation location) {
        final LiveEntry entry = LIVE.get(id).orElse(null);
        if (entry == null) {
            rehydrateIfNeeded(stack, id, location);
            return false;
        }
        final int prevCount = entry.count();
        entry.setCount(stack.getCount());
        if (!location.isConcrete() || entry.locations().contains(location)) {
            if (entry.count() != prevCount) {
                persistLive(entry, false);
            }
            return false;
        }
        if (entry.locations().isEmpty()) {
            entry.addLocation(location);
            persistLive(entry, false);
            return false;
        }
        // Second concrete location for one live identity.
        if (entry.count() != prevCount) {
            persistLive(entry, false);
        }
        final StackLocation existing = entry.locations().iterator().next();
        recordCollision(id, ProvenanceCollisionKind.DUPLICATE_LOCATION, existing, location);
        return true;
    }

    /**
     * Move one tracked instance of this stack's identity to {@code to}.
     * A transfer is a legitimate handoff and never records a collision.
     */
    public static boolean transfer(final @NotNull ItemStack stack, final @NotNull StackLocation to) {
        if (!enabled || stack.isEmpty()) {
            return false;
        }
        final Optional<UUID> id = StackStamp.readId(stack);
        if (id.isEmpty()) {
            birth(stack, ProvenanceSource.LEGACY, to);
            return false;
        }
        final LiveEntry entry = LIVE.get(id.get()).orElse(null);
        if (entry == null) {
            rehydrateIfNeeded(stack, id.get(), to);
            return false;
        }
        entry.setCount(stack.getCount());
        if (!to.isConcrete()) {
            return false;
        }
        if (entry.locations().contains(to)) {
            return false;
        }
        if (entry.locations().isEmpty()) {
            entry.addLocation(to);
        } else {
            // Move the tracked instance: drop one existing location, keep the rest.
            final StackLocation existing = entry.locations().iterator().next();
            entry.removeLocation(existing);
            entry.addLocation(to);
        }
        persistLive(entry, false);
        return false;
    }

    public static void onSpecialRecipe(
        final @NotNull ItemStack result,
        final @NotNull List<UUID> ingredientIds,
        final @NotNull StackLocation location
    ) {
        if (!enabled || result.isEmpty()) {
            return;
        }
        birth(result, ProvenanceSource.SPECIAL_RECIPE, location, ingredientIds);
    }

    public static void onSmelted(
        final @NotNull ItemStack result,
        final @Nullable UUID inputParent,
        final @NotNull StackLocation location
    ) {
        if (!enabled || result.isEmpty()) {
            return;
        }
        final List<UUID> parents = inputParent == null ? List.of() : List.of(inputParent);
        birth(result, ProvenanceSource.SMELT, location, parents);
    }

    /**
     * Furnace/campfire output merged into an existing result stack: keep the survivor
     * identity but record every additional input parent so accumulated lineage is not lost.
     */
    public static void onSmeltedAccumulate(
        final @NotNull ItemStack resultStack,
        final @Nullable UUID inputParent,
        final @NotNull StackLocation location
    ) {
        if (!enabled || resultStack.isEmpty()) {
            return;
        }
        final Optional<StackProvenance> stamp = StackStamp.read(resultStack);
        if (stamp.isEmpty() || inputParent == null) {
            observe(resultStack, location);
            return;
        }
        final List<UUID> parents = new ArrayList<>(stamp.get().parents());
        if (parents.contains(inputParent)) {
            observe(resultStack, location);
            return;
        }
        parents.add(inputParent);
        final long now = System.currentTimeMillis();
        StackStamp.write(resultStack, new StackProvenance(stamp.get().id(), stamp.get().source(), parents, stamp.get().bornEpochMs()));
        final Optional<LineageNode> node = LINEAGE.get(stamp.get().id());
        if (node.isPresent()) {
            LINEAGE.put(new LineageNode(
                stamp.get().id(),
                node.get().itemId(),
                node.get().source(),
                parents,
                node.get().bornEpochMs(),
                node.get().bornHolder()
            ));
        }
        observe(resultStack, location);
    }

    public static void onTrade(
        final @NotNull ItemStack result,
        final @NotNull List<UUID> paymentIds,
        final @NotNull StackLocation location
    ) {
        if (!enabled || result.isEmpty()) {
            return;
        }
        birth(result, ProvenanceSource.TRADE, location, paymentIds);
    }

    // -------------------------------------------------------------------------
    // Placement memory (anti place→break wash)
    // -------------------------------------------------------------------------

    public static void onBlockPlaced(
        final @NotNull Level level,
        final @NotNull BlockPos pos,
        final @NotNull ItemStack stack,
        final @Nullable Player player
    ) {
        if (!enabled || stack.isEmpty() || level.isClientSide()) {
            return;
        }
        final StackLocation loc = player != null
            ? StackLocation.playerSlot(player.getUUID(), -1)
            : StackLocation.labeled("machine");
        final Optional<UUID> id = ensure(stack, loc);
        if (id.isEmpty()) {
            return;
        }
        final String placer = player != null ? "player:" + player.getUUID() : "machine";
        PLACEMENTS.put(
            level,
            pos.immutable(),
            new PlacementRecord(id.get(), itemId(stack), placer, System.currentTimeMillis())
        );
        final long now = System.currentTimeMillis();
        AUDIT.append(new ProvenanceEvent(
            now,
            ProvenanceEventType.CLAIM,
            id.get(),
            itemId(stack),
            null,
            null,
            List.of(),
            "placed:" + pos.getX() + "," + pos.getY() + "," + pos.getZ(),
            player != null ? "player place" : "machine place (dispenser/etc)"
        ));
    }

    public static void movePlacement(
        final @NotNull Level level,
        final @NotNull BlockPos from,
        final @NotNull BlockPos to
    ) {
        if (!enabled || level.isClientSide()) {
            return;
        }
        PLACEMENTS.move(level, from.immutable(), to.immutable());
    }

    public static void movePlacement(
        final @NotNull String dimensionId,
        final @NotNull BlockPos from,
        final @NotNull BlockPos to
    ) {
        if (!enabled) {
            return;
        }
        PLACEMENTS.move(dimensionId, from.immutable(), to.immutable());
    }

    public static void onEntityPicksBlock(
        final @NotNull UUID entityId,
        final @NotNull Level level,
        final @NotNull BlockPos pos
    ) {
        if (!enabled || level.isClientSide()) {
            return;
        }
        final PlacementRecord record = PLACEMENTS.remove(level, pos.immutable());
        if (record != null) {
            CARRIED_BY_ENTITY.put(entityId, record);
        }
    }

    public static void onEntityPlacesBlock(
        final @NotNull UUID entityId,
        final @NotNull Level level,
        final @NotNull BlockPos pos
    ) {
        if (!enabled || level.isClientSide()) {
            return;
        }
        final PlacementRecord record = CARRIED_BY_ENTITY.remove(entityId);
        if (record != null) {
            PLACEMENTS.put(level, pos.immutable(), record);
        }
    }

    public static void releaseCarriedOntoStack(
        final @NotNull UUID entityId,
        final @NotNull ItemStack stack
    ) {
        if (!enabled || stack.isEmpty()) {
            CARRIED_BY_ENTITY.remove(entityId);
            return;
        }
        final PlacementRecord record = CARRIED_BY_ENTITY.remove(entityId);
        if (record != null) {
            birth(
                stack,
                ProvenanceSource.BLOCK_RECOVER,
                StackLocation.labeled("entity_carried_drop:" + shortUuid(entityId)),
                List.of(record.parentStackId())
            );
        }
    }

    public static void discardCarried(final @NotNull UUID entityId) {
        CARRIED_BY_ENTITY.remove(entityId);
    }

    public static void putCarriedForTest(final @NotNull UUID entityId, final @NotNull PlacementRecord record) {
        CARRIED_BY_ENTITY.put(entityId, record);
    }

    public static @NotNull Optional<PlacementRecord> getCarried(final @NotNull UUID entityId) {
        return Optional.ofNullable(CARRIED_BY_ENTITY.get(entityId));
    }

    public static void saveCarriedTo(
        final @NotNull UUID entityId,
        final @NotNull net.minecraft.world.level.storage.ValueOutput output
    ) {
        final PlacementRecord record = CARRIED_BY_ENTITY.get(entityId);
        if (record == null) {
            return;
        }
        output.putString("MintyProvParent", record.parentStackId().toString());
        output.putString("MintyProvItem", record.blockItemId());
        if (record.placer() != null) {
            output.putString("MintyProvPlacer", record.placer());
        }
        output.putLong("MintyProvTime", record.placedEpochMs());
    }

    public static void loadCarriedFrom(
        final @NotNull UUID entityId,
        final @NotNull net.minecraft.world.level.storage.ValueInput input
    ) {
        final String parentRaw = input.getStringOr("MintyProvParent", "");
        if (parentRaw.isEmpty()) {
            return;
        }
        try {
            final UUID parent = UUID.fromString(parentRaw);
            final String item = input.getStringOr("MintyProvItem", "unknown");
            final String placerRaw = input.getStringOr("MintyProvPlacer", "");
            final String placer = placerRaw.isEmpty() ? null : placerRaw;
            final long t = input.getLongOr("MintyProvTime", System.currentTimeMillis());
            CARRIED_BY_ENTITY.put(entityId, new PlacementRecord(parent, item, placer, t));
        } catch (final IllegalArgumentException ignored) {
            // bad uuid
        }
    }

    /**
     * Mint a distinct child identity for one materialized copy (quick-craft drag
     * destinations), linking to {@code parentId} and updating the parent's remaining
     * count. The child starts at an unknown location until observed.
     */
    public static @NotNull Optional<UUID> mintChild(
        final @NotNull ItemStack child,
        final @NotNull UUID parentId,
        final int parentRemainingCount
    ) {
        if (!enabled || child.isEmpty()) {
            return Optional.empty();
        }
        final Optional<UUID> id = birth(child, ProvenanceSource.SPLIT, StackLocation.unknown(), List.of(parentId));
        LIVE.get(parentId).ifPresent(e -> e.setCount(parentRemainingCount));
        return id;
    }

    // -------------------------------------------------------------------------
    // Splits and merges
    // -------------------------------------------------------------------------

    /**
     * Child was produced by {@link ItemStack#split} or {@code consumeAndReturn}.
     * Full extraction (parent emptied) moves the identity; partial extraction mints
     * a child UUID with parent linkage.
     */
    public static void onSplit(final @NotNull ItemStack parent, final @NotNull ItemStack child) {
        if (!enabled || child.isEmpty()) {
            return;
        }
        if (parent.isEmpty()) {
            // Full take: identity moves with the items.
            rehydrate(child, StackLocation.unknown());
            StackStamp.readId(child).flatMap(LIVE::get).ifPresent(e -> e.setCount(child.getCount()));
            return;
        }

        ensure(parent, StackLocation.unknown());
        final Optional<UUID> parentId = StackStamp.readId(parent);
        final List<UUID> parentList = parentId.map(List::of).orElseGet(() ->
            StackStamp.readId(child).map(List::of).orElse(List.of())
        );

        // Child still shares parent's UUID from copyWithCount — mint a distinct identity.
        birth(child, ProvenanceSource.SPLIT, StackLocation.unknown(), parentList);

        final Optional<UUID> childId = StackStamp.readId(child);
        if (childId.isPresent()) {
            final long now = System.currentTimeMillis();
            AUDIT.append(new ProvenanceEvent(
                now,
                ProvenanceEventType.SPLIT,
                childId.get(),
                itemId(child),
                ProvenanceSource.SPLIT,
                null,
                parentList,
                null,
                "split child"
            ));
        }
        parentId.flatMap(LIVE::get).ifPresent(e -> e.setCount(parent.getCount()));
    }

    /**
     * Two independent stacks combined: survivor keeps identity; absorbed UUID dies.
     * Merging two stacks that already share a UUID is recorded as a duplicate-merge
     * collision instead of being silently normalized.
     */
    public static boolean onMerge(
        final @NotNull ItemStack survivor,
        final @NotNull ItemStack absorbed,
        final @NotNull StackLocation survivorLocation,
        final @NotNull StackLocation absorbedLocation
    ) {
        if (!enabled) {
            return false;
        }
        final Optional<UUID> absorbedId = StackStamp.readId(absorbed);
        if (!survivor.isEmpty()) {
            ensure(survivor, survivorLocation);
        }
        if (absorbedId.isEmpty()) {
            return false;
        }
        final Optional<UUID> survivorId = StackStamp.readId(survivor);
        if (survivorId.isPresent() && survivorId.get().equals(absorbedId.get())) {
            recordCollision(absorbedId.get(), ProvenanceCollisionKind.DUPLICATE_MERGE, absorbedLocation, survivorLocation);
            return true;
        }
        death(absorbedId.get(), ProvenanceReason.MERGED, survivorId.orElse(null));
        if (survivorId.isPresent()) {
            final long now = System.currentTimeMillis();
            AUDIT.append(new ProvenanceEvent(
                now,
                ProvenanceEventType.MERGE,
                survivorId.get(),
                itemId(survivor),
                null,
                ProvenanceReason.MERGED,
                List.of(absorbedId.get()),
                survivorLocation.display(),
                null
            ));
            final LiveEntry live = LIVE.get(survivorId.get()).orElse(null);
            if (live != null) {
                live.setCount(survivor.getCount());
            }
        }
        return false;
    }

    public static boolean onInventoryMergeFullyAbsorbed(
        final @NotNull ItemStack survivor,
        final @NotNull ItemStack absorbed,
        final @NotNull StackLocation survivorLocation,
        final @NotNull StackLocation absorbedLocation
    ) {
        if (!enabled || survivor.isEmpty()) {
            return false;
        }
        final Optional<UUID> absorbedId = StackStamp.readId(absorbed);
        if (absorbedId.isEmpty()) {
            return false;
        }
        final Optional<UUID> survivorId = StackStamp.readId(survivor);
        if (survivorId.isPresent() && survivorId.get().equals(absorbedId.get())) {
            recordCollision(absorbedId.get(), ProvenanceCollisionKind.DUPLICATE_MERGE, absorbedLocation, survivorLocation);
            return true;
        }
        if (survivorId.isEmpty()) {
            ensure(survivor, survivorLocation);
        }
        return onMerge(survivor, absorbed, survivorLocation, absorbedLocation);
    }

    /**
     * Universal container merge (player inv, hopper, chest slot, menu click, …).
     * Call <strong>after</strong> {@code source} has been shrunk and {@code target}
     * grown, with {@code absorbedIdBefore} captured from the source before the shrink.
     *
     * @return true when a duplicate-merge collision was recorded
     */
    public static boolean afterContainerMerge(
        final @NotNull ItemStack targetSurvivor,
        final @NotNull ItemStack sourceRemaining,
        final @NotNull Optional<UUID> absorbedIdBefore,
        final int amountMoved,
        final @NotNull StackLocation sourceLocation,
        final @NotNull StackLocation targetLocation
    ) {
        if (!enabled || amountMoved <= 0) {
            return false;
        }
        if (absorbedIdBefore.isEmpty()) {
            // Unstamped incoming: track whatever remains so partial legacy stacks are not invisible.
            if (!sourceRemaining.isEmpty()) {
                ensure(sourceRemaining, sourceLocation);
            }
            return false;
        }
        final UUID absorbedId = absorbedIdBefore.get();
        final Optional<UUID> survivorId = StackStamp.readId(targetSurvivor);
        final boolean fullyAbsorbed = sourceRemaining.isEmpty() || sourceRemaining.getCount() <= 0;
        if (survivorId.isPresent() && survivorId.get().equals(absorbedId)) {
            // Two independent stacks sharing one identity merged — laundering attempt.
            recordCollision(absorbedId, ProvenanceCollisionKind.DUPLICATE_MERGE, sourceLocation, targetLocation);
            return true;
        }
        if (fullyAbsorbed) {
            if (survivorId.isEmpty() && !targetSurvivor.isEmpty()) {
                ensure(targetSurvivor, targetLocation);
            }
            death(absorbedId, ProvenanceReason.MERGED, StackStamp.readId(targetSurvivor).orElse(null));
            if (!targetSurvivor.isEmpty()) {
                observe(targetSurvivor, targetLocation);
            }
            return false;
        }
        // Partial: both identities remain.
        observe(targetSurvivor, targetLocation);
        observe(sourceRemaining, sourceLocation);
        return false;
    }

    public static boolean afterContainerMerge(
        final @NotNull ItemStack targetSurvivor,
        final @NotNull ItemStack sourceRemaining,
        final int amountMoved,
        final @NotNull StackLocation sourceLocation,
        final @NotNull StackLocation targetLocation
    ) {
        return afterContainerMerge(
            targetSurvivor,
            sourceRemaining,
            StackStamp.readId(sourceRemaining),
            amountMoved,
            sourceLocation,
            targetLocation
        );
    }

    public static void birthLoot(final @NotNull ItemStack stack, final @NotNull StackLocation location) {
        birthIfAbsent(stack, ProvenanceSource.LOOT, location);
    }

    public static void stampBlockDrop(
        final @NotNull Level level,
        final @NotNull BlockPos pos,
        final @NotNull ItemStack stack
    ) {
        if (!enabled || stack.isEmpty()) {
            return;
        }
        stampFromPlacement(PLACEMENTS.get(level, pos), pos, stack);
    }

    public static void stampBlockDrop(
        final @NotNull String dimensionId,
        final @NotNull BlockPos pos,
        final @NotNull ItemStack stack
    ) {
        if (!enabled || stack.isEmpty()) {
            return;
        }
        stampFromPlacement(PLACEMENTS.get(dimensionId, pos), pos, stack);
    }

    private static void stampFromPlacement(
        final Optional<PlacementRecord> placed,
        final BlockPos pos,
        final ItemStack stack
    ) {
        if (placed.isPresent()) {
            birth(
                stack,
                ProvenanceSource.BLOCK_RECOVER,
                StackLocation.labeled("block_recover:" + pos.getX() + "," + pos.getY() + "," + pos.getZ()),
                List.of(placed.get().parentStackId())
            );
        } else {
            birthIfAbsent(stack, ProvenanceSource.BLOCK_DROP, StackLocation.labeled("block_drop"));
        }
    }

    public static void clearPlacement(final @NotNull Level level, final @NotNull BlockPos pos) {
        if (!enabled) {
            return;
        }
        PLACEMENTS.remove(level, pos);
    }

    public static @NotNull Optional<PlacementRecord> placementAt(
        final @NotNull Level level,
        final @NotNull BlockPos pos
    ) {
        return PLACEMENTS.get(level, pos);
    }

    // -------------------------------------------------------------------------
    // Consume / death
    // -------------------------------------------------------------------------

    public static void noteConsumed(final @NotNull ItemStack stack) {
        if (!enabled) {
            return;
        }
        final Optional<UUID> id = StackStamp.readId(stack);
        if (id.isEmpty()) {
            return;
        }
        if (stack.isEmpty() || stack.getCount() <= 0) {
            death(id.get(), ProvenanceReason.CONSUMED, null);
        } else {
            LIVE.get(id.get()).ifPresent(e -> e.setCount(stack.getCount()));
        }
    }

    public static void noteDestroyed(final @NotNull ItemStack stack) {
        if (!enabled) {
            return;
        }
        StackStamp.readId(stack).ifPresent(id -> death(id, ProvenanceReason.DESTROYED, null));
    }

    public static void death(
        final @NotNull UUID id,
        final @NotNull ProvenanceReason reason,
        final @Nullable UUID related
    ) {
        if (!enabled) {
            return;
        }
        final LiveEntry removed = LIVE.remove(id);
        final Optional<LineageNode> existingNode = LINEAGE.get(id);
        if (removed == null && existingNode.isPresent() && existingNode.get().dead()) {
            return; // idempotent
        }
        final long now = System.currentTimeMillis();
        final String itemId = removed != null
            ? removed.itemId()
            : existingNode.map(LineageNode::itemId).orElse(null);
        if (removed != null) {
            persistLive(removed, true);
        }
        existingNode.ifPresent(node -> {
            if (!node.dead()) {
                node.markDead(reason, now);
                LINEAGE.put(node); // persist the death
            }
        });
        final List<UUID> relatedList = related == null ? List.of() : List.of(related);
        AUDIT.append(new ProvenanceEvent(
            now,
            ProvenanceEventType.DEATH,
            id,
            itemId,
            null,
            reason,
            relatedList,
            removed != null ? removed.location().display() : null,
            null
        ));
    }

    public static void afterConsume(final @NotNull ItemStack stack) {
        noteConsumed(stack);
    }

    public static void onBroken(final @NotNull ItemStack stack) {
        if (!enabled) {
            return;
        }
        StackStamp.readId(stack).ifPresent(id -> death(id, ProvenanceReason.DESTROYED, null));
    }

    public static void onParked(final @NotNull ItemStack stack, final @NotNull StackLocation location) {
        ensure(stack, location);
    }

    public static void afterInputShrink(
        final @NotNull Optional<UUID> idBefore,
        final @NotNull ItemStack after
    ) {
        if (!enabled) {
            return;
        }
        if (after.isEmpty() || after.getCount() <= 0) {
            idBefore.ifPresent(id -> death(id, ProvenanceReason.CONSUMED, null));
        } else {
            noteConsumed(after);
        }
    }

    // -------------------------------------------------------------------------
    // Transforms
    // -------------------------------------------------------------------------

    public static void onCrafted(
        final @NotNull ItemStack result,
        final @NotNull List<UUID> ingredientIds,
        final @NotNull StackLocation location
    ) {
        if (!enabled || result.isEmpty()) {
            return;
        }
        // Shift-click stamps before moveItemStackTo; onTake may re-enter with the
        // same stack after its identity has already been absorbed by a merge.
        final Optional<StackProvenance> existing = StackStamp.read(result);
        if (existing.isPresent() && existing.get().source() == ProvenanceSource.CRAFT) {
            return;
        }
        birth(result, ProvenanceSource.CRAFT, location, ingredientIds);
    }

    public static @NotNull List<UUID> collectParents(final @NotNull Iterable<ItemStack> ingredients) {
        final LinkedHashSet<UUID> ids = new LinkedHashSet<>();
        for (final ItemStack stack : ingredients) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ensure(stack, StackLocation.unknown()).ifPresent(ids::add);
        }
        return List.copyOf(ids);
    }

    public static @NotNull Optional<UUID> onIdentityHandoff(
        final @Nullable ItemStack previous,
        final @NotNull ItemStack next,
        final @NotNull StackLocation location
    ) {
        if (!enabled || next.isEmpty()) {
            return Optional.empty();
        }
        return birth(next, ProvenanceSource.SPECIAL_RECIPE, location, parentIdsForHandoff(previous, location));
    }

    static @NotNull List<UUID> parentIdsForHandoff(
        final @Nullable ItemStack previous,
        final @Nullable StackLocation location
    ) {
        if (previous == null) {
            return List.of();
        }
        final Optional<UUID> existing = StackStamp.readId(previous);
        if (existing.isPresent()) {
            return List.of(existing.get());
        }
        if (!previous.isEmpty()) {
            return birth(previous, ProvenanceSource.LEGACY, location == null ? StackLocation.unknown() : location)
                .map(List::of)
                .orElse(List.of());
        }
        return List.of();
    }

    public static @NotNull Optional<UUID> onIdentityHandoff(
        final @NotNull ItemStack next,
        final @NotNull List<UUID> parents,
        final @NotNull StackLocation location
    ) {
        if (!enabled || next.isEmpty()) {
            return Optional.empty();
        }
        return birth(next, ProvenanceSource.SPECIAL_RECIPE, location, parents);
    }

    // -------------------------------------------------------------------------
    // Load-time rehydration
    // -------------------------------------------------------------------------

    /**
     * Load-time / first-touch: stack already has a UUID but is not in the live census.
     * The lineage node is loaded from the repository (or rebuilt from the stamp), so
     * history survives restarts.
     */
    public static void rehydrate(final @NotNull ItemStack stack, final @NotNull StackLocation location) {
        if (!enabled || stack.isEmpty()) {
            return;
        }
        final Optional<StackProvenance> stamp = StackStamp.read(stack);
        if (stamp.isEmpty()) {
            birth(stack, ProvenanceSource.LEGACY, location);
            return;
        }
        rehydrateIfNeeded(stack, stamp.get().id(), location);
    }

    private static void rehydrateIfNeeded(
        final ItemStack stack,
        final UUID id,
        final StackLocation location
    ) {
        final LiveEntry entry = LIVE.get(id).orElse(null);
        if (entry != null) {
            final int prevCount = entry.count();
            final StackLocation prevLocation = entry.location();
            entry.setCount(stack.getCount());
            if (location.isConcrete() && !entry.locations().contains(location)) {
                if (entry.locations().isEmpty()) {
                    entry.addLocation(location);
                } else {
                    // A loaded copy of an identity already tracked elsewhere.
                    final StackLocation existing = entry.locations().iterator().next();
                    recordCollision(id, ProvenanceCollisionKind.DUPLICATE_LOCATION, existing, location);
                }
            }
            // Already in LIVE: persist when count or accepted location actually changes.
            if (entry.count() != prevCount || !entry.location().equals(prevLocation)) {
                persistLive(entry, false);
            }
            return;
        }
        final long now = System.currentTimeMillis();
        final String itemId = itemId(stack);
        final Optional<StackProvenance> stamp = StackStamp.read(stack);
        final ProvenanceSource source = stamp.map(StackProvenance::source).orElse(ProvenanceSource.REHYDRATE);
        final List<UUID> parents = stamp.map(StackProvenance::parents).orElse(List.of());
        final long born = stamp.map(StackProvenance::bornEpochMs).orElse(now);

        if (LINEAGE.get(id).isEmpty()) {
            LINEAGE.put(new LineageNode(id, itemId, source, parents, born, location.display()));
        }
        final Optional<LineageNode> node = LINEAGE.get(id);
        if (node.isPresent() && node.get().dead()) {
            AUDIT.append(new ProvenanceEvent(
                now,
                ProvenanceEventType.ZOMBIE,
                id,
                itemId,
                source,
                null,
                List.of(),
                location.display(),
                "dead uuid reappeared"
            ));
        }
        final LiveEntry fresh = new LiveEntry(id, itemId, StackLocation.unknown(), stack.getCount(), born);
        if (location.isConcrete()) {
            fresh.addLocation(location);
        }
        LIVE.put(fresh);
        persistLive(fresh, false);
        AUDIT.append(new ProvenanceEvent(
            now,
            ProvenanceEventType.REHYDRATE,
            id,
            itemId,
            ProvenanceSource.REHYDRATE,
            null,
            List.of(),
            location.display(),
            null
        ));
    }

    // -------------------------------------------------------------------------
    // Durable live census
    // -------------------------------------------------------------------------

    private static void persistLive(final @NotNull LiveEntry entry, final boolean dead) {
        final LiveRecord record = new LiveRecord(
            entry.id(),
            entry.itemId(),
            entry.location().display(),
            entry.count(),
            System.currentTimeMillis(),
            dead
        );
        ProvenanceWriter.enqueueLive(record);
    }

    // -------------------------------------------------------------------------
    // Collisions
    // -------------------------------------------------------------------------

    private static void recordCollision(
        final UUID id,
        final ProvenanceCollisionKind kind,
        final StackLocation existing,
        final StackLocation observed
    ) {
        final String sig = kind.name() + '|' + existing.display() + '|' + observed.display();
        final String prior = COLLISION_SEEN.putIfAbsent(id, sig);
        if (prior != null && prior.equals(sig)) {
            return; // already recorded this exact pair
        }
        COLLISION_SEEN.put(id, sig);
        final long now = System.currentTimeMillis();
        final CollisionRecord record = new CollisionRecord(id, kind, existing, observed, now);
        synchronized (COLLISIONS) {
            if (COLLISIONS.size() >= COLLISION_CAPACITY) {
                COLLISIONS.removeFirst();
            }
            COLLISIONS.addLast(record);
        }
        ProvenanceWriter.enqueueCollision(record);
        AUDIT.append(new ProvenanceEvent(
            now,
            ProvenanceEventType.COLLISION,
            id,
            null,
            null,
            null,
            List.of(),
            observed.display(),
            kind.name() + " existing=" + existing.display()
        ));
    }

    /**
     * Simulate a dupe for tests / admin diagnostics: observe the same identity at
     * two distinct locations.
     */
    public static boolean simulateDupe(
        final @NotNull ItemStack original,
        final @NotNull StackLocation locationA,
        final @NotNull StackLocation locationB
    ) {
        Objects.requireNonNull(original, "original");
        if (original.isEmpty()) {
            return false;
        }
        ensure(original, locationA);
        final ItemStack clone = original.copy();
        return observe(clone, locationB);
    }

    public static @NotNull String itemId(final @NotNull ItemStack stack) {
        final Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null ? key.toString() : "unknown";
    }

    private static @NotNull List<UUID> dedupe(final List<UUID> parents) {
        if (parents == null || parents.isEmpty()) {
            return List.of();
        }
        return List.copyOf(new LinkedHashSet<>(parents));
    }
}
