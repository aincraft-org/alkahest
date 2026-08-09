package dev.mintychochip.season;

/**
 * Wall-clock season derivation. A pure function of epoch millis since a fixed
 * or persisted anchor — no ticking, crash- and downtime-proof.
 */
public final class SeasonClock {
    public static final long DAY_MILLIS = 86_400_000L;

    /** Default real-time length of one season. */
    public static final int DEFAULT_LENGTH_DAYS = 7;

    /**
     * Default anchor (2024-01-01T00:00:00Z) so SPRING starts at day 1 of that
     * epoch. Servers may replace this via {@link Seasons#setClock(SeasonClock)}.
     */
    public static final long DEFAULT_ANCHOR_EPOCH_MILLIS = 1_704_067_200_000L;

    private final Season[] order;
    private final int lengthDays;
    private final long seasonStartEpochMillis;

    public SeasonClock(final Season[] order, final int lengthDays, final long seasonStartEpochMillis) {
        if (order == null || order.length == 0) {
            throw new IllegalArgumentException("order required");
        }
        if (lengthDays <= 0) {
            throw new IllegalArgumentException("lengthDays must be positive");
        }
        this.order = order.clone();
        this.lengthDays = lengthDays;
        this.seasonStartEpochMillis = seasonStartEpochMillis;
    }

    /** Clock with default order, length, and spring-start anchor. */
    public static SeasonClock defaults() {
        return new SeasonClock(Season.ORDER, DEFAULT_LENGTH_DAYS, DEFAULT_ANCHOR_EPOCH_MILLIS);
    }

    public Season currentSeason(final long nowMillis) {
        return this.order[Math.floorMod(this.seasonIndex(nowMillis), this.order.length)];
    }

    /** 1-based day index within the current season. */
    public int dayOfSeason(final long nowMillis) {
        return Math.floorMod(this.elapsedDays(nowMillis), this.lengthDays) + 1;
    }

    /** Fraction 0..1 through the current season. */
    public double seasonProgress(final long nowMillis) {
        final long elapsed = Math.max(0L, nowMillis - this.seasonStartEpochMillis);
        final long dayIndex = Math.floorMod(this.elapsedDays(nowMillis), this.lengthDays);
        final double dayFraction = (elapsed % DAY_MILLIS) / (double) DAY_MILLIS;
        return (dayIndex + dayFraction) / this.lengthDays;
    }

    public long seasonStartEpochMillis() {
        return this.seasonStartEpochMillis;
    }

    public int lengthDays() {
        return this.lengthDays;
    }

    public Season[] order() {
        return this.order.clone();
    }

    /**
     * Anchor for {@code now} such that {@code desired} is the current season on day 1.
     */
    public static long anchorFor(
        final long nowMillis,
        final Season desired,
        final Season[] order,
        final int lengthDays
    ) {
        if (order == null || order.length == 0) {
            throw new IllegalArgumentException("order required");
        }
        if (lengthDays <= 0) {
            throw new IllegalArgumentException("lengthDays must be positive");
        }
        int idx = -1;
        for (int i = 0; i < order.length; i++) {
            if (order[i] == desired) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            throw new IllegalArgumentException("season not in order: " + desired);
        }
        return nowMillis - (long) idx * lengthDays * DAY_MILLIS;
    }

    private long elapsedDays(final long nowMillis) {
        return Math.max(0L, nowMillis - this.seasonStartEpochMillis) / DAY_MILLIS;
    }

    private long seasonIndex(final long nowMillis) {
        return this.elapsedDays(nowMillis) / this.lengthDays;
    }
}
