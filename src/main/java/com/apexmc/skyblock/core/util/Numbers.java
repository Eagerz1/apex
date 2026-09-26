package com.apexmc.skyblock.core.util;

/** Overflow-safe numeric helpers used by every progression calculation. */
public final class Numbers {

    private Numbers() {
    }

    /** Adds two longs, saturating instead of overflowing (exploit protection). */
    public static long safeAdd(long a, long b) {
        long result = a + b;
        if (((a ^ result) & (b ^ result)) < 0) {
            return a > 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
        return result;
    }

    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Multiplies an amount by a multiplier, never returning a negative result. */
    public static long applyMultiplier(long amount, double multiplier) {
        if (amount <= 0 || multiplier <= 0) {
            return 0;
        }
        double scaled = amount * multiplier;
        if (scaled >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return (long) Math.floor(scaled);
    }

    public static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
