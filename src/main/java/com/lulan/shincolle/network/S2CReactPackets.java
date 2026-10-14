package com.lulan.shincolle.network;

import com.lulan.shincolle.proxy.ClientProxy;
import com.lulan.shincolle.utility.BlockHelper;
import com.lulan.shincolle.utility.CommandHelper;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.PacketHelper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * CLIENT TO SERVER: REACT PACKETS
 * server端針對client端input的回應 (ex: key, command)
 */
public class S2CReactPackets implements IMessage {

    private World world;
    private EntityPlayer player;
    //set when fromBytes failed: the half-decoded message is dropped by the handler (issue #77)
    private boolean decodeFailed = false;
    private byte packetType;
    private int[] valueInt, valueInt2;
    private boolean[] valueBoolean;
    private String[] valueStr;
    //CmdShipList reply bytes, captured on the constructing (server) thread (issue #35)
    private byte[] shipListBytes;

    public S2CReactPackets() {
    }  //必須要有空參數constructor, forge才能使用此class


    /**
     * type 0: (2 parms) command: change owner: 0:sender eid, 1:owner eid
     * type 1: (1 parms) command: show ship info: 0:sender eid
     * type 2: (8 parms) command: change ship attrs: 0:sender eid, 1:ship lv, 2~7:bonus value
     * type 3: (2 parms) command: show/get/del ship list: 0:cmd type, 1:number
     * type 20:(8 parms) flare light effect: 0:x, 1:y, 2:z
     */
    public S2CReactPackets(byte type, int... parms) {
        this.packetType = type;
        this.valueInt = parms;

        //issue #35: processSendShipList reads and refreshes live world/ship caches, so run it here
        //(server thread) and keep only the bytes; toBytes (netty thread) just copies them
        if (type == PID.CmdShipList && parms != null && parms.length > 0 && parms[0] == 0) {
            ByteBuf snapshot = Unpooled.buffer();
            CommandHelper.processSendShipList(snapshot, parms[1]);
            this.shipListBytes = new byte[snapshot.readableBytes()];
            snapshot.readBytes(this.shipListBytes);
        }
    }

    //packet handle method
    private static void handle(S2CReactPackets msg, MessageContext ctx) {
        if (msg.decodeFailed) {
            LogHelper.debug("DEBUG: packet handler: S2CReactPackets: dropped packet that failed to decode, type: " + msg.packetType);
            return;
        }

        switch (msg.packetType) {
            case PID.CmdChOwner:    //cmd: change owner packet
                CommandHelper.processShipChangeOwner(msg.valueInt[0], msg.valueInt[1]);
                break;
            case PID.CmdShipInfo:    //cmd: show ship info
                CommandHelper.processShowShipInfo(msg.valueInt[0]);
                break;
            case PID.CmdShipAttr:    //cmd: change ship attrs
                CommandHelper.processSetShipAttrs(msg.valueInt);
                break;
            case PID.CmdShipList:    //cmd: show/get/del ship list
                CommandHelper.processGetShipList(msg.valueInt, msg.valueInt2, msg.valueBoolean, msg.valueStr);
                break;
            case PID.FlareEffect: {
                BlockPos pos = new BlockPos(msg.valueInt[0], msg.valueInt[1], msg.valueInt[2]);
                float light = ClientProxy.getClientWorld().getLight(pos, true);

                //place new light block
                if (light < 11F) {
                    BlockHelper.placeLightBlock(ClientProxy.getClientWorld(), pos);
                }
                //search light block, renew lifespan
                else {
                    BlockHelper.updateNearbyLightBlock(ClientProxy.getClientWorld(), pos);
                }
            }
            break;
        }//end switch
    }

    //接收packet方法, client side
    @Override
    public void fromBytes(ByteBuf buf) {
        //get packet type
        this.packetType = buf.readByte();

        switch (this.packetType) {
            case PID.CmdChOwner:    //cmd: change owner packet
            case PID.CmdShipInfo:    //cmd: show ship info
            case PID.CmdShipAttr:    //cmd: change ship attrs
            case PID.FlareEffect:    //flare effect server to client sync
            {
                try {
                    int length = buf.readInt();  //int array length

                    //get data
                    if (length > 0) {
                        this.valueInt = PacketHelper.readIntArray(buf, length);
                    }
                } catch (Exception e) {
                    this.decodeFailed = true;
                    LogHelper.error("EXCEPTION: S2C input packet: get data fail: " + e, e);
                }
            }
            break;
            case PID.CmdShipList:    //cmd: show/get/del ship list
            {
                //total size, page num, list size
                this.valueInt = PacketHelper.readIntArray(buf, 3);

                //list size comes from the packet: validate before allocating (min bytes per entry:
                //8 ints + 2 booleans + the 4-byte string flag)
                PacketHelper.checkLength(buf, this.valueInt[2], 8 * 4 + 2 + 4, PacketHelper.MAX_ARRAY_LENGTH);

                if (this.valueInt[2] > 0) {
                    this.valueInt2 = new int[this.valueInt[2] * 8];
                    this.valueBoolean = new boolean[this.valueInt[2] * 2];
                    this.valueStr = new String[this.valueInt[2]];

                    for (int i = 0; i < this.valueInt[2]; i++) {
                        //get int data
                        for (int j = 0; j < 8; j++) this.valueInt2[i * 8 + j] = buf.readInt();
                        //get boolean data
                        for (int k = 0; k < 2; k++) this.valueBoolean[i * 2 + k] = buf.readBoolean();
                        //get string data
                        this.valueStr[i] = PacketHelper.readString(buf);
                    }
                } else {
                    this.valueInt2 = null;
                    this.valueStr = null;
                }
            }
            break;
        }//end switch
    }

    //發出packet方法, server side
    @Override
    public void toBytes(ByteBuf buf) {
        //send packet type
        buf.writeByte(this.packetType);

        switch (this.packetType) {
            case PID.CmdChOwner:    //cmd: change owner packet
            case PID.CmdShipInfo:    //cmd: show ship info
            case PID.CmdShipAttr:    //cmd: change ship attrs
            case PID.FlareEffect:    //flare effect server to client sync
            {
                //send int array
                if (this.valueInt != null) {
                    //send array length
                    buf.writeInt(this.valueInt.length);

                    for (int i : this.valueInt) {
                        buf.writeInt(i);
                    }
                }
                //if array null
                else {
                    buf.writeInt(0);
                }
            }
            break;
            case PID.CmdShipList:    //cmd: show/get/del ship list
            {
                if (this.valueInt != null) {
                    if (this.valueInt[0] == 0) {        //cmd: /ship list X
                        //send ship list (snapshot taken in the constructor)
                        buf.writeBytes(this.shipListBytes);
                    }
                }
            }
            break;
        }//end switch
    }

    //packet id
    public static final class PID {
        public static final byte CmdChOwner = 0;
        public static final byte CmdShipInfo = 1;
        public static final byte CmdShipAttr = 2;
        public static final byte CmdShipList = 3;
        public static final byte FlareEffect = 20;
    }

    //packet handler (inner class)
    public static class Handler implements IMessageHandler<S2CReactPackets, IMessage> {
        //收到封包時顯示debug訊息
        @Override
        public IMessage onMessage(S2CReactPackets message, MessageContext ctx) {
            /**
             * 1.8之後minecraft主程式分為minecraft server/clinet跟networking兩個thread執行
             * 因此handler這邊必須使用addScheduledTask將封包處理方法加入到並行控制佇列中處理
             * 以避免多執行緒下各種並行處理問題
             */
            FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> S2CReactPackets.handle(message, ctx));
            return null;
        }
    }


}