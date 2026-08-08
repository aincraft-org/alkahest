package dev.mintychochip.ecology;

/**
 * Distance-weighted surface-water proximity (from azoth-season ecology).
 * Only already-loaded chunks contribute — never triggers chunk loads.
 */
public final class WaterProximity {
    private WaterProximity() {
    }

    public interface ColumnProbe {
        boolean waterAt(int blockX, int blockZ);

        boolean loadedAt(int blockX, int blockZ);
    }

    /**
     * Water bonus in {@code [0, cap]}: each water column within radius
     * contributes {@code (1 - distance/radius)}.
     */
    public static double bonus(final int radiusBlocks, final double cap, final ColumnProbe probe) {
        if (radiusBlocks <= 0) {
            return 0.0;
        }
        double sum = 0.0;
        final int r = radiusBlocks;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (!probe.loadedAt(dx, dz)) {
                    continue;
                }
                if (!probe.waterAt(dx, dz)) {
                    continue;
                }
                final double dist = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (dist > r) {
                    continue;
                }
                sum += 1.0 - dist / r;
            }
        }
        return Math.min(cap, sum);
    }
}