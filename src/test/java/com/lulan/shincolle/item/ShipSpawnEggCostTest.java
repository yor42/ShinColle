package com.lulan.shincolle.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issues #18 and #17: egg XP cost with a missing Attrs array, and consume-after-spawn decisions.
 */
class ShipSpawnEggCostTest {

    // ---- #18: costLevel ----

    @Test
    void missingAttrsArrayCostsZero() {
        assertEquals(0, ShipSpawnEgg.costLevel(new int[0]));
        assertEquals(0, ShipSpawnEgg.costLevel(null));
    }

    @Test
    void attrsFirstElementIsDividedByThree() {
        assertEquals(3, ShipSpawnEgg.costLevel(new int[]{9, 1, 1, 1, 1, 1, 1}));
        assertEquals(0, ShipSpawnEgg.costLevel(new int[]{2}));
        assertEquals(33, ShipSpawnEgg.costLevel(new int[]{100, 0, 0, 0, 0, 0, 0}));
    }

    // ---- #17: nothing is spent unless the ship actually spawned ----

    @Test
    void failedSpawnConsumesNothing() {
        assertFalse(ShipSpawnEgg.shouldConsume(false, false));
        assertEquals(0, ShipSpawnEgg.levelsToCharge(false, false, 5));
    }

    @Test
    void successfulSpawnConsumesEggAndCost() {
        assertTrue(ShipSpawnEgg.shouldConsume(false, true));
        assertEquals(5, ShipSpawnEgg.levelsToCharge(false, true, 5));
    }

    @Test
    void creativeModeConsumesNothing() {
        assertFalse(ShipSpawnEgg.shouldConsume(true, true));
        assertEquals(0, ShipSpawnEgg.levelsToCharge(true, true, 5));
    }
}
