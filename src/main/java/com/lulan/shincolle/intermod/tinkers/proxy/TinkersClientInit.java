package com.lulan.shincolle.intermod.tinkers.proxy;

import com.lulan.shincolle.reference.Reference;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fluids.Fluid;
import slimeknights.tconstruct.library.client.MaterialRenderInfo;
import slimeknights.tconstruct.library.materials.Material;

import javax.annotation.Nonnull;

public class TinkersClientInit {
    public static void registerFluidModels(Fluid fluid) {
        if (fluid == null) return;
        Block block = fluid.getBlock();
        if (block != null) {
            Item item = Item.getItemFromBlock(block);
            FluidStateMapper mapper = new FluidStateMapper(fluid);
            ModelBakery.registerItemVariants(item);
            ModelLoader.setCustomMeshDefinition(item, mapper);
            ModelLoader.setCustomStateMapper(block, mapper);
        }
    }

    public static void setRenderInfo(Material mat, int color) {
        mat.setRenderInfo(color);
    }

    public static void setRenderInfo(Material mat, int lo, int mid, int hi) {
        mat.setRenderInfo(new MaterialRenderInfo.MultiColor(lo, mid, hi));
    }

    public static class FluidStateMapper extends StateMapperBase implements ItemMeshDefinition {
        public final Fluid fluid;
        public final ModelResourceLocation location;

        public FluidStateMapper(Fluid fluid) {
            this.fluid = fluid;
            this.location = new ModelResourceLocation(new ResourceLocation(Reference.MOD_ID, "fluid_block"),
                    fluid.getName());
        }

        @Nonnull
        @Override
        protected ModelResourceLocation getModelResourceLocation(@Nonnull IBlockState state) {
            return location;
        }

        @Nonnull
        @Override
        public ModelResourceLocation getModelLocation(@Nonnull ItemStack stack) {
            return location;
        }
    }
}
