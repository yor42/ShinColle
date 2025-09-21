package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipFloating;
import com.lulan.shincolle.entity.IShipGuardian;
import com.lulan.shincolle.reference.ID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

/**
 * SHIP FLOATING ON WATER AI
 * If in water with air above, attempts to float up and stand on water surface
 * (entity body remains in water)
 */
public class EntityAIShipFloating extends EntityAIBase {

    // Constants for motion adjustments - much more maintainable
    private static final double DEEP_WATER_MOTION = 0.025D;
    private static final double MEDIUM_WATER_MOTION = 0.015D;
    private static final double SHALLOW_WATER_MOTION = 0.007D;
    private static final double SURFACE_WATER_MOTION = 0.003D;
    private static final double VERY_SHALLOW_MOTION = 0.0015D;

    private static final double DEEP_WATER_THRESHOLD = 4.0D;
    private static final double MEDIUM_WATER_THRESHOLD = 2.0D;
    private static final double SHALLOW_WATER_THRESHOLD = 1.3D;
    private static final double SURFACE_WATER_THRESHOLD = 0.47D;
    private static final double VERY_SHALLOW_THRESHOLD = 0.15D;

    private final IShipFloating host;
    private final EntityLivingBase hostLiving;
    private final boolean isShip;
    private final boolean isMount;

    public EntityAIShipFloating(IShipFloating entity) {
        this.host = entity;
        this.hostLiving = (EntityLivingBase) entity;
        this.isShip = entity instanceof BasicEntityShip;
        this.isMount = entity instanceof BasicEntityMount;
        this.setMutexBits(8);
    }

    @Override
    public boolean shouldExecute() {
        // Quick check: if not deep enough to float, exit early
        if (host.getShipDepth() <= host.getShipFloatingDepth()) {
            return false;
        }

        if (isShip) {
            return shouldShipFloat((BasicEntityShip) host);
        } else if (isMount) {
            return shouldMountFloat((BasicEntityMount) host);
        }

        // Default case for other IShipFloating implementations
        return true;
    }

    private boolean shouldShipFloat(BasicEntityShip ship) {
        // Must be able to float up
        if (!ship.getStateFlag(ID.F.CanFloatUp)) {
            return false;
        }

        // Cannot float if in any of these states
        return !ship.isRiding()
                && !ship.isSitting()
                && ship.getStateMinor(ID.M.CraneState) <= 0
                && ship.getShipNavigate().noPath()
                && !isInGuardPosition(ship);
    }

    private boolean shouldMountFloat(BasicEntityMount mount) {
        BasicEntityShip hostShip = getHostShip(mount);
        if (hostShip == null) {
            return false;
        }

        // Cannot float if host ship is in restricted state
        if (hostShip.isSitting()
                || hostShip.getStateMinor(ID.M.CraneState) > 0
                || !hostShip.getShipNavigate().noPath()
                || isInGuardPosition(hostShip)) {
            return false;
        }

        // Mount itself must also be free to move
        return mount.getShipNavigate().noPath() && !isInGuardPosition(mount);
    }

    private BasicEntityShip getHostShip(BasicEntityMount mount) {
        Entity hostEntity = mount.getHostEntity();
        return hostEntity instanceof BasicEntityShip ? (BasicEntityShip) hostEntity : null;
    }

    @Override
    public void updateTask() {
        double depth = host.getShipDepth();
        double motionToAdd = calculateFloatingMotion(depth);

        if (motionToAdd > 0) {
            hostLiving.motionY += motionToAdd;
        }
    }

    /**
     * Calculate floating motion based on depth using a more elegant approach
     */
    private double calculateFloatingMotion(double depth) {
        if (depth > DEEP_WATER_THRESHOLD) {
            return DEEP_WATER_MOTION;
        } else if (depth > MEDIUM_WATER_THRESHOLD) {
            return MEDIUM_WATER_MOTION;
        } else if (depth > SHALLOW_WATER_THRESHOLD) {
            return SHALLOW_WATER_MOTION;
        } else if (depth > SURFACE_WATER_THRESHOLD) {
            return SURFACE_WATER_MOTION;
        } else if (depth > VERY_SHALLOW_THRESHOLD) {
            return VERY_SHALLOW_MOTION;
        }
        return 0.0D;
    }

    /**
     * Check if ship is in guard position - extracted and optimized
     * This could potentially be moved to a utility class or the IShipGuardian interface
     */
    public static boolean isInGuardPosition(IShipGuardian host) {
        Entity entity = (Entity) host;

        // Quick check: if air above, can float up
        if (entity.world.getBlockState(new BlockPos(entity).up()).getBlock() == Blocks.AIR) {
            return false;
        }

        // Check guard vs follow state
        if (!host.getStateFlag(ID.F.CanFollow)) {
            return isWithinGuardDistance(host, entity);
        } else {
            return isWithinFollowDistance(host, entity);
        }
    }

    private static boolean isWithinGuardDistance(IShipGuardian host, Entity entity) {
        float minDistance = host.getStateMinor(ID.M.FollowMin) + entity.width * 0.5F;
        float minDistanceSq = minDistance * minDistance;

        // Check guarded entity distance
        Entity guardedEntity = host.getGuardedEntity();
        if (guardedEntity != null) {
            return entity.getDistanceSq(guardedEntity) < minDistanceSq;
        }

        // Check guard position distance
        float guardY = host.getStateMinor(ID.M.GuardY);
        if (guardY > 0) {
            double distanceSq = entity.getDistanceSq(
                    host.getStateMinor(ID.M.GuardX),
                    guardY,
                    host.getStateMinor(ID.M.GuardZ)
            );
            return distanceSq < minDistanceSq && entity.posY >= guardY;
        }

        return false;
    }

    private static boolean isWithinFollowDistance(IShipGuardian host, Entity entity) {
        Entity hostEntity = host.getHostEntity();
        if (hostEntity == null) {
            return false;
        }

        float maxDistance = host.getStateMinor(ID.M.FollowMax) + entity.width * 0.5F;
        float maxDistanceSq = maxDistance * maxDistance;

        return hostEntity.getDistanceSq(entity) <= maxDistanceSq;
    }
}