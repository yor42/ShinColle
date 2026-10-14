package com.lulan.shincolle.ai;

import net.minecraft.util.math.AxisAlignedBB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
