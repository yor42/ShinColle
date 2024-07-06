package com.lulan.shincolle.intermod.tinkers;

import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.intermod.tinkers.traits.Grudgeful;
import com.lulan.shincolle.intermod.tinkers.traits.Omnivore;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.init.Items;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.StringUtils;
import slimeknights.tconstruct.library.MaterialIntegration;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.fluid.FluidMolten;
import slimeknights.tconstruct.library.materials.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static slimeknights.tconstruct.library.materials.MaterialTypes.EXTRA;
import static slimeknights.tconstruct.library.materials.MaterialTypes.HEAD;
import static slimeknights.tconstruct.library.utils.HarvestLevels.IRON;
import static slimeknights.tconstruct.library.utils.HarvestLevels.STONE;
import static slimeknights.tconstruct.tools.TinkerTraits.aquadynamic;
import static slimeknights.tconstruct.tools.TinkerTraits.depthdigger;

public class TinkersInit {

    public static final Map<String, Material> materials = new LinkedHashMap<>();
    public static final Map<String, MaterialIntegration> materialIntegrations = new Object2ObjectOpenHashMap<>();
    public static final Map<String, CompletionStage<?>> materialIntegrationStages = new Object2ObjectOpenHashMap<>();
    public static final Map<String, String> materialOreDicts = new Object2ObjectOpenHashMap<>();

    public static void preinit(){
        Material abyssium = TinkerRegistry.getMaterial("abyssium");
        if(abyssium == Material.UNKNOWN){
            abyssium = new Material("abyssium", TextFormatting.DARK_RED);
            abyssium.addTrait(Grudgeful.GRUDGEFUL);
            abyssium.addTrait(depthdigger, HEAD);
            abyssium.addItem("ingotAbyssium", 1, Material.VALUE_Ingot);
            abyssium.addItem("nuggetAbyssium", 1, Material.VALUE_Nugget);
            abyssium.addItem("blockAbyssium", 1, Material.VALUE_Block);
            abyssium.setCraftable(false).setCastable(true);
            if(FMLLaunchHandler.side() == Side.CLIENT){
                com.lulan.shincolle.intermod.tinkers.proxy.TinkersClientInit.setRenderInfo(abyssium, 0x880000);
            }

            FluidMolten abyssiumFluid = TinkersUtil.fluidMetal("abyssium", 0x880000);
            abyssiumFluid.setTemperature(870);

            TinkersUtil.initFluidMetal(abyssiumFluid);
            abyssium.setFluid(abyssiumFluid);

            TinkerRegistry.addMaterialStats(abyssium,
                    new HeadMaterialStats(195, 6.5f, 5f, IRON),
                    new ExtraMaterialStats(40),
                    new BowMaterialStats(0.3f, 1.2f, 8f));
            materials.put("abyssium", abyssium);
        }

        Material polymetal = TinkerRegistry.getMaterial("polymetal");
        if(polymetal == Material.UNKNOWN){
            polymetal = new Material("polymetal", TextFormatting.DARK_GRAY);
            polymetal.addTrait(aquadynamic, EXTRA);
            polymetal.addTrait(Omnivore.OMNIVORE, HEAD);
            polymetal.addItem("ingotpolymetal", 1, Material.VALUE_Ingot);
            polymetal.addItem("nuggetpolymetal", 1, Material.VALUE_Nugget);
            polymetal.addItem("blockpolymetal", 1, Material.VALUE_Block);
            polymetal.setCraftable(false).setCastable(true);
            if(FMLLaunchHandler.side() == Side.CLIENT){
                com.lulan.shincolle.intermod.tinkers.proxy.TinkersClientInit.setRenderInfo(polymetal, 0x666666);
            }

            FluidMolten polymetalFluid = TinkersUtil.fluidMetal("polymetal", 0x666666);
            polymetalFluid.setTemperature(740);

            TinkersUtil.initFluidMetal(polymetalFluid);
            polymetal.setFluid(polymetalFluid);

            TinkerRegistry.addMaterialStats(polymetal,
                    new HeadMaterialStats(150, 6f, 3f, STONE),
                    new ExtraMaterialStats(20));
            materials.put("polymetal", polymetal);
        }

        materials.forEach((k, v) -> {
            if (!materialIntegrations.containsKey(k)) {
                materialIntegrationStages.getOrDefault(k, CompletableFuture.completedFuture(null)).thenRun(() -> {
                    MaterialIntegration mi;
                    if (v.getRepresentativeItem().getItem() == Items.EMERALD) {
                        mi = new MaterialIntegration(v, v.getFluid()).toolforge();
                    } else if (v.getFluid() != null) {
                        mi = new MaterialIntegration(v, v.getFluid(), StringUtils.capitalize(k)).toolforge();
                    } else {
                        mi = new MaterialIntegration(v);
                    }
                    if (materialOreDicts.containsKey(k)) {
                        mi.representativeItem = materialOreDicts.get(k);
                    }
                    TinkerRegistry.integrate(mi).preInit();
                    materialIntegrations.put(k, mi);
                });
            }
        });
    }

    public static void init() {
        TinkersUtil.setDispItem(TinkerRegistry.getMaterial("abyssium"), "ingotAbyssium");
        TinkersUtil.setDispItem(TinkerRegistry.getMaterial("polymetal"), "ingotPolymetal");
    }
}
