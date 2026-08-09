package dev.mintychochip.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class WaterProximityTest {

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
}
