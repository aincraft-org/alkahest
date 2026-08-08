package dev.mintychochip.customblock.display;

import com.mojang.math.Transformation;
import dev.mintychochip.customblock.CustomBlockDefinition;
import dev.mintychochip.customblock.CustomBlocks;
import dev.mintychochip.customblock.PacketHostSpec;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display.ItemDisplay;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Client-only item display (not added to the world entity list) — PacketBlocks style.
 *
 * <p>Spawns via packets only so the server never tracks the entity; clients still
 * render the item model at the block cell.
 */
public final class PacketItemDisplay {

    private final ItemDisplay handle;
    private final Set<Player> viewers = new HashSet<>();

    private PacketItemDisplay(final ItemDisplay handle) {
        this.handle = handle;
    }

    public static @NotNull PacketItemDisplay create(
        @NotNull final Location blockLoc,
        @NotNull final CustomBlockDefinition definition
    ) {
        Objects.requireNonNull(blockLoc, "blockLoc");
        Objects.requireNonNull(definition, "definition");
        final ServerLevel level = ((CraftWorld) Objects.requireNonNull(blockLoc.getWorld(), "world")).getHandle();

        // ignoreChecks so peaceful / feature flags cannot null the factory.
        final ItemDisplay display = EntityTypes.ITEM_DISPLAY.create(
            level,
            new EntitySpawnRequest(EntitySpawnReason.COMMAND, true)
        );
        if (display == null) {
            throw new IllegalStateException("Failed to create ItemDisplay");
        }

        // Entity sits on the block-cell corner; PacketHostSpec translation centers the model.
        display.setPos(blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ());
        display.setYRot(0.0f);
        display.setXRot(0.0f);

        final PacketHostSpec host = definition.host() instanceof PacketHostSpec p
            ? p
            : PacketHostSpec.defaults();

        final Vector3f translation = host.translation();
        final Vector3f scale = host.scale();
        final Quaternionf left = host.leftRotation();
        final Quaternionf right = host.rightRotation();
        display.setTransformation(new Transformation(translation, left, scale, right));
        display.setTransformationInterpolationDuration(0);
        display.setTransformationInterpolationDelay(0);
        display.setBillboardConstraints(net.minecraft.world.entity.Display.BillboardConstraints.FIXED);

        // Same stack clients get in inventory (item model + base material).
        final ItemStack visual = CustomBlocks.createItemStack(definition);
        final net.minecraft.world.item.ItemStack nmsStack =
            net.minecraft.world.item.ItemStack.fromBukkitCopy(visual);
        display.setItemStack(nmsStack);
        // NONE: skip the model's item-frame "fixed" half-scale so cube models fill the cell.
        // (FIXED would apply ~0.5 scale from the default block item display transform.)
        display.setItemTransform(ItemDisplayContext.NONE);
        display.setInvisible(false);
        // No culling / no shadow — keep the cube readable from all angles.
        display.setShadowRadius(0.0f);
        display.setShadowStrength(0.0f);
        // Force entity-data dirty so packAll()/packDirty() includes item + transform.
        display.getEntityData().set(
            net.minecraft.world.entity.Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID,
            0
        );

        return new PacketItemDisplay(display);
    }

    public int entityId() {
        return this.handle.getId();
    }

    public boolean isViewing(final Player player) {
        return this.viewers.contains(player);
    }

    public Set<Player> viewers() {
        return Set.copyOf(this.viewers);
    }

    public void show(final Player player) {
        if (!this.viewers.add(player)) {
            return;
        }
        if (!(player instanceof CraftPlayer craftPlayer)) {
            return;
        }
        final ServerPlayer sp = craftPlayer.getHandle();
        final ClientboundAddEntityPacket spawn = new ClientboundAddEntityPacket(
            this.handle.getId(),
            this.handle.getUUID(),
            this.handle.getX(),
            this.handle.getY(),
            this.handle.getZ(),
            this.handle.getXRot(),
            this.handle.getYRot(),
            this.handle.getType(),
            0,
            Vec3.ZERO,
            this.handle.getYHeadRot()
        );
        final Packet<? super ClientGamePacketListener> meta = metadataPacket();
        sp.connection.send(new ClientboundBundlePacket(List.of(spawn, meta)));
    }

    public void hide(final Player player) {
        if (!this.viewers.remove(player)) {
            return;
        }
        if (player instanceof CraftPlayer craftPlayer) {
            craftPlayer.getHandle().connection.send(
                new ClientboundRemoveEntitiesPacket(this.handle.getId())
            );
        }
    }

    public void hideAll() {
        for (final Player viewer : List.copyOf(this.viewers)) {
            hide(viewer);
        }
    }

    private Packet<? super ClientGamePacketListener> metadataPacket() {
        final SynchedEntityData data = this.handle.getEntityData();
        List<SynchedEntityData.DataValue<?>> values = data.packAll();
        if (values == null || values.isEmpty()) {
            values = data.packDirty();
        }
        if (values == null) {
            values = List.of();
        }
        return new ClientboundSetEntityDataPacket(this.handle.getId(), values);
    }
}
