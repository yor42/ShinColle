package com.lulan.shincolle.network;

import com.lulan.shincolle.handler.ConfigHandler;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.IntFunction;

import static com.lulan.shincolle.network.PacketTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #26: the client-side S2CEntitySync handler must ignore packets that do not match the
 * client world (missing world, unknown entity id, wrong entity type, missing payload) instead of
 * throwing on the client thread, and the local ringAbility must survive a server config sync.
 * <p>
 * The handler body is driven through the package-private seam handle(msg, world, lookup); no
 * wire-format involvement.
 */
class S2CEntitySyncHandlerTest {

    private int[] savedRingAbility;

    @BeforeEach
    void saveConfig() {
        savedRingAbility = ConfigHandler.ringAbility;
    }

    @AfterEach
    void restoreConfig() {
        S2CEntitySync.restoreRingAbility();
        ConfigHandler.ringAbility = savedRingAbility;
    }

    private static S2CEntitySync msg(byte type, int entityId, int[] valueInt1) {
        S2CEntitySync m = new S2CEntitySync();
        setField(m, "packetType", type);
        setField(m, "entityID", entityId);
        setField(m, "valueInt1", valueInt1);
        return m;
    }

    private static final IntFunction<Entity> NO_ENTITY = id -> null;

    @Test
    void nullWorld_isIgnored() {
        World world = null;
        assertDoesNotThrow(() -> S2CEntitySync.handle(
                msg(S2CEntitySync.PID.SyncEntity_PlayerUID, -1, new int[]{1, 2, 3, 4}), world, NO_ENTITY));
        assertDoesNotThrow(() -> S2CEntitySync.handle(
                msg(S2CEntitySync.PID.SyncEntity_Motion, 5, null), world, NO_ENTITY));
    }

    @Test
    void unknownEntityId_isIgnoredForEveryEntityPacket() {
        World world = fakeWorld(0);
        byte[] types = {
                S2CEntitySync.PID.SyncShip_AllMisc, S2CEntitySync.PID.SyncShip_Attrs, S2CEntitySync.PID.SyncShip_Emo,
                S2CEntitySync.PID.SyncShip_Flag, S2CEntitySync.PID.SyncShip_Minor, S2CEntitySync.PID.SyncShip_Riders,
                S2CEntitySync.PID.SyncShip_Scale, S2CEntitySync.PID.SyncShip_Formation, S2CEntitySync.PID.SyncShip_Buffmap,
                S2CEntitySync.PID.SyncShip_Timer, S2CEntitySync.PID.SyncShip_Guard, S2CEntitySync.PID.SyncShip_ID,
                S2CEntitySync.PID.SyncShip_UnitName, S2CEntitySync.PID.SyncShip_PlayerSkillTimer,
                S2CEntitySync.PID.SyncEntity_Emo, S2CEntitySync.PID.SyncEntity_PosRot, S2CEntitySync.PID.SyncEntity_Rot,
                S2CEntitySync.PID.SyncEntity_Motion, S2CEntitySync.PID.SyncEntity_Host,
                S2CEntitySync.PID.SyncEntity_CustomData, S2CEntitySync.PID.SyncProjectile};
        for (byte type : types) {
            assertDoesNotThrow(() -> S2CEntitySync.handle(msg(type, 4242, new int[]{0, 0, 0, 0, 0, 0}), world, NO_ENTITY),
                    "packet type " + type);
        }
    }

    @Test
    void wrongEntityType_isIgnored() {
        World world = fakeWorld(0);
        Entity plain = fakeEntity(7, 0); // neither IShipEmotion nor EntityLivingBase
        IntFunction<Entity> lookup = id -> plain;
        // emotion packet carrying only the 5 emotion ints (index 5 does not exist): the fallback
        // morph-player lookup used to read valueInt1[5] unconditionally
        assertDoesNotThrow(() -> S2CEntitySync.handle(
                msg(S2CEntitySync.PID.SyncEntity_Emo, 7, new int[]{0, 0, 0, 0, 0}), world, lookup));
        assertDoesNotThrow(() -> S2CEntitySync.handle(
                msg(S2CEntitySync.PID.SyncShip_Emo, 7, null), world, lookup));
        // ship packets on a non-ship entity
        assertDoesNotThrow(() -> S2CEntitySync.handle(
                msg(S2CEntitySync.PID.SyncShip_Minor, 7, new int[30]), world, lookup));
        // host packet whose target is not a fishing hook
        S2CEntitySync host = msg(S2CEntitySync.PID.SyncEntity_Host, 7, null);
        setField(host, "valueInt", 9);
        assertDoesNotThrow(() -> S2CEntitySync.handle(host, world, id -> plain));
    }

    @Test
    void playerUidPacket_unknownEntityOrWrongType_isIgnored() {
        World world = fakeWorld(0);
        // host is an entity that does not exist
        assertDoesNotThrow(() -> S2CEntitySync.handle(
                msg(S2CEntitySync.PID.SyncEntity_PlayerUID, 0, new int[]{99, 0, -1, 5}), world, NO_ENTITY));
        // host is an entity of the wrong type
        Entity plain = fakeEntity(99, 0);
        assertDoesNotThrow(() -> S2CEntitySync.handle(
                msg(S2CEntitySync.PID.SyncEntity_PlayerUID, 0, new int[]{99, 0, -1, 5}), world, id -> plain));
    }

    @Test
    void configPacketWithoutPayload_isIgnoredAndKeepsLocalValue() {
        ConfigHandler.ringAbility = new int[]{1, 2, 3, 4, 5};
        World world = fakeWorld(0);
        assertDoesNotThrow(() -> S2CEntitySync.handle(
                msg(S2CEntitySync.PID.SyncSystem_Config, -1, null), world, NO_ENTITY));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, ConfigHandler.ringAbility);
    }

    @Test
    void configSync_thenRestore_returnsLocalRingAbility() {
        ConfigHandler.ringAbility = new int[]{1, 2, 3, 4, 5};
        World world = fakeWorld(0);

        S2CEntitySync.handle(msg(S2CEntitySync.PID.SyncSystem_Config, -1, new int[]{9, 8, 7, 6, 5}), world, NO_ENTITY);
        assertArrayEquals(new int[]{9, 8, 7, 6, 5}, ConfigHandler.ringAbility, "server values applied");

        // a second sync (server re-send) must not overwrite the saved local snapshot
        S2CEntitySync.handle(msg(S2CEntitySync.PID.SyncSystem_Config, -1, new int[]{4, 4, 4, 4, 4}), world, NO_ENTITY);
        assertArrayEquals(new int[]{4, 4, 4, 4, 4}, ConfigHandler.ringAbility);

        S2CEntitySync.restoreRingAbility();
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, ConfigHandler.ringAbility, "local value restored");

        // restore without a preceding sync leaves the value alone
        ConfigHandler.ringAbility = new int[]{7, 7, 7, 7, 7};
        S2CEntitySync.restoreRingAbility();
        assertArrayEquals(new int[]{7, 7, 7, 7, 7}, ConfigHandler.ringAbility);
    }

    @Test
    void configSync_withDifferentLength_stillRestores() {
        ConfigHandler.ringAbility = new int[]{1, 2, 3, 4, 5};
        S2CEntitySync.handle(msg(S2CEntitySync.PID.SyncSystem_Config, -1, new int[]{9, 9}), fakeWorld(0), NO_ENTITY);
        assertArrayEquals(new int[]{9, 9}, ConfigHandler.ringAbility);
        S2CEntitySync.restoreRingAbility();
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, ConfigHandler.ringAbility);
    }
}
