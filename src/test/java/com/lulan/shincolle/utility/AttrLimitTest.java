package com.lulan.shincolle.utility;

import org.junit.jupiter.api.Test;

import static com.lulan.shincolle.utility.AttrLimit.limitAttr;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * #78: limitShipAttrs -1 (any negative) means "no limit" at every clamp site.
 */
class AttrLimitTest {

    @Test
    void negativeLimitLeavesValueUnclamped() {
        assertEquals(5F, limitAttr(5F, -1D));
        assertEquals(0F, limitAttr(0F, -1D));
        assertEquals(1000000F, limitAttr(1000000F, -1D));
    }

    @Test
    void positiveLimitClampsDownOnly() {
        assertEquals(2F, limitAttr(5F, 2D));
        assertEquals(1F, limitAttr(1F, 2D));
        assertEquals(2F, limitAttr(2F, 2D));
    }

    @Test
    void zeroLimitIsARealCap() {
        assertEquals(0F, limitAttr(5F, 0D));
        assertEquals(0F, limitAttr(0F, 0D));
    }

    @Test
    void negativeValuesPassThrough() {
        assertEquals(-3F, limitAttr(-3F, 2D));
        assertEquals(-3F, limitAttr(-3F, -1D));
    }

    @Test
    void movSiteSequenceWithUnlimitedConfig() {
        // server MOV site: clamp, then floor at 0. With -1 the old code returned -1 here.
        float mov = limitAttr(1.2F, -1D);
        if (mov < 0F) mov = 0F;
        assertEquals(1.2F, mov);
    }

    @Test
    void behavesLikeLimitDodge() {
        for (float v : new float[]{0F, 0.5F, 3F}) {
            for (double l : new double[]{-1D, 0D, 1D}) {
                assertEquals(CombatHelper.limitDodge(v, l), limitAttr(v, l));
            }
        }
    }
}
