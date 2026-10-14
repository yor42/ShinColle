package com.lulan.shincolle.handler;

import net.minecraftforge.common.config.Property;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Issue #15 (P-10): Tile_Crane grew from 2 values (legacy 1.10: fluid tank mB, EU capacity) to 5.
 * A legacy 2-value array migrates into the first two slots; the new slots take current defaults.
 * Expected arrays are literals taken from the documented defaults.
 */
class ConfigArrayMigrationTest {

    private static final int[] DEFAULTS = {2048000, 160000000, 640000000, 500, 2000};

    private static Property prop(int... values) {
        String[] s = new String[values.length];
        for (int i = 0; i < values.length; i++) s[i] = Integer.toString(values[i]);
        return new Property("Tile_Crane", s, Property.Type.INTEGER);
    }

    @Test
    void legacyTwoValueArrayKeepsOldValuesAndFillsDefaults() {
        Property p = prop(1000, 5000);
        int[] result = ConfigHandler.getCraneArrayFromConfig(DEFAULTS.clone(), p);
        assertArrayEquals(new int[]{1000, 5000, 640000000, 500, 2000}, result);
        // migrated value is written back so the config file is upgraded
        assertArrayEquals(new int[]{1000, 5000, 640000000, 500, 2000}, p.getIntList());
    }

    @Test
    void fiveValueArrayIsUnchanged() {
        int[] result = ConfigHandler.getCraneArrayFromConfig(DEFAULTS.clone(), prop(1, 2, 3, 4, 5));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void otherLengthsResetToDefaults() {
        for (int[] bad : new int[][]{{}, {7}, {1, 2, 3}, {1, 2, 3, 4}, {1, 2, 3, 4, 5, 6}}) {
            Property p = prop(bad);
            assertArrayEquals(DEFAULTS, ConfigHandler.getCraneArrayFromConfig(DEFAULTS.clone(), p));
            assertArrayEquals(DEFAULTS, p.getIntList());
        }
    }

    @Test
    void otherArraysKeepResetBehaviour() {
        // getIntArrayFromConfig is shared by many arrays and must not migrate
        Property p = prop(1000, 5000);
        assertArrayEquals(DEFAULTS, ConfigHandler.getIntArrayFromConfig(DEFAULTS.clone(), p));
    }
}
