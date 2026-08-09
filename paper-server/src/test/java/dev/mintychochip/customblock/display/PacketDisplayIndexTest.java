package dev.mintychochip.customblock.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import org.bukkit.support.environment.Normal;
import org.junit.jupiter.api.Test;

/**
 * Index-consistency tests for {@link PacketDisplayIndex}: every display put into
 * the location map must also be visible through the chunk index (and vice versa),
 * across restore, despawn, and clear.
 */
@Normal
class PacketDisplayIndexTest {

    @Test
    void putRegistersDisplayInItsChunk() {
        final PacketDisplayIndex<String> index = new PacketDisplayIndex<>();
        index.put("locA", "world:0,0", "displayA");
        index.put("locB", "world:0,0", "displayB");
        index.put("locC", "world:1,0", "displayC");

        assertEquals(List.of("displayA", "displayB"), sorted(index.displaysInChunk("world:0,0")));
        assertEquals(List.of("displayC"), sorted(index.displaysInChunk("world:1,0")));
        assertEquals(List.of(), sorted(index.displaysInChunk("world:9,9")));
    }

    @Test
    void restoreFollowedByDespawnRemovesFromBothMaps() {
        final PacketDisplayIndex<String> index = new PacketDisplayIndex<>();
        index.put("locA", "world:0,0", "displayA");

        // Despawn removes from chunk index and location map.
        final String removed = index.remove("locA", "world:0,0");

        assertEquals("displayA", removed);
        assertTrue(index.displaysInChunk("world:0,0").isEmpty());
        assertTrue(index.get("locA") == null);
    }

    @Test
    void clearRemovesAllDisplaysAndChunkEntries() {
        final PacketDisplayIndex<String> index = new PacketDisplayIndex<>();
        index.put("locA", "world:0,0", "displayA");
        index.put("locB", "world:1,0", "displayB");

        index.clear();

        assertTrue(index.all().isEmpty());
        assertTrue(index.displaysInChunk("world:0,0").isEmpty());
        assertTrue(index.displaysInChunk("world:1,0").isEmpty());
    }

    @Test
    void despawnOfUnknownKeyLeavesChunkIndexUntouched() {
        final PacketDisplayIndex<String> index = new PacketDisplayIndex<>();
        index.put("locA", "world:0,0", "displayA");

        final String removed = index.remove("locMissing", "world:0,0");

        assertTrue(removed == null);
        assertEquals(List.of("displayA"), sorted(index.displaysInChunk("world:0,0")));
    }

    private static java.util.List<String> sorted(final Collection<String> values) {
        return values.stream().sorted().toList();
    }
}