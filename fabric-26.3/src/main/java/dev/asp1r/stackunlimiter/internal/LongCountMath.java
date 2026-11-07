package dev.asp1r.stackunlimiter.internal;

public final class LongCountMath {
    private LongCountMath() {
    }

    public static long requireValid(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Stack count cannot be negative: " + value);
        }
        return value;
    }

    public static long addExactNonNegative(long current, long delta) {
        requireValid(current);
        long result = Math.addExact(current, delta);
        return requireValid(result);
    }

    public static long subtractFloorZero(long current, long amount) {
        requireValid(current);
        requireValid(amount);
        return amount >= current ? 0 : current - amount;
    }

    public static int facade(long exact) {
        requireValid(exact);
        return exact > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) exact;
    }

    public static int safeSerializedCount(long exact, int vanillaMaximum) {
        requireValid(exact);
        int safeMaximum = Math.max(1, Math.min(Byte.MAX_VALUE, vanillaMaximum));
        return (int) Math.min(exact, safeMaximum);
    }
}
