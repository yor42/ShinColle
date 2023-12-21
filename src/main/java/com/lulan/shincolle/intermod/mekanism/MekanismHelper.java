package com.lulan.shincolle.intermod.mekanism;

import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.init.ModItems;
import mekanism.common.recipe.RecipeHandler;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.oredict.OreDictionary;

public class MekanismHelper {

    public static void registerCompat(RegistryEvent.Register<IRecipe> event){
        RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(ModBlocks.BlockPolymetalOre), new ItemStack(ModItems.AbyssMetal, 6, 1));
        RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(ModBlocks.BlockPolymetalGravel), new ItemStack(ModItems.AbyssMetal, 12, 1));
    }

}
