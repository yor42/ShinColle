package com.lulan.shincolle.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #29 batch 2: tile / container authorization, pairing and array guards (pure predicates only).
 */
class PacketAuthorizationBatch2Test {

    /** owner 10 has ally 20 (directional). */
    private static boolean ally(int owner, int actor) {
        return owner == 10 && actor == 20;
    }

    private static boolean tile(int actor, int owner, boolean op, double distSq, boolean loaded) {
        return PacketAuthorization.mayUseOwnedTile(actor, owner, op, PacketAuthorizationBatch2Test::ally, distSq, loaded);
    }

    @Test
    void ownerAllyAndOpMayUseTileInReach() {
        assertTrue(tile(10, 10, false, 4, true));
        assertTrue(tile(20, 10, false, 4, true));
        assertTrue(tile(30, 10, true, 4, true));
    }

    @Test
    void strangerIsRejectedEvenInReach() {
        assertFalse(tile(30, 10, false, 4, true));
        assertFalse(tile(10, 20, false, 4, true)); // ally is directional
    }

    @Test
    void creativeIsNotAnOpSoStrangerStaysRejected() {
        // callers pass isServerOp (canSendCommands), never isCreativeMode
        assertFalse(tile(30, 10, false, 4, true));
    }

    @Test
    void reachBoundaryIsInclusiveAtEightBlocks() {
        assertTrue(tile(10, 10, false, 64.0, true));
        assertFalse(tile(10, 10, false, 64.01, true));
        assertFalse(tile(30, 10, true, 100.0, true));
    }

    @Test
    void unloadedChunkIsRejectedEvenForOp() {
        assertFalse(tile(10, 10, false, 1, false));
        assertFalse(tile(30, 10, true, 1, false));
    }

    @Test
    void ownerlessAndMobOwnedTilesAcceptOpOnly() {
        assertFalse(tile(10, -1, false, 1, true));
        assertFalse(tile(10, 0, false, 1, true));
        assertFalse(tile(10, -5, false, 1, true));
        assertTrue(tile(30, -1, true, 1, true));
    }

    @Test
    void invalidActorIsRejected() {
        assertFalse(tile(0, 0, true, 1, true));
        assertFalse(tile(-1, 10, true, 1, true));
    }

    private static boolean pair(int actor, int owner, boolean op, double dA, boolean lA, double dB, boolean lB) {
        return PacketAuthorization.mayPairTiles(actor, owner, op, dA, lA, dB, lB);
    }

    @Test
    void ownerAndOpMayPairWhenOneEndpointIsInReach() {
        assertTrue(pair(10, 10, false, 4, true, 900, true));
        assertTrue(pair(10, 10, false, 900, true, 4, true));
        assertTrue(pair(30, 10, true, 4, true, 4, true));
    }

    @Test
    void allyMayNotPair() {
        assertFalse(pair(20, 10, false, 4, true, 4, true));
    }

    @Test
    void pairingRejectsStrangerSpoofedUidAndOwnerless() {
        assertFalse(pair(30, 10, false, 4, true, 4, true));
        assertFalse(pair(0, 0, false, 4, true, 4, true));
        assertFalse(pair(10, -1, false, 4, true, 4, true));
        assertTrue(pair(30, -1, true, 4, true, 4, true));
    }

    @Test
    void pairingRejectsNoEndpointInReachOrUnloadedChunk() {
        assertFalse(pair(10, 10, false, 100, true, 100, true));
        assertFalse(pair(30, 10, true, 100, true, 100, true));
        assertFalse(pair(10, 10, false, 4, false, 4, true));
        assertFalse(pair(10, 10, false, 4, true, 4, false));
    }

    @Test
    void arrayGuardsDropShortOrMissingPayloads() {
        assertFalse(PacketAuthorization.hasLength((int[]) null, 1));
        assertFalse(PacketAuthorization.hasLength(new int[6], 7));
        assertTrue(PacketAuthorization.hasLength(new int[7], 7));
        assertTrue(PacketAuthorization.hasLength(new int[9], 7));
        assertFalse(PacketAuthorization.hasLength((byte[]) null, 1));
        assertFalse(PacketAuthorization.hasLength(new byte[3], 4));
        assertTrue(PacketAuthorization.hasLength(new byte[4], 4));
    }
}
