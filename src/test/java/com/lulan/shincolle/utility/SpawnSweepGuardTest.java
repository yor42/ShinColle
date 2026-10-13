package com.lulan.shincolle.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #10 (ENT-13): the vanilla spawn-setting sweep must not delete friendly ships, but must still
 * remove every other animal, water mob and NPC.
 */
class SpawnSweepGuardTest {

    static class Animal {
    }

    static class Ship extends Animal {
    }

    static class SubShip extends Ship {
    }

    static class Cow extends Animal {
    }

    @Test
    void shipIsExempt() {
        assertTrue(SpawnSweepGuard.isExempt(Ship.class, Ship.class));
    }

    @Test
    void shipSubclassIsExempt() {
        assertTrue(SpawnSweepGuard.isExempt(SubShip.class, Ship.class));
    }

    @Test
    void otherAnimalIsNotExempt() {
        assertFalse(SpawnSweepGuard.isExempt(Cow.class, Ship.class));
    }

    @Test
    void shipBaseItselfIsNotExemptWhenEntityIsItsSuperclass() {
        assertFalse(SpawnSweepGuard.isExempt(Animal.class, Ship.class));
    }

    @Test
    void nullInputsAreNotExempt() {
        assertFalse(SpawnSweepGuard.isExempt(null, Ship.class));
        assertFalse(SpawnSweepGuard.isExempt(Ship.class, null));
    }
}
