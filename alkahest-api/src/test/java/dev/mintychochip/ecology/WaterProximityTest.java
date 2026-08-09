package dev.mintychochip.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class WaterProximityTest {

    private static final class CountingProbe implements WaterProximity.ColumnProbe {
        private final AtomicInteger loadedCalls = new AtomicInteger();
        private final AtomicInteger waterCalls = new AtomicInteger();
        private final java.util.function.Predicate<int[]> water;
        private final java.util.function.Predicate<int[]> loaded;

        CountingProbe(final java.util.function.Predicate<int[]> loaded, final java.util.function.Predicate<int[]> water) {
            this.loaded = loaded;
            this.water = water;
        }

        @Override
        public boolean loadedAt(final int blockX, final int blockZ) {
            this.loadedCalls.incrementAndGet();
            return this.loaded.test(new int[] { blockX, blockZ });
        }

        @Override
        public boolean waterAt(final int blockX, final int blockZ) {
            this.waterCalls.incrementAndGet();
            return this.water.test(new int[] { blockX, blockZ });
        }
    }

    @Test
    public void noWaterYieldsZero() {
        final double bonus = WaterProximity.bonus(8, 0.4, new WaterProximity.ColumnProbe() {
            @Override
            public boolean waterAt(final int blockX, final int blockZ) {
                return false;
            }

            @Override
            public boolean loadedAt(final int blockX, final int blockZ) {
                return true;
            }
        });
        assertEquals(0.0, bonus, 1e-9);
    }

    @Test
    public void waterAtOriginGivesFullUnitContributionCapped() {
        final double bonus = WaterProximity.bonus(8, 0.4, new WaterProximity.ColumnProbe() {
            @Override
            public boolean waterAt(final int blockX, final int blockZ) {
                return blockX == 0 && blockZ == 0;
            }

            @Override
            public boolean loadedAt(final int blockX, final int blockZ) {
                return true;
            }
        });
        // distance 0 → contribution 1.0, then capped at 0.4
        assertEquals(0.4, bonus, 1e-9);
    }

    @Test
    public void unloadedColumnsDoNotContribute() {
        final double bonus = WaterProximity.bonus(4, 1.0, new WaterProximity.ColumnProbe() {
            @Override
            public boolean waterAt(final int blockX, final int blockZ) {
                return true;
            }

            @Override
            public boolean loadedAt(final int blockX, final int blockZ) {
                return false;
            }
        });
        assertEquals(0.0, bonus, 1e-9);
    }

    @Test
    public void nearbyWaterLessThanCapWithoutFlood() {
        final double bonus = WaterProximity.bonus(2, 1.0, new WaterProximity.ColumnProbe() {
            @Override
            public boolean waterAt(final int blockX, final int blockZ) {
                return blockX == 1 && blockZ == 0;
            }

            @Override
            public boolean loadedAt(final int blockX, final int blockZ) {
                return true;
            }
        });
        assertTrue(bonus > 0.0 && bonus < 1.0);
        assertEquals(1.0 - 1.0 / 2.0, bonus, 1e-9);
    }

    @Test
    public void sampleCountMatchesCircleAreaWhenAllLoadedAndDry() {
        final int radius = 8;
        final CountingProbe probe = new CountingProbe(
            pos -> true,
            pos -> false
        );

        final double bonus = WaterProximity.bonus(radius, 1.0, probe);

        assertEquals(0.0, bonus, 1e-9);
        assertEquals(circleArea(radius), probe.loadedCalls.get());
        assertEquals(circleArea(radius), probe.waterCalls.get());
    }

    @Test
    public void waterProbeNeverCalledOutsideLoadedColumns() {
        final int radius = 4;
        final CountingProbe probe = new CountingProbe(
            pos -> pos[0] == 0 && pos[1] == 0,
            pos -> true
        );

        final double bonus = WaterProximity.bonus(radius, 1.0, probe);

        assertEquals(1.0, bonus, 1e-9);
        assertEquals(1, probe.loadedCalls.get());
        assertEquals(1, probe.waterCalls.get());
    }

    @Test
    public void nonPositiveRadiusReturnsZeroWithoutProbing() {
        final CountingProbe probe = new CountingProbe(pos -> true, pos -> true);

        assertEquals(0.0, WaterProximity.bonus(0, 1.0, probe), 1e-9);
        assertEquals(0.0, WaterProximity.bonus(-1, 1.0, probe), 1e-9);
        assertEquals(0, probe.loadedCalls.get());
        assertEquals(0, probe.waterCalls.get());
    }

    private static int circleArea(final int radius) {
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius) {
                    count++;
                }
            }
        }
        return count;
    }
}
