package com.lulan.shincolle.tileentity;

public interface ITileFurnace {


    /**
     * 本次處理已經消耗掉的燃料值
     */
    int getPowerConsumed();

    void setPowerConsumed(int par1);

    /**
     * 本次處理要達到的目標燃料值
     */
    int getPowerGoal();

    void setPowerGoal(int par1);

    /**
     * 剩下的燃料值
     */
    int getPowerRemained();

    void setPowerRemained(int par1);

    /**
     * 燃料值上限
     */
    int getPowerMax();

    void setPowerMax(int par1);

    /**
     * fuel magnification by condig
     */
    float getFuelMagni();


}