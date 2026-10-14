package com.lulan.shincolle.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #28 (ENT-04, P-15): ring requirement follows Check_Ring for mob and boss spawning.
 */
class RingSpawnPredicateTest {

    @Test
    void noCheckAllowsRegardlessOfRing() {
        assertTrue(EntityHelper.canSpawnByRing(false, false));
        assertTrue(EntityHelper.canSpawnByRing(false, true));
    }

    @Test
    void checkRequiresRing() {
        assertFalse(EntityHelper.canSpawnByRing(true, false));
        assertTrue(EntityHelper.canSpawnByRing(true, true));
        assertEquals(true, EntityHelper.canSpawnByRing(true, true));
    }
}
