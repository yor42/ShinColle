package com.lulan.shincolle.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #57: the inventory has-space result is reused for at most 16 ticks.
 */
class EntityAIShipPickItemTest {

    @Test
    void firstCheckAlwaysRuns() {
        assertTrue(EntityAIShipPickItem.needSpaceRecheck(false, 0, 0));
    }

    @Test
    void reusedUntilSixteenTicksPass() {
        assertFalse(EntityAIShipPickItem.needSpaceRecheck(true, 100, 100));
        assertFalse(EntityAIShipPickItem.needSpaceRecheck(true, 100, 115));
        assertTrue(EntityAIShipPickItem.needSpaceRecheck(true, 100, 116));
        assertTrue(EntityAIShipPickItem.needSpaceRecheck(true, 100, 500));
    }

    @Test
    void clockGoingBackwardsRechecks() {
        assertTrue(EntityAIShipPickItem.needSpaceRecheck(true, 100, 3));
    }
}
