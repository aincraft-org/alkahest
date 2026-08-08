package dev.mintychochip.chunkload;

/**
 * Bulk pack/unpack helpers for {@code SimpleBitStorage} (chunk load path).
 * Scalar fallbacks keep the server compiling without native SIMD.
 */
public final class BitStorageKernels {

    private BitStorageKernels() {
    }

    /**
     * Pack int values into long cells (same layout as vanilla SimpleBitStorage ctor from ints).
     */
    public static void pack(final int[] values, final int bits, final int size, final long[] data) {
        if (bits <= 0 || bits > 32) {
            throw new IllegalArgumentException("bits out of range: " + bits);
        }
        final long mask = (1L << bits) - 1L;
        final int valuesPerLong = 64 / bits;
        int inputOffset = 0;
        int outputIndex = 0;
        for (; inputOffset <= size - valuesPerLong; inputOffset += valuesPerLong) {
            long packedValue = 0L;
            for (int indexInLong = valuesPerLong - 1; indexInLong >= 0; indexInLong--) {
                packedValue <<= bits;
                packedValue |= values[inputOffset + indexInLong] & mask;
            }
            data[outputIndex++] = packedValue;
        }
        final int remainderCount = size - inputOffset;
        if (remainderCount > 0) {
            long lastPackedValue = 0L;
            for (int indexInLong = remainderCount - 1; indexInLong >= 0; indexInLong--) {
                lastPackedValue <<= bits;
                lastPackedValue |= values[inputOffset + indexInLong] & mask;
            }
            data[outputIndex] = lastPackedValue;
        }
    }

    /**
     * Unpack long cells into an int array (full materialization).
     */
    public static void unpack(final long[] data, final int bits, final int size, final int[] output) {
        if (bits <= 0 || bits > 32) {
            throw new IllegalArgumentException("bits out of range: " + bits);
        }
        final long mask = (1L << bits) - 1L;
        final int valuesPerLong = 64 / bits;
        int outputOffset = 0;
        for (final long cellValueRaw : data) {
            long cellValue = cellValueRaw;
            final int remaining = size - outputOffset;
            final int count = Math.min(valuesPerLong, remaining);
            for (int i = 0; i < count; i++) {
                output[outputOffset++] = (int) (cellValue & mask);
                cellValue >>= bits;
            }
            if (outputOffset >= size) {
                break;
            }
        }
    }
}
