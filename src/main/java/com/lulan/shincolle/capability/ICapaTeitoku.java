package com.lulan.shincolle.capability;

import net.minecraft.nbt.NBTTagCompound;

public interface ICapaTeitoku {

    //save data to nbt
    NBTTagCompound saveNBTData(NBTTagCompound nbt);

    //load data from nbt
    void loadNBTData(NBTTagCompound nbt);

}
