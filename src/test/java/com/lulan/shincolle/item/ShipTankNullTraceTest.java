package com.lulan.shincolle.item;

import net.minecraft.util.EnumActionResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Issue #46: a null ray trace result (open air) must yield FAIL without throwing.
 */
class ShipTankNullTraceTest {

    @Test
    void nullTraceReturnsFailWithoutThrowing() {
        assertEquals(EnumActionResult.FAIL, ShipTank.resultForMissingTrace(null));
    }

    @Test
    void nonNullTraceDefersToNormalHandling() {
        // a non-null trace is never short-circuited; the normal block/fluid path decides
        assertNull(ShipTank.resultForMissingTrace(new net.minecraft.util.math.RayTraceResult(
                net.minecraft.util.math.RayTraceResult.Type.MISS, net.minecraft.util.math.Vec3d.ZERO,
                net.minecraft.util.EnumFacing.UP, net.minecraft.util.math.BlockPos.ORIGIN)));
    }
}
