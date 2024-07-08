package com.lulan.shincolle.intermod.tinkers;

import com.lulan.shincolle.intermod.tinkers.proxy.TinkersClientInit;
import com.lulan.shincolle.reference.Reference;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;
import slimeknights.tconstruct.library.fluid.FluidMolten;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.smeltery.block.BlockMolten;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;


public class TinkersUtil {

    public static FluidMolten fluidMetal(String name, int color) {
        return registerFluid(new FluidMolten(name, color));
    }

    public static void initFluidMetal(Fluid fluid) {
        registerMoltenBlock(fluid);
        FluidRegistry.addBucketForFluid(fluid);
        if(FMLLaunchHandler.side() == Side.CLIENT) {
            TinkersClientInit.registerFluidModels(fluid);
        }
    }

    public static <T extends Fluid> T registerFluid(T fluid) {
        fluid.setUnlocalizedName(Reference.MOD_ID + "." + fluid.getName().toLowerCase(Locale.US));
        FluidRegistry.registerFluid(fluid);
        return fluid;
    }

    public static void registerMoltenBlock(Fluid fluid) {
        BlockMolten block = new BlockMolten(fluid) {
            @Nonnull
            @Override
            public String getTranslationKey() {
                Fluid fluid = FluidRegistry.getFluid(fluidName);
                if(fluid != null) {
                    return fluid.getUnlocalizedName().substring(0, fluid.getUnlocalizedName().length() - 5); // chop off .name
                }
                return super.getTranslationKey();
            }
        };
        registerBlock(block, "molten_" + fluid.getName());
    }

    public static void setDispItem(Material mat, String modid, String name) {
        if (mat == null)
            return;
        mat.setRepresentativeItem(Item.REGISTRY.getObject(new ResourceLocation(modid, name)));
    }

    public static void setDispItem(Material mat, String modid, String name, int meta) {
        if (mat == null)
            return;
        ItemStack is = new ItemStack(Item.REGISTRY.getObject(new ResourceLocation(modid, name)), 1, meta);
        mat.setRepresentativeItem(is);
    }

    public static void setDispItem(Material mat, String ore) {
        List<ItemStack> ores = OreDictionary.getOres(ore);
        if (mat == null || ores.isEmpty())
            return;
        mat.setRepresentativeItem(ores.get(0));
    }

    public static <T extends Block> T registerBlock(T block, String name) {
        block.setTranslationKey(Reference.MOD_ID + "." + name);
        block.setRegistryName(Reference.MOD_ID + "." + name);
        Item ib = new ItemBlock(block).setRegistryName(block.getRegistryName()).setCreativeTab(null);
        ForgeRegistries.BLOCKS.register(block);
        ForgeRegistries.ITEMS.register(ib);
        return block;
    }

}
