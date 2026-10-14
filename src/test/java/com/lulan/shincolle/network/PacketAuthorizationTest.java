package com.lulan.shincolle.network;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #2 (NET-02): CmdChOwner / CmdShipAttr must only be accepted from a server-side OP.
 * Players are plain strings here; the OP query is injected.
 */
class PacketAuthorizationTest {

    private static final Set<String> OPS = Set.of("admin");

    @Test
    void nonOpSenderIsRejected() {
        assertFalse(PacketAuthorization.mayEditShips("player", OPS::contains));
    }

    @Test
    void opSenderIsAccepted() {
        assertTrue(PacketAuthorization.mayEditShips("admin", OPS::contains));
    }

    @Test
    void missingSenderIsRejected() {
        assertFalse(PacketAuthorization.mayEditShips(null, s -> true));
    }

    // ---- issue #29 batch 1: mayAct / withinReach ----

    /** owner 10 has ally 20 (directional): 20 may act on 10's ships, not the other way round. */
    private static boolean ally(int owner, int actor) {
        return owner == 10 && actor == 20;
    }

    @Test
    void ownerMayAct() {
        assertTrue(PacketAuthorization.mayAct(10, 10, false, PacketAuthorizationTest::ally));
    }

    @Test
    void allyMayAct() {
        assertTrue(PacketAuthorization.mayAct(20, 10, false, PacketAuthorizationTest::ally));
    }

    @Test
    void wrongDirectionAllyIsRejected() {
        assertFalse(PacketAuthorization.mayAct(10, 20, false, PacketAuthorizationTest::ally));
    }

    @Test
    void strangerIsRejected() {
        assertFalse(PacketAuthorization.mayAct(30, 10, false, PacketAuthorizationTest::ally));
    }

    @Test
    void serverOpMayActOnAnyShip() {
        assertTrue(PacketAuthorization.mayAct(30, 10, true, (o, a) -> false));
    }

    @Test
    void creativeIsNotOp() {
        // creative players reach mayAct with isServerOp=false (isServerOp(EntityPlayerMP) ignores creative)
        assertFalse(PacketAuthorization.mayAct(30, 10, false, (o, a) -> false));
    }

    @Test
    void ownerlessShipAcceptsOpOnly() {
        assertFalse(PacketAuthorization.mayAct(10, -1, false, (o, a) -> true));
        assertTrue(PacketAuthorization.mayAct(10, -1, true, (o, a) -> false));
    }

    @Test
    void mobOwnedShipAcceptsOpOnly() {
        assertFalse(PacketAuthorization.mayAct(10, -5, false, (o, a) -> true));
        assertTrue(PacketAuthorization.mayAct(10, -5, true, (o, a) -> false));
    }

    @Test
    void actorWithoutPlayerUidIsRejectedEvenIfOp() {
        assertFalse(PacketAuthorization.mayAct(0, 10, true, (o, a) -> true));
        assertFalse(PacketAuthorization.mayAct(-1, -1, true, (o, a) -> true));
    }

    @Test
    void ownerOnlyVariantExcludesAllies() {
        assertTrue(PacketAuthorization.mayActAsOwner(10, 10, false));
        assertFalse(PacketAuthorization.mayActAsOwner(20, 10, false));
        assertTrue(PacketAuthorization.mayActAsOwner(30, 10, true));
        assertFalse(PacketAuthorization.mayActAsOwner(10, -1, false));
    }

    @Test
    void reachBoundary() {
        assertTrue(PacketAuthorization.withinReach(64.0, true));
        assertFalse(PacketAuthorization.withinReach(64.01, true));
        assertFalse(PacketAuthorization.withinReach(1.0, false));
    }
}
