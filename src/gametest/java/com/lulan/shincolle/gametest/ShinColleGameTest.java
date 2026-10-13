package com.lulan.shincolle.gametest;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * Dev-only in-game test harness. Never shipped: the gametest source set is on
 * the runClient classpath only with -Pgametest (build.gradle).
 * Activated by -Dshincolle.gametest=true.
 */
@Mod(modid = "shincolle_gametest", name = "ShinColle Game Test Harness", version = "dev",
        dependencies = "required-after:shincolle", clientSideOnly = true)
public class ShinColleGameTest {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("shincolle.gametest")) {
            MinecraftForge.EVENT_BUS.register(new GameTestRunner());
        }
    }

}
