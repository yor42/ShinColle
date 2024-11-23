package com.lulan.shincolle.capability;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

public class CapaEnergyStorage extends EnergyStorage {

    public CapaEnergyStorage(int capacity)
    {
        super(capacity);
    }

    public CapaEnergyStorage readFromNBT(NBTTagCompound nbt)
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

    public int getMaxExtract(){
        return this.maxExtract;
    }

    public int getMaxReceive(){
        return this.maxReceive;
    }

    public void setMaxTransfer(int value){
        this.maxExtract = value;
        this.maxReceive = value;
    }
}
