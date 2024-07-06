package com.lulan.shincolle.entity;

import net.minecraft.entity.Entity;

/**
 * USE AIRFORCE TO ATTACK
 * include aircraft attack method, airplane getter/setter
 */
public interface IShipAircraftAttack extends IShipAttackBase {

    /**
     * get amount of airplane
     */
    int getNumAircraftLight();

    /**
     * set amount of airplane
     */
    void setNumAircraftLight(int par1);

    int getNumAircraftHeavy();

    void setNumAircraftHeavy(int par1);

    /**
     * check has airplane
     */
    boolean hasAirLight();

    boolean hasAirHeavy();

    /**
     * airplane attack
     */
    boolean attackEntityWithAircraft(Entity target);

    boolean attackEntityWithHeavyAircraft(Entity target);


}