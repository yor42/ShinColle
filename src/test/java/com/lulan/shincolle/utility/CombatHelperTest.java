package com.lulan.shincolle.utility;

import org.junit.jupiter.api.Test;

import static com.lulan.shincolle.utility.CombatHelper.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * #21 (CMB-01): triple hit must be reachable; #23: a negative dodge limit means "no limit".
 * #22: coef 1 = day table, coef 0 = night table (tables injected; Values cannot be loaded in unit tests).
 */
class CombatHelperTest {

    // individual rates: miss .1, cri .1, dhit .1, thit .1 -> cumulative thresholds .1 / .2 / .3 / .4
    private static int roll(float roll, boolean multi) {
        return rollOutcome(roll, 0.1F, 0.1F, 0.1F, 0.1F, multi);
    }

    @Test
    void bandsInPriorityOrder() {
        assertEquals(HIT_MISS, roll(0.05F, true));
        assertEquals(HIT_CRI, roll(0.15F, true));
        assertEquals(HIT_DOUBLE, roll(0.25F, true));
        assertEquals(HIT_TRIPLE, roll(0.35F, true));
        assertEquals(HIT_NORMAL, roll(0.45F, true));
    }

    @Test
    void multiHitDisabledSkipsDoubleAndTriple() {
        assertEquals(HIT_MISS, roll(0.05F, false));
        assertEquals(HIT_CRI, roll(0.15F, false));
        assertEquals(HIT_NORMAL, roll(0.25F, false));
        assertEquals(HIT_NORMAL, roll(0.35F, false));
    }

    @Test
    void thresholdsAreInclusive() {
        assertEquals(HIT_MISS, roll(0.1F, true));
        assertEquals(HIT_TRIPLE, rollOutcome(0.4F, 0.1F, 0.1F, 0.1F, 0.1F, true));
    }

    @Test
    void negativeRatesDoNotLowerThresholds() {
        // cri/dhit/thit below zero are floored at the previous band
        assertEquals(HIT_MISS, rollOutcome(0.1F, 0.1F, -1F, -1F, -1F, true));
        assertEquals(HIT_NORMAL, rollOutcome(0.11F, 0.1F, -1F, -1F, -1F, true));
    }

    @Test
    void multipliersAndParticleTypes() {
        assertEquals(0F, outcomeMultiplier(HIT_MISS), 0F);
        assertEquals(1.5F, outcomeMultiplier(HIT_CRI), 0F);
        assertEquals(2F, outcomeMultiplier(HIT_DOUBLE), 0F);
        assertEquals(3F, outcomeMultiplier(HIT_TRIPLE), 0F);
        assertEquals(1F, outcomeMultiplier(HIT_NORMAL), 0F);
        assertEquals(3, HIT_TRIPLE);
    }

    @Test
    void negativeDodgeLimitMeansNoLimit() {
        assertEquals(0.5F, limitDodge(0.5F, -1D), 0F);
        assertEquals(1.2F, limitDodge(1.2F, -1D), 0F);
    }

    @Test
    void nonNegativeDodgeLimitClamps() {
        assertEquals(0.75F, limitDodge(0.9F, 0.75D), 0F);
        assertEquals(0.5F, limitDodge(0.5F, 0.75D), 0F);
        assertEquals(0F, limitDodge(0.3F, 0D), 0F);
    }

    private static final float[][] DAY = {{2F, 1F}, {0.5F, 4F}};
    private static final float[][] NIGHT = {{1F, 0.5F}, {0.25F, 2F}};

    @Test
    void lightCoefOneIsDayAndZeroIsNight() {
        assertEquals(2F, modDamageByLight(1F, 1, 1, 1F, DAY, NIGHT), 1.0E-6F);
        assertEquals(1F, modDamageByLight(1F, 1, 1, 0F, DAY, NIGHT), 1.0E-6F);
        assertEquals(8F, modDamageByLight(2F, 2, 2, 1F, DAY, NIGHT), 1.0E-6F);
    }

    @Test
    void lightCoefHalfIsMidpoint() {
        assertEquals(1.5F, modDamageByLight(1F, 1, 1, 0.5F, DAY, NIGHT), 1.0E-6F);
        assertEquals(0.75F, modDamageByLight(1F, 1, 2, 0.5F, DAY, NIGHT), 1.0E-6F);
    }

    @Test
    void lightCoefIsClamped() {
        assertEquals(1F, modDamageByLight(1F, 1, 1, -3F, DAY, NIGHT), 1.0E-6F);
        assertEquals(2F, modDamageByLight(1F, 1, 1, 5F, DAY, NIGHT), 1.0E-6F);
    }

    @Test
    void undefinedTypeReturnsDamageUnchanged() {
        assertEquals(7F, modDamageByLight(7F, 0, 1, 1F, DAY, NIGHT), 0F);
        assertEquals(7F, modDamageByLight(7F, 1, 0, 0F, DAY, NIGHT), 0F);
        assertEquals(7F, modDamageByLight(7F, -1, 1, 0.5F, DAY, NIGHT), 0F);
    }
}
