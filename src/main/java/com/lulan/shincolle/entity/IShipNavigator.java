package com.lulan.shincolle.entity;

import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathNavigate;


/**
 * Path navigator for ships
 */
public interface IShipNavigator {

    /**
     * ship navigator
     */
    ShipPathNavigate getShipNavigate();

    /**
     * ship move helper
     */
    ShipMoveHelper getShipMoveHelper();

    /**
     * can entity fly flag
     */
    boolean canFly();

    /**
     * entity is jumping
     */
    boolean isJumping();

    /**
     * move speed
     */
    float getMoveSpeed();

    /**
     * jump strength
     */
    float getJumpSpeed();


}