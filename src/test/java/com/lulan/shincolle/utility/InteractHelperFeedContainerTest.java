package com.lulan.shincolle.utility;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #33: feeding gives the food's container item on every feed, nothing in creative mode.
 * The inventory add / drop-when-full part runs on a live player and needs gametest or manual verification.
 */
class InteractHelperFeedContainerTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    void survivalFeedGivesContainerWhenFoodHasOne() {
        assertTrue(InteractHelper.shouldGiveContainer(false, true));
    }

    @Test
    void foodWithoutContainerGivesNothing() {
        assertFalse(InteractHelper.shouldGiveContainer(false, false));
    }

    @Test
    void creativeFeedGivesNothing() {
        assertFalse(InteractHelper.shouldGiveContainer(true, true));
    }

    @Test
    void containerItemOfFoodStackIsResolved() {
        Item food = new Item().setContainerItem(Items.BOWL);
        ItemStack stack = new ItemStack(food, 3);
        ItemStack container = stack.getItem().getContainerItem(stack);
        assertEquals(Items.BOWL, container.getItem());
        assertFalse(container.isEmpty());
    }
}
