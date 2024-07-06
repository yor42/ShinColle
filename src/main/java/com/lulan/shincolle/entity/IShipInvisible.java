package com.lulan.shincolle.entity;

/**
 * for submarine or invisible entity
 * <p>
 * Invisible LV:
 * calc by distance, high level = high dodge rate
 * entity get dodge chance from invisible level
 * <p>
 * level = 0% when attack within 6 blocks
 */
public interface IShipInvisible {

    /**
     * invisible level
     */
    float getInvisibleLevel();

    void setInvisibleLevel(float level);


}