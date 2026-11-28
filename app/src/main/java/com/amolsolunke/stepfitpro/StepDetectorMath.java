package com.amolsolunke.stepfitpro;

/** Pure validation and accumulation for TYPE_STEP_DETECTOR events. */
final class StepDetectorMath {
    private StepDetectorMath() {}

    /**
     * Android TYPE_STEP_DETECTOR reports exactly 1.0 for each step event.
     * Reject any other payload rather than treating arbitrary positive values
     * as a valid event.
     */
    static boolean isValidEvent(float detectorValue) {
        return detectorValue == 1.0f;
    }

    static long increment(long currentSteps, float detectorValue) {
        long total = Math.max(0L, currentSteps);
        if (!isValidEvent(detectorValue) || total == Long.MAX_VALUE) {
            return total;
        }
        return total + 1L;
    }
}
