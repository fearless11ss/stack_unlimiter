package dev.asp1r.stackunlimiter.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class LongCountMathTest {
    @Test
    void facadeIsExactUntilIntegerMaximum() {
        assertEquals(42, LongCountMath.facade(42));
        assertEquals(Integer.MAX_VALUE, LongCountMath.facade(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, LongCountMath.facade((long) Integer.MAX_VALUE + 1));
        assertEquals(Integer.MAX_VALUE, LongCountMath.facade(Long.MAX_VALUE));
    }

    @Test
    void additionRejectsOverflowAndNegativeResults() {
        assertEquals(101, LongCountMath.addExactNonNegative(100, 1));
        assertThrows(ArithmeticException.class, () -> LongCountMath.addExactNonNegative(Long.MAX_VALUE, 1));
        assertThrows(IllegalArgumentException.class, () -> LongCountMath.addExactNonNegative(0, -1));
    }

    @Test
    void subtractionStopsAtZero() {
        assertEquals(9, LongCountMath.subtractFloorZero(10, 1));
        assertEquals(0, LongCountMath.subtractFloorZero(10, 10));
        assertEquals(0, LongCountMath.subtractFloorZero(10, Long.MAX_VALUE));
    }

    @Test
    void serializedFallbackRespectsByteAndVanillaLimits() {
        assertEquals(16, LongCountMath.safeSerializedCount(1_000, 16));
        assertEquals(64, LongCountMath.safeSerializedCount(1_000, 64));
        assertEquals(Byte.MAX_VALUE, LongCountMath.safeSerializedCount(1_000, 1_000));
        assertEquals(7, LongCountMath.safeSerializedCount(7, 64));
    }
}
