package dev.mintychochip.season;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class SeasonClockTest {
    private static final Season[] ORDER = Season.ORDER;
    private static final long DAY = SeasonClock.DAY_MILLIS;
    private static final long ANCHOR = 1_700_000_000_000L;

    @AfterEach
    public void resetClock() {
        Seasons.resetClock();
    }

    @Test
    public void startsAtSeasonDayOne() {
        final SeasonClock clock = new SeasonClock(ORDER, 7, ANCHOR);
        assertEquals(Season.SPRING, clock.currentSeason(ANCHOR));
        assertEquals(1, clock.dayOfSeason(ANCHOR));
        assertEquals(0.0, clock.seasonProgress(ANCHOR), 1e-9);
    }

    @Test
    public void advancesExactlyAtDayBoundary() {
        final SeasonClock clock = new SeasonClock(ORDER, 7, ANCHOR);
        assertEquals(Season.SPRING, clock.currentSeason(ANCHOR + 6 * DAY));
        assertEquals(7, clock.dayOfSeason(ANCHOR + 6 * DAY));
        assertEquals(Season.SUMMER, clock.currentSeason(ANCHOR + 7 * DAY));
        assertEquals(1, clock.dayOfSeason(ANCHOR + 7 * DAY));
    }

    @Test
    public void rollsOverYear() {
        final SeasonClock clock = new SeasonClock(ORDER, 7, ANCHOR);
        assertEquals(Season.WINTER, clock.currentSeason(ANCHOR + 27 * DAY));
        assertEquals(Season.SPRING, clock.currentSeason(ANCHOR + 28 * DAY));
        assertEquals(1, clock.dayOfSeason(ANCHOR + 28 * DAY));
    }

    @Test
    public void progressWithinDay() {
        final SeasonClock clock = new SeasonClock(ORDER, 7, ANCHOR);
        assertEquals(3.5 / 7.0, clock.seasonProgress(ANCHOR + 3 * DAY + DAY / 2), 1e-9);
        assertEquals(4, clock.dayOfSeason(ANCHOR + 3 * DAY + DAY / 2));
    }

    @Test
    public void anchorForMakesDesiredSeasonCurrent() {
        final long anchor = SeasonClock.anchorFor(ANCHOR, Season.WINTER, ORDER, 7);
        final SeasonClock clock = new SeasonClock(ORDER, 7, anchor);
        assertEquals(Season.WINTER, clock.currentSeason(ANCHOR));
        assertEquals(1, clock.dayOfSeason(ANCHOR));
        assertEquals(0.0, clock.seasonProgress(ANCHOR), 1e-9);
    }

    @Test
    public void lengthChangeShiftsBoundariesFromAnchor() {
        final SeasonClock clock = new SeasonClock(ORDER, 2, ANCHOR);
        assertEquals(Season.SUMMER, clock.currentSeason(ANCHOR + 3 * DAY));
        assertEquals(2, clock.dayOfSeason(ANCHOR + 3 * DAY));
    }

    @Test
    public void rejectsBadArguments() {
        assertThrows(IllegalArgumentException.class, () -> new SeasonClock(ORDER, 0, ANCHOR));
        assertThrows(IllegalArgumentException.class, () -> new SeasonClock(new Season[0], 7, ANCHOR));
        assertThrows(
            IllegalArgumentException.class,
            () -> SeasonClock.anchorFor(ANCHOR, Season.SPRING, new Season[]{Season.SUMMER}, 7)
        );
    }

    @Test
    public void seasonsFacadeUsesActiveClock() {
        final long anchor = SeasonClock.anchorFor(ANCHOR, Season.WINTER, ORDER, 7);
        Seasons.setClock(new SeasonClock(ORDER, 7, anchor));
        assertEquals(Season.WINTER, Seasons.current(ANCHOR));
        assertEquals(1, Seasons.dayOfSeason(ANCHOR));
        assertTrue(Seasons.isWinter(ANCHOR));
        assertTrue(Seasons.shouldForcePrecipitation(ANCHOR));
    }
}
