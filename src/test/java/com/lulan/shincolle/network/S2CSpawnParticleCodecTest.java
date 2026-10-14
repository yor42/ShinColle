package com.lulan.shincolle.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.lulan.shincolle.network.PacketTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #70: characterization of the S2CSpawnParticle codec (current wire format, ADR-0004).
 * <p>
 * All six packet IDs (0..5) can be built without a running world: the constructors only call
 * getEntityId() (and instanceof checks) on the entity.
 * <p>
 * Known asymmetry (NET-08 / #44): an empty or null int array for Ints_Path writes zero bytes,
 * not even the packet type, so nothing can be decoded.
 */
class S2CSpawnParticleCodecTest {

    private static S2CSpawnParticle decode(S2CSpawnParticle msg) {
        return roundTrip(msg, S2CSpawnParticle::new);
    }

    @Test
    void entityAnimate_id0() {
        Entity e = fakeEntity(1234, 0);
        S2CSpawnParticle msg = new S2CSpawnParticle(e, 7, true);

        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(0).writeInt(1234).writeByte(7).writeBoolean(true);
        assertArrayEquals(toArray(expected), encode(msg));

        S2CSpawnParticle d = decode(msg);
        assertEquals((byte) 0, (byte) field(d, "packetType"));
        assertEquals(1234, (int) field(d, "entityID"));
        assertEquals((byte) 7, (byte) field(d, "particleType"));
        assertTrue((boolean) field(d, "setAtkTime"));
    }

    @Test
    void entityAnimate_id0_nullEntityWritesNothing() {
        // current behavior: a null entity makes toBytes return before writing anything
        S2CSpawnParticle msg = new S2CSpawnParticle((Entity) null, 7, false);
        assertEquals(0, encode(msg).length);
    }

    @Test
    void entityPosLookAnimate_id1() {
        Entity e = fakeEntity(55, 0);
        S2CSpawnParticle msg = new S2CSpawnParticle(e, 3, 1.5D, -2.25D, 3.0D, 0.5D, 0.25D, -1.0D, false);

        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(1).writeInt(55).writeByte(3).writeBoolean(false);
        for (float f : new float[]{1.5F, -2.25F, 3.0F, 0.5F, 0.25F, -1.0F}) expected.writeFloat(f);
        assertArrayEquals(toArray(expected), encode(msg));

        S2CSpawnParticle d = decode(msg);
        assertEquals((byte) 1, (byte) field(d, "packetType"));
        assertEquals(55, (int) field(d, "entityID"));
        assertEquals((byte) 3, (byte) field(d, "particleType"));
        assertFalse((boolean) field(d, "setAtkTime"));
        assertArrayEquals(new float[]{1.5F, -2.25F, 3.0F, 0.5F, 0.25F, -1.0F}, field(d, "valueFloat1"));
    }

    @Test
    void entityPosLook_id2() {
        S2CSpawnParticle msg = new S2CSpawnParticle(9, 10.5D, 64.0D, -3.5D, 0D, 1D, 0D);

        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(2).writeByte(9);
        for (float f : new float[]{10.5F, 64.0F, -3.5F, 0F, 1F, 0F}) expected.writeFloat(f);
        assertArrayEquals(toArray(expected), encode(msg));

        S2CSpawnParticle d = decode(msg);
        assertEquals((byte) 2, (byte) field(d, "packetType"));
        assertEquals((byte) 9, (byte) field(d, "particleType"));
        assertArrayEquals(new float[]{10.5F, 64.0F, -3.5F, 0F, 1F, 0F}, field(d, "valueFloat1"));
    }

    @Test
    void entityTargetAnimate_id3() {
        Entity host = fakeEntity(100, 0);
        Entity target = fakeEntity(200, 0);
        S2CSpawnParticle msg = new S2CSpawnParticle(host, target, 1D, 2D, 3D, 11, true);

        // note: id 3 writes particleType and setAtkTime BEFORE the entity ids
        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(3).writeByte(11).writeBoolean(true).writeInt(100).writeInt(200);
        for (float f : new float[]{1F, 2F, 3F}) expected.writeFloat(f);
        assertArrayEquals(toArray(expected), encode(msg));

        S2CSpawnParticle d = decode(msg);
        assertEquals((byte) 3, (byte) field(d, "packetType"));
        assertEquals((byte) 11, (byte) field(d, "particleType"));
        assertTrue((boolean) field(d, "setAtkTime"));
        assertEquals(100, (int) field(d, "entityID"));
        assertEquals(200, (int) field(d, "entityID2"));
        assertArrayEquals(new float[]{1F, 2F, 3F}, field(d, "valueFloat1"));
    }

    @Test
    void entityPar3_id4() {
        Entity e = fakeEntity(77, 0);
        S2CSpawnParticle msg = new S2CSpawnParticle(e, 5, 0.5D, 0.25D, 0.125D);

        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(4).writeInt(77).writeByte(5);
        for (float f : new float[]{0.5F, 0.25F, 0.125F}) expected.writeFloat(f);
        assertArrayEquals(toArray(expected), encode(msg));

        S2CSpawnParticle d = decode(msg);
        assertEquals((byte) 4, (byte) field(d, "packetType"));
        assertEquals(77, (int) field(d, "entityID"));
        assertEquals((byte) 5, (byte) field(d, "particleType"));
        assertArrayEquals(new float[]{0.5F, 0.25F, 0.125F}, field(d, "valueFloat1"));
    }

    @Test
    void intsPath_id5() {
        int[] path = {1, 10, 64, -20, 11, 64, -21};
        S2CSpawnParticle msg = new S2CSpawnParticle(0, path);

        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(5).writeByte(0).writeInt(path.length);
        for (int v : path) expected.writeInt(v);
        assertArrayEquals(toArray(expected), encode(msg));

        S2CSpawnParticle d = decode(msg);
        assertEquals((byte) 5, (byte) field(d, "packetType"));
        assertEquals((byte) 0, (byte) field(d, "particleType"));
        assertArrayEquals(path, field(d, "valueInt1"));
    }

    @Test
    void intsPath_id5_emptyOrNullWritesNothing_currentBehavior() {
        assertEquals(0, encode(new S2CSpawnParticle(0, new int[0])).length);
        assertEquals(0, encode(new S2CSpawnParticle(0, (int[]) null)).length);
    }

    @Disabled("NET-08 / #44: empty or null Ints_Path array writes zero bytes (no packet type), so fromBytes has nothing to read")
    @Test
    void intsPath_id5_emptyArrayRoundTrips_knownAsymmetry() {
        S2CSpawnParticle d = decode(new S2CSpawnParticle(0, new int[0]));
        assertEquals((byte) 5, (byte) field(d, "packetType"));
    }

    private static byte[] toArray(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }
}
