package com.lulan.shincolle.tileentity;

import com.lulan.shincolle.capability.CapaForgeEnergyStorage;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Issue #14: a Crane saved before the energy keys existed must keep config-default EU and FE capacity
 * instead of loading 0. Saved key set is unchanged.
 */
class CraneLegacyNbtLoadTest {

    @Test
    void feStorageWithoutKeysKeepsConstructorCapacityAndRates() {
        CapaForgeEnergyStorage s = new CapaForgeEnergyStorage(640000000, 2000);
        s.readFromNBT(new NBTTagCompound());
        assertEquals(640000000, s.getMaxEnergyStored());
        assertEquals(0, s.getEnergyStored());
        assertEquals(2000, s.receiveEnergy(5000, false));
        assertEquals(2000, s.extractEnergy(5000, true));
    }

    @Test
    void feStorageWithKeysRestoresSavedValues() {
        CapaForgeEnergyStorage src = new CapaForgeEnergyStorage(1000, 2000);
        src.receiveEnergy(300, false);
        NBTTagCompound nbt = src.writeToNBT(new NBTTagCompound());
        CapaForgeEnergyStorage dst = new CapaForgeEnergyStorage(640000000, 2000);
        dst.readFromNBT(nbt);
        assertEquals(1000, dst.getMaxEnergyStored());
        assertEquals(300, dst.getEnergyStored());
    }

    @Test
    void euWithoutKeysKeepsDefaultCapacityAndZeroEnergy() {
        NBTTagCompound nbt = new NBTTagCompound();
        assertEquals(160000000D, CraneEnergyNbt.loadEUCapacity(nbt, 160000000D));
        assertEquals(0D, CraneEnergyNbt.loadEUEnergy(nbt, 160000000D));
    }

    @Test
    void euWithKeysRestoresSavedValues() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setDouble("energy_EUCapacity", 5000D);
        nbt.setDouble("energy_EUEnergy", 1234D);
        assertEquals(5000D, CraneEnergyNbt.loadEUCapacity(nbt, 160000000D));
        assertEquals(1234D, CraneEnergyNbt.loadEUEnergy(nbt, 5000D));
    }

    @Test
    void euEnergyIsClampedToCapacity() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setDouble("energy_EUEnergy", 9999D);
        assertEquals(100D, CraneEnergyNbt.loadEUEnergy(nbt, 100D));
    }
}
