package dev.mintychochip.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ClimateHumidityTest {

    @Test
    public void dryBaseWithoutWaterOrRainStaysBase() {
        assertEquals(0.3, ClimateHumidity.compose(0.3, 0.0, false, 0.2), 1e-9);
    }

    @Test
    public void waterBonusPullsTowardOne() {
        // base 0.2 + 0.4*(1-0.2) = 0.2 + 0.32 = 0.52
        assertEquals(0.52, ClimateHumidity.compose(0.2, 0.4, false, 0.2), 1e-9);
    }

    @Test
    public void rainBumpsHumidityWhenRaining() {
        final double dry = ClimateHumidity.compose(0.3, 0.0, false, 0.2);
        final double wet = ClimateHumidity.compose(0.3, 0.0, true, 0.2);
        assertEquals(0.3, dry, 1e-9);
        // 0.3 + 0.2*(1-0.3) = 0.3 + 0.14 = 0.44
        assertEquals(0.44, wet, 1e-9);
        assertTrue(wet > dry);
    }

    @Test
    public void rainDoesNotApplyWhenNotRaining() {
        assertEquals(
            ClimateHumidity.compose(0.5, 0.1, false, 0.5),
            ClimateHumidity.compose(0.5, 0.1, false, 0.0),
            1e-9
        );
    }

    @Test
    public void rainAndWaterStackAsymptotically() {
        // water first: 0.2 + 0.4*0.8 = 0.52; rain: 0.52 + 0.2*0.48 = 0.616
        assertEquals(0.616, ClimateHumidity.compose(0.2, 0.4, true, 0.2), 1e-9);
    }

    @Test
    public void zeroRainBonusIsNoOpEvenWhenRaining() {
        assertEquals(0.5, ClimateHumidity.compose(0.5, 0.0, true, 0.0), 1e-9);
    }

    @Test
    public void resultClampedToUnitInterval() {
        assertEquals(1.0, ClimateHumidity.compose(1.0, 1.0, true, 1.0), 1e-9);
        assertEquals(0.0, ClimateHumidity.compose(-1.0, 0.0, false, 0.0), 1e-9);
    }
}
