package com.lulan.shincolle.entity;

import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.unitclass.MissileData;
import net.minecraft.entity.Entity;

import java.util.HashMap;

/**
 * SHIP ATTACK BASE
 * include attacker states getter/setter
 */
public interface IShipAttackBase extends IShipNavigator, IShipEmotion, IShipOwner, IShipAttrs {

    /**
     * get attack target
     */
    Entity getEntityTarget();

    void setEntityTarget(Entity target);

    /**
     * get revenge target for next attack target
     */
    Entity getEntityRevengeTarget();

    void setEntityRevengeTarget(Entity target);

    int getEntityRevengeTime();

    void setEntityRevengeTime();

    /**
     * damage type, index: {@link ID.ShipDmgType}
     */
    int getDamageType();

    /**
     * available attack method: light, heavy, air light, air heavy
     */
    boolean getAttackType(int par1);

    int getAmmoLight();                //get ammo info

    void setAmmoLight(int num);

    int getAmmoHeavy();

    void setAmmoHeavy(int num);

    boolean hasAmmoLight();

    boolean hasAmmoHeavy();

    int getLevel();                    //get ship level

    /**
     * skill attack method for skill attack AI
     */
    boolean updateSkillAttack(Entity target);

    /**
     * buffs map, map<buff id, buff level>
     */
    HashMap<Integer, Integer> getBuffMap();

    void setBuffMap(HashMap<Integer, Integer> map);

    /**
     * attack effect map, map<potion id, potion data[ampLevel, ticks, chance(0~100)]>
     */
    HashMap<Integer, int[]> getAttackEffectMap();

    void setAttackEffectMap(HashMap<Integer, int[]> map);

    /**
     * missile data
     * type: 0:melee, 1:light, 2:heavy, 3:air-light, 4:air-heavy
     */
    MissileData getMissileData(int type);

    void setMissileData(int type, MissileData data);


}