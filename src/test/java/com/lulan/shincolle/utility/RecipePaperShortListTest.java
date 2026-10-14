package com.lulan.shincolle.utility;

import com.lulan.shincolle.crafting.InventoryCraftingFake;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #41 (AI-09): TaskHelper.java:157/196/236 read a recipe paper's "Recipe" list with a fixed
 * {@code for (i < 9)} loop.
 * <p>
 * MECHANISM EVIDENCE ONLY. The crafting task method needs a ship host, a waypoint tile entity and a
 * chest in a world, so it cannot be called headlessly. The recipe paper writer
 * (ContainerRecipePaper.java:98-116) appends ONLY non-empty craft slots, plus a result entry with
 * Slot=9, so lists shorter than 9 are the normal case, not a hand-edited one. This test replays the
 * writer and the reader loop verbatim against InventoryCraftingFake. Minimal seam: extract the
 * reader loop into a static helper (for example TaskHelper.readRecipe(NBTTagList)) returning the
 * 3x3 matrix.
 */
class RecipePaperShortListTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    /** Same shape as ContainerRecipePaper: one entry per non-empty slot, then the result as Slot 9. */
    private static NBTTagList writePaperLikeTheGui(ItemStack[] matrix, ItemStack result) {
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < 9; i++) {
            if (!matrix[i].isEmpty()) {
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setInteger("Slot", i);
                matrix[i].writeToNBT(itemTag);
                list.appendTag(itemTag);
            }
        }
        NBTTagCompound resultTag = new NBTTagCompound();
        resultTag.setInteger("Slot", 9);
        result.writeToNBT(resultTag);
        list.appendTag(resultTag);
        return list;
    }

    /** Verbatim copy of the TaskHelper reader loop (fixed bound of 9). */
    private static InventoryCraftingFake readLikeTaskHelper(NBTTagList tagList) {
        InventoryCraftingFake recipe = new InventoryCraftingFake(3, 3);
        for (int i = 0; i < 9; i++) {
            NBTTagCompound itemTags = tagList.getCompoundTagAt(i);
            int slot = itemTags.getInteger("Slot");
            if (slot >= 0 && slot < 9) {
                recipe.setInventorySlotContents(slot, new ItemStack(itemTags));
            }
        }
        return recipe;
    }

    private static ItemStack[] emptyMatrix() {
        ItemStack[] m = new ItemStack[9];
        Arrays.fill(m, ItemStack.EMPTY);
        return m;
    }

    @Test
    void currentlyErasesSlotZeroWhenTheListHasFewerThanNineEntries() {
        // 2x2 recipe in slots 0,1,3,4 -> list = 4 ingredients + 1 result = 5 entries.
        ItemStack[] m = emptyMatrix();
        m[0] = new ItemStack(Items.APPLE);
        m[1] = new ItemStack(Items.APPLE);
        m[3] = new ItemStack(Items.APPLE);
        m[4] = new ItemStack(Items.APPLE);
        NBTTagList paper = writePaperLikeTheGui(m, new ItemStack(Items.GOLDEN_APPLE));
        assertEquals(5, paper.tagCount());

        InventoryCraftingFake recipe = readLikeTaskHelper(paper);

        // Suspected defect: entries 5..8 are empty compounds with Slot 0, which overwrite the real
        // slot 0 with an empty stack. A future bound of min(9, tagCount()) flips the first assertion.
        assertTrue(recipe.getStackInSlot(0).isEmpty(), "slot 0 ingredient is lost");
        assertEquals(Items.APPLE, recipe.getStackInSlot(1).getItem());
        assertEquals(Items.APPLE, recipe.getStackInSlot(3).getItem());
        assertEquals(Items.APPLE, recipe.getStackInSlot(4).getItem());
    }

    @Test
    void slotZeroSurvivesWhenSlotZeroIsEmptyOrTheListHasNineOrMoreEntries() {
        // No slot-0 ingredient: the stray empty writes are harmless.
        ItemStack[] m = emptyMatrix();
        m[4] = new ItemStack(Items.APPLE);
        InventoryCraftingFake recipe = readLikeTaskHelper(writePaperLikeTheGui(m, new ItemStack(Items.GOLDEN_APPLE)));
        assertEquals(Items.APPLE, recipe.getStackInSlot(4).getItem());

        // 8 ingredients including slot 0 + result = 9 entries: the loop stays inside the list.
        ItemStack[] full = emptyMatrix();
        for (int i = 0; i < 8; i++) full[i] = new ItemStack(Items.APPLE);
        NBTTagList nine = writePaperLikeTheGui(full, new ItemStack(Items.GOLDEN_APPLE));
        assertEquals(9, nine.tagCount());
        assertEquals(Items.APPLE, readLikeTaskHelper(nine).getStackInSlot(0).getItem());
    }

    @Test
    void outOfRangeListIndexReturnsAnEmptyCompoundWithSlotZero() {
        // Vanilla behavior the defect relies on.
        NBTTagList list = new NBTTagList();
        NBTTagCompound only = new NBTTagCompound();
        only.setInteger("Slot", 3);
        list.appendTag(only);
        NBTTagCompound beyond = list.getCompoundTagAt(5);
        assertEquals(0, beyond.getSize());
        assertEquals(0, beyond.getInteger("Slot"));
        assertTrue(new ItemStack(beyond).isEmpty());
    }
}
