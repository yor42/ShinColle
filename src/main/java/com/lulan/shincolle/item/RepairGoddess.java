package com.lulan.shincolle.item;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.List;

public class RepairGoddess extends BasicItem {

    private static final String NAME = "RepairGoddess";

    public RepairGoddess() {
        super();
        this.setTranslationKey(NAME);
        this.setMaxStackSize(16);
    }

    //display equip information
    @Override
    public void addInformation(@Nonnull ItemStack itemstack, World world, @Nonnull List<String> list, @Nonnull ITooltipFlag par4) {
        list.add(TextFormatting.RED + I18n.format("gui.shincolle:repairgoddess"));
    }

    //item glow effect
    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(@Nonnull ItemStack item) {
        return true;
    }


}
