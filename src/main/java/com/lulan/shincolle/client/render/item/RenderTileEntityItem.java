package com.lulan.shincolle.client.render.item;

import com.lulan.shincolle.init.ModBlocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import com.lulan.shincolle.tileentity.TileEntityDesk;
import com.lulan.shincolle.tileentity.TileEntitySmallShipyard;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;

/**
 * tile entity itemblock renderer
 * <p>
 * ref: http://www.minecraftforge.net/forum/index.php?topic=28643.0
 * [1.8] Custom model block rendering in inventory?
 */
public class RenderTileEntityItem extends TileEntityItemStackRenderer {
    private static final TileEntity SmallShipyard = new TileEntitySmallShipyard();
    private static final TileEntity Desk = new TileEntityDesk();
    private final TileEntityItemStackRenderer parent;

    public RenderTileEntityItem(TileEntityItemStackRenderer parent) {
        this.parent = parent;
    }

    @Override
    public void renderByItem(ItemStack itemStack) {
        //load tile entity as item model
        if (itemStack.getItem() == Item.getItemFromBlock(ModBlocks.BlockSmallShipyard)) {
            TileEntityRendererDispatcher.instance.render(SmallShipyard, 0D, 0D, 0D, 0F);
        } else if (itemStack.getItem() == Item.getItemFromBlock(ModBlocks.BlockDesk)) {
            TileEntityRendererDispatcher.instance.render(Desk, 0D, 0D, 0D, 0F);
        }
        //if neither of the above, render item normally
        else {
            parent.renderByItem(itemStack);
        }
    }

    @Override
    public void renderByItem(ItemStack itemStack, float partialTicks) {
        //load tile entity as item model
        if (itemStack.getItem() == Item.getItemFromBlock(ModBlocks.BlockSmallShipyard)) {
            TileEntityRendererDispatcher.instance.render(SmallShipyard, 0D, 0D, 0D, 0F);
        } else if (itemStack.getItem() == Item.getItemFromBlock(ModBlocks.BlockDesk)) {
            TileEntityRendererDispatcher.instance.render(Desk, 0D, 0D, 0D, 0F);
        }
        //if neither of the above, render item normally
        else {
            parent.renderByItem(itemStack, partialTicks);
        }
    }

}
