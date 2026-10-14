package com.lulan.shincolle.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Issue #63: burst loops are strided by the client particle setting. At 'All' every iteration
 * must still run so the output is identical to before.
 */
class ParticleStepTest {

    private static int iterations(int bound, int setting) {
        int step = ParticleHelper.particleStepFor(setting);
        int n = 0;
        for (int i = 0; i < bound; i += step) n++;
        return n;
    }

    @Test
    void allKeepsEveryIteration() {
        assertEquals(1, ParticleHelper.particleStepFor(0));
        assertEquals(24, iterations(24, 0));
        assertEquals(15, iterations(15, 0));
    }

    @Test
    void decreasedAndMinimalThin() {
        assertEquals(12, iterations(24, 1));
        assertEquals(8, iterations(15, 1));
        assertEquals(6, iterations(24, 2));
        assertEquals(4, iterations(15, 2));
    }

    @Test
    void outOfRangeSettingsAreSafe() {
        assertEquals(1, ParticleHelper.particleStepFor(-1));
        assertEquals(4, ParticleHelper.particleStepFor(3));
    }
}
