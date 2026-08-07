package dev.mintychochip.customblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class CustomBlockDefinitionTest {

    @AfterEach
    public void tearDown() {
        CustomBlocks.reset();
    }

    @Test
    public void packetDefinitionDefaults() {
        final CustomBlockDefinition def = CustomBlockDefinition.builder("mintychochip:electrum_ore")
            .host(PacketHostSpec.defaults())
            .itemMaterial(Material.GLASS)
            .itemModel(Key.key("mintychochip", "electrum_ore"))
            .displayName(Component.text("Electrum Ore"))
            .build();

        assertEquals(BlockHostType.PACKET, def.hostType());
        assertTrue(def.isPacket());
        assertFalse(def.isBaked());
        assertEquals(Material.GLASS, def.itemMaterial());
        assertEquals(Key.key("mintychochip", "electrum_ore"), def.itemModel());
        assertNotNull(def.displayName());

        // Builder default is a placeable block so clients play place (not use) animation.
        final CustomBlockDefinition defaults = CustomBlockDefinition.builder("mintychochip:defaults")
            .host(PacketHostSpec.defaults())
            .build();
        assertEquals(Material.GLASS, defaults.itemMaterial());

        final PacketHostSpec host = (PacketHostSpec) def.host();
        assertEquals("minecraft:glass", host.collisionMaterialKey());
        assertEquals(new Vector3f(0.5f, 0.5f, 0.5f), host.translation());
        assertEquals(new Vector3f(1.001f, 1.001f, 1.001f), host.scale());
    }

    @Test
    public void bakedHosts() {
        final CustomBlockDefinition chorus = CustomBlockDefinition.builder("mintychochip:chorus_ore")
            .host(ChorusHostSpec.ofState(3))
            .itemMaterial(Material.STONE)
            .build();
        assertEquals(BlockHostType.CHORUS, chorus.hostType());
        assertTrue(chorus.isBaked());
        assertEquals(3, ((ChorusHostSpec) chorus.host()).stateIndex());

        final CustomBlockDefinition mush = CustomBlockDefinition.builder("mintychochip:shroom_crate")
            .host(MushroomHostSpec.red())
            .itemMaterial(Material.PAPER)
            .build();
        assertEquals(BlockHostType.MUSHROOM, mush.hostType());
        assertEquals(MushroomVariant.RED, ((MushroomHostSpec) mush.host()).variant());

        final CustomBlockDefinition wire = CustomBlockDefinition.builder("mintychochip:wire_lamp")
            .host(TripwireHostSpec.unassigned())
            .itemMaterial(Material.STRING)
            .build();
        assertEquals(BlockHostType.TRIPWIRE, wire.hostType());
        assertEquals(null, ((TripwireHostSpec) wire.host()).stateIndex());
    }

    @Test
    public void builderRequiresHost() {
        assertThrows(IllegalStateException.class, () ->
            CustomBlockDefinition.builder("mintychochip:no_host")
                .itemMaterial(Material.STONE)
                .build()
        );
    }

    @Test
    public void rejectsAirItemMaterial() {
        assertThrows(IllegalArgumentException.class, () ->
            CustomBlockDefinition.builder("mintychochip:air_block")
                .host(ChorusHostSpec.unassigned())
                .itemMaterial(Material.AIR)
                .build()
        );
    }

    @Test
    public void rejectsNegativeStateIndex() {
        assertThrows(IllegalArgumentException.class, () -> ChorusHostSpec.ofState(-1));
        assertThrows(IllegalArgumentException.class, () -> TripwireHostSpec.ofState(-1));
        assertThrows(IllegalArgumentException.class, () ->
            MushroomHostSpec.of(MushroomVariant.BROWN, -1)
        );
    }

    @Test
    public void catalogRegisterAndLookup() {
        final CustomBlockDefinition ore = CustomBlockDefinition.builder(
                NamespacedKey.fromString("mintychochip:electrum_ore"))
            .host(PacketHostSpec.defaults())
            .build();

        CustomBlocks.register(ore);

        assertTrue(CustomBlocks.contains(ore.namespacedKey()));
        assertEquals(ore, CustomBlocks.get("mintychochip:electrum_ore").orElseThrow());
        assertEquals(1, CustomBlocks.all().size());

        assertThrows(IllegalStateException.class, () -> CustomBlocks.register(ore));
    }

    @Test
    public void hostTypeHelpers() {
        assertTrue(BlockHostType.CHORUS.isBaked());
        assertTrue(BlockHostType.MUSHROOM.isBaked());
        assertTrue(BlockHostType.TRIPWIRE.isBaked());
        assertFalse(BlockHostType.PACKET.isBaked());
        assertTrue(BlockHostType.PACKET.isPacket());
    }

    @Test
    public void packetBuilderCustomCollision() {
        final PacketHostSpec host = PacketHostSpec.builder()
            .collisionMaterialKey("minecraft:barrier")
            .scale(new Vector3f(1.05f))
            .build();
        assertEquals("minecraft:barrier", host.collisionMaterialKey());
        assertEquals(new Vector3f(1.05f, 1.05f, 1.05f), host.scale());
    }

    @Test
    public void defaultItemModelMatchesKey() {
        final CustomBlockDefinition def = CustomBlockDefinition.builder("mintychochip:crate")
            .host(MushroomHostSpec.brown())
            .build();
        assertEquals(Key.key("mintychochip", "crate"), def.itemModel());
    }

    @Test
    public void blockFeelExplicitStrength() {
        final BlockFeel feel = BlockFeel.of(3.0F, 3.0F, true, Material.IRON_ORE);
        assertEquals(3.0F, feel.hardness());
        assertEquals(3.0F, feel.blastResistance());
        assertTrue(feel.requiresCorrectToolForDrops());
        assertEquals(Material.IRON_ORE, feel.toolTemplate());
        assertFalse(feel.isUnbreakable());

        final BlockFeel bedrock = BlockFeel.builder().hardness(-1.0F).blastResistance(3600000.0F).build();
        assertTrue(bedrock.isUnbreakable());

        final CustomBlockDefinition ore = CustomBlockDefinition.builder("mintychochip:feel_ore")
            .host(PacketHostSpec.defaults())
            .feel(feel)
            .build();
        assertEquals(3.0F, ore.feel().hardness());
    }
}
