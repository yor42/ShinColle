package com.lulan.shincolle.server;

import com.lulan.shincolle.proxy.ServerProxy;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issues #31 / #58 (safe half): the ship snapshot is only stored, and the world data only marked dirty, when the
 * ship changed; the static loaded-NBT copy can be dropped. The serialized layout is not touched.
 */
class ShipWorldDataCacheTest {

    private ShinWorldData data;
    private HashMap<Integer, CacheDataShip> map;

    @BeforeEach
    void setUp() throws Exception {
        data = new ShinWorldData();
        ServerProxy.serverData = data;
        map = new HashMap<>();
        Field f = ServerProxy.class.getDeclaredField("mapShipID");
        f.setAccessible(true);
        f.set(null, map);
        data.setDirty(false);
    }

    @AfterEach
    void tearDown() throws Exception {
        ServerProxy.serverData = null;
        Field f = ServerProxy.class.getDeclaredField("mapShipID");
        f.setAccessible(true);
        f.set(null, null);
        ShinWorldData.clearCachedNbt();
    }

    private static CacheDataShip snapshot(int x, boolean dead, int hp) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("hp", hp);
        nbt.setString("id", "shincolle.ShipDD");
        return new CacheDataShip(7, 0, 2, dead, x, 64, 8, nbt);
    }

    @Test
    void firstSnapshotIsStoredAndMarksDirty() {
        assertTrue(ServerProxy.setShipWorldData(100, snapshot(1, false, 20)));
        assertTrue(data.isDirty());
        assertEquals(1, map.size());
    }

    @Test
    void identicalSnapshotDoesNotDirtyOrReplace() {
        CacheDataShip first = snapshot(1, false, 20);
        ServerProxy.setShipWorldData(100, first);
        data.setDirty(false);

        assertFalse(ServerProxy.setShipWorldData(100, snapshot(1, false, 20)));
        assertFalse(data.isDirty());
        assertSame(first, map.get(100));
    }

    @Test
    void changedNbtPositionOrDeathDirtiesAndReplaces() {
        ServerProxy.setShipWorldData(100, snapshot(1, false, 20));

        data.setDirty(false);
        assertTrue(ServerProxy.setShipWorldData(100, snapshot(1, false, 19)));
        assertTrue(data.isDirty());

        data.setDirty(false);
        assertTrue(ServerProxy.setShipWorldData(100, snapshot(2, false, 19)));
        assertTrue(data.isDirty());

        data.setDirty(false);
        CacheDataShip dead = snapshot(2, true, 19);
        assertTrue(ServerProxy.setShipWorldData(100, dead));
        assertTrue(data.isDirty());
        assertSame(dead, map.get(100));
    }

    @Test
    void invalidArgumentsAreIgnored() {
        assertFalse(ServerProxy.setShipWorldData(0, snapshot(1, false, 20)));
        assertFalse(ServerProxy.setShipWorldData(100, null));
        assertFalse(data.isDirty());
        assertTrue(map.isEmpty());
    }

    @Test
    void clearCachedNbtDropsStaticCopy() {
        ShinWorldData.nbtData = new NBTTagCompound();
        ShinWorldData.clearCachedNbt();
        assertNull(ShinWorldData.nbtData);
    }

    @Test
    void writtenLayoutIsUnchanged() {
        ServerProxy.setShipWorldData(100, snapshot(1, false, 20));
        NBTTagCompound out = data.writeToNBT(new NBTTagCompound());

        NBTTagList ships = out.getTagList(ShinWorldData.TAG_SHIPDATA, 10);
        assertEquals(1, ships.tagCount());
        NBTTagCompound s = ships.getCompoundTagAt(0);
        assertEquals(100, s.getInteger(ShinWorldData.TAG_ShipUID));
        assertEquals(7, s.getInteger(ShinWorldData.TAG_ShipEID));
        assertEquals(2, s.getInteger(ShinWorldData.TAG_ShipCID));
        for (String k : new String[]{ShinWorldData.TAG_ShipUID, ShinWorldData.TAG_ShipEID, ShinWorldData.TAG_ShipWID,
                ShinWorldData.TAG_ShipCID, ShinWorldData.TAG_ShipPOS, ShinWorldData.TAG_ShipDead, ShinWorldData.TAG_ShipNBT}) {
            assertTrue(s.hasKey(k), k);
        }
        assertEquals(7, s.getKeySet().size());
    }

    /**
     * Measurement only (no gameplay claim): compressed size of the written world data against ship count, using a
     * synthetic ~2 KB entity NBT per ship. Real ship NBT size must come from a dedicated-server run.
     */
    @Test
    void measureSerializedSizeAgainstShipCount() throws IOException {
        StringBuilder sb = new StringBuilder("SIZE-MEASURE (synthetic 2KB-ish ship NBT):");
        for (int n : new int[]{0, 10, 100, 1000}) {
            map.clear();
            for (int i = 1; i <= n; i++) ServerProxy.setShipWorldData(i, bigSnapshot(i));
            NBTTagCompound out = data.writeToNBT(new NBTTagCompound());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(out, bos);
            sb.append(" ships=").append(n).append(" gzBytes=").append(bos.size());
        }
        System.out.println(sb);
        assertTrue(true);
    }

    private static CacheDataShip bigSnapshot(int i) {
        NBTTagCompound nbt = new NBTTagCompound();
        for (int k = 0; k < 60; k++) nbt.setInteger("Key" + k, i * 31 + k);
        nbt.setString("id", "shincolle:entityshipdd");
        return new CacheDataShip(i, 0, 2, false, i, 64, i, nbt);
    }
}
