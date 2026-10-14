package com.lulan.shincolle.intermod.metamorph;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #4 (NET-04/ATTR-01/02): decrSupplies must report "consumed" only when an item was removed.
 */
class MetamorphSuppliesTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    void nothingFoundIsNotConsumed() {
        assertFalse(MetamorphHelper.wasItemConsumed(ItemStack.EMPTY));
    }

    @Test
    void removedItemIsConsumed() {
        assertTrue(MetamorphHelper.wasItemConsumed(new ItemStack(Items.IRON_INGOT, 1)));
    }
}
