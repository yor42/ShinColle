package com.lulan.shincolle.ai.path;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Issue #24 (AI-04): yOffset must use y coordinates; stuck-recovery push thresholds must be symmetric.
 */
class ShipPathNavigateGeometryTest {

    @Test
    void offsetVectorUsesYForVerticalComponent() {
        double[] off = ShipPathNavigate.offsetVector(new Vec3d(1, 2, 3), new Vec3d(4, 8, 7));
        assertArrayEquals(new double[]{3D, 6D, 4D}, off, 1.0E-9D);
        assertEquals(9D + 36D + 16D, ShipPathNavigate.offsetSq(off), 1.0E-9D);
    }

    @Test
    void pureVerticalDifferenceHasNonZeroLength() {
        double[] off = ShipPathNavigate.offsetVector(new Vec3d(0, 0, 0), new Vec3d(0, 5, 0));
        assertEquals(25D, ShipPathNavigate.offsetSq(off), 1.0E-9D);
    }

    @Test
    void pushDirectionIsSymmetricAroundZero() {
        double[] in = {-0.5D, -0.1D, 0D, 0.1D, 0.5D};
        int[] expected = {-1, 0, 0, 0, 1};
        for (int i = 0; i < in.length; i++) {
            assertEquals(expected[i], ShipPathNavigate.pushDirection(in[i]), "dx=" + in[i]);
        }
    }
}
