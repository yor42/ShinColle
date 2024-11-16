package com.lulan.shincolle.intermod.tinkers.traits;

import net.minecraft.block.BlockOre;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.world.BlockEvent;
import slimeknights.tconstruct.library.traits.AbstractTrait;

import java.util.Random;

public class Omnivore extends AbstractTrait {

    public static final Omnivore OMNIVORE = new Omnivore();

    public Omnivore() {
        super("omnivore", TextFormatting.GRAY);
    }

    @Override
    public void beforeBlockBreak(ItemStack tool, BlockEvent.BreakEvent event) {
        IBlockState state = event.getState();
        Random rand = event.getWorld().rand;
        if(!(state.getBlock() instanceof BlockOre)){
            return;
        }
        if(rand.nextInt(3) == 0){
            tool.setItemDamage(tool.getItemDamage()-(rand.nextInt(2)+1));
        }
    }
}
