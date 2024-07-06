package com.lulan.shincolle.item;

/**
 * ship food item
 */
public interface IShipFoodItem {


    /**
     * get resource value
     */
    float getFoodValue(int meta);

    /**
     * get saturation value
     */
    float getSaturationValue(int meta);

    /**
     * get special effect
     */
    int getSpecialEffect(int meta);


}