package com.lulan.shincolle.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.lulan.shincolle.network.PacketTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #70: characterization of the C2SGUIPackets codec (current wire format, ADR-0004).
 * <p>
 * Every ID is buildable without a running world using stand-ins (see PacketTestSupport):
 * the writer only calls getEntityId(), world.provider.getDimension(), tile.getWorld()/getPos().
 * <p>
 * Decoded state differs from the constructor arguments by design: the receiver gets
 * valueInt = [entityId, dimension, parms...] (entity families), [dimension, x, y, z] (tile
 * families) and has no Entity/TileEntity reference. These tests pin that shape.
 * <p>
 * Not covered: ID 34 (NO_USE) has no switch case; it writes only the type byte.
 */
class C2SGUIPacketsCodecTest {

    private static C2SGUIPackets decode(C2SGUIPackets msg) {
        return roundTrip(msg, C2SGUIPackets::new);
    }

    @ParameterizedTest
    @ValueSource(bytes = {C2SGUIPackets.PID.ShipBtn, C2SGUIPackets.PID.MorphBtn, C2SGUIPackets.PID.MorphBtn2})
    void entityButton_ids0_2_3(byte id) {
        Entity e = fakeEntity(321, -1);
        C2SGUIPackets msg = new C2SGUIPackets(e, id, 5, -9);

        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(id).writeInt(321).writeInt(-1).writeInt(5).writeInt(-9);
        assertArrayEquals(bytes(expected), encode(msg));

        C2SGUIPackets d = decode(msg);
        assertEquals(id, (byte) field(d, "packetType"));
        assertArrayEquals(new int[]{321, -1, 5, -9}, field(d, "valueInt"));
    }

    @Test
    void tileButton_id1_valuesAreWrittenAsBytes() {
        TileEntity t = fakeTile(0, 10, 64, -20);
        C2SGUIPackets msg = new C2SGUIPackets(t, C2SGUIPackets.PID.TileBtn, 3, 4, 300);

        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(1).writeInt(0).writeInt(10).writeInt(64).writeInt(-20);
        expected.writeByte(3).writeByte(4).writeByte(300);
        assertArrayEquals(bytes(expected), encode(msg));

        C2SGUIPackets d = decode(msg);
        assertArrayEquals(new int[]{0, 10, 64, -20}, field(d, "valueInt"));
        // 300 is truncated to its low byte (44) by writeByte: current behavior
        assertArrayEquals(new byte[]{3, 4, 44}, field(d, "valueByte"));
    }

    @Test
    void deskFuncSync_id77() {
        TileEntity t = fakeTile(1, 1, 2, 3);
        C2SGUIPackets msg = new C2SGUIPackets(t, C2SGUIPackets.PID.Desk_FuncSync, 1, 0, 1);

        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(77).writeInt(1).writeInt(1).writeInt(2).writeInt(3).writeInt(3);
        expected.writeByte(1).writeByte(0).writeByte(1);
        assertArrayEquals(bytes(expected), encode(msg));

        C2SGUIPackets d = decode(msg);
        // the receiver's valueInt is the header [dim, x, y, z, byteCount]
        assertArrayEquals(new int[]{1, 1, 2, 3, 3}, field(d, "valueInt"));
        assertArrayEquals(new byte[]{1, 0, 1}, field(d, "valueByte"));
    }

    @ParameterizedTest
    @ValueSource(bytes = {20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 31, 32, 33, 35, 51, 72, 73, 74, 75, 76})
    void entityIntList_ids20to35_51_72to76(byte id) {
        Entity e = fakeEntity(900, 7);
        C2SGUIPackets msg = new C2SGUIPackets(e, id, 1, 2, 3);

        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(id).writeInt(5).writeInt(900).writeInt(7).writeInt(1).writeInt(2).writeInt(3);
        assertArrayEquals(bytes(expected), encode(msg));

        C2SGUIPackets d = decode(msg);
        assertEquals(id, (byte) field(d, "packetType"));
        assertArrayEquals(new int[]{900, 7, 1, 2, 3}, field(d, "valueInt"));
    }

    @Test
    void entityIntList_noParms_writesEntityAndDimensionOnly() {
        Entity e = fakeEntity(900, 7);
        C2SGUIPackets msg = new C2SGUIPackets(e, C2SGUIPackets.PID.ClearTeam);
        // varargs with no values is an empty (non-null) array
        C2SGUIPackets d = decode(msg);
        assertArrayEquals(new int[]{900, 7}, field(d, "valueInt"));
    }

    @Test
    void entityIntList_nullParms_writesEntityAndDimensionOnly() {
        Entity e = fakeEntity(900, 7);
        C2SGUIPackets msg = new C2SGUIPackets(e, C2SGUIPackets.PID.ClearTeam, (int[]) null);
        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(26).writeInt(2).writeInt(900).writeInt(7);
        assertArrayEquals(bytes(expected), encode(msg));
        assertArrayEquals(new int[]{900, 7}, field(decode(msg), "valueInt"));
    }

    @ParameterizedTest
    @ValueSource(bytes = {30, 36, 50, 70, 71})
    void playerString_ids30_36_50_70_71(byte id) {
        C2SGUIPackets msg = new C2SGUIPackets(fakePlayer(12, 0), id, "Hime\u8266\u5a18");

        ByteBuf head = Unpooled.buffer();
        head.writeByte(id).writeInt(12).writeInt(0).writeInt(1);
        byte[] enc = encode(msg);
        byte[] exp = bytes(head);
        assertArrayEquals(exp, java.util.Arrays.copyOf(enc, exp.length));

        C2SGUIPackets d = decode(msg);
        assertArrayEquals(new int[]{12, 0}, field(d, "valueInt"));
        assertEquals("Hime\u8266\u5a18", field(d, "valueStr"));
    }

    @Test
    void playerString_nullAndEmpty() {
        C2SGUIPackets d = decode(new C2SGUIPackets(fakePlayer(12, 0), C2SGUIPackets.PID.SetUnitName, (String) null));
        assertNull(field(d, "valueStr"));

        d = decode(new C2SGUIPackets(fakePlayer(12, 0), C2SGUIPackets.PID.SetUnitName, ""));
        assertEquals("", field(d, "valueStr"));
    }

    @Test
    void noUseId34_writesOnlyTheTypeByte_currentBehavior() {
        C2SGUIPackets msg = new C2SGUIPackets(fakeEntity(1, 0), C2SGUIPackets.PID.NO_USE, 1);
        assertArrayEquals(new byte[]{34}, encode(msg));
    }

    private static byte[] bytes(ByteBuf buf) {
        byte[] b = new byte[buf.readableBytes()];
        buf.readBytes(b);
        return b;
    }
}
