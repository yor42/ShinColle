package com.lulan.shincolle.utility;

/**
 * Exp needed for the next level (issue #19, ATTR-07): {@code level * expMod + expMod}.
 */
public final class ExpCurve {

    private ExpCurve() {
    }

    public static int expNext(int level, int expMod) {
        return level * expMod + expMod;
    }
}
