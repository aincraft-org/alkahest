package dev.mintychochip.season;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.bukkit.support.environment.Normal;
import org.junit.jupiter.api.Test;

@Normal
public class SeasonClimateTest {

    @AfterEach
    public void resetClock() {
        Seasons.resetClock();
    }

    @Test
    public void winterIsColderThanSummer() {
        assertTrue(SeasonClimate.temperatureSwing(Season.WINTER) < SeasonClimate.temperatureSwing(Season.SUMMER));
        assertEquals(-0.3F, SeasonClimate.temperatureSwing(Season.WINTER), 1e-6);
        assertEquals(0.3F, SeasonClimate.temperatureSwing(Season.SUMMER), 1e-6);
        assertEquals(-0.1F, SeasonClimate.temperatureSwing(Season.AUTUMN), 1e-6);
        assertEquals(0.1F, SeasonClimate.temperatureSwing(Season.SPRING), 1e-6);
    }

    @Test
    public void borderlineBiomeSnowsInWinterNotSummer() {
        final float taigaBase = 0.25F;
        assertTrue(SeasonClimate.coldEnoughToSnow(taigaBase, Season.WINTER));
        assertFalse(SeasonClimate.coldEnoughToSnow(taigaBase, Season.SUMMER));
        assertFalse(SeasonClimate.coldEnoughToSnow(taigaBase, Season.SPRING));
    }

    @Test
    public void borderlineBiomeMeltsSnowOutsideWinter() {
        final float taigaBase = 0.25F;
        assertFalse(SeasonClimate.warmEnoughToMeltSnow(taigaBase, Season.WINTER));
        assertTrue(SeasonClimate.warmEnoughToMeltSnow(taigaBase, Season.SPRING));
        assertTrue(SeasonClimate.warmEnoughToMeltSnow(taigaBase, Season.SUMMER));
        assertTrue(SeasonClimate.warmEnoughToMeltSnow(taigaBase, Season.AUTUMN));
    }

    @Test
    public void alreadyColdBiomeStaysSnowyInWinter() {
        final float frozenBase = -0.5F;
        assertTrue(SeasonClimate.coldEnoughToSnow(frozenBase, Season.WINTER));
        assertTrue(SeasonClimate.coldEnoughToSnow(frozenBase, Season.SUMMER));
    }

    @Test
    public void permanentlyFrozenBiomeDoesNotMeltSnowInSummer() {
        final float frozenBase = -0.5F;
        assertFalse(SeasonClimate.warmEnoughToMeltSnow(frozenBase, Season.WINTER));
        assertFalse(SeasonClimate.warmEnoughToMeltSnow(frozenBase, Season.SUMMER));
    }

    @Test
    public void hotBiomeStillRainsInWinter() {
        final float desertBase = 2.0F;
        assertFalse(SeasonClimate.coldEnoughToSnow(desertBase, Season.WINTER));
        assertFalse(SeasonClimate.coldEnoughToSnow(desertBase, Season.SUMMER));
    }

    @Test
    public void adjustTemperatureAppliesSwing() {
        assertEquals(0.1F, SeasonClimate.adjustTemperature(0.4F, Season.WINTER), 1e-6);
        assertEquals(0.7F, SeasonClimate.adjustTemperature(0.4F, Season.SUMMER), 1e-6);
    }

    @Test
    public void onlyWinterForcesPrecipitation() {
        assertTrue(SeasonClimate.shouldForcePrecipitation(Season.WINTER));
        assertFalse(SeasonClimate.shouldForcePrecipitation(Season.SPRING));
        assertFalse(SeasonClimate.shouldForcePrecipitation(Season.SUMMER));
        assertFalse(SeasonClimate.shouldForcePrecipitation(Season.AUTUMN));
    }

    @Test
    public void seasonsTemperatureSwingTracksClock() {
        final long now = 1_700_000_000_000L;
        final long winterAnchor = SeasonClock.anchorFor(now, Season.WINTER, Season.ORDER, 7);
        Seasons.setClock(new SeasonClock(Season.ORDER, 7, winterAnchor));
        assertEquals(SeasonClimate.temperatureSwing(Season.WINTER), Seasons.temperatureSwing(now), 1e-6);
        assertTrue(Seasons.shouldForcePrecipitation(now));

        final long summerAnchor = SeasonClock.anchorFor(now, Season.SUMMER, Season.ORDER, 7);
        Seasons.setClock(new SeasonClock(Season.ORDER, 7, summerAnchor));
        assertEquals(SeasonClimate.temperatureSwing(Season.SUMMER), Seasons.temperatureSwing(now), 1e-6);
        assertFalse(Seasons.shouldForcePrecipitation(now));
    }
}
