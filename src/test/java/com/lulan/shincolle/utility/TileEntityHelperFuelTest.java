package com.lulan.shincolle.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Issue #50: fuel magnification must be applied as float (matches the liquid path and 1.10).
 */
class TileEntityHelperFuelTest {

    @Test
    void multiplierOneIsIdentity() {
        assertEquals(1600, TileEntityHelper.applyFuelMagni(1600, 1F));
        assertEquals(20000, TileEntityHelper.applyFuelMagni(20000, 1F));
    }

    @Test
    void multiplierHalfGivesHalf() {
        assertEquals(800, TileEntityHelper.applyFuelMagni(1600, 0.5F));
    }

    @Test
    void multiplierOneAndHalfGivesOneAndHalf() {
        assertEquals(2400, TileEntityHelper.applyFuelMagni(1600, 1.5F));
    }

    @Test
    void integerMultipliersUnchanged() {
        assertEquals(3200, TileEntityHelper.applyFuelMagni(1600, 2F));
        assertEquals(4800, TileEntityHelper.applyFuelMagni(1600, 3F));
    }
}
