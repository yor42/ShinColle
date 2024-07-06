package com.lulan.shincolle.client.model;

import com.lulan.shincolle.entity.IShipEmotion;

/**
 * add more emotion
 */
public interface IModelEmotionAdv extends IModelEmotion {


    /**
     * set mouth: 0:普通, 1:歪嘴, 2:扁嘴, 3:無口, 4:張開, 5:大嘴
     */
    void setMouth(int par1);

    /**
     * set flush
     */
    void setFlush(boolean par1);

    /**
     * set face by emotion
     */
    void setFaceNormal(IShipEmotion ent);    //平常

    void setFaceBlink0(IShipEmotion ent);    //眨眼, 開

    void setFaceBlink1(IShipEmotion ent);    //眨眼, 閉

    void setFaceCry(IShipEmotion ent);        //重傷

    void setFaceAttack(IShipEmotion ent);    //攻擊

    void setFaceDamaged(IShipEmotion ent);    //受攻擊

    void setFaceHungry(IShipEmotion ent);    //無燃料

    void setFaceAngry(IShipEmotion ent);        //生氣

    void setFaceScorn(IShipEmotion ent);        //瞪人

    void setFaceBored(IShipEmotion ent);        //無聊

    void setFaceShy(IShipEmotion ent);        //害羞

    void setFaceHappy(IShipEmotion ent);        //高興


}