package com.lulan.shincolle.capability;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issues #30 and #45: ship NBT keys are presence-checked on load. A missing key takes its default, a present key
 * (including 0 / false) loads exactly as saved. The ship-level wiring (fallback = current constructor value) needs a
 * live entity and is covered by the in-game check, not here.
 */
class CapaShipSavedValuesLoadDefaultsTest {

    /** every key written by saveNBTData into the Minor section */
    private static final String[] MINOR_KEYS = {
            "Level", "Kills", "Exp", "NumAmmoL", "NumAmmoH", "NumGrudge", "NumAirL", "NumAirH", "FMin", "FMax", "FHP",
            "GuardX", "GuardY", "GuardZ", "GuardDim", "GuardID", "GuardType", "PlayerUID", "ShipUID", "FType", "FPos",
            "Morale", "Food", "Crane", "WpStay", "AutoCR", "Task", "Side"
    };

    /** every boolean key written into the ShipFlags section */
    private static final String[] FLAG_KEYS = {
            "CanFloat", "IsMarried", "NoFuel", "Melee", "AmmoL", "AmmoH", "AirL", "AirH", "WedEffect", "CanDrop",
            "CanFollow", "OnSight", "PVPFirst", "AA", "ASM", "PassiveAI", "TimeKeeper", "PickItem", "HeldItem",
            "AutoPump", "IsMorph"
    };

    @Test
    void explicitDefaultsMatchTheDecision() {
        assertEquals(1, CapaShipSavedValues.DEFAULT_LEVEL);
        assertEquals(60, CapaShipSavedValues.DEFAULT_MORALE);
        assertTrue(CapaShipSavedValues.DEFAULT_CAN_DROP);
    }

    @Test
    void emptyCompoundYieldsFallbacksForEveryKey() {
        NBTTagCompound empty = new NBTTagCompound();
        assertEquals(1, CapaShipSavedValues.defaultedLevel(empty));
        for (String key : MINOR_KEYS) {
            assertEquals(-7, CapaShipSavedValues.intOr(empty, key, -7), key);
        }
        assertEquals(60, CapaShipSavedValues.intOr(empty, "Morale", CapaShipSavedValues.DEFAULT_MORALE));
        assertTrue(CapaShipSavedValues.boolOr(empty, "CanDrop", CapaShipSavedValues.DEFAULT_CAN_DROP));
        for (String key : FLAG_KEYS) {
            assertTrue(CapaShipSavedValues.boolOr(empty, key, true), key);
            assertFalse(CapaShipSavedValues.boolOr(empty, key, false), key);
        }
        assertEquals((byte) 3, CapaShipSavedValues.byteOr(empty, "HP", (byte) 3));
    }

    @Test
    void partialCompoundMixesSavedValuesAndDefaults() {
        NBTTagCompound minor = new NBTTagCompound();
        minor.setInteger("NumAmmoL", 42);
        minor.setInteger("Food", 0);
        assertEquals(42, CapaShipSavedValues.intOr(minor, "NumAmmoL", 5));
        assertEquals(0, CapaShipSavedValues.intOr(minor, "Food", 5), "a saved 0 is not replaced by the fallback");
        assertEquals(5, CapaShipSavedValues.intOr(minor, "NumGrudge", 5));
        assertEquals(1, CapaShipSavedValues.defaultedLevel(minor));
        assertEquals(60, CapaShipSavedValues.intOr(minor, "Morale", CapaShipSavedValues.DEFAULT_MORALE));
    }

    @Test
    void savedFalseCanDropIsNotOverriddenByTheDefault() {
        NBTTagCompound flags = new NBTTagCompound();
        flags.setBoolean("CanDrop", false);
        assertFalse(CapaShipSavedValues.boolOr(flags, "CanDrop", CapaShipSavedValues.DEFAULT_CAN_DROP));
    }

    @Test
    void initAiForcesCanDropOnlyWhenNoSavedValueWasLoaded() {
        assertTrue(CapaShipSavedValues.shouldForceCanDropOnInit(false));
        assertFalse(CapaShipSavedValues.shouldForceCanDropOnInit(true));
    }

    @Test
    void savedMoraleZeroAndLevelAreNeverAltered() {
        NBTTagCompound minor = new NBTTagCompound();
        minor.setInteger("Morale", 0);
        minor.setInteger("Level", 0);
        assertEquals(0, CapaShipSavedValues.intOr(minor, "Morale", CapaShipSavedValues.DEFAULT_MORALE));
        assertEquals(0, CapaShipSavedValues.defaultedLevel(minor));
    }

    @Test
    void fullCompoundRoundTripsEveryKey() {
        NBTTagCompound minor = new NBTTagCompound();
        for (int i = 0; i < MINOR_KEYS.length; i++) minor.setInteger(MINOR_KEYS[i], 100 + i);
        for (int i = 0; i < MINOR_KEYS.length; i++) {
            assertEquals(100 + i, CapaShipSavedValues.intOr(minor, MINOR_KEYS[i], -1), MINOR_KEYS[i]);
        }

        NBTTagCompound flags = new NBTTagCompound();
        for (int i = 0; i < FLAG_KEYS.length; i++) flags.setBoolean(FLAG_KEYS[i], i % 2 == 0);
        for (int i = 0; i < FLAG_KEYS.length; i++) {
            boolean expected = i % 2 == 0;
            // the saved value wins over either fallback
            assertEquals(expected, CapaShipSavedValues.boolOr(flags, FLAG_KEYS[i], true), FLAG_KEYS[i]);
            assertEquals(expected, CapaShipSavedValues.boolOr(flags, FLAG_KEYS[i], false), FLAG_KEYS[i]);
        }

        NBTTagCompound point = new NBTTagCompound();
        point.setByte("HP", (byte) -4);
        assertEquals((byte) -4, CapaShipSavedValues.byteOr(point, "HP", (byte) 9));
    }
}
