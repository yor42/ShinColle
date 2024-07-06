package com.lulan.shincolle.intermod.tinkers.fluid;

import com.lulan.shincolle.reference.Reference;
import slimeknights.tconstruct.library.fluid.*;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.text.translation.*;
import net.minecraftforge.fluids.*;

//From plustic -yor42
public class FluidMoltenMetal extends FluidColored {

    public static ResourceLocation ICON_MetalStill = new ResourceLocation(Reference.MOD_ID, "blocks/fluids/molten_metal");
    public static ResourceLocation ICON_MetalFlowing = new ResourceLocation(Reference.MOD_ID, "blocks/fluids/molten_metal_flow");

    public FluidMoltenMetal(String fluidName, int color) {
        this(fluidName, color, ICON_MetalStill, ICON_MetalFlowing);
    }

    public FluidMoltenMetal(String fluidName, int color, ResourceLocation still, ResourceLocation flow) {
        super(fluidName, color, still, flow);

        this.setDensity(2000); // thicker than a bowl of oatmeal
        this.setViscosity(10000); // sloooow moving
        this.setTemperature(1000); // not exactly lava, but still hot. Should depend on the material
        this.setLuminosity(10); // glowy by default!
    }

    @Override
    public String getUnlocalizedName() {
        return super.getUnlocalizedName() + ".name";
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getLocalizedName(FluidStack stack) {
        String s = this.getUnlocalizedName();
        return s == null ? "" : I18n.translateToLocal(s);
    }
}
