package com.lulan.shincolle.entity;

/**
 * STATE getter/setter
 */
public interface IShipFlags {

    /**
     * minor states like level, kills, ammo...
     */
    int getStateMinor(int id);

    void setStateMinor(int state, int par1);

    /**
     * state flags
     */
    boolean getStateFlag(int flag);

    void setStateFlag(int id, boolean flag);

    /**
     * update flag
     */
    void setUpdateFlag(int id, boolean value);

    boolean getUpdateFlag(int id);


}