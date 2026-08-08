package dev.mintychochip.season;

/**
 * The four seasons of the mintychochip calendar, in chronological order.
 */
public enum Season {
    SPRING,
    SUMMER,
    AUTUMN,
    WINTER;

    /** Default calendar order: spring → summer → autumn → winter. */
    public static final Season[] ORDER = {SPRING, SUMMER, AUTUMN, WINTER};
}
