package com.amolsolunke.stepfitpro;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class StepCountMathTest {
    @Test
    public void readsLegacyIntegerAndLongPreferences() {
        assertEquals(123L, StepCountMath.nonNegativeLong(Integer.valueOf(123)));
        assertEquals((long) Integer.MAX_VALUE + 500L,
                StepCountMath.nonNegativeLong(Long.valueOf((long) Integer.MAX_VALUE + 500L)));
    }

    @Test
    public void treatsMissingOrNegativeValuesAsZero() {
        assertEquals(0L, StepCountMath.nonNegativeLong(null));
        assertEquals(0L, StepCountMath.nonNegativeLong(Long.valueOf(-1L)));
        assertEquals(0L, StepCountMath.nonNegativeLong("invalid"));
    }

    @Test
    public void saturatesWeeklyTotalInsteadOfOverflowing() {
        assertEquals(Long.MAX_VALUE,
                StepCountMath.saturatingAdd(Long.MAX_VALUE - 2L, 10L));
        assertEquals(9L, StepCountMath.saturatingAdd(4L, 5L));
    }

    @Test
    public void calculatesGoalProgressWithoutIntegerOverflow() {
        assertEquals(50, StepCountMath.goalPercent(4_500L, 9_000));
        assertEquals(100, StepCountMath.goalPercent(Long.MAX_VALUE, 9_000));
        assertEquals(0, StepCountMath.goalPercent(500L, 0));
    }
}
