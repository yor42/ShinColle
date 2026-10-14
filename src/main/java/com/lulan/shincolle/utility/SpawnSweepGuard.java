package com.lulan.shincolle.utility;

/**
 * Decision seam for the vanilla spawn-setting sweep (issue #10, ENT-13).
 * <p>
 * With {@code spawn-animals=false} or {@code spawn-npcs=false}, vanilla
 * {@code WorldServer.updateEntityWithOptionalForce} calls {@code setDead()} on every animal, water mob
 * and NPC. Friendly ships extend {@code EntityTameable} (an animal), so they would be deleted on the first tick.
 * The mixin on that method asks this class whether an entity is exempt.
 */
public final class SpawnSweepGuard {

    private SpawnSweepGuard() {
    }

    /**
     * @param entityClass runtime class of the entity about to be swept (may be null)
     * @param shipBase    the friendly ship base class (BasicEntityShip)
     * @return true when the sweep must not remove this entity
     */
    public static boolean isExempt(Class<?> entityClass, Class<?> shipBase) {
        return entityClass != null && shipBase != null && shipBase.isAssignableFrom(entityClass);
    }
}
