package com.lulan.shincolle;

import com.lulan.shincolle.handler.ChunkLoaderHandler;
import com.lulan.shincolle.handler.CommandHandler;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.handler.GuiHandler;
import com.lulan.shincolle.init.*;
import com.lulan.shincolle.intermod.mekanism.MekanismHelper;
import com.lulan.shincolle.intermod.tinkers.TinkersInit;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.proxy.ServerProxy;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.LogHelper;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.Optional;


@Mod(modid = Reference.MOD_ID,
        name = Reference.MOD_NAME,
        version = Reference.MOD_VERSION,
        dependencies = "required-after:forge@[14.23.5.2768,)",
        guiFactory = "com.lulan.shincolle.config.ConfigGuiFactory")
@Mod.EventBusSubscriber
public class ShinColle {

    //mod instance
    @Mod.Instance(Reference.MOD_ID)
    public static ShinColle instance;

    //proxy for client/server event
    @SidedProxy(clientSide = Reference.CLIENT_PROXY, serverSide = Reference.SERVER_PROXY)
    public static CommonProxy proxy;

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (Loader.isModLoaded("mekanism")) {
            MekanismHelper.registerCompat(event);
        }
    }

    private static final String[] renamesToHandle = new String[] {
            "abyssium"};

    /**
     * pre-init: config/block/tile/item/entity init, render/packet/capability register
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) throws Exception {
        //config inti
        ConfigHandler.init(event);    //load config file

        // ModEntity.init(); // ModEntity now registers entities via the registerEntities event.

        ModSounds.init();

        //render & model register
        proxy.registerRender();

        //Packet channel register (simple network)
        proxy.registerChannel();

        //capability register
        proxy.registerCapability();

        if(Loader.isModLoaded("tconstruct")){
            TinkersInit.preinit();
        }

        LogHelper.info("INFO: Pre-Init completed.");
    }

    /**
     * initial: recipe/gui/worldgen init, event handler regist, create data handler,
     * request mod interact ,oreDictionary registr
     */
    @Mod.EventHandler
    public void Init(FMLInitializationEvent event) {
        ModBlocks.init();

        //GUI register
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

        ModEvents.init();

        ModRecipes.init();

        ModOres.oreDictRegister();

        if(Loader.isModLoaded("tconstruct")){
            TinkersInit.init();
        }

        LogHelper.info("INFO: Init completed.");

        //Waila tooltip provider TODO
        //FMLInterModComms.sendMessage("Waila", "register", "com.lulan.shincolle.waila.WailaDataProvider.callbackRegister");
    }

    @SubscribeEvent
    public static void missingBlockMappings(RegistryEvent.MissingMappings<Block> event) {
        event.getMappings().forEach(mapping -> {
            for (String name: renamesToHandle) {
                if (mapping.key.equals(new ResourceLocation(Reference.MOD_ID, Reference.MOD_ID+".molten_"+name))) {
                    Optional.ofNullable(FluidRegistry.getFluid(name))
                            .map(Fluid::getBlock)
                            .ifPresent(mapping::remap);
                }
            }
        });
    }

    @SubscribeEvent
    public static void missingItemMappings(RegistryEvent.MissingMappings<Item> event) {
        event.getMappings().forEach(mapping -> {
                    for (String name : renamesToHandle) {
                        if (mapping.key.equals(new ResourceLocation(Reference.MOD_ID, Reference.MOD_ID + ".molten_" + name))) {
                            Optional.ofNullable(FluidRegistry.getFluid(name))
                                    .map(Fluid::getBlock)
                                    .map(Item::getItemFromBlock)
                                    .ifPresent(mapping::remap);
                        }
                    }
                });
    }

                /**
                 * post-init: mod interact
                 */
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        //world gen跟entity spawn放在postInit, 以便能讀取到其他mod的biome
        ModWorldGen.init();

        //register chunk loader callback
        ForgeChunkManager.setForcedChunkLoadingCallback(instance, new ChunkLoaderHandler());

        //check config changed
        ConfigHandler.checkChange(ConfigHandler.config);

        //inter-mod
        CommonProxy.checkModLoaded();

//		//DEBUG
//        Map<String, ModContainer> modlist = Loader.instance().getIndexedModList();
//        modlist.forEach((name, v) ->
//        {
//        	LogHelper.info("AAAAAAAA "+name);
//        });
//		LogHelper.info("DEBUG : biome spawn: "+this.worldObj.getBiomeGenForCoords((int)this.posX, (int)this.posZ).getSpawnableList(EnumCreatureType.waterCreature).get(1));
//		for (String oreName : OreDictionary.getOreNames())
//		{	//list all oreDictionary  (DEBUG)
//			LogHelper.info(oreName);
//		}

        LogHelper.info("INFO: Post-Init completed.");
    }

    /**
     * server about to start
     * 當開啟一個存檔或者MP伺服器開啟時會丟出此事件
     * 在此事件中將MapStorage的讀取紀錄設為false
     * 使每次開不同存檔都會重讀該存檔的MapStorage
     */
    @Mod.EventHandler
    public void onServerAboutToStart(FMLServerAboutToStartEvent event) {
        LogHelper.info("INFO: server about to start: is MP server? " + event.getSide().isServer());
        //set init flag
        ServerProxy.initServerFile = true;
        ServerProxy.saveServerFile = false;
        CommonProxy.isMultiplayer = event.getSide().isServer();
    }

    /**
     * server starting
     * command必須在此註冊 (每個地圖檔會依照權限設定, 註冊不同command)
     */
    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        LogHelper.info("INFO: Server starting event: is MP server? " + event.getSide().isServer());

        //register command
        CommandHandler.init(event);
    }

    /**
     * server stopping
     * before world unload
     * 標記server即將關閉, server world data需要標記存回disk
     */
    @Mod.EventHandler
    public void onServerStopping(FMLServerStoppingEvent event) {
        LogHelper.info("INFO: Server stopping event");
        //set init flag
        ServerProxy.initServerFile = false;
        ServerProxy.saveServerFile = true;
    }


}