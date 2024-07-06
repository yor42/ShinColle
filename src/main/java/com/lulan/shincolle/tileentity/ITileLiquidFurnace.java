package com.lulan.shincolle.tileentity;

/**
 * Fluid Fuel Methods for tile entity
 */
public interface ITileLiquidFurnace extends ITileFurnace {


    /**
     * get fuel amount
     */
    int getFluidFuelAmount();

    /**
     * consume fuel, return consume amount
     */
    int consumeFluidFuel(int amount);


}