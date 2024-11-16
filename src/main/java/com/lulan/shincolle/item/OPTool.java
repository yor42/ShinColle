package com.lulan.shincolle.item;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * OP tool
 * <p>
 * 1. add/remove unattackable target (press numpad 1/2)
 * 2. show unattackable target list (press numpad 3)
 * 3. -
 */
public class OPTool extends BasicItem {

    private static final String NAME = "OPTool";


    public OPTool() {
        super();
        this.setTranslationKey(NAME);
        this.setMaxStackSize(1);
        this.setFull3D();
    }

    //item glow effect
    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(@Nonnull ItemStack item) {
        return true;
    }

    @Override
    public void addInformation(@Nonnull ItemStack itemstack, @Nullable World world, List<String> list, @Nonnull ITooltipFlag par4) {
        list.add(TextFormatting.RED + I18n.format("gui.shincolle:optool1"));
        list.add(TextFormatting.AQUA + I18n.format("gui.shincolle:optool2"));
    }


}