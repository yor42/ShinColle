package com.lulan.shincolle.gametest;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/**
 * Dev-only in-game test harness. Never shipped: the gametest source set is on
 * the runClient/runServer classpath only with -Pgametest (build.gradle).
 * Client runner: -Dshincolle.gametest=true. Dedicated-server hooks: -Dshincolle.gametest.server=true.
 * Not client-side-only any more, so the dedicated server can load the server hooks; both
 * parts do nothing unless their property is set.
 */
@Mod(modid = "shincolle_gametest", name = "ShinColle Game Test Harness", version = "dev",
        dependencies = "required-after:shincolle")
public class ShinColleGameTest {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("shincolle.gametest")) {
            MinecraftForge.EVENT_BUS.register(new GameTestRunner());
        }
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        if (Boolean.getBoolean("shincolle.gametest.server")) {
            event.registerServerCommand(new GameTestServerHooks.ProbeCommand());
            MinecraftForge.EVENT_BUS.register(new GameTestServerHooks());
        }
    }

}
