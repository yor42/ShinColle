package com.lulan.shincolle.intermod.tinkers.traits;

import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.init.ModItems;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import slimeknights.tconstruct.library.traits.AbstractTrait;

public class Grudgeful extends AbstractTrait {

    public static final Grudgeful GRUDGEFUL = new Grudgeful();

    public Grudgeful() {
        super("grudgeful", TextFormatting.RED);
    }

    @Override
    public void afterHit(ItemStack tool, EntityLivingBase player, EntityLivingBase target, float damageDealt, boolean wasCritical, boolean wasHit) {
        World world = player.getEntityWorld();
        if(target.isEntityAlive() || world.isRemote){
            return;
        }
        ItemStack stack = new ItemStack(ModItems.Grudge, 3);
        if(target instanceof IMob) {
            stack = new ItemStack(ModBlocks.BlockGrudge);
        }
        EntityItem item = new EntityItem(world, target.posX, target.posY, target.posZ, stack);
        world.spawnEntity(item);
    }
}
