package dev.mintychochip.season;

import java.util.Objects;

/**
 * Public entry point for the mintychochip wall-clock season system.
 *
 * <p>Callers can read the current season without the external ecology plugin.
 * The active {@link SeasonClock} defaults to a fixed spring-start anchor and
 * may be replaced by the server (or tests) via {@link #setClock(SeasonClock)}.
 */
public final class Seasons {
    private static volatile SeasonClock clock = SeasonClock.defaults();

    private Seasons() {
    }

    /** Active wall-clock used for season derivation. */
    public static SeasonClock clock() {
        return clock;
    }

    /**
     * Replace the active clock (e.g. after loading a persisted anchor).
     * Not thread-safe against concurrent readers beyond volatile visibility.
     */
    public static void setClock(final SeasonClock newClock) {
        clock = Objects.requireNonNull(newClock, "clock");
    }

    /** Reset to {@link SeasonClock#defaults()}. */
    public static void resetClock() {
        clock = SeasonClock.defaults();
    }

    public static Season current() {
        return current(System.currentTimeMillis());
    }

    public static Season current(final long nowMillis) {
        return clock.currentSeason(nowMillis);
    }

    public static int dayOfSeason() {
        return dayOfSeason(System.currentTimeMillis());
    }

    public static int dayOfSeason(final long nowMillis) {
        return clock.dayOfSeason(nowMillis);
    }

    public static double seasonProgress() {
        return seasonProgress(System.currentTimeMillis());
    }

    public static double seasonProgress(final long nowMillis) {
        return clock.seasonProgress(nowMillis);
    }

    public static boolean isWinter() {
        return isWinter(System.currentTimeMillis());
    }

    public static boolean isWinter(final long nowMillis) {
        return current(nowMillis) == Season.WINTER;
    }

    /** Seasonal temperature offset for the current wall-clock season. */
    public static float temperatureSwing() {
        return temperatureSwing(System.currentTimeMillis());
    }

    public static float temperatureSwing(final long nowMillis) {
        return SeasonClimate.temperatureSwing(current(nowMillis));
    }

    /** Whether winter should hold world rain for native precipitation ticks. */
    public static boolean shouldForcePrecipitation() {
        return shouldForcePrecipitation(System.currentTimeMillis());
    }

    public static boolean shouldForcePrecipitation(final long nowMillis) {
        return SeasonClimate.shouldForcePrecipitation(current(nowMillis));
    }
}
