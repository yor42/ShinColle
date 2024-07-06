package com.lulan.shincolle.tileentity;

import com.lulan.shincolle.block.BlockWaypoint;
import com.lulan.shincolle.block.ItemBlockWaypoint;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.item.PointerItem;
import com.lulan.shincolle.network.S2CGUIPackets;
import com.lulan.shincolle.proxy.ClientProxy;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.PacketHelper;
import com.lulan.shincolle.utility.ParticleHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

public class TileEntityWaypoint extends BasicTileEntity implements ITileWaypoint, ITickable {

    public EntityPlayer owner;
    //waypoint
    private int tick, wpstay, playerUID;
    private BlockPos lastPos, nextPos, chestPos;


    public TileEntityWaypoint() {
        super();
        this.tick = 0;
        this.wpstay = 0;
        this.playerUID = 0;
        this.lastPos = BlockPos.ORIGIN;
        this.nextPos = BlockPos.ORIGIN;
        this.chestPos = BlockPos.ORIGIN;
    }

    @Override
    public String getRegName() {
        return BlockWaypoint.TILENAME;
    }

    @Override
    public byte getPacketID(int type) {
        if (type == 0) {
            return S2CGUIPackets.PID.TileWaypoint;
        }

        return -1;
    }

    //讀取nbt資料
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);    //從nbt讀取方塊的xyz座標

        wpstay = nbt.getInteger("wpstay");
        playerUID = nbt.getInteger("pid");

        //load pos
        int[] pos = nbt.getIntArray("lastPos");
        if (pos.length != 3) this.lastPos = BlockPos.ORIGIN;
        else this.lastPos = new BlockPos(pos[0], pos[1], pos[2]);

        pos = nbt.getIntArray("nextPos");
        if (pos.length != 3) this.nextPos = BlockPos.ORIGIN;
        else this.nextPos = new BlockPos(pos[0], pos[1], pos[2]);

        pos = nbt.getIntArray("chestPos");
        if (pos.length != 3) this.chestPos = BlockPos.ORIGIN;
        else this.chestPos = new BlockPos(pos[0], pos[1], pos[2]);
    }

    //將資料寫進nbt
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        nbt.setInteger("wpstay", wpstay);
        nbt.setInteger("pid", playerUID);

        //save pos
        if (this.lastPos != null)
            nbt.setIntArray("lastPos", new int[]{this.lastPos.getX(), this.lastPos.getY(), this.lastPos.getZ()});
        else nbt.setIntArray("lastPos", new int[]{0, 0, 0});

        if (this.nextPos != null)
            nbt.setIntArray("nextPos", new int[]{this.nextPos.getX(), this.nextPos.getY(), this.nextPos.getZ()});
        else nbt.setIntArray("nextPos", new int[]{0, 0, 0});

        if (this.chestPos != null)
            nbt.setIntArray("chestPos", new int[]{this.chestPos.getX(), this.chestPos.getY(), this.chestPos.getZ()});
        else nbt.setIntArray("chestPos", new int[]{0, 0, 0});

        return nbt;
    }

    @Override
    public void update() {
        //both side
        this.tick++;

        //client side
        if (this.world.isRemote) {
            //valid tile
            this.world.getBlockState(this.pos).getBlock();
            this.invalidate();
            return;

            //show client particle: player hold waypoint or target wrench
        }//end client side
        //server side
        else {
            //get owner entity
            if ((this.tick & 15) == 0 && this.owner == null && this.playerUID > 0) {
                this.owner = EntityHelper.getEntityPlayerByUID(this.playerUID);
            }

            //sync waypoint
            if (this.playerUID > 0 || this.lastPos.getY() > 0 || this.nextPos.getY() > 0 || this.chestPos.getY() > 0) {
                if (this.tick > 128) {
                    this.tick = 0;
                    this.sendSyncPacket();
                }
            }
        }//end server side
    }

    @Override
    public BlockPos getNextWaypoint() {
        return this.nextPos;
    }

    @Override
    public void setNextWaypoint(BlockPos pos) {
        if (pos != null) {
            this.nextPos = pos;

            //sync to client
            if (!this.world.isRemote) this.sendSyncPacket();
        }
    }

    @Override
    public BlockPos getLastWaypoint() {
        return this.lastPos;
    }

    @Override
    public void setLastWaypoint(BlockPos pos) {
        if (pos != null) {
            this.lastPos = pos;

            //sync to client
            if (!this.world.isRemote) this.sendSyncPacket();
        }
    }

    @Override
    public int getWpStayTime() {
        return BasicEntityShip.wpStayTime2Ticks(wpstay);
    }

    @Override
    public void setWpStayTime(int wpstay) {
        this.wpstay = wpstay;
    }

    public void nextWpStayTime() {
        this.wpstay++;
        if (this.wpstay > 16) this.wpstay = 0;
    }

    @Override
    public int getPlayerUID() {
        return this.playerUID;
    }

    @Override
    public void setPlayerUID(int uid) {
        this.playerUID = uid;

        //sync uid to all around
        if (!this.world.isRemote) {
            PacketHelper.sendS2CEntitySync(0, this, this.world, this.pos, null);
        }
    }

    @Override
    public Entity getHostEntity() {
        return null;
    }

    @Override
    public BlockPos getPairedChest() {
        return this.chestPos;
    }

    @Override
    public void setPairedChest(BlockPos pos) {
        if (pos != null) {
            TileEntity tile = this.world.getTileEntity(pos);

            if (tile instanceof IInventory) {
                this.chestPos = pos;
            }

            //sync to client
            if (!this.world.isRemote) this.sendSyncPacket();
        }
    }


}