package com.lulan.shincolle.tileentity;

import com.lulan.shincolle.entity.IShipOwner;
import net.minecraft.util.math.BlockPos;

public interface ITileWaypoint extends IShipOwner, ITileGuardPoint {


    BlockPos getLastWaypoint();

    /**
     * last waypoint
     */
    void setLastWaypoint(BlockPos pos);

    BlockPos getNextWaypoint();

    /**
     * next waypoint
     */
    void setNextWaypoint(BlockPos pos);

    int getWpStayTime();

    /**
     * waypoint stay time
     */
    void setWpStayTime(int time);

    BlockPos getPairedChest();

    /**
     * paired chest
     */
    void setPairedChest(BlockPos pos);


}