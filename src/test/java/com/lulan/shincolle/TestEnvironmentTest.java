package com.lulan.shincolle;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Phase 3 task 0: checks that the test source set runs and can use vanilla
 * item classes without launching the game. Characterization tests that need
 * ItemStack depend on this.
 */
class TestEnvironmentTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    void junitRuns() {
        assertEquals(2, 1 + 1);
    }

    @Test
    void itemStackCountArithmetic() {
        ItemStack stack = new ItemStack(Items.APPLE, 5);
        stack.shrink(2);
        assertEquals(3, stack.getCount());
    }

}
