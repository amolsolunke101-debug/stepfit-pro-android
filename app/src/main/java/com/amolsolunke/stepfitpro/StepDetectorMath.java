package com.amolsolunke.stepfitpro;

/** Pure validation and accumulation for TYPE_STEP_DETECTOR events. */
final class StepDetectorMath {
    private StepDetectorMath() {}

    static boolean isValidEvent(float detectorValue) {
        return detectorValue > 0f
                && !Float.isNaN(detectorValue)
                && !Float.isInfinite(detectorValue);
    }

    static long increment(long currentSteps, float detectorValue) {
        long total = Math.max(0L, currentSteps);
        if (!isValidEvent(detectorValue) || total == Long.MAX_VALUE) {
            return total;
        }
        return total + 1L;
    }
}
