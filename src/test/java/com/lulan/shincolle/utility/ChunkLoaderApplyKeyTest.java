package com.lulan.shincolle.utility;

import net.minecraft.util.math.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #56: applyChunkLoader early-out key, and the chunk sets for modes 0, 1 and 2.
 */
class ChunkLoaderApplyKeyTest {

    @Test
    void chunkSetByMode() {
        assertTrue(BlockHelper.getChunksWithinRange(null, 4, -7, 0).isEmpty());

        Set<ChunkPos> one = BlockHelper.getChunksWithinRange(null, 4, -7, 1);
        assertEquals(1, one.size());
        assertTrue(one.contains(new ChunkPos(4, -7)));

        Set<ChunkPos> nine = BlockHelper.getChunksWithinRange(null, 4, -7, 2);
        assertEquals(9, nine.size());
        assertTrue(nine.contains(new ChunkPos(3, -8)));
        assertTrue(nine.contains(new ChunkPos(5, -6)));
    }

    @Test
    void noPreviousApplyNeverSkips() {
        assertFalse(ChunkLoaderApplyKey.isUnchanged(null, 0, 0, 2, new Object()));
    }

    @Test
    void skipsOnlyWhenChunkModeAndTicketAreIdentical() {
        Object ticket = new Object();
        ChunkLoaderApplyKey last = new ChunkLoaderApplyKey(3, 4, 2, ticket);

        assertTrue(ChunkLoaderApplyKey.isUnchanged(last, 3, 4, 2, ticket));
        assertFalse(ChunkLoaderApplyKey.isUnchanged(last, 4, 4, 2, ticket));   // moved chunk x
        assertFalse(ChunkLoaderApplyKey.isUnchanged(last, 3, 5, 2, ticket));   // moved chunk z
        assertFalse(ChunkLoaderApplyKey.isUnchanged(last, 3, 4, 1, ticket));   // mode changed
        assertFalse(ChunkLoaderApplyKey.isUnchanged(last, 3, 4, 2, new Object())); // new ticket
        assertFalse(ChunkLoaderApplyKey.isUnchanged(last, 3, 4, 2, null));     // ticket cleared
    }
}
