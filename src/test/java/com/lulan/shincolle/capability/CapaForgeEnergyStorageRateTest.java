package com.lulan.shincolle.capability;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * #76: the constructor transferRate (Tile_Crane[4], "FE transfer rate to other machines") sets the external
 * maxReceive/maxExtract. The ship-side CraneTransferRate stays 0 until the drums set it.
 */
class CapaForgeEnergyStorageRateTest {

    @Test
    void constructorRateSetsExternalReceiveAndExtract() {
        CapaForgeEnergyStorage s = new CapaForgeEnergyStorage(100000, 500);
        assertEquals(500, s.receiveEnergy(5000, false));
        assertEquals(500, s.extractEnergy(5000, true));
    }

    @Test
    void defaultRateBehavesAsBefore() {
        CapaForgeEnergyStorage s = new CapaForgeEnergyStorage(100000, 2000);
        assertEquals(2000, s.receiveEnergy(5000, true));
    }

    @Test
    void shipSideRateIsStillIndependentOfConstructorRate() {
        CapaForgeEnergyStorage s = new CapaForgeEnergyStorage(100000, 500);
        assertEquals(0, s.getMaxCraneTransferRate());
        assertFalse(s.canTransferShip());
        s.setMaxShipTransfer(64);
        assertEquals(64, s.getMaxCraneTransferRate());
    }

    @Test
    void savedRatesOverrideConstructorRateOnLoad() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("fe_maxReceive", 100);
        nbt.setInteger("fe_maxExtract", 200);
        CapaForgeEnergyStorage s = new CapaForgeEnergyStorage(100000, 500).readFromNBT(nbt);
        assertEquals(100, s.receiveEnergy(5000, true));
        s.receiveEnergy(300, false); // limited to 100 by the saved maxReceive
        s.receiveEnergy(300, false);
        s.receiveEnergy(300, false); // 300 stored
        assertEquals(200, s.extractEnergy(5000, true)); // saved maxExtract 200, below constructor 500
    }

    @Test
    void missingKeysKeepConstructorRate() {
        CapaForgeEnergyStorage s = new CapaForgeEnergyStorage(100000, 500).readFromNBT(new NBTTagCompound());
        assertEquals(500, s.receiveEnergy(5000, true));
    }
}
