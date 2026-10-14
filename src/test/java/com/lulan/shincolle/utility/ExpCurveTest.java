package com.lulan.shincolle.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Issue #19 (ATTR-07): exp needed for the next level is (level + 1) * expMod, for every expMod.
 * Expected values are literals from the documented formula "level * expMod + expMod".
 */
class ExpCurveTest {

    @Test
    void defaultExpModLevelOneIsFortyAsInitialValue() {
        assertEquals(40, ExpCurve.expNext(1, 20));
    }

    @Test
    void levelFifteenWithDefaultExpModMatchesConfigComment() {
        // ConfigHandler comment: "lv 15 exp req = 15*20+20"
        assertEquals(320, ExpCurve.expNext(15, 20));
    }

    @Test
    void customExpModScales() {
        assertEquals(20, ExpCurve.expNext(1, 10));
        assertEquals(1010, ExpCurve.expNext(100, 10));
        assertEquals(2 * 10000, ExpCurve.expNext(1, 10000));
    }
}
