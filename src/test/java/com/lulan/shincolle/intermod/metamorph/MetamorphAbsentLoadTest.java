package com.lulan.shincolle.intermod.metamorph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Issue #43 (MOD-01/NET-11): with the Metamorph mod absent, loading MetamorphHelper and calling the
 * entry points that other classes invoke without an activeMetamorph guard must not throw NoClassDefFoundError.
 * The test classpath has Metamorph on the compile side only (modCompileOnly), so it models the absent-mod case.
 */
class MetamorphAbsentLoadTest {

    @Test
    void metamorphApiIsNotOnTheTestRuntimeClasspath() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("mchorse.metamorph.api.MorphAPI"));
    }

    @Test
    void helperLoadsAndUnguardedPacketEntryPointIsSafe() {
        //C2SGUIPackets calls this without a guard; a null capability returns before any Metamorph class is touched
        assertDoesNotThrow(() -> MetamorphHelper.handleGUIPacketInput(null, 0, 0));
    }
}
