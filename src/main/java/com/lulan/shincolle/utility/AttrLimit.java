package com.lulan.shincolle.utility;

/**
 * Pure helper for the ConfigHandler.limitShipAttrs clamps. A negative limit (default -1) means "no limit".
 */
public final class AttrLimit {

    private AttrLimit() {}

    public static float limitAttr(float value, double limit) {
        if (limit >= 0D && value > limit) value = (float) limit;
        return value;
    }
}
