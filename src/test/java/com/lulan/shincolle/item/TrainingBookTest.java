package com.lulan.shincolle.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Issue #39: Training Book level arithmetic uses the ship level cap (100 unmarried, 150 married).
 */
class TrainingBookTest {

    @Test
    void addsFivePlusRollBelowCap() {
        assertEquals(15, TrainingBook.calcTrainedLevel(10, 100, 0));
        assertEquals(20, TrainingBook.calcTrainedLevel(10, 100, 5));
    }

    @Test
    void unmarriedShipIsClampedTo100() {
        assertEquals(100, TrainingBook.calcTrainedLevel(98, 100, 5));
        assertEquals(100, TrainingBook.calcTrainedLevel(95, 100, 0));
    }

    @Test
    void marriedShipIsClampedTo150() {
        assertEquals(150, TrainingBook.calcTrainedLevel(148, 150, 3));
        // levels above 100 are only reachable with the married cap
        assertEquals(120, TrainingBook.calcTrainedLevel(110, 150, 5));
    }
}
