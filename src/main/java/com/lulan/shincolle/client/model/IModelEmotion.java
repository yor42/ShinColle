package com.lulan.shincolle.client.model;

import com.lulan.shincolle.entity.IShipEmotion;

/**
 * model interface for EmotionHelper
 */
public interface IModelEmotion {


    /**
     * set display face
     */
    void setFace(int par1);

    /**
     * show/hide equip
     */
    void showEquip(IShipEmotion ent);

    /**
     * sync rotation to glow part, mainly for face0~face4
     */
    void syncRotationGlowPart();

    /**
     * normal and dead pose
     */
    void applyDeadPose(float f, float f1, float f2, float f3, float f4, IShipEmotion ent);

    void applyNormalPose(float f, float f1, float f2, float f3, float f4, IShipEmotion ent);

    /**
     * for debug or packet usage
     */
    int getFieldCount();

    void setField(int id, float value);

    float getField(int id);


}