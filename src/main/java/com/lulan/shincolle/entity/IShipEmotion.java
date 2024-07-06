package com.lulan.shincolle.entity;

import java.util.Random;

/**
 * SHIP EMOTION
 * include emtion time and state for model display
 */
public interface IShipEmotion extends IShipFlags {

    /**
     * get emotion value
     */
    int getStateEmotion(int id);

    void setStateEmotion(int id, int value, boolean sync);

    /**
     * get emotion timer
     */
    int getStateTimer(int id);

    void setStateTimer(int id, int value);

    /**
     * GET/SET emotion start time
     * emotion start time for individual entity
     * note: these ticks are render tick (ex: 60~N FPS), NOT game tick (fixed 20 FPS)
     */
    int getFaceTick();

    void setFaceTick(int par1);

    int getHeadTiltTick();

    void setHeadTiltTick(int par1);

    int getAttackTick();        //game tick (20 FPS) = AttackTime (1.7.10-)

    void setAttackTick(int par1);

    int getAttackTick2();    //another attack timer for some animation

    void setAttackTick2(int par1);

    int getDeathTick();

    void setDeathTick(int par1);

    /**
     * 用於手持物品render
     * par1: 0:X 1:Y 2:Z
     * par2: angle (rad)
     */
    float getModelRotate(int par1);

    void setModelRotate(int par1, float par2);

    /**
     * Get tick time for emotion count
     */
    int getTickExisted();

    /**
     * Get sit, run, ride state
     */
    float getSwingTime(float partialTick);

    boolean getIsRiding();

    boolean getIsSprinting();

    boolean getIsSitting();

    boolean getIsSneaking();

    boolean getIsLeashed();

    /**
     * set entity sit
     */
    void setEntitySit(boolean sit);

    /**
     * riding state for model display
     */
    int getRidingState();

    void setRidingState(int state);

    /**
     * get model scale level for model rendering: 0:normal, 1~N:scale level
     */
    int getScaleLevel();

    void setScaleLevel(int par1);

    /**
     * get rand for model display
     */
    Random getRand();

    /**
     * get depth: 0:self depth, 1:mounts depth, 2:host depth
     */
    double getShipDepth(int type);


}