package com.lulan.shincolle.entity;

import net.minecraft.entity.Entity;

/**
 * CANNON ATTACK METHOD
 */
public interface IShipCannonAttack extends IShipAttackBase {

    /**
     * Use light ammo to attack
     */
    boolean attackEntityWithAmmo(Entity target);

    /**
     * Use heavy ammo to attack
     */
    boolean attackEntityWithHeavyAmmo(Entity target);

    /**
     * Use ammo type
     */
    boolean useAmmoLight();

    boolean useAmmoHeavy();


}