package com.lulan.shincolle.utility;

/**
 * Decision seam for the ship death animation completion (issue #13, ENT-02).
 * <p>
 * The original code tested {@code deathTime >= deathMaxTick}; with {@code Death_Time=0} the counter is
 * incremented before the test (first value 1), so the test never matched and no egg dropped.
 * A threshold comparison completes the animation for every configured value; the caller keeps the
 * {@code isDead} guard and the {@code CanDrop} one-shot flag so the completion work runs once.
 */
public final class DeathDropRule {

    private DeathDropRule() {
    }

    public static boolean isDeathComplete(int deathTime, int deathMaxTick) {
        return deathTime >= deathMaxTick;
    }

    public static boolean shouldDropEgg(int deathTime, int deathMaxTick, boolean canDrop) {
        return canDrop && isDeathComplete(deathTime, deathMaxTick);
    }
}
