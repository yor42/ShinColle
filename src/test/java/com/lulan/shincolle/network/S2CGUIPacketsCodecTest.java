package com.lulan.shincolle.network;

import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.team.TeamData;
import com.lulan.shincolle.tileentity.TileEntityCrane;
import com.lulan.shincolle.tileentity.TileEntityDesk;
import com.lulan.shincolle.tileentity.TileEntitySmallShipyard;
import com.lulan.shincolle.tileentity.TileEntityVolCore;
import com.lulan.shincolle.tileentity.TileEntityWaypoint;
import com.lulan.shincolle.tileentity.TileMultiGrudgeHeavy;
import com.lulan.shincolle.utility.EntityIdGuard;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.lulan.shincolle.network.PacketTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #70: characterization of the S2CGUIPackets codec (current wire format, ADR-0004).
 * <p>
 * Covered (buildable without a world): TileSmallSY 0, TileLargeSY 1, TileDesk 2, TileVolCore 3,
 * TileWaypoint 4, TileCrane 5 (TileEntityCrane needs the IC2 API on the test runtime classpath, see
 * gradle/scripts/dependencies.gradle) (tile entities built with their public constructors, position set
 * with setPos), SyncPlayerProp 40, TargetClass 41, TeamData 42 (decode only), Formation 43,
 * ShipsAll 44, ShipsInTeam 45, ColledShip 46, ColledEquip 47, Misc 48, UnitName 49 (CapaTeitoku
 * built with its public constructor, player left null), FlagInitSID 80, FlagShowPlayerSkill 81,
 * ShipList 101, EntityItemList 102.
 * <p>
 * Not covered, with the reason:
 * <ul>
 * <li>SyncGUI_ShipInv 100: needs a BasicEntityShip (state arrays, inventory capability, optional
 * morph host); no world-free seam.</li>
 * <li>SyncPlayerProp_TeamData 42 encode: the writer ignores the message and reads
 * ServerProxy.getAllTeamWorldData() (global server state); only the decode side is pinned, from
 * hand-written bytes.</li>
 * <li>SyncPlayerProp_TargetClass 41 via the CapaTeitoku constructor: reads
 * ServerProxy.getPlayerTargetClass(uid), server state. The (type, List) constructor is covered
 * instead.</li>
 * </ul>
 */
class S2CGUIPacketsCodecTest {

    private static S2CGUIPackets decode(S2CGUIPackets msg) {
        return roundTrip(msg, S2CGUIPackets::new);
    }

    // ---------- flags and lists (no world objects at all) ----------

    @Test
    void flagInitSID_id80_and_showPlayerSkill_id81() {
        for (byte id : new byte[]{S2CGUIPackets.PID.FlagInitSID, S2CGUIPackets.PID.FlagShowPlayerSkill}) {
            for (boolean flag : new boolean[]{true, false}) {
                S2CGUIPackets msg = new S2CGUIPackets(id, 0, flag);
                assertArrayEquals(new byte[]{id, (byte) (flag ? 1 : 0)}, encode(msg));
                S2CGUIPackets d = decode(msg);
                assertEquals(id, (byte) field(d, "packetType"));
                assertEquals(flag, (boolean) field(d, "valueBoolean"));
            }
        }
    }

    @Test
    void intLists_ids101_46_47() {
        for (byte id : new byte[]{S2CGUIPackets.PID.SyncGUI_ShipList, S2CGUIPackets.PID.SyncPlayerProp_ColledShip,
                S2CGUIPackets.PID.SyncPlayerProp_ColledEquip}) {
            List<Integer> list = List.of(3, -4, 1000000);
            S2CGUIPackets msg = new S2CGUIPackets(id, list);

            ByteBuf expected = Unpooled.buffer();
            expected.writeByte(id).writeInt(3).writeInt(3).writeInt(-4).writeInt(1000000);
            assertArrayEquals(bytes(expected), encode(msg));

            S2CGUIPackets d = decode(msg);
            assertEquals(id, (byte) field(d, "packetType"));
            assertEquals(list, field(d, "dataList"));
        }
    }

