package com.lulan.shincolle.utility;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Issue #55: matching rule of getEntityPlayerByUID. The entity found by id is accepted only if it
 * is a player AND is listed in the world's player list. Stand-in types replace EntityPlayer so the
 * rule runs without a world.
 */
class EntityHelperPlayerLookupTest {

    private static final class Player {
    }

    private static final class Mob {
    }

    @Test
    void listedPlayerIsReturned() {
        Player a = new Player();
        Player b = new Player();
        List<Player> listed = Arrays.asList(a, null, b);
        assertSame(b, EntityHelper.pickListedPlayer(b, Player.class, listed));
    }

    @Test
    void nullLookupGivesNull() {
        assertNull(EntityHelper.pickListedPlayer(null, Player.class, Arrays.asList(new Player())));
    }

    @Test
    void nonPlayerEntityIsRejected() {
        Mob mob = new Mob();
        assertNull(EntityHelper.pickListedPlayer(mob, Player.class, Arrays.asList(new Player())));
    }

    @Test
    void playerNotInThisWorldsListIsRejected() {
        Player stale = new Player();
        assertNull(EntityHelper.pickListedPlayer(stale, Player.class, Arrays.asList(new Player())));
    }
}
