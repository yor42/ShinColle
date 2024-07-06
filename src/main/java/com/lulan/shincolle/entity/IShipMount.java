package com.lulan.shincolle.entity;

/**
 * for mount type entity
 */
public interface IShipMount extends IShipFloating {

    /**
     * rider position: ship rider
     */
    float[] getSeatPos();

    void setSeatPos(float[] pos);

    /**
     * rider position: other entity rider
     */
    float[] getSeatPos2();

    void setSeatPos2(float[] pos);


}
