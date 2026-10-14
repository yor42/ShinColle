package com.lulan.shincolle.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #5 (ENT-08): owner sync radius is 64 blocks; getDistanceSq returns a squared distance.
 */
class OwnerSyncRadiusTest {

    @Test
    void ownerAt20BlocksIsInRange() {
        assertTrue(EntityHelper.isWithinRadius(20D * 20D, EntityHelper.OWNER_SYNC_RADIUS, false));
        assertTrue(EntityHelper.isWithinRadius(20D * 20D, EntityHelper.OWNER_SYNC_RADIUS, true));
    }

    @Test
    void ownerAt63BlocksIsInRangeAt65IsNot() {
        assertTrue(EntityHelper.isWithinRadius(63D * 63D, EntityHelper.OWNER_SYNC_RADIUS, false));
        assertFalse(EntityHelper.isWithinRadius(65D * 65D, EntityHelper.OWNER_SYNC_RADIUS, false));
    }

    @Test
    void boundaryAt64BlocksFollowsInclusiveFlag() {
        assertFalse(EntityHelper.isWithinRadius(4096D, EntityHelper.OWNER_SYNC_RADIUS, false));
        assertTrue(EntityHelper.isWithinRadius(4096D, EntityHelper.OWNER_SYNC_RADIUS, true));
    }
}
