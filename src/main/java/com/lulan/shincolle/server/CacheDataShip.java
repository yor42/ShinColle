package com.lulan.shincolle.server;

import com.lulan.shincolle.entity.BasicEntityShip;
import net.minecraft.nbt.NBTTagCompound;

public class CacheDataShip {

    public int entityID;
    public int worldID;
    public int classID;
    public boolean isDead;
    public int posX;
    public int posY;
    public int posZ;
    public NBTTagCompound entityNBT;


    public CacheDataShip(BasicEntityShip ship) {
        if (ship != null) {
            this.entityID = ship.getEntityId();
            this.worldID = ship.world.provider.getDimension();
            this.classID = ship.getShipClass();
            this.isDead = ship.isDead;
            this.posX = (int) ship.posX;
            this.posY = (int) ship.posY;
            this.posZ = (int) ship.posZ;
            this.entityNBT = ship.writeToNBT(new NBTTagCompound());
        }
    }

    public CacheDataShip(int eid, int wid, int cid, boolean isDead, double posX, double posY, double posZ, NBTTagCompound nbt) {
        this.entityID = eid;
        this.worldID = wid;
        this.classID = cid;
        this.isDead = isDead;
        this.posX = (int) posX;
        this.posY = (int) posY;
        this.posZ = (int) posZ;
        this.entityNBT = nbt;
    }

    /**
     * True if this snapshot would serialize to the same world data entry as the other one
     * (same entity id, dimension, class, dead flag, block position and entity NBT).
     * Used to skip rewriting and re-dirtying the world data when a ship did not change.
     */
    public boolean hasSameContent(CacheDataShip other) {
        if (other == null) return false;
        if (this == other) return true;

        return this.entityID == other.entityID &&
                this.worldID == other.worldID &&
                this.classID == other.classID &&
                this.isDead == other.isDead &&
                this.posX == other.posX &&
                this.posY == other.posY &&
                this.posZ == other.posZ &&
                java.util.Objects.equals(this.entityNBT, other.entityNBT);
    }

}
