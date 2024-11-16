package com.lulan.shincolle.init;

import com.lulan.shincolle.block.*;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.tileentity.*;
import com.lulan.shincolle.utility.LogHelper;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;

@Mod.EventBusSubscriber
@GameRegistry.ObjectHolder(Reference.MOD_ID)
public class ModBlocks {

    //list for blocks
    private static final ArrayList<Block> BLOCKS = new ArrayList<>();

    //blocks
    public static final Block BlockAbyssium = initBlocks(new BlockAbyssium());
    public static final Block BlockCrane = initBlocks(new BlockCrane());
    public static final Block BlockDesk = initBlocks(new BlockDesk());
    public static final Block BlockFrame = initBlocks(new BlockFrame());
    public static final Block BlockGrudge = initBlocks(new BlockGrudge());
    public static final Block BlockGrudgeXP = initBlocks(new BlockGrudgeXP());
    public static final Block BlockGrudgeHeavy = initBlocks(new BlockGrudgeHeavy());
    public static final Block BlockGrudgeHeavyDeco = initBlocks(new BlockGrudgeHeavyDeco());
    public static final Block BlockLightAir = initBlocks(new BlockLightAir());
    public static final Block BlockLightLiquid = initBlocks(new BlockLightLiquid());
    public static final Block BlockPolymetal = initBlocks(new BlockPolymetal());
    public static final Block BlockPolymetalGravel = initBlocks(new BlockPolymetalGravel());
    public static final Block BlockPolymetalOre = initBlocks(new BlockPolymetalOre());
    public static final Block BlockSmallShipyard = initBlocks(new BlockSmallShipyard());
    public static final Block BlockVolBlock = initBlocks(new BlockVolBlock());
    public static final Block BlockVolCore = initBlocks(new BlockVolCore());
    public static final Block BlockWaypoint = initBlocks(new BlockWaypoint());
    
    private static Block initBlocks(Block block) {
        BLOCKS.add(block);
        return block;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(BLOCKS.toArray(new Block[0]));
    }

    @SubscribeEvent
    public static void registerItemBlocks(RegistryEvent.Register<Item> event) {
        for (Block block : BLOCKS) {
            if (block instanceof BlockAbyssium || block instanceof BlockGrudge || block instanceof BlockPolymetal || block instanceof BlockPolymetalGravel || block instanceof BlockGrudgeHeavyDeco) {
                event.getRegistry().register(new ItemBlockResourceBlock(block).setRegistryName(block.getRegistryName()));
            } else if (block instanceof BlockWaypoint) {
                event.getRegistry().register(new ItemBlockWaypoint(block).setRegistryName(block.getRegistryName()));
            } else if (block instanceof BlockGrudgeHeavy) {
                event.getRegistry().register(new ItemBlockGrudgeHeavy(block).setRegistryName(block.getRegistryName()));
            } else {
                event.getRegistry().register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
            }
        }
    }

    public static void init() {
        GameRegistry.registerTileEntity(TileEntityWaypoint.class, new ResourceLocation(Reference.MOD_ID, "TileEntityWaypoint"));
        GameRegistry.registerTileEntity(TileEntityVolCore.class, new ResourceLocation(Reference.MOD_ID, "TileEntityVolCore"));
        GameRegistry.registerTileEntity(TileEntitySmallShipyard.class, new ResourceLocation(Reference.MOD_ID, "TileEntitySmallShipyard"));
        GameRegistry.registerTileEntity(TileMultiPolymetal.class, new ResourceLocation(Reference.MOD_ID, "TileMultiPolymetal"));
        GameRegistry.registerTileEntity(TileEntityLightBlock.class, new ResourceLocation(Reference.MOD_ID, "TileEntityLightBlock"));
        GameRegistry.registerTileEntity(TileEntityDesk.class, new ResourceLocation(Reference.MOD_ID, "TileEntityDesk"));
        GameRegistry.registerTileEntity(TileEntityCrane.class, new ResourceLocation(Reference.MOD_ID, "TileEntityCrane"));
        GameRegistry.registerTileEntity(TileMultiGrudgeHeavy.class, new ResourceLocation(Reference.MOD_ID, "TileMultiLargeShipyard"));
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void loadBlockModels(ModelRegistryEvent event) {
        for (Block b : BLOCKS) {
            try {
                ((ICustomModels) b).initModel();
            } catch (Exception e) {
                //抓model失敗, 此例外必須丟出以強制中止遊戲
                LogHelper.info("EXCEPTION: block texture init fail: " + b);
                e.printStackTrace();
                throw e;
            }
        }
    }
}