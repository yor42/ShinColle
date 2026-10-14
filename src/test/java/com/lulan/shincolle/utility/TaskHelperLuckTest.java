package com.lulan.shincolle.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Issue #20 (ATTR-08): fishing luck bonus from ship level must use floating point division.
 */
class TaskHelperLuckTest {

    @Test
    void halfLevelGivesHalfOfMaxBonus() {
        assertEquals(0.75F, TaskHelper.levelLuckBonus(75, 150), 1.0E-6F);
    }

    @Test
    void maxLevelGivesFullBonus() {
        assertEquals(1.5F, TaskHelper.levelLuckBonus(150, 150), 1.0E-6F);
    }

    @Test
    void levelOneGivesAboutOneHundredth() {
        assertEquals(0.01F, TaskHelper.levelLuckBonus(1, 150), 1.0E-6F);
    }

    @Test
    void levelZeroGivesNothing() {
        assertEquals(0F, TaskHelper.levelLuckBonus(0, 150), 0F);
    }
}
