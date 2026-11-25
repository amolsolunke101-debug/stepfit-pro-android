package com.amolsolunke.stepfitpro;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class StepSensorMathTest {
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
