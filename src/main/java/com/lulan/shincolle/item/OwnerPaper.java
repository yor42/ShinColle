package com.lulan.shincolle.item;

import com.lulan.shincolle.capability.CapaTeitoku;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.List;

public class OwnerPaper extends BasicItem {

    public static final String SignNameA = "SignNameA";    //player name tag
    public static final String SignNameB = "SignNameB";
    public static final String SignIDA = "SignIDA";        //player id tag
    public static final String SignIDB = "SignIDB";
    private static final String NAME = "OwnerPaper";


    public OwnerPaper() {
        super();
        this.setTranslationKey(NAME);
        this.setMaxStackSize(1);
    }

    //right click to sign the paper
    @Override
    @Nonnull
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, @Nonnull EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        //server side
        if (!world.isRemote && !player.isSneaking()) {
            CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(player);

            if (capa != null) {
                //first time use
                if (!stack.hasTagCompound() || stack.getTagCompound() == null) {
                    NBTTagCompound compound = new NBTTagCompound();
                    compound.setString(SignNameA, player.getName());
                    compound.setString(SignNameB, "");
                    compound.setInteger(SignIDA, capa.getPlayerUID());
                    compound.setInteger(SignIDB, -1);
                    compound.setBoolean("signPos", false);
                    stack.setTagCompound(compound);
                }
                //use > second time
                else {
                    //signPos: true -> sign at A, false -> sign at B
                    if (stack.getTagCompound().getBoolean("signPos")) {
                        stack.getTagCompound().setString(SignNameA, player.getName());
                        stack.getTagCompound().setInteger(SignIDA, capa.getPlayerUID());
                        stack.getTagCompound().setBoolean("signPos", false);
                    } else {
                        stack.getTagCompound().setString(SignNameB, player.getName());
                        stack.getTagCompound().setInteger(SignIDB, capa.getPlayerUID());
                        stack.getTagCompound().setBoolean("signPos", true);
                    }
                }
            }//end extprops != null
        }

        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    @Override
    public void addInformation(ItemStack itemstack, World world, @Nonnull List<String> list, @Nonnull ITooltipFlag par4) {

        if(!itemstack.hasTagCompound() || itemstack.getTagCompound() == null){
            return;
        }
        list.add(TextFormatting.RED + String.valueOf(itemstack.getTagCompound().getInteger(SignIDA)) +
                " " + TextFormatting.AQUA + itemstack.getTagCompound().getString(SignNameA));
        list.add(TextFormatting.RED + String.valueOf(itemstack.getTagCompound().getInteger(SignIDB)) +
                " " + TextFormatting.AQUA + itemstack.getTagCompound().getString(SignNameB));
    }


}