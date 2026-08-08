package dev.mintychochip.customblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Lifecycle unit tests with mocks (no full server world).
 */
public class CustomBlockLifecycleTest {

    private MemoryCustomBlockLookup lookup;

    @BeforeEach
    public void setUp() {
        CustomBlocks.reset();
        this.lookup = new MemoryCustomBlockLookup();
        CustomBlocks.setLookup(this.lookup);

        final CustomBlockDefinition ore = CustomBlockDefinition.builder("mintychochip:electrum_ore")
            .host(PacketHostSpec.defaults())
            .itemMaterial(Material.PAPER)
            .build();
        CustomBlocks.register(ore);
    }

    @AfterEach
    public void tearDown() {
        CustomBlocks.reset();
    }

    @Test
    public void carrierMaterialsByHost() {
        final CustomBlockDefinition packet = CustomBlocks.get("mintychochip:electrum_ore").orElseThrow();
        assertEquals(Material.GLASS, CustomBlockPlacement.carrierMaterial(packet));

        final CustomBlockDefinition chorus = CustomBlockDefinition.builder("mintychochip:chorus_x")
            .host(ChorusHostSpec.unassigned())
            .itemMaterial(Material.PAPER)
            .build();
        assertEquals(Material.CHORUS_PLANT, CustomBlockPlacement.carrierMaterial(chorus));

        final CustomBlockDefinition red = CustomBlockDefinition.builder("mintychochip:mush_x")
            .host(MushroomHostSpec.red())
            .itemMaterial(Material.PAPER)
            .build();
        assertEquals(Material.RED_MUSHROOM_BLOCK, CustomBlockPlacement.carrierMaterial(red));

        final CustomBlockDefinition wire = CustomBlockDefinition.builder("mintychochip:wire_x")
            .host(TripwireHostSpec.unassigned())
            .itemMaterial(Material.PAPER)
            .build();
        assertEquals(Material.TRIPWIRE, CustomBlockPlacement.carrierMaterial(wire));
    }

    @Test
    public void prepareBreakSuppressesVanillaDrops() {
        final Block block = mockBlock(10, 64, 10);
        final Player player = mock(Player.class);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);

        final CustomBlockDefinition def = CustomBlocks.get("mintychochip:electrum_ore").orElseThrow();
        this.lookup.put(block, def);

        final BlockBreakEvent event = new BlockBreakEvent(block, player);
        assertTrue(event.isDropItems());

        final var prepared = CustomBlockLifecycle.prepareBreak(event);
        assertTrue(prepared.isPresent());
        assertEquals(def, prepared.get());
        assertFalse(event.isDropItems());
        assertEquals(0, event.getExpToDrop());
    }

    @Test
    public void prepareBreakIgnoresVanillaBlocks() {
        final Block block = mockBlock(1, 1, 1);
        final Player player = mock(Player.class);
        final BlockBreakEvent event = new BlockBreakEvent(block, player);

        assertTrue(CustomBlockLifecycle.prepareBreak(event).isEmpty());
        assertTrue(event.isDropItems());
    }

    @Test
    public void finishBreakRemovesPlacement() {
        final World world = mock(World.class);
        final Block block = mockBlock(5, 70, 5);
        when(block.getWorld()).thenReturn(world);
        final Location loc = new Location(world, 5, 70, 5);
        when(block.getLocation()).thenReturn(loc);

        final Player player = mock(Player.class);
        // Creative: clears identity, no ItemStack factory needed for drops.
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);

        final CustomBlockDefinition def = CustomBlocks.get("mintychochip:electrum_ore").orElseThrow();
        this.lookup.put(block, def);
        assertTrue(this.lookup.keyAt(block).isPresent());

        final BlockBreakEvent event = new BlockBreakEvent(block, player);
        CustomBlockLifecycle.finishBreak(event, def);

        assertTrue(this.lookup.keyAt(block).isEmpty());
        verify(world, never()).dropItemNaturally(any(), any());
    }

    @Test
    public void memoryLookupPutGetRemove() {
        final Block block = mockBlock(0, 0, 0);
        final CustomBlockDefinition def = CustomBlocks.get("mintychochip:electrum_ore").orElseThrow();

        this.lookup.put(block, def);
        assertEquals(def.namespacedKey(), this.lookup.keyAt(block).orElseThrow());
        assertTrue(this.lookup.remove(block));
        assertTrue(this.lookup.keyAt(block).isEmpty());
    }

    private static Block mockBlock(final int x, final int y, final int z) {
        final World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        final Block block = mock(Block.class);
        final Location loc = new Location(world, x, y, z);
        when(block.getLocation()).thenReturn(loc);
        when(block.getWorld()).thenReturn(world);
        when(block.getX()).thenReturn(x);
        when(block.getY()).thenReturn(y);
        when(block.getZ()).thenReturn(z);
        // Block#getCustomBlock defaults call CustomBlocks.of(this) which uses lookup.keyAt
        return block;
    }
}
