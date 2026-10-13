package com.lulan.shincolle.utility;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #1 (ATTR-03): getAndRemoveItem must return exactly the number of
 * items it removes. Historical behavior (8bca563) set the returned stack size
 * to the amount taken.
 */
class InventoryHelperTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    private static IInventory inventoryWith(ItemStack... stacks) {
        InventoryBasic inv = new InventoryBasic("test", false, 9);
        for (int i = 0; i < stacks.length; i++) inv.setInventorySlotContents(i, stacks[i]);
        return inv;
    }

    private static int countItems(IInventory inv) {
        int total = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++) total += inv.getStackInSlot(i).getCount();
        return total;
    }

    private static ItemStack take(IInventory inv, int number) {
        return InventoryHelper.getAndRemoveItem(inv, new ItemStack(Items.APPLE), number, true, false, false, null);
    }

    @Test
    void partialTakeReturnsOnlyTheTakenAmount() {
        IInventory inv = inventoryWith(new ItemStack(Items.APPLE, 10));

        ItemStack got = take(inv, 3);

        assertEquals(3, got.getCount());
        assertEquals(7, countItems(inv));
    }

    @Test
    void singleItemTakeFromLargeStack() {
        // call pattern of TaskHelper (crafting task) and MetamorphHelper.decrSupplies
        IInventory inv = inventoryWith(new ItemStack(Items.APPLE, 10));

        ItemStack got = take(inv, 1);

        assertEquals(1, got.getCount());
        assertEquals(9, countItems(inv));
    }

    @Test
    void wholeStackTakeEmptiesSlot() {
        IInventory inv = inventoryWith(new ItemStack(Items.APPLE, 10));

        ItemStack got = take(inv, 10);

        assertEquals(10, got.getCount());
        assertTrue(inv.getStackInSlot(0).isEmpty());
    }

    @Test
    void takeAcrossSlotsConservesItems() {
        IInventory inv = inventoryWith(new ItemStack(Items.APPLE, 2), new ItemStack(Items.APPLE, 5));

        ItemStack got = take(inv, 4);

        assertEquals(4, got.getCount());
        assertEquals(3, countItems(inv));
    }

    @Test
    void takeMoreThanAvailableReturnsAll() {
        IInventory inv = inventoryWith(new ItemStack(Items.APPLE, 2));

        ItemStack got = take(inv, 5);

        assertEquals(2, got.getCount());
        assertEquals(0, countItems(inv));
    }

    @Test
    void partialTakeFromVanillaChest() {
        TileEntityChest chest = new TileEntityChest();
        chest.setInventorySlotContents(0, new ItemStack(Items.APPLE, 10));

        ItemStack got = take(chest, 3);

        assertEquals(3, got.getCount());
        assertEquals(7, countItems(chest));
    }

}
