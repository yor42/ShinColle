package com.lulan.shincolle.ai;

import net.minecraft.util.math.AxisAlignedBB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #3 (AI-01/AI-02): guard search box and stationary attack cadence.
 */
class EntityAIShipGuardingTest {

    @Test
    void searchBoxIsShipBoxExtendedSymmetrically() {
        AxisAlignedBB ship = new AxisAlignedBB(0, 0, 0, 1, 2, 1);
        double range = 10D;
        AxisAlignedBB box = EntityAIShipGuarding.getSearchBox(ship, range);

        assertEquals(-9.0, box.minX, 1e-9);
        assertEquals(10.0, box.maxX, 1e-9);
        assertEquals(-6.0, box.minY, 1e-9);
        assertEquals(8.0, box.maxY, 1e-9);
        assertEquals(-9.0, box.minZ, 1e-9);
        assertEquals(10.0, box.maxZ, 1e-9);
    }

    @Test
    void targetAtHalfRangeIsInsideSearchBox() {
        AxisAlignedBB ship = new AxisAlignedBB(0, 0, 0, 1, 2, 1);
        double range = 10D;
        AxisAlignedBB box = EntityAIShipGuarding.getSearchBox(ship, range);
        AxisAlignedBB target = new AxisAlignedBB(5.5, 0, 0, 6.5, 2, 1); // 0.5 x range away in x
        assertTrue(box.intersects(target));
    }

    /** simulate the delay model: polled every pollInterval ticks, returns shots after totalTicks */
    private static int shots(int totalTicks, int pollInterval, int maxDelay) {
        int delay = maxDelay;
        int last = Integer.MIN_VALUE;
        int shots = 0;
        for (int tick = 1; tick <= totalTicks; tick++) {
            if (tick % pollInterval != 0) continue;
            delay -= EntityAIShipGuarding.elapsedTicks(last, tick);
            last = tick;
            if (delay <= 0) {
                shots++;
                delay = maxDelay;
            }
        }
        return shots;
    }

    @Test
    void perTickPollGivesFloorNOverMaxDelayShots() {
        // first poll counts as one tick and the delay starts at maxDelay: shot k at tick k*maxDelay
        assertEquals(1000 / 20, shots(1000, 1, 20));
    }

    @Test
    void sparsePollingKeepsTheSameCadence() {
        // polled every 3 ticks (task scheduler tick rate): at most one poll of granularity lost per shot
        int perTick = shots(960, 1, 20);
        int sparse = shots(960, 3, 20);
        assertTrue(Math.abs(perTick - sparse) <= perTick / 6 + 1, perTick + " vs " + sparse);
        assertTrue(sparse > 960 / 32, "sparse polling must beat the old once-per-32-ticks cadence");
    }

    @Test
    void sameTickIsCountedOnce() {
        assertEquals(0, EntityAIShipGuarding.elapsedTicks(100, 100));
    }

    // ---- issue #37: shared fire condition of the stationary and the moving path ----

    @Test
    void canFireRequiresActiveAiAndGuardType() {
        assertTrue(EntityAIShipGuarding.canFire(false, 1));
        assertTrue(EntityAIShipGuarding.canFire(false, 2));
        assertFalse(EntityAIShipGuarding.canFire(false, 0));
        assertFalse(EntityAIShipGuarding.canFire(true, 1));
        assertFalse(EntityAIShipGuarding.canFire(true, 0));
    }

    // ---- issue #38: idle start check cadence independent of the poll interval ----

    private static final int NEVER = Integer.MIN_VALUE;

    /** ticks at which the start check fires when polled every pollInterval ticks from firstPoll */
    private static java.util.List<Integer> checks(int totalTicks, int firstPoll, int pollInterval) {
        java.util.List<Integer> fired = new java.util.ArrayList<>();
        int last = NEVER;
        for (int tick = firstPoll; tick <= totalTicks; tick += pollInterval) {
            if (EntityAIShipGuarding.isStartCheckDue(last, tick, 32)) {
                last = tick;
                fired.add(tick);
            }
        }
        return fired;
    }

    @Test
    void firstStartCheckIsAlwaysDue() {
        assertTrue(EntityAIShipGuarding.isStartCheckDue(NEVER, 5, 32));
    }

    @Test
    void startCheckWaitsFullIntervalSinceLastCheck() {
        assertFalse(EntityAIShipGuarding.isStartCheckDue(100, 131, 32));
        assertTrue(EntityAIShipGuarding.isStartCheckDue(100, 132, 32));
    }

    @Test
    void startCheckSpacingIsBoundedByIntervalPlusPollForAnyPollInterval() {
        for (int poll : new int[]{1, 3, 5, 7, 10}) {
            for (int firstPoll = 1; firstPoll <= poll; firstPoll++) {
                java.util.List<Integer> fired = checks(2000, firstPoll, poll);
                for (int i = 1; i < fired.size(); i++) {
                    int gap = fired.get(i) - fired.get(i - 1);
                    assertTrue(gap >= 32 && gap < 32 + poll, "poll " + poll + " gap " + gap);
                }
            }
        }
    }

    @Test
    void checkStillFiresWhenPollingNeverLandsOnAMultipleOf32() {
        // the former "ticksExisted % 32 == 0" check never fired when polling every 4 ticks from tick 1
        int moduloHits = 0;
        for (int tick = 1; tick <= 2000; tick += 4) {
            if (tick % 32 == 0) moduloHits++;
        }
        assertEquals(0, moduloHits);
        assertTrue(checks(2000, 1, 4).size() > 2000 / 40);
    }
}
