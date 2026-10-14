package com.lulan.shincolle.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Issue #6 (AI-03): depth must follow the 1.10.2 formula. N liquid blocks in the
 * column give N - frac(posY); a column without liquid gives no liquid blocks.
 */
class EntityHelperDepthTest {

    /** column: true = liquid, index 0 is the block at the entity's feet */
    private static int run(boolean... column) {
        return EntityHelper.liquidRunLength(dy -> dy < column.length && column[dy], 64);
    }

    @Test
    void currentBlockIsCountedOnce() {
        assertEquals(1, run(true, false, false));
    }

    @Test
    void nLiquidBlocksGiveRunLengthN() {
        assertEquals(3, run(true, true, true, false, true));
    }

    @Test
    void airAtFeetGivesZero() {
        assertEquals(0, run(false, true, true));
    }

    @Test
    void depthIsBlocksMinusFraction() {
        assertEquals(3 - 0.25, EntityHelper.calcDepth(3, 62.25), 1e-9);
        assertEquals(1.0, EntityHelper.calcDepth(1, 62.0), 1e-9);
    }

    // (#60) checkDepth now uses an inline loop with the same rule; these pin that rule's bounds.
    @Test
    void scanIsCappedAtMaxOffsetInclusive() {
        assertEquals(4, EntityHelper.liquidRunLength(dy -> true, 3));
        assertEquals(1, EntityHelper.liquidRunLength(dy -> true, 0));
    }

    @Test
    void scanStopsAtFirstGapEvenIfLiquidFollows() {
        assertEquals(2, run(true, true, false, true, true));
    }

    @Test
    void negativeMaxOffsetScansNothing() {
        assertEquals(0, EntityHelper.liquidRunLength(dy -> true, -1));
    }

    @Test
    void depthIsZeroWithoutLiquid() {
        assertEquals(0.0, EntityHelper.calcDepth(0, 62.0), 1e-9);
    }
}
