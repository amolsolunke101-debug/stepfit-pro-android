package com.amolsolunke.stepfitpro;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StepDetectorMathTest {
    @Test
    public void validDetectorEventAddsExactlyOneStep() {
        assertTrue(StepDetectorMath.isValidEvent(1f));
        assertEquals(101L, StepDetectorMath.increment(100L, 1f));
    }

    @Test
    public void zeroAndNegativeEventsAreIgnored() {
        assertFalse(StepDetectorMath.isValidEvent(0f));
        assertFalse(StepDetectorMath.isValidEvent(-1f));
        assertEquals(100L, StepDetectorMath.increment(100L, 0f));
        assertEquals(100L, StepDetectorMath.increment(100L, -1f));
    }

    @Test
    public void nonFiniteEventsAreIgnored() {
        assertFalse(StepDetectorMath.isValidEvent(Float.NaN));
        assertFalse(StepDetectorMath.isValidEvent(Float.POSITIVE_INFINITY));
        assertFalse(StepDetectorMath.isValidEvent(Float.NEGATIVE_INFINITY));
        assertEquals(100L, StepDetectorMath.increment(100L, Float.NaN));
        assertEquals(100L, StepDetectorMath.increment(100L, Float.POSITIVE_INFINITY));
    }

    @Test
    public void negativeStoredTotalIsNormalizedBeforeIncrement() {
        assertEquals(1L, StepDetectorMath.increment(-50L, 1f));
    }

    @Test
    public void totalSaturatesInsteadOfOverflowing() {
        assertEquals(Long.MAX_VALUE, StepDetectorMath.increment(Long.MAX_VALUE, 1f));
    }
}
