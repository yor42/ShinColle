package com.lulan.shincolle.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.lulan.shincolle.network.PacketTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #77: a packet whose fromBytes failed must be marked invalid and dropped by the handler
 * instead of being dispatched half-decoded. Valid packets keep decoding and dispatching as before.
 */
class DecodeFailureDropTest {

    /** length 3 but a single int of payload: PacketHelper.readIntArray throws DecoderException. */
    private static ByteBuf truncated(byte packetId) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(packetId).writeInt(3).writeInt(1);
        return buf;
    }

    private static void invokeHandle(Class<?> type, Object msg) throws Throwable {
        Method m = type.getDeclaredMethod("handle", type, MessageContext.class);
        m.setAccessible(true);
        try {
            // null context: a dispatched packet dereferences it, a dropped one never touches it
            m.invoke(null, msg, null);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test
    void c2sInput_failedDecode_isDropped() {
        C2SInputPackets msg = new C2SInputPackets();
        msg.fromBytes(truncated(C2SInputPackets.PID.MountMove));
        assertDoesNotThrow(() -> invokeHandle(C2SInputPackets.class, msg));
    }

    @Test
    void s2cReact_failedDecode_isDropped() {
        S2CReactPackets msg = new S2CReactPackets();
        msg.fromBytes(truncated(S2CReactPackets.PID.CmdShipInfo));
        assertDoesNotThrow(() -> invokeHandle(S2CReactPackets.class, msg));
    }

    @Test
    void validPackets_stillDecodeAndAreNotMarkedFailed() {
        C2SInputPackets c2s = roundTrip(new C2SInputPackets(C2SInputPackets.PID.MountMove, 1, 2), C2SInputPackets::new);
        assertArrayEquals(new int[]{1, 2}, field(c2s, "value3"));
        assertFalse((boolean) field(c2s, "decodeFailed"));

        S2CReactPackets react = roundTrip(new S2CReactPackets(S2CReactPackets.PID.CmdChOwner, 11, 22), S2CReactPackets::new);
        assertArrayEquals(new int[]{11, 22}, field(react, "valueInt"));
        assertFalse((boolean) field(react, "decodeFailed"));
    }

    @Test
    void failedDecode_setsFlag() {
        C2SInputPackets c2s = new C2SInputPackets();
        c2s.fromBytes(truncated(C2SInputPackets.PID.MountMove));
        assertTrue((boolean) field(c2s, "decodeFailed"));
        S2CReactPackets react = new S2CReactPackets();
        react.fromBytes(truncated(S2CReactPackets.PID.CmdShipInfo));
        assertTrue((boolean) field(react, "decodeFailed"));
    }
}
