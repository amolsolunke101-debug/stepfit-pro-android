package com.amolsolunke.stepfitpro;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StepSensorMathTest {
    @Test
    public void acceptsZeroAndFiniteNonNegativeIntegralRawCounterValues() {
        assertTrue(StepSensorMath.isValidRawCounter(0f));
        assertTrue(StepSensorMath.isValidRawCounter(1f));
        assertTrue(StepSensorMath.isValidRawCounter(12_345f));
        assertTrue(StepSensorMath.isValidRawCounter(0x1.fffffep62f));
    }

    @Test
    public void rejectsNegativeFractionalNonFiniteAndOutOfRangeRawCounterValues() {
        assertFalse(StepSensorMath.isValidRawCounter(-1f));
        assertFalse(StepSensorMath.isValidRawCounter(0.5f));
        assertFalse(StepSensorMath.isValidRawCounter(1.25f));
        assertFalse(StepSensorMath.isValidRawCounter(12_345.5f));
        assertFalse(StepSensorMath.isValidRawCounter(Float.NaN));
        assertFalse(StepSensorMath.isValidRawCounter(Float.POSITIVE_INFINITY));
        assertFalse(StepSensorMath.isValidRawCounter(Float.NEGATIVE_INFINITY));
        assertFalse(StepSensorMath.isValidRawCounter(0x1.0p63f));
        assertFalse(StepSensorMath.isValidRawCounter(Float.MAX_VALUE));
    }

    @Test
    public void firstEventEstablishesBaselineWithoutCountingOldSteps() {
        StepSensorMath.Result result = StepSensorMath.update(12_000L, 0L, -1L, -1L);
        assertEquals(0L, result.todaySteps);
        assertEquals(12_000L, result.baseSteps);
        assertEquals(12_000L, result.lastRaw);
    }

    @Test
    public void normalCounterIncreaseAddsOnlyNewSteps() {
        StepSensorMath.Result result = StepSensorMath.update(12_450L, 400L, 12_000L, 12_400L);
        assertEquals(450L, result.todaySteps);
        assertEquals(12_000L, result.baseSteps);
        assertEquals(12_450L, result.lastRaw);
    }

    @Test
    public void sensorResetPreservesAlreadyAccumulatedSteps() {
        StepSensorMath.Result result = StepSensorMath.update(25L, 5_000L, 1_000L, 6_000L);
        assertEquals(5_000L, result.todaySteps);
        assertEquals(-4_975L, result.baseSteps);
        assertEquals(25L, result.lastRaw);
    }

    @Test
    public void countingContinuesAfterCounterReset() {
        StepSensorMath.Result result = StepSensorMath.update(26L, 5_000L, -4_975L, 25L);
        assertEquals(5_001L, result.todaySteps);
        assertEquals(-4_975L, result.baseSteps);
        assertEquals(26L, result.lastRaw);
    }

    @Test
    public void negativeBaselineCannotOverflowTheCalculatedTotal() {
        StepSensorMath.Result result = StepSensorMath.update(
                Long.MAX_VALUE, 1L, -1L, Long.MAX_VALUE - 1L);
        assertEquals(Long.MAX_VALUE, result.todaySteps);
        assertEquals(-1L, result.baseSteps);
        assertEquals(Long.MAX_VALUE, result.lastRaw);
    }
}
