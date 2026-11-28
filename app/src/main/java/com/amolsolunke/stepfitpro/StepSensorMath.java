package com.amolsolunke.stepfitpro;

/** Pure step-counter baseline math; no Android APIs are used here. */
final class StepSensorMath {
    private StepSensorMath() {}

    static final class Result {
        final long todaySteps;
        final long baseSteps;
        final long lastRaw;

        Result(long todaySteps, long baseSteps, long lastRaw) {
            this.todaySteps = todaySteps;
            this.baseSteps = baseSteps;
            this.lastRaw = lastRaw;
        }
    }

    /** Reject malformed TYPE_STEP_COUNTER values before float-to-long conversion. */
    static boolean isValidRawCounter(float rawSteps) {
        // Long.MAX_VALUE rounds up to 2^63 when converted to double. Use the
        // exclusive 2^63 boundary directly so that out-of-range float is rejected.
        // TYPE_STEP_COUNTER is a count, so fractional payloads are malformed too.
        return rawSteps >= 0f
                && !Float.isNaN(rawSteps)
                && !Float.isInfinite(rawSteps)
                && rawSteps < 0x1.0p63f
                && rawSteps == Math.rint(rawSteps);
    }

    static Result update(long rawSteps, long todaySteps, long baseSteps, long lastRaw) {
        long raw = Math.max(0L, rawSteps);
        long total = Math.max(0L, todaySteps);
        long base = baseSteps;

        if (lastRaw < 0L) {
            // First event after install or missing sensor baseline.
            base = raw;
        } else if (raw < lastRaw) {
            // Hardware counter reset: keep the accumulated daily total.
            base = raw - total;
        }

        // A reset can make the baseline negative. Saturate instead of allowing
        // raw - base to wrap around when a corrupted/very large value is read.
        long calculated = nonNegativeDifference(raw, base);
        total = Math.max(total, calculated);
        return new Result(total, base, raw);
    }

    private static long nonNegativeDifference(long raw, long base) {
        if (base < 0L && raw > Long.MAX_VALUE + base) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, raw - base);
    }
}
