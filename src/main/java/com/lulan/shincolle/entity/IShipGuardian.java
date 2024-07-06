package com.lulan.shincolle.entity;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public interface IShipGuardian extends IShipAttackBase {

    /**
     * get entity be guarded
     */
    Entity getGuardedEntity();

    /**
     * set entity be guarded
     */
    void setGuardedEntity(Entity entity);

    /**
     * get position be guarded vec: 0:x 1:y 2:z 3:dim 4:type
     */
    int getGuardedPos(int vec);

    /**
     * set position be guarded
     */
    void setGuardedPos(int x, int y, int z, int dim, int type);

    /**
     * get last waypoint
     */
    BlockPos getLastWaypoint();

    /**
     * set last waypoint
     */
    void setLastWaypoint(BlockPos pos);

    /**
     * get waypoint stay time
     */
    int getWpStayTime();

    /**
     * set waypoint stay time
     */
    void setWpStayTime(int time);

    int getWpStayTimeMax();


}
