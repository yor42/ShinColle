package com.lulan.shincolle.entity;

import net.minecraft.entity.player.EntityPlayer;

/**
 * inter-mod support interface for Metamorph
 */
public interface IShipMorph {


    /**
     * entity is morph
     */
    boolean isMorph();

    void setIsMorph(boolean par1);

    /**
     * morph host
     */
    EntityPlayer getMorphHost();

    void setMorphHost(EntityPlayer player);


}