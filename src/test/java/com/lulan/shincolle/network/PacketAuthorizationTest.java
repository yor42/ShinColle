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
}