    @Test
    void intLists_emptyAndNull() {
        S2CGUIPackets d = decode(new S2CGUIPackets(S2CGUIPackets.PID.SyncGUI_ShipList, List.of()));
        assertEquals(List.of(), field(d, "dataList"));

        // null list writes length -1; decoder produces an empty list (null and empty are not distinguishable)
        S2CGUIPackets nullMsg = new S2CGUIPackets(S2CGUIPackets.PID.SyncGUI_ShipList, (List) null);
        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(101).writeInt(-1);
        assertArrayEquals(bytes(expected), encode(nullMsg));
        assertEquals(List.of(), field(decode(nullMsg), "dataList"));
    }

    @Test
    void targetClass_id41_stringListBecomesHashMap() {
        S2CGUIPackets msg = new S2CGUIPackets(S2CGUIPackets.PID.SyncPlayerProp_TargetClass,
                List.of("EntityZombie", "EntitySkeleton"));
        S2CGUIPackets d = decode(msg);
        Map<Integer, String> map = field(d, "dataMap");
        assertEquals(2, map.size());
        assertEquals("EntityZombie", map.get("EntityZombie".hashCode()));
        assertEquals("EntitySkeleton", map.get("EntitySkeleton".hashCode()));
    }

    @Test
    void targetClass_id41_nullListWritesMinusOne() {
        S2CGUIPackets msg = new S2CGUIPackets(S2CGUIPackets.PID.SyncPlayerProp_TargetClass, (List) null);
        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(41).writeInt(-1);
        assertArrayEquals(bytes(expected), encode(msg));
        assertTrue(((Map<?, ?>) field(decode(msg), "dataMap")).isEmpty());
    }

    @Test
    void formation_id43_nineValuesAsBytes() {
        int[] fid = {0, 1, 2, 3, 4, 5, 0, 1, 2};
        S2CGUIPackets msg = new S2CGUIPackets(S2CGUIPackets.PID.SyncPlayerProp_Formation, fid);
        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(43);
        for (int v : fid) expected.writeByte(v);
        assertArrayEquals(bytes(expected), encode(msg));
        assertArrayEquals(new byte[]{0, 1, 2, 3, 4, 5, 0, 1, 2}, field(decode(msg), "valueByte1"));
    }

    @Disabled("NET-08 / #44: Formation writes one byte per element of valueInt1, but the reader always reads exactly 9 (shorter arrays underflow, longer leave bytes)")
    @Test
    void formation_id43_shortArrayRoundTrips_knownAsymmetry() {
        decode(new S2CGUIPackets(S2CGUIPackets.PID.SyncPlayerProp_Formation, 1, 2, 3));
    }

    @Test
    void entityItemList_id102() {
        float[] data = {1.5F, -2F, 0F, 1000.25F};
        S2CGUIPackets msg = new S2CGUIPackets(S2CGUIPackets.PID.SyncGUI_EntityItemList, data);
        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(102).writeInt(4);
        for (float f : data) expected.writeFloat(f);
        assertArrayEquals(bytes(expected), encode(msg));
        assertArrayEquals(data, field(decode(msg), "valueFloat1"));
    }

    @Test
    void entityItemList_id102_nullAndEmptyBothDecodeToEmptyArray() {
        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(102).writeInt(0);      // null array: the writer emits length 0 (not -1)
        S2CGUIPackets nullMsg = new S2CGUIPackets(S2CGUIPackets.PID.SyncGUI_EntityItemList, (float[]) null);
        assertArrayEquals(bytes(expected), encode(nullMsg));
        assertArrayEquals(new float[0], field(decode(nullMsg), "valueFloat1"));
        assertArrayEquals(new float[0], field(decode(
                new S2CGUIPackets(S2CGUIPackets.PID.SyncGUI_EntityItemList, new float[0])), "valueFloat1"));
    }

