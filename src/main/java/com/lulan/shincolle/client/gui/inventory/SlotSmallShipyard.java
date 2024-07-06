package com.lulan.shincolle.client.gui.inventory;

import com.lulan.shincolle.tileentity.TileEntitySmallShipyard;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * slot for small shipyard
 * check the item type in spec slot
 */
public class SlotSmallShipyard extends Slot {

    private final int slotIndex;
    private final TileEntitySmallShipyard tile;

    public SlotSmallShipyard(IInventory tile, int slotIndex, int x, int y) {
        super(tile, slotIndex, x, y);
        this.slotIndex = slotIndex;
        this.tile = (TileEntitySmallShipyard) tile;
    }

    @Override
    public boolean isItemValid(ItemStack itemstack) {
        return tile.isItemValidForSlot(this.slotIndex, itemstack);
    }


}
