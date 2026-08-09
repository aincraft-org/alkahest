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

    private static final java.util.Map<Integer, Kernel> KERNELS = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Immutable per-radius scan table: cells strictly inside the circle, sorted by
     * distance ascending (nearest first) so water-prone positions early-exit once
     * the cap is reached. Built once per radius and reused across all probes.
     */
    private static final class Kernel {
        final int radius;
        final int[] dx;
        final int[] dz;
        final double[] weight;
        final int size;

        Kernel(final int radius) {
            this.radius = radius;
            final java.util.List<double[]> cells = new java.util.ArrayList<>();
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    final double distSq = (double) x * x + (double) z * z;
                    if (distSq > (double) radius * radius) {
                        continue;
                    }
                    final double dist = Math.sqrt(distSq);
                    cells.add(new double[] { x, z, 1.0 - dist / radius });
                }
            }
            cells.sort((a, b) -> Double.compare(b[2], a[2]));
            this.size = cells.size();
            this.dx = new int[this.size];
            this.dz = new int[this.size];
            this.weight = new double[this.size];
            for (int i = 0; i < this.size; i++) {
                final double[] c = cells.get(i);
                this.dx[i] = (int) c[0];
                this.dz[i] = (int) c[1];
                this.weight[i] = c[2];
            }
        }
    }

    /**
     * Water bonus in {@code [0, cap]}: each water column within radius
     * contributes {@code (1 - distance/radius)}. Only already-loaded chunks
     * contribute — never triggers chunk loads. Scans only the circular kernel
     * (not the bounding square) and stops once the cap is reached.
     */
    public static double bonus(final int radiusBlocks, final double cap, final ColumnProbe probe) {
        if (radiusBlocks <= 0) {
            return 0.0;
        }
        final Kernel kernel = KERNELS.computeIfAbsent(radiusBlocks, Kernel::new);
        double sum = 0.0;
        for (int i = 0; i < kernel.size; i++) {
            if (!probe.loadedAt(kernel.dx[i], kernel.dz[i])) {
                continue;
            }
            if (!probe.waterAt(kernel.dx[i], kernel.dz[i])) {
                continue;
            }
            sum += kernel.weight[i];
            if (sum >= cap) {
                return cap;
            }
        }
        return sum;
    }
}