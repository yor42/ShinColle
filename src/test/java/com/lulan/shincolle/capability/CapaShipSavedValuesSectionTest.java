package com.lulan.shincolle.capability;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Issues #11 and #7: ship NBT loading must treat ShipExtProps and each of its sections as optional.
 * getSection is the guard used by loadNBTData; a null result means "keep the defaults".
 */
class CapaShipSavedValuesSectionTest {

    @Test
    void missingSectionIsNull() {
        assertNull(CapaShipSavedValues.getSection(new NBTTagCompound(), CapaShipSavedValues.SHIP_EXTPROP_NAME));
    }

    @Test
    void nullParentIsNull() {
        assertNull(CapaShipSavedValues.getSection(null, "Minor"));
    }

    @Test
    void emptyStubSectionsAreReturnedNotNull() {
        NBTTagCompound ext = new NBTTagCompound();
        ext.setTag("Minor", new NBTTagCompound());
        assertNotNull(CapaShipSavedValues.getSection(ext, "Minor"));
        assertNull(CapaShipSavedValues.getSection(ext, "Display"));
    }

    @Test
    void presentSectionKeepsItsData() {
        NBTTagCompound minor = new NBTTagCompound();
        minor.setInteger("Level", 7);
        NBTTagCompound ext = new NBTTagCompound();
        ext.setTag("Minor", minor);
        assertEquals(7, CapaShipSavedValues.getSection(ext, "Minor").getInteger("Level"));
    }

    @Test
    void wrongTypedSectionIsNullNotClassCast() {
        NBTTagCompound ext = new NBTTagCompound();
        ext.setTag("Minor", new NBTTagString("oops"));
        assertNull(CapaShipSavedValues.getSection(ext, "Minor"));
    }

    @Test
    void missingLevelDefaultsToOne() {
        assertEquals(1, CapaShipSavedValues.defaultedLevel(new NBTTagCompound()));
    }

    @Test
    void savedLevelIsNeverAltered() {
        NBTTagCompound minor = new NBTTagCompound();
        minor.setInteger("Level", 0);
        assertEquals(0, CapaShipSavedValues.defaultedLevel(minor));
        minor.setInteger("Level", 99);
        assertEquals(99, CapaShipSavedValues.defaultedLevel(minor));
    }
}
