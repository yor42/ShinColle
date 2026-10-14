package com.lulan.shincolle.network;

import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.proxy.ServerProxy;
import com.lulan.shincolle.server.CacheDataShip;
import com.lulan.shincolle.team.TeamData;
import com.lulan.shincolle.utility.PacketHelper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static com.lulan.shincolle.network.PacketTestSupport.encode;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #35: state read by S2C packets is copied in the constructor (server thread), so
 * toBytes (netty thread) only copies bytes and later changes to the source cannot leak in.
 * Wire layout is unchanged; expected bytes are written by hand with the same primitives.
 */
class S2CSnapshotTest {

    private static void setStatic(String name, Object value) throws Exception {
        Field f = ServerProxy.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    private static Object getStatic(String name) throws Exception {
        Field f = ServerProxy.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }

    private static CapaTeitoku capa() {
        PacketTestSupport.bootstrap();
        return new CapaTeitoku();
    }

    // ---------- SyncPlayerProp_TeamData (S2CGUIPackets) ----------

    private static TeamData team(int id, String name, String leader, Integer... ban) {
        TeamData t = new TeamData();
        t.setTeamID(id);
        t.setTeamName(name);
        t.setTeamLeaderName(leader);
        t.getTeamBannedList().addAll(Arrays.asList(ban));
        return t;
    }

    private static byte[] expectedTeam(int id, String name, String leader, int[] ban) {
        ByteBuf b = Unpooled.buffer();
        b.writeByte(S2CGUIPackets.PID.SyncPlayerProp_TeamData);
        b.writeInt(1);
        b.writeInt(id);
        b.writeInt(ban.length);
        for (int i : ban) b.writeInt(i);
        b.writeInt(0); // empty ally list
        PacketHelper.sendString(b, name);
        PacketHelper.sendString(b, leader);
        byte[] out = new byte[b.readableBytes()];
        b.readBytes(out);
        return out;
    }

    @Test
    void teamData_encodesStateAtConstruction_evenIfMutatedAfterwards() throws Exception {
        Object saved = getStatic("mapTeamID");
        try {
            HashMap<Integer, TeamData> map = new HashMap<>();
            TeamData t = team(7, "Alpha", "Lead", 3, 4);
            map.put(7, t);
            setStatic("mapTeamID", map);

            S2CGUIPackets msg = new S2CGUIPackets(capa(), S2CGUIPackets.PID.SyncPlayerProp_TeamData);

            // mutate the live data after construction
            t.setTeamName("Renamed");
            t.getTeamBannedList().add(99);
            map.put(8, team(8, "Other", "X"));
            map.remove(7);

            assertArrayEquals(expectedTeam(7, "Alpha", "Lead", new int[]{3, 4}), encode(msg));
            // encoding twice yields the same bytes (no state consumed)
            assertArrayEquals(encode(msg), encode(msg));
        } finally {
            setStatic("mapTeamID", saved);
        }
    }

    @Test
    void teamData_nullMapWritesMinusOne() throws Exception {
        Object saved = getStatic("mapTeamID");
        try {
            setStatic("mapTeamID", null);
            S2CGUIPackets msg = new S2CGUIPackets(capa(), S2CGUIPackets.PID.SyncPlayerProp_TeamData);
            ByteBuf b = Unpooled.buffer();
            b.writeByte(S2CGUIPackets.PID.SyncPlayerProp_TeamData);
            b.writeInt(-1);
            byte[] exp = new byte[b.readableBytes()];
            b.readBytes(exp);
            assertArrayEquals(exp, encode(msg));
        } finally {
            setStatic("mapTeamID", saved);
        }
    }

    // ---------- CmdShipList (S2CReactPackets) ----------

    @Test
    void shipList_encodesStateAtConstruction_evenIfMapChangesAfterwards() throws Exception {
        Object saved = getStatic("mapShipID");
        try {
            HashMap<Integer, CacheDataShip> map = new HashMap<>();
            setStatic("mapShipID", map);

            S2CReactPackets msg = new S2CReactPackets(S2CReactPackets.PID.CmdShipList, 0, 1);

            // an entry appearing after construction must not show up (no entity lookup needed to prove it)
            map.put(5, new CacheDataShip(1, 0, 2, false, 0, 0, 0, null));

            // empty list, page 1: type, maxPage 1, page 1, count 0 (same primitives as processSendShipList)
            ByteBuf b = Unpooled.buffer();
            b.writeByte(S2CReactPackets.PID.CmdShipList);
            b.writeInt(1);
            b.writeInt(1);
            b.writeInt(0);
            byte[] exp = new byte[b.readableBytes()];
            b.readBytes(exp);
            assertArrayEquals(exp, encode(msg));
        } finally {
            setStatic("mapShipID", saved);
        }
    }

    // ---------- SyncShip_Attrs (S2CEntitySync) ----------

    @Test
    void attrs_bodyIsByteIdenticalToTheLegacyLayout_andCopiedByValue() {
        float[] raw = new float[5];
        for (int i = 0; i < raw.length; i++) raw[i] = i + 0.5F;
        float[] formation = {1F, 2F};

        // flags: Bonus off, Raw on, Equip off, Morale off, Potion off, Formation on, Buffed off
        boolean[] flags = {false, true, false, false, false, true, false};
        byte[] got = S2CEntitySync.writeAttrs(flags, null, raw, null, null, null, formation, null, 0.25F);

        ByteBuf exp = Unpooled.buffer();
        for (boolean f : flags) exp.writeBoolean(f);
        PacketHelper.sendArrayFloat(exp, raw);
        PacketHelper.sendArrayFloat(exp, formation);
        exp.writeFloat(0.25F);
        byte[] expected = new byte[exp.readableBytes()];
        exp.readBytes(expected);
        assertArrayEquals(expected, got);

        // mutating the source arrays afterwards does not change the captured bytes
        raw[0] = 123F;
        formation[1] = 456F;
        assertArrayEquals(expected, got);
    }

    @Test
    void attrs_allFlagsOff_writesSevenFalseBooleans() {
        byte[] got = S2CEntitySync.writeAttrs(new boolean[7], null, null, null, null, null, null, null, 0F);
        assertEquals(7, got.length);
        for (byte b : got) assertEquals(0, b);
    }
}
