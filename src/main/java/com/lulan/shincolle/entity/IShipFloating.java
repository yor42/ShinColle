package com.lulan.shincolle.entity;


public interface IShipFloating extends IShipFlags {

    /**
     * getter/setter for depth value for floating AI
     */
    double getShipDepth();

    void setShipDepth(double par1);

    /**
     * the min depth for ship floating
     */
    double getShipFloatingDepth();

    void setShipFloatingDepth(double par1);


}