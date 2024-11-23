package com.lulan.shincolle.capability;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.energy.EnergyStorage;

public class CapaForgeEnergyStorage extends EnergyStorage {

    private int CraneTransferRate;

    public CapaForgeEnergyStorage(int capacity, int transferRate)
    {
        super(capacity, 2000, 2000);
        this.CraneTransferRate = 0;
    }

    public CapaForgeEnergyStorage readFromNBT(NBTTagCompound nbt)
    {
        this.capacity = nbt.getInteger("fe_capacity");
        this.energy = nbt.getInteger("fe_energy");
        this.maxExtract = nbt.getInteger("fe_maxExtract");
        this.maxReceive = nbt.getInteger("fe_maxReceive");
        return this;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        nbt.setInteger("fe_capacity", this.capacity);
        nbt.setInteger("fe_energy", this.energy);
        nbt.setInteger("fe_maxExtract", this.maxExtract);
        nbt.setInteger("fe_maxReceive", this.maxReceive);
        return nbt;
    }

    public void setMaxShipTransfer(int value){
        this.CraneTransferRate = value;
    }

    public int receiveEnergyShip(int maxReceive, boolean simulate)
    {
        if (!canTransferShip())
            return 0;

        int energyReceived = Math.min(getRemainingStorage(), Math.min(getMaxCraneTransferRate(), maxReceive));
        if (!simulate)
            energy += energyReceived;
        return energyReceived;
    }

    public int extractEnergyShip(int maxExtract, boolean simulate)
    {
        if (!canTransferShip())
            return 0;

        int energyExtracted = Math.min(energy, Math.min(this.getMaxCraneTransferRate(), maxExtract));
        if (!simulate)
            energy -= energyExtracted;
        return energyExtracted;
    }

    public int getMaxCraneTransferRate() {
        return CraneTransferRate;
    }

    public int getRemainingStorage(){
        return this.getMaxEnergyStored()-this.getEnergyStored();
    }

    public boolean canTransferShip(){
        return this.getMaxCraneTransferRate()>0;
    }
}
