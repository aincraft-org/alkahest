package dev.mintychochip.customblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import org.bukkit.NamespacedKey;
import org.bukkit.support.environment.Normal;
import org.junit.jupiter.api.Test;

/**
 * Chunk-scoped iteration tests for {@link CustomBlockPlacementsData}. Entries must
 * be retrievable by chunk without scanning every placement in the dimension.
 */
@Normal
class CustomBlockPlacementsDataChunkTest {

    private static final NamespacedKey KEY = NamespacedKey.minecraft("foo");

    private static long chunkOf(final BlockPos pos) {
        return (pos.getX() >> 4) | ((long) (pos.getZ() >> 4) << 32);
    }

    @Test
    void entriesInChunkReturnsOnlyThatChunk() {
        final CustomBlockPlacementsData data = new CustomBlockPlacementsData();
        final BlockPos inChunk = new BlockPos(3, 64, 7); // chunk 0,0
        final BlockPos otherChunk = new BlockPos(20, 64, 20); // chunk 1,1

        data.put(inChunk, KEY);
        data.put(otherChunk, KEY);

        final List<Map.Entry<BlockPos, NamespacedKey>> chunk = data.entriesInChunk(0, 0);
        assertEquals(1, chunk.size());
        assertEquals(inChunk, chunk.get(0).getKey());
    }

    @Test
    void removeDropsPlacementFromChunk() {
        final CustomBlockPlacementsData data = new CustomBlockPlacementsData();
        final BlockPos pos = new BlockPos(3, 64, 7);

        data.put(pos, KEY);
        assertTrue(data.entriesInChunk(0, 0).stream().anyMatch(e -> e.getKey().equals(pos)));

        data.remove(pos);
        assertTrue(data.entriesInChunk(0, 0).isEmpty());
    }

    @Test
    void entriesInChunkEmptyWhenNoPlacements() {
        final CustomBlockPlacementsData data = new CustomBlockPlacementsData();
        assertTrue(data.entriesInChunk(0, 0).isEmpty());
        assertTrue(data.entriesInChunk(5, -3).isEmpty());
    }

    @Test
    void negativeChunkCoordinatesDoNotCollide() {
        final CustomBlockPlacementsData data = new CustomBlockPlacementsData();
        // -1,0 and 0,-1 are distinct chunks; the packed key must not collide.
        final BlockPos negX = new BlockPos(-1, 64, 0); // chunk -1,0
        final BlockPos negZ = new BlockPos(0, 64, -1); // chunk 0,-1

        data.put(negX, KEY);
        data.put(negZ, KEY);

        assertEquals(1, data.entriesInChunk(-1, 0).size());
        assertEquals(1, data.entriesInChunk(0, -1).size());
        assertEquals(negX, data.entriesInChunk(-1, 0).get(0).getKey());
        assertEquals(negZ, data.entriesInChunk(0, -1).get(0).getKey());
    }
}