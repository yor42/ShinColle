package com.lulan.shincolle.utility;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #16: packet-supplied lengths are validated before any allocation (ADR-0004).
 * Valid input must decode exactly as before and the encoders are untouched.
 */
class PacketHelperBoundsTest {

    private static ByteBuf bufWithLength(int length, int payloadBytes) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(length);
        buf.writeZero(payloadBytes);
        return buf;
    }

    // ---- oversize: claims more than the buffer holds ----

    @Test
    void intArray_lengthLargerThanBuffer_rejected() {
        assertThrows(DecoderException.class, () -> PacketHelper.readIntArray(bufWithLength(Integer.MAX_VALUE, 16)));
    }

    @Test
    void intArray_explicitLengthLargerThanBuffer_rejected() {
        assertThrows(DecoderException.class, () -> PacketHelper.readIntArray(Unpooled.buffer().writeZero(16), Integer.MAX_VALUE));
    }

    @Test
    void intArray_lengthTimesElementSizeExceedsBuffer_rejected() {
        // 5 ints need 20 bytes, only 19 present: element size must be taken into account
        assertThrows(DecoderException.class, () -> PacketHelper.readIntArray(bufWithLength(5, 19)));
    }

    @Test
    void byteArray_lengthLargerThanBuffer_rejected() {
        assertThrows(DecoderException.class, () -> PacketHelper.readByteArray(bufWithLength(Integer.MAX_VALUE, 4)));
        assertThrows(DecoderException.class, () -> PacketHelper.readByteArray(Unpooled.buffer().writeZero(4), 1_000_000_000));
    }

    @Test
    void floatDoubleBooleanArrays_lengthLargerThanBuffer_rejected() {
        assertThrows(DecoderException.class, () -> PacketHelper.readFloatArray(bufWithLength(Integer.MAX_VALUE, 8)));
        assertThrows(DecoderException.class, () -> PacketHelper.readDoubleArray(bufWithLength(1_000_000, 8)));
        assertThrows(DecoderException.class, () -> PacketHelper.readBooleanArray(bufWithLength(1_000_000, 8)));
    }

    @Test
    void lists_andMap_lengthLargerThanBuffer_rejected() {
        assertThrows(DecoderException.class, () -> PacketHelper.readListInt(bufWithLength(Integer.MAX_VALUE, 8)));
        assertThrows(DecoderException.class, () -> PacketHelper.readListString(bufWithLength(Integer.MAX_VALUE, 8)));
        assertThrows(DecoderException.class, () -> PacketHelper.readMapInt(bufWithLength(Integer.MAX_VALUE, 8)));
    }

    @Test
    void string_lengthLargerThanBuffer_rejected() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(1);          // has-string flag
        buf.writeByte(0x7F);      // varint length 127
        buf.writeZero(3);         // only 3 bytes follow
        assertThrows(DecoderException.class, () -> PacketHelper.readString(buf));
    }

    // ---- negative ----

    @Test
    void negativeLengths_rejected() {
        assertThrows(DecoderException.class, () -> PacketHelper.readIntArray(Unpooled.buffer().writeZero(16), -2));
        assertThrows(DecoderException.class, () -> PacketHelper.readByteArray(Unpooled.buffer().writeZero(16), -1));
        assertThrows(DecoderException.class, () -> PacketHelper.readIntArray(bufWithLength(Integer.MIN_VALUE, 16)));
        assertThrows(DecoderException.class, () -> PacketHelper.readFloatArray(bufWithLength(-2, 16)));
    }

    // ---- valid input is unchanged ----

    @Test
    void nullMarkerAndZeroLength_stillDecodeToEmpty() {
        // senders write -1 for a null array; 0 for an empty one
        assertEquals(0, PacketHelper.readIntArray(bufWithLength(-1, 0)).length);
        assertEquals(0, PacketHelper.readByteArray(bufWithLength(0, 0)).length);
        assertEquals(0, PacketHelper.readFloatArray(bufWithLength(-1, 0)).length);
        assertTrue(PacketHelper.readListInt(bufWithLength(-1, 0)).isEmpty());
        assertTrue(PacketHelper.readMapInt(bufWithLength(0, 0)).isEmpty());
        assertTrue(PacketHelper.readListString(bufWithLength(-1, 0)).isEmpty());
    }

    @Test
    void validArrays_roundTripAndStayByteIdentical() {
        ByteBuf buf = Unpooled.buffer();
        PacketHelper.sendArrayInt(buf, new int[]{1, -2, Integer.MAX_VALUE});
        PacketHelper.sendArrayByte(buf, new byte[]{7, -8});
        PacketHelper.sendArrayFloat(buf, new float[]{1.5F, -2.5F});
        PacketHelper.sendArrayDouble(buf, new double[]{3.25D});
        PacketHelper.sendArrayBoolean(buf, new boolean[]{true, false, true});

        // exact layout: [int len][payload], big endian
        ByteBuf expected = Unpooled.buffer();
        expected.writeInt(3).writeInt(1).writeInt(-2).writeInt(Integer.MAX_VALUE);
        expected.writeInt(2).writeByte(7).writeByte(-8);
        expected.writeInt(2).writeFloat(1.5F).writeFloat(-2.5F);
        expected.writeInt(1).writeDouble(3.25D);
        expected.writeInt(3).writeBoolean(true).writeBoolean(false).writeBoolean(true);
        assertEquals(expected, buf);

        assertArrayEquals(new int[]{1, -2, Integer.MAX_VALUE}, PacketHelper.readIntArray(buf));
        assertArrayEquals(new byte[]{7, -8}, PacketHelper.readByteArray(buf));
        assertArrayEquals(new float[]{1.5F, -2.5F}, PacketHelper.readFloatArray(buf));
        assertArrayEquals(new double[]{3.25D}, PacketHelper.readDoubleArray(buf));
        assertArrayEquals(new boolean[]{true, false, true}, PacketHelper.readBooleanArray(buf));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void validFixedLengthReads_unchanged() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(4).writeInt(5).writeInt(6).writeByte(9).writeByte(8);
        assertArrayEquals(new int[]{4, 5, 6}, PacketHelper.readIntArray(buf, 3));
        assertArrayEquals(new byte[]{9, 8}, PacketHelper.readByteArray(buf, 2));
        assertEquals(0, PacketHelper.readIntArray(Unpooled.buffer(), 0).length);
    }

    @Test
    void validListsMapsAndStrings_roundTrip() {
        ByteBuf buf = Unpooled.buffer();
        PacketHelper.sendListInt(buf, Arrays.asList(3, 1, 2));
        PacketHelper.sendListString(buf, Arrays.asList("abc", "", "艦娘"));
        Map<Integer, Integer> map = new HashMap<>();
        map.put(1, 10);
        map.put(2, 20);
        PacketHelper.sendMapInt(buf, map);
        PacketHelper.sendString(buf, "hello");
        PacketHelper.sendString(buf, null);

        assertEquals(Arrays.asList(3, 1, 2), PacketHelper.readListInt(buf));
        List<String> strs = PacketHelper.readListString(buf);
        assertEquals(Arrays.asList("abc", "", "艦娘"), strs);
        assertEquals(map, PacketHelper.readMapInt(buf));
        assertEquals("hello", PacketHelper.readString(buf));
        assertNull(PacketHelper.readString(buf));
        assertEquals(0, buf.readableBytes());
        assertEquals(Collections.emptyList(), PacketHelper.readListInt(bufWithLength(0, 0)));
    }

    // ---- per-call maximum ----

    @Test
    void perCallMaximum_rejectsEvenWhenBytesArePresent() {
        ByteBuf buf = Unpooled.buffer().writeZero(64);
        assertThrows(DecoderException.class, () -> PacketHelper.readIntArray(buf, 8, 4));
        assertThrows(DecoderException.class, () -> PacketHelper.readByteArray(buf, 33, 32));
        assertEquals(0, buf.readerIndex(), "nothing consumed on rejection");
        assertEquals(4, PacketHelper.readIntArray(buf, 4, 4).length);
    }

    @Test
    void defaultMaximum_rejectsAboveMaxArrayLength() {
        ByteBuf buf = Unpooled.buffer().writeZero(PacketHelper.MAX_ARRAY_LENGTH + 1);
        assertThrows(DecoderException.class, () -> PacketHelper.readByteArray(buf, PacketHelper.MAX_ARRAY_LENGTH + 1));
        assertEquals(PacketHelper.MAX_ARRAY_LENGTH, PacketHelper.readByteArray(buf, PacketHelper.MAX_ARRAY_LENGTH).length);
    }
}
