package com.lulan.shincolle.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #9 (NET-13): the first entity on a fresh server (usually the first player) has entity id 0,
 * so lookups by Minecraft entity id must accept 0 and reject only negatives.
 */
class EntityIdGuardTest {

    @Test
    void entityIdZeroIsLookupable() {
        assertTrue(EntityIdGuard.isLookupable(0));
    }

    @Test
    void positiveEntityIdIsLookupable() {
        assertTrue(EntityIdGuard.isLookupable(1));
        assertTrue(EntityIdGuard.isLookupable(4096));
    }

    @Test
    void negativeEntityIdIsRejected() {
        assertFalse(EntityIdGuard.isLookupable(-1));
        assertFalse(EntityIdGuard.isLookupable(Integer.MIN_VALUE));
    }

    @Test
    void absentEntityEncodesToNoneAndIsRejected() {
        int encoded = EntityIdGuard.encodeOptional(null, e -> 0);
        assertEquals(EntityIdGuard.NO_ENTITY, encoded);
        assertEquals(-1, encoded);
        assertFalse(EntityIdGuard.isLookupable(encoded));
    }

    @Test
    void presentEntityWithIdZeroEncodesToZeroAndIsLookupable() {
        int encoded = EntityIdGuard.encodeOptional("firstPlayer", e -> 0);
        assertEquals(0, encoded);
        assertTrue(EntityIdGuard.isLookupable(encoded));
    }
}
