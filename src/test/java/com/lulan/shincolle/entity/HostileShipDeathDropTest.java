package com.lulan.shincolle.entity;

import com.lulan.shincolle.entity.battleship.EntityBattleshipNGTMob;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.utility.DeathDropRule;
import net.minecraft.init.Bootstrap;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #75: BasicEntityShipHostile.onDeathUpdate (line ~1500) completes the death animation with
 * {@code deathTime == ConfigHandler.deathMaxTick}, while the player-ship version was fixed to
 * {@code >=} through {@link DeathDropRule} (issue #13).
 * <p>
 * The hostile ship is allocated without running any constructor and given a world stand-in that is
 * a server-side WorldServer shell (isRemote == false); only fields touched by the Death_Time=0 path
 * are used. The default (400) completion branch is NOT exercised because it needs a real world
 * (GameRules, particles, entity spawning): minimal seam would be extracting the completion
 * decision into DeathDropRule.isDeathComplete (already exists) and calling it from the hostile class.
 */
class HostileShipDeathDropTest {

    private int savedDeathMaxTick;

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @BeforeEach
    void saveConfig() {
        savedDeathMaxTick = ConfigHandler.deathMaxTick;
    }

    @AfterEach
    void restoreConfig() {
        ConfigHandler.deathMaxTick = savedDeathMaxTick;
    }

    private static Object allocate(Class<?> type) throws ReflectiveOperationException {
        Field uf = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        uf.setAccessible(true);
        Object unsafe = uf.get(null);
        return unsafe.getClass().getMethod("allocateInstance", Class.class).invoke(unsafe, type);
    }

    private static EntityBattleshipNGTMob hostileShipReadyToDie() {
        try {
            WorldProvider provider = (WorldProvider) allocate(WorldProviderSurface.class);
            World world = (World) allocate(WorldServer.class);
            Field pf = World.class.getField("provider");
            pf.setAccessible(true);
            pf.set(world, provider);

            EntityBattleshipNGTMob ship = (EntityBattleshipNGTMob) allocate(EntityBattleshipNGTMob.class);
            ship.world = world;
            ship.canDrop = true;
            ship.deathTime = 0;
            return ship;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void currentlyRemovesTheShipWithoutDroppingTheEggWhenDeathTimeIsZero() {
        ConfigHandler.deathMaxTick = 0;
        EntityBattleshipNGTMob ship = hostileShipReadyToDie();

        ship.onDeathUpdate();   // deathTime 0 -> 1; 1 == 0 is false

        assertEquals(1, ship.deathTime);
        // The rule used for player ships says the drop is due on this very tick...
        assertTrue(DeathDropRule.shouldDropEgg(ship.deathTime, ConfigHandler.deathMaxTick, true));
        // ...but the hostile ship takes the "past the end" branch: setDead() with the drop flag untouched.
        // Suspected defect (#75): the egg and the XP orbs are skipped. A future DeathDropRule fix flips both.
        assertTrue(ship.isDead, "ship is removed by the deathTime > deathMaxTick branch");
        assertTrue(ship.canDrop, "canDrop is still set: the egg branch never ran");
    }

    @Test
    void doesNotFinishBeforeTheConfiguredTickWhenDeathTimeIsLarge() {
        ConfigHandler.deathMaxTick = 400;
        EntityBattleshipNGTMob ship = hostileShipReadyToDie();
        ship.deathTime = 10;

        ship.onDeathUpdate();

        assertEquals(11, ship.deathTime);
        assertFalse(ship.isDead);
        assertTrue(ship.canDrop);
    }
}