    @Test
    void teamData_id42_decodeFromHandWrittenBytes() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(42).writeInt(1);
        buf.writeInt(7);                                   // team id
        buf.writeInt(2).writeInt(3).writeInt(4);           // banned list
        buf.writeInt(0);                                   // ally list (empty)
        buf.writeInt(1);                                   // name flag
        ByteBufUtils.writeUTF8String(buf, "Fleet");
        buf.writeInt(-1);                                  // leader name: none
        S2CGUIPackets d = new S2CGUIPackets();
        d.fromBytes(buf);
        assertEquals(0, buf.readableBytes());
        Map<Integer, TeamData> map = field(d, "dataMap");
        TeamData t = map.get(7);
        assertNotNull(t);
        assertEquals("Fleet", t.getTeamName());
        assertEquals(List.of(3, 4), t.getTeamBannedList());
    }

    // ---------- CapaTeitoku based packets ----------

    private static CapaTeitoku capa() {
        bootstrap();
        CapaTeitoku c = new CapaTeitoku();
        c.setRingActive(true);
        c.setMarriageNum(4);
        c.setPlayerUID(321);
        c.setPlayerTeamCooldown(55);
        setPrivate(c, "hasTeam", true);
        setPrivate(c, "teamId", 2);
        int[][] sids = c.getSID();
        sids[2][0] = 10;
        sids[2][5] = 60;
        c.getSelectStateAllTeams()[2][1] = true;
        c.setFormatID(new int[]{0, 1, 2, 3, 4, 5, 0, 1, 2});
        return c;
    }

    private static void setPrivate(Object o, String name, Object value) {
        // tile setters send packets / touch the world, so the stored field is set directly
        try {
            Class<?> c = o.getClass();
            while (true) {
                try {
                    var f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    f.set(o, value);
                    return;
                } catch (NoSuchFieldException e) {
                    c = c.getSuperclass();
                    if (c == null) throw e;
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void misc_id48() {
        S2CGUIPackets msg = new S2CGUIPackets(capa(), S2CGUIPackets.PID.SyncPlayerProp_Misc);

        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(48).writeByte(1).writeByte(1).writeByte(2).writeInt(4).writeInt(321).writeInt(55);
        assertArrayEquals(bytes(expected), encode(msg));

        S2CGUIPackets d = decode(msg);
        assertArrayEquals(new int[]{1, 1, 2, 4, 321, 55}, field(d, "valueInt1"));
    }

    @Test
    void playerProp_id40() {
        S2CGUIPackets msg = new S2CGUIPackets(capa(), S2CGUIPackets.PID.SyncPlayerProp);
        S2CGUIPackets d = decode(msg);
        int[] v = field(d, "valueInt1");
        assertEquals(27, v.length);
        assertArrayEquals(new int[]{1, 1, 2, 4, 321, 55}, Arrays.copyOfRange(v, 0, 6));
        assertArrayEquals(new int[]{0, 1, 2, 3, 4, 5, 0, 1, 2}, Arrays.copyOfRange(v, 6, 15));
        // ship slot 0 of team 2: no entity (-1), uid 10, not selected
        assertEquals(-1, v[15]);
        assertEquals(10, v[16]);
        // slot 5: uid 60
        assertEquals(60, v[26]);
        boolean[] sel = field(d, "valueBoolean1");
        assertArrayEquals(new boolean[]{false, true, false, false, false, false}, sel);
    }

    @Test
    void unitName_id49_blankNamesBecomeSingleSpace() {
        CapaTeitoku c = capa();
        String[] names = new String[9];
        names[0] = "First";
        names[1] = "";
        c.setUnitName(names);
        S2CGUIPackets d = decode(new S2CGUIPackets(c, S2CGUIPackets.PID.SyncPlayerProp_UnitName));
        List<String> out = field(d, "valueStrs");
        assertEquals(9, out.size());
        assertEquals("First", out.get(0));
        assertEquals(" ", out.get(1));
        assertEquals(" ", out.get(2));
    }

    @Test
    void shipsAll_id44() {
        S2CGUIPackets d = decode(new S2CGUIPackets(capa(), S2CGUIPackets.PID.SyncPlayerProp_ShipsAll));
        int[] v = field(d, "valueInt1");
        assertEquals(2, v[0]);                                   // current team id
        assertArrayEquals(new int[]{0, 1, 2, 3, 4, 5, 0, 1, 2}, Arrays.copyOfRange(v, 1, 10));
        assertEquals(1, v[10]);                                  // has-data flag
        int[][] eids = field(d, "valueInt2");
        int[][] sids = field(d, "valueInt2a");
        boolean[][] sels = field(d, "valueBoolean2");
        assertEquals(-1, eids[2][0]);
        assertEquals(10, sids[2][0]);
        assertEquals(60, sids[2][5]);
        assertTrue(sels[2][1]);
        assertFalse(sels[0][0]);
    }

    @Test
    void shipsAll_id44_noDataFlag() {
        CapaTeitoku c = capa();
        setPrivate(c, "teamList", null);
        S2CGUIPackets d = decode(new S2CGUIPackets(c, S2CGUIPackets.PID.SyncPlayerProp_ShipsAll));
        int[] v = field(d, "valueInt1");
        assertEquals(-1, v[10]);
        assertNull(field(d, "valueInt2"));
    }

    @Test
    void shipsInTeam_id45() {
        S2CGUIPackets msg = new S2CGUIPackets(capa(), S2CGUIPackets.PID.SyncPlayerProp_ShipsInTeam, 2);
        S2CGUIPackets d = decode(msg);
        int[] v = field(d, "valueInt1");
        assertEquals(2, v[0]);                 // team id
        assertEquals(2, v[1]);                 // formation id of team 2
        assertEquals(1, v[2]);                 // has data
        assertEquals(-1, v[3]);                // slot 0 entity id (no ship)
        assertEquals(10, v[4]);                // slot 0 uid
        assertEquals(60, v[14]);               // slot 5 uid
        boolean[] sel = field(d, "valueBoolean1");
        assertTrue(sel[1]);
    }

    // ---------- tile entity based packets ----------

    private static <T extends TileEntity> T at(T tile, int x, int y, int z) {
        tile.setPos(new BlockPos(x, y, z));
        return tile;
    }

    @Test
    void tileSmallShipyard_id0() {
        bootstrap();
        TileEntitySmallShipyard t = at(new TileEntitySmallShipyard(), 1, 2, 3);
        t.setPowerConsumed(11);
        t.setPowerRemained(22);
        t.setPowerGoal(33);
        setPrivate(t, "playerUID", 44);
        S2CGUIPackets d = decode(new S2CGUIPackets(t, S2CGUIPackets.PID.TileSmallSY));
        assertArrayEquals(new int[]{1, 2, 3, 11, 22, 33, 44}, field(d, "valueInt1"));
    }

    @Test
    void tileLargeShipyard_id1() {
        bootstrap();
        TileMultiGrudgeHeavy t = at(new TileMultiGrudgeHeavy(), 4, 5, 6);
        t.setPowerConsumed(1);
        t.setPowerRemained(2);
        t.setPowerGoal(3);
        setPrivate(t, "playerUID", 9);
        S2CGUIPackets d = decode(new S2CGUIPackets(t, S2CGUIPackets.PID.TileLargeSY));
        int[] v = field(d, "valueInt1");
        assertEquals(15, v.length);
        assertArrayEquals(new int[]{4, 5, 6, 1, 2, 3}, Arrays.copyOfRange(v, 0, 6));
        assertEquals(9, v[14]);
    }

    @Test
    void tileDesk_id2() {
        bootstrap();
        TileEntityDesk t = at(new TileEntityDesk(), 7, 8, 9);
        S2CGUIPackets d = decode(new S2CGUIPackets(t, S2CGUIPackets.PID.TileDesk));
        int[] v = field(d, "valueInt1");
        assertEquals(7, v.length);
        assertArrayEquals(new int[]{7, 8, 9}, Arrays.copyOfRange(v, 0, 3));
    }

    @Test
    void tileVolCore_id3() {
        bootstrap();
        TileEntityVolCore t = at(new TileEntityVolCore(), -1, 70, 2);
        t.setPowerRemained(500);
        setPrivate(t, "playerUID", 12);
        S2CGUIPackets d = decode(new S2CGUIPackets(t, S2CGUIPackets.PID.TileVolCore));
        int[] v = field(d, "valueInt1");
        assertEquals(6, v.length);
        assertArrayEquals(new int[]{-1, 70, 2, 500}, Arrays.copyOfRange(v, 0, 4));
        assertEquals(12, v[5]);
    }

    @Test
    void tileWaypoint_id4() {
        bootstrap();
        TileEntityWaypoint t = at(new TileEntityWaypoint(), 1, 1, 1);
        setPrivate(t, "lastPos", new BlockPos(2, 2, 2));
        setPrivate(t, "nextPos", new BlockPos(3, 3, 3));
        setPrivate(t, "chestPos", new BlockPos(4, 4, 4));
        setPrivate(t, "playerUID", 77);
        S2CGUIPackets d = decode(new S2CGUIPackets(t, S2CGUIPackets.PID.TileWaypoint));
        int[] v = field(d, "valueInt1");
        assertEquals(14, v.length);
        assertArrayEquals(new int[]{1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 77}, Arrays.copyOfRange(v, 0, 13));
        // owner == null: pin whatever EntityIdGuard encodes for "absent"
        assertEquals(EntityIdGuard.encodeOptional(null, EntityPlayer::getEntityId), v[13]);
    }

    @Test
    void tileCrane_id5() {
        bootstrap();
        TileEntityCrane t = at(new TileEntityCrane(), 1, 2, 3);
        setPrivate(t, "lastPos", new BlockPos(4, 5, 6));
        setPrivate(t, "nextPos", new BlockPos(7, 8, 9));
        setPrivate(t, "chestPos", new BlockPos(10, 11, 12));
        setPrivate(t, "isActive", true);
        setPrivate(t, "checkMetadata", false);
        setPrivate(t, "checkOredict", true);
        setPrivate(t, "checkNbt", true);
        setPrivate(t, "craneMode", 7);
        setPrivate(t, "playerUID", 88);
        S2CGUIPackets msg = new S2CGUIPackets(t, S2CGUIPackets.PID.TileCrane);

        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(5);
        for (int v : new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}) expected.writeInt(v);
        expected.writeInt(-1);                                                   // no ship
        expected.writeInt(7).writeInt(88);                                       // craneMode, playerUID
        expected.writeInt(EntityIdGuard.encodeOptional(null, EntityPlayer::getEntityId)); // owner absent
        expected.writeByte(1).writeByte(0).writeByte(1).writeByte(1);            // fields 2, 3, 4, 8
        assertArrayEquals(bytes(expected), encode(msg));

        S2CGUIPackets d = decode(msg);
        int[] v = field(d, "valueInt1");
        assertEquals(16, v.length);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, -1, 7, 88},
                Arrays.copyOfRange(v, 0, 15));
        assertArrayEquals(new byte[]{1, 0, 1, 1}, field(d, "valueByte1"));
    }

    private static byte[] bytes(ByteBuf buf) {
        byte[] b = new byte[buf.readableBytes()];
        buf.readBytes(b);
        return b;
    }
}
