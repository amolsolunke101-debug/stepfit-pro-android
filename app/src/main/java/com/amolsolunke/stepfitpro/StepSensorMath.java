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

        long calculated = Math.max(0L, raw - base);
        total = Math.max(total, calculated);
        return new Result(total, base, raw);
    }
}
