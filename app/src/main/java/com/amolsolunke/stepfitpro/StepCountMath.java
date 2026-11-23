package com.amolsolunke.stepfitpro;

/** Small, side-effect-free helpers for step totals and goal progress. */
final class StepCountMath {
    private StepCountMath() {}

    static long nonNegativeLong(Object storedValue) {
        if (!(storedValue instanceof Number)) return 0L;
        return Math.max(0L, ((Number) storedValue).longValue());
    }

    static long saturatingAdd(long total, long value) {
        long safeTotal = Math.max(0L, total);
        long safeValue = Math.max(0L, value);
        return safeTotal > Long.MAX_VALUE - safeValue
                ? Long.MAX_VALUE
                : safeTotal + safeValue;
    }

    static int goalPercent(long steps, int goal) {
        if (goal <= 0) return 0;
        double percent = Math.max(0L, steps) * 100.0 / goal;
        return (int) Math.min(100.0, percent);
    }
}
