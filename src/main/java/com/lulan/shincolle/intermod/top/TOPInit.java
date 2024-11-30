package com.lulan.shincolle.intermod.top;

import com.lulan.shincolle.Tags;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.tileentity.*;
import mcjty.theoneprobe.api.*;
import mcjty.theoneprobe.apiimpl.styles.ProgressStyle;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.event.FMLInterModComms;

import javax.annotation.Nullable;
import java.util.function.Function;

public class TOPInit {

    private static boolean registered;

    public static void register() {
        if (registered)
            return;
        registered = true;
        FMLInterModComms.sendFunctionMessage("theoneprobe", "getTheOneProbe", "com.lulan.shincolle.intermod.top.TOPInit$GetTheOneProbe");
    }

    public static class GetTheOneProbe implements Function<ITheOneProbe, Void> {
        public static ITheOneProbe probe;
        @Override @Nullable
        public Void apply(ITheOneProbe theOneProbe) {
            probe = theOneProbe;
            probe.registerEntityProvider(new IProbeInfoEntityProvider() {
                @Override
                public String getID() {
                    return Tags.MOD_ID + ":ships";
                }

                @Override
                public void addProbeEntityInfo(ProbeMode probeMode, IProbeInfo iProbeInfo, EntityPlayer entityPlayer, World world, Entity entity, IProbeHitEntityData iProbeHitEntityData) {
                    if(!(entity instanceof BasicEntityShip ship)){
                        return;
                    }
                    EntityLivingBase owner = ship.getOwner();

                    iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                            .text(owner == null? "No Owner":"Owner: " + owner.getDisplayName().getFormattedText());

                    if(owner == null){
                        return;
                    }

                    iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                            .item(new ItemStack(ModItems.Grudge))
                            .text("Grudges: "+ship.getGrudge());
                    iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                            .item(new ItemStack(ModItems.CombatRation))
                            .text("Morale: "+ship.getMorale());
                    iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                            .item(new ItemStack(Items.EXPERIENCE_BOTTLE))
                            .text("Lvl: "+ship.getLevel());
                    if(ship.getStateFlag(ID.F.IsMarried)) {
                        iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                                .item(new ItemStack(ModItems.MarriageRing));
                    }
                }
            });

            probe.registerProvider(new IProbeInfoProvider() {
                @Override
                public String getID() {
                    return Tags.MOD_ID + ":machines";
                }

                @Override
                public void addProbeInfo(ProbeMode probeMode, IProbeInfo iProbeInfo, EntityPlayer entityPlayer, World world, IBlockState iBlockState, IProbeHitData iProbeHitData) {
                    TileEntity entity = world.getTileEntity(iProbeHitData.getPos());

                    if(entity instanceof TileEntitySmallShipyard device){
                        iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                                .item(new ItemStack(device.getBuildType()%2 == 1?ModItems.ShipSpawnEgg:ModItems.EquipCannon)).text(device.getBuildTimeString());
                        iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                                .item(new ItemStack(Items.LAVA_BUCKET))
                                .progress(device.getFluidFuelAmount(), device.getFluidFuelCapacity(), new ProgressStyle().showText(true).prefix("Fuel: ").suffix("mb"));
                    }
                    else if (entity instanceof TileMultiGrudgeHeavy device){
                        iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                                .item(new ItemStack(device.getBuildType()%2 == 1?ModItems.ShipSpawnEgg:ModItems.EquipCannon)).text(device.getBuildTimeString());
                        iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                                .item(new ItemStack(Items.LAVA_BUCKET))
                                .progress(device.getFluidFuelAmount(), device.getFluidFuelCapacity(), new ProgressStyle().showText(true).prefix("Fuel: ").suffix("mb"));
                    }
                    else if (entity instanceof ITileWaypoint device){
                        if(device.getNextWaypoint() != null && device.getNextWaypoint() != BlockPos.ORIGIN){
                            iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                                    .item(new ItemStack(ModBlocks.BlockWaypoint)).text("Next Waypoint: " + device.getNextWaypoint().toString());
                        }
                        if(device.getLastWaypoint() != null && device.getLastWaypoint() != BlockPos.ORIGIN){
                            iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                                    .item(new ItemStack(ModBlocks.BlockWaypoint)).text("Last Waypoint: " + device.getLastWaypoint().toString());
                        }
                        if(device.getPairedChest() != null && device.getPairedChest() != BlockPos.ORIGIN){
                            iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                                    .item(new ItemStack(Blocks.CHEST)).text("Paired Chest: " + device.getPairedChest().toString());
                        }
                    }
                    else if (entity instanceof TileEntityVolCore device){
                        iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                                .item(new ItemStack(ModItems.Grudge)).progress(device.getPowerRemained(), device.getPowerMax(), new ProgressStyle().showText(true).prefix("Fuel:"));
                    }
                }
            });

            return null;
        }
    }

}
