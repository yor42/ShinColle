package com.lulan.shincolle.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.lulan.shincolle.network.PacketTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #70: characterization of the C2SInputPackets codec (current wire format, ADR-0004).
 * <p>
 * All 15 packet IDs (0..14) share one layout: [id][int length][int x length]. Every ID can be
 * built without a world because the constructor only stores the id and the int array.
 */
class C2SInputPacketsCodecTest {

    private static C2SInputPackets decode(C2SInputPackets msg) {
        return roundTrip(msg, C2SInputPackets::new);
    }

    @ParameterizedTest
    @ValueSource(bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14})
    void everyId_roundTripsIntArray(byte id) {
        int[] parms = {id, -1, Integer.MAX_VALUE, 0, 42};
        C2SInputPackets msg = new C2SInputPackets(id, parms);

        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(id).writeInt(parms.length);
        for (int p : parms) expected.writeInt(p);
        byte[] exp = new byte[expected.readableBytes()];
        expected.readBytes(exp);
        assertArrayEquals(exp, encode(msg));

        C2SInputPackets d = decode(msg);
        assertEquals(id, (byte) field(d, "packetID"));
        assertEquals(parms.length, (int) field(d, "value"));
        assertArrayEquals(parms, field(d, "value3"));
    }

    @ParameterizedTest
    @ValueSource(bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14})
    void everyId_emptyArray_writesZeroLength_decodesToNull(byte id) {
        C2SInputPackets msg = new C2SInputPackets(id);
        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(id).writeInt(0);
        byte[] exp = new byte[expected.readableBytes()];
        expected.readBytes(exp);
        assertArrayEquals(exp, encode(msg));

        C2SInputPackets d = decode(msg);
        assertEquals(0, (int) field(d, "value"));
        assertNull(field(d, "value3"));
    }

    @Test
    void nullArray_writesZeroLength() {
        C2SInputPackets msg = new C2SInputPackets(C2SInputPackets.PID.MountMove, (int[]) null);
        assertArrayEquals(new byte[]{0, 0, 0, 0, 0}, encode(msg));
    }

    @Test
    void unknownId_writesOnlyTheIdByte_currentBehavior() {
        // ids outside the switch (e.g. 99) write the type byte and nothing else; the reader reads nothing more
        C2SInputPackets msg = new C2SInputPackets((byte) 99, 1, 2, 3);
        assertArrayEquals(new byte[]{99}, encode(msg));
        C2SInputPackets d = decode(msg);
        assertEquals((byte) 99, (byte) field(d, "packetID"));
        assertNull(field(d, "value3"));
    }
}
