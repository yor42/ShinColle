package com.lulan.shincolle.utility;

public class MathHelper {
    public static float DegreesToRadian(float degrees){
        return degrees * (float)Math.PI / 180;
    }

    public static float RadianToDegrees(float radian){
        return radian * 180 / (float)Math.PI;
    }
}
