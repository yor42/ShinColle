package com.lulan.shincolle.item;

import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.proxy.CommonProxy;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.List;

public class TrainingBook extends BasicItem {

    private static final String NAME = "TrainingBook";

    public TrainingBook() {
        super();
        this.setTranslationKey(NAME);
    }

    //display equip information
    @Override
    public void addInformation(@Nonnull ItemStack itemstack, World world, List<String> list, @Nonnull ITooltipFlag par4) {
        list.add(TextFormatting.GOLD + I18n.format("gui.shincolle:trainingbook"));
    }

    //start use item
    @Override
    @Nonnull
    public ActionResult<ItemStack> onItemRightClick(@Nonnull World world, @Nonnull EntityPlayer player, @Nonnull EnumHand hand) {
        if (CommonProxy.activeMetamorph && ConfigHandler.enableMetamorphSkill && hand == EnumHand.MAIN_HAND) {
            player.setActiveHand(hand);
            return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItemMainhand());
        } else {
            return new ActionResult<>(EnumActionResult.FAIL, player.getHeldItem(hand));
        }
    }

    @Override
    @Nonnull
    public EnumAction getItemUseAction(@Nonnull ItemStack stack) {
        return EnumAction.EAT;
    }

    @Override
    public int getMaxItemUseDuration(@Nonnull ItemStack stack) {
        if (CommonProxy.activeMetamorph && ConfigHandler.enableMetamorphSkill) {
            return 80;
        } else {
            return 0;
        }
    }

    /**
     * Level after one use: current level + 5 + roll (roll = 0..5), clamped to the level cap.
     */
    public static int calcTrainedLevel(int level, int lvcap, int roll) {
        return Math.min(level + 5 + roll, lvcap);
    }

    @Override
    @Nonnull
    public ItemStack onItemUseFinish(@Nonnull ItemStack stack, @Nonnull World world, @Nonnull EntityLivingBase host) {
        if (host instanceof EntityPlayer player && !world.isRemote && CommonProxy.activeMetamorph && ConfigHandler.enableMetamorphSkill) {
            CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(player);

            if (capa != null && capa.morphEntity instanceof BasicEntityShip ship) {

                //level up when use finish
                //level cap of the ship: 100 unmarried, 150 married (deliberate deviation from 1.10, which used 150)
                int lvcap = ship.getLVCap();
                if (ship.getLevel() < lvcap) {
                    int lv = calcTrainedLevel(ship.getLevel(), lvcap, ship.getRNG().nextInt(6));

                    ship.setShipLevel(lv, true);

                    //level up sound
                    ship.playSound(ModSounds.SHIP_LEVEL, 0.75F, 1F);
                    ship.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_LEVELUP, ship.getSoundCategory(), 0.75F, 1F);

                    //item--
                    if (!player.capabilities.isCreativeMode) stack.shrink(1);
                }

            }
        }

        return stack;
    }


}