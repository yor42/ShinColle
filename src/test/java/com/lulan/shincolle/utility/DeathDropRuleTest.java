package com.lulan.shincolle.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #13 (ENT-02): a ship allowed to drop an egg must drop it when the death animation completes,
 * including Death_Time=0, where deathTime is already 1 on the first onDeathUpdate call.
 */
class DeathDropRuleTest {

    @Test
    void zeroDeathTimeCompletesOnFirstDeathTick() {
        assertTrue(DeathDropRule.isDeathComplete(1, 0));
        assertTrue(DeathDropRule.shouldDropEgg(1, 0, true));
    }

    @Test
    void defaultDeathTimeDropsOnlyAtEndOfAnimation() {
        assertFalse(DeathDropRule.shouldDropEgg(1, 400, true));
        assertFalse(DeathDropRule.shouldDropEgg(399, 400, true));
        assertTrue(DeathDropRule.shouldDropEgg(400, 400, true));
    }

    @Test
    void noDropWithoutCanDrop() {
        assertFalse(DeathDropRule.shouldDropEgg(1, 0, false));
        assertFalse(DeathDropRule.shouldDropEgg(400, 400, false));
    }
}
