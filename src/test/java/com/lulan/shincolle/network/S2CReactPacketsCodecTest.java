package com.lulan.shincolle.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.lulan.shincolle.network.PacketTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #70: characterization of the S2CReactPackets codec (current wire format, ADR-0004).
 * <p>
 * Buildable without a world: CmdChOwner (0), CmdShipInfo (1), CmdShipAttr (2), FlareEffect (20).
 * <p>
 * CmdShipList (3): the writer delegates to CommandHelper.processSendShipList, which reads the
 * server player ship list (world/server state), so the encode side cannot be built here. The
 * decode side is pinned with hand-written bytes instead. Also, for valueInt[0] != 0 the writer
 * emits only the packet type while the reader always reads 3 ints (NET-08 / #44).
 */
class S2CReactPacketsCodecTest {

    private static S2CReactPackets decode(S2CReactPackets msg) {
        return roundTrip(msg, S2CReactPackets::new);
    }

    @Test
    void cmdChOwner_id0() {
        S2CReactPackets msg = new S2CReactPackets(S2CReactPackets.PID.CmdChOwner, 11, 22);

        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(0).writeInt(2).writeInt(11).writeInt(22);
        assertArrayEquals(bytes(expected), encode(msg));

        S2CReactPackets d = decode(msg);
        assertEquals((byte) 0, (byte) field(d, "packetType"));
        assertArrayEquals(new int[]{11, 22}, field(d, "valueInt"));
    }

    @Test
    void cmdShipInfo_id1() {
        S2CReactPackets d = decode(new S2CReactPackets(S2CReactPackets.PID.CmdShipInfo, 5));
        assertEquals((byte) 1, (byte) field(d, "packetType"));
        assertArrayEquals(new int[]{5}, field(d, "valueInt"));
    }

    @Test
    void cmdShipAttr_id2() {
        int[] attrs = {7, 150, 1, 2, 3, 4, 5, 6};
        S2CReactPackets d = decode(new S2CReactPackets(S2CReactPackets.PID.CmdShipAttr, attrs));
        assertEquals((byte) 2, (byte) field(d, "packetType"));
        assertArrayEquals(attrs, field(d, "valueInt"));
    }

    @Test
    void flareEffect_id20_negativeCoordinates() {
        S2CReactPackets d = decode(new S2CReactPackets(S2CReactPackets.PID.FlareEffect, -100, 64, 3000));
        assertEquals((byte) 20, (byte) field(d, "packetType"));
        assertArrayEquals(new int[]{-100, 64, 3000}, field(d, "valueInt"));
    }

    @Test
    void emptyIntArray_writesZeroLength_decodesToNull() {
        // current behavior: length 0 is written, the reader only allocates when length > 0
        S2CReactPackets msg = new S2CReactPackets(S2CReactPackets.PID.CmdChOwner);
        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(0).writeInt(0);
        assertArrayEquals(bytes(expected), encode(msg));

        S2CReactPackets d = decode(msg);
        assertNull(field(d, "valueInt"));
    }

    @Test
    void nullIntArray_writesZeroLength_decodesToNull() {
        S2CReactPackets msg = new S2CReactPackets(S2CReactPackets.PID.CmdShipInfo, (int[]) null);
        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(1).writeInt(0);
        assertArrayEquals(bytes(expected), encode(msg));

        assertNull(field(decode(msg), "valueInt"));
    }

    @Disabled("NET-08 / #44 (round-trip): empty and null int arrays both decode to null, not to the original empty array")
    @Test
    void emptyIntArray_roundTripsToEmptyArray_knownAsymmetry() {
        S2CReactPackets d = decode(new S2CReactPackets(S2CReactPackets.PID.CmdChOwner));
        assertArrayEquals(new int[0], field(d, "valueInt"));
    }

    @Test
    void cmdShipList_id3_decodeFromHandWrittenBytes() {
        // layout read by fromBytes: [type][total, page, size][size x (8 ints, 2 booleans, string)]
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(3);
        buf.writeInt(40).writeInt(2).writeInt(2);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 8; j++) buf.writeInt(i * 100 + j);
            buf.writeBoolean(i == 0).writeBoolean(i == 1);
            buf.writeInt(1);                                   // PacketHelper.sendString flag: has string
            net.minecraftforge.fml.common.network.ByteBufUtils.writeUTF8String(buf, "ship" + i);
        }

        S2CReactPackets d = new S2CReactPackets();
        d.fromBytes(buf);
        assertEquals(0, buf.readableBytes());
        assertArrayEquals(new int[]{40, 2, 2}, field(d, "valueInt"));
        int[] ints = field(d, "valueInt2");
        assertEquals(16, ints.length);
        assertEquals(0, ints[0]);
        assertEquals(107, ints[15]);
        assertArrayEquals(new boolean[]{true, false, false, true}, field(d, "valueBoolean"));
        assertArrayEquals(new String[]{"ship0", "ship1"}, field(d, "valueStr"));
    }

    @Test
    void cmdShipList_id3_decodeEmptyList() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(3).writeInt(0).writeInt(0).writeInt(0);
        S2CReactPackets d = new S2CReactPackets();
        d.fromBytes(buf);
        assertEquals(0, buf.readableBytes());
        assertNull(field(d, "valueInt2"));
        assertNull(field(d, "valueStr"));
    }

    @Test
    void cmdShipList_id3_nonZeroSubcommandWritesOnlyTheType_currentBehavior() {
        // valueInt[0] != 0 skips CommandHelper.processSendShipList (the only world-dependent call)
        S2CReactPackets msg = new S2CReactPackets(S2CReactPackets.PID.CmdShipList, 1, 0);
        assertArrayEquals(new byte[]{3}, encode(msg));
    }

    @Test
    void cmdShipList_id3_nullArrayWritesOnlyTheType_currentBehavior() {
        assertArrayEquals(new byte[]{3}, encode(new S2CReactPackets(S2CReactPackets.PID.CmdShipList, (int[]) null)));
    }

    @Disabled("NET-08 / #44: CmdShipList with valueInt[0] != 0 writes only the type byte but fromBytes always reads 3 ints (IndexOutOfBounds)")
    @Test
    void cmdShipList_id3_nonZeroSubcommandRoundTrips_knownAsymmetry() {
        decode(new S2CReactPackets(S2CReactPackets.PID.CmdShipList, 1, 0));
    }

    private static byte[] bytes(ByteBuf buf) {
        byte[] b = new byte[buf.readableBytes()];
        buf.readBytes(b);
        return b;
    }
}
