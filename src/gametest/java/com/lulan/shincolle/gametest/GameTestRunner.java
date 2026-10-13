package com.lulan.shincolle.gametest;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.reference.ID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Drives a scripted single-player session from the client thread: creates a
 * world, acts through the real client controller (packets, GUIs), checks
 * server state, takes screenshots, then quits. Results go to
 * run/client/gametest/results.txt and to log lines prefixed [GAMETEST].
 */
public class GameTestRunner {

    private static final Logger LOG = LogManager.getLogger("GameTest");

    private final Minecraft mc = Minecraft.getMinecraft();
    private final String worldFolder = "gametest-" + System.currentTimeMillis();
    private final List<String> results = new ArrayList<>();
    private final List<Step> steps = new ArrayList<>();
    private int index = -1;
    private int stepTicks;
    private boolean started;
    private boolean finished;
    private int passed;
    private int failed;

    //state shared between steps
    private UUID shipId;
    private int shipLevel;
    private int shipUid;

    private abstract static class Step {
        final String name;
        final int timeout;

        Step(String name, int timeout) {
            this.name = name;
            this.timeout = timeout;
        }

        /** @return true when the step is finished; the step records its own result */
        abstract boolean tick(int t) throws Exception;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;

        if (!started) {
            if (mc.currentScreen instanceof GuiMainMenu) {
                started = true;
                mc.gameSettings.pauseOnLostFocus = false;
                buildSteps();
                nextStep();
            }
            return;
        }

        Step step = steps.get(index);
        stepTicks++;

        try {
            if (step.tick(stepTicks)) {
                nextStep();
            } else if (stepTicks > step.timeout) {
                fail(step.name, "timeout after " + step.timeout + " ticks");
                nextStep();
            }
        } catch (Throwable t) {
            LOG.error("[GAMETEST] step {} threw", step.name, t);
            fail(step.name, "exception: " + t);
            nextStep();
        }
    }

    private void nextStep() {
        index++;
        stepTicks = 0;

        if (index >= steps.size()) {
            finish();
        } else {
            LOG.info("[GAMETEST] step {}", steps.get(index).name);
        }
    }

    private void buildSteps() {
        steps.add(new Step("enter-world", 1200) {
            @Override
            boolean tick(int t) throws Exception {
                if (t == 1) {
                    WorldSettings settings = new WorldSettings(20261013L, GameType.CREATIVE, false, false, WorldType.FLAT);
                    settings.enableCommands();
                    mc.launchIntegratedServer(worldFolder, worldFolder, settings);
                    return false;
                }
                if (mc.player == null || mc.getIntegratedServer() == null) return false;
                if (mc.player.ticksExisted < 60) return false;

                callServer(() -> {
                    WorldServer w = server().getWorld(0);
                    w.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
                    w.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
                    w.setWorldTime(6000L);
                    return null;
                });
                screenshot("world");
                pass(name, "world " + worldFolder + ", " + Loader.instance().getActiveModList().size() + " mods");
                return true;
            }
        });

        steps.add(new Step("spawn-ship-with-egg", 400) {
            @Override
            boolean tick(int t) throws Exception {
                mc.player.rotationPitch = 90F;    //look down: the egg ray-traces the block below

                if (t == 1) {
                    ItemStack egg = pickEgg();
                    info(name, "egg meta " + egg.getMetadata());
                    setHeldItem(egg);
                    return false;
                }
                if (t == 10) {
                    EnumActionResult r = mc.playerController.processRightClick(mc.player, mc.world, EnumHand.MAIN_HAND);
                    info(name, "client processRightClick -> " + r);
                    return false;
                }
                if (t < 40) return false;

                String detail = callServer(() -> {
                    EntityPlayerMP p = serverPlayer();
                    List<BasicEntityShip> ships = p.world.getEntitiesWithinAABB(BasicEntityShip.class, p.getEntityBoundingBox().grow(8D));
                    if (ships.isEmpty()) return null;
                    BasicEntityShip s = ships.get(0);
                    shipId = s.getUniqueID();
                    shipLevel = s.getLevel();
                    shipUid = s.getPlayerUID();
                    return s.getClass().getSimpleName() + " level=" + s.getLevel() + " hp=" + s.getHealth() + "/" + s.getMaxHealth()
                            + " ownerUID=" + s.getPlayerUID() + " ownerUUID=" + s.getOwnerId() + " playerUUID=" + p.getUniqueID()
                            + " CanDrop=" + s.getStateFlag(ID.F.CanDrop) + " ticks=" + s.ticksExisted;
                });
                if (detail == null) return false;    //keep polling until timeout

                boolean owned = shipUid > 0 && detail.contains("ownerUUID=" + serverPlayer().getUniqueID());
                if (owned && detail.contains("CanDrop=true")) pass(name, detail);
                else fail(name, "spawned but owner or CanDrop wrong: " + detail);
                setHeldItem(ItemStack.EMPTY);
                return true;
            }
        });

        steps.add(new Step("open-ship-gui", 200) {
            @Override
            boolean tick(int t) throws Exception {
                if (shipId == null) {
                    fail(name, "no ship from previous step");
                    return true;
                }
                if (t == 5) {
                    sneak(true);
                    interact(clientShip());
                    sneak(false);
                    return false;
                }
                if (t < 5 || mc.currentScreen == null) return false;
                if (t < 25) return false;    //let the GUI render a few frames

                String gui = mc.currentScreen.getClass().getName();
                screenshot("ship-gui");
                mc.player.closeScreen();
                if (gui.endsWith("GuiShipInventory")) pass(name, gui);
                else fail(name, "unexpected screen " + gui);
                return true;
            }
        });

        steps.add(new Step("feed-ship", 200) {
            private int foodBefore;
            private boolean sitBefore;

            @Override
            boolean tick(int t) throws Exception {
                if (shipId == null) {
                    fail(name, "no ship from previous step");
                    return true;
                }
                if (t == 1) {
                    setHeldItem(new ItemStack(Items.COOKED_BEEF, 8));
                    foodBefore = callServer(() -> serverShip().getFoodSaturation());
                    sitBefore = callServer(() -> serverShip().isSitting());
                    return false;
                }
                if (t == 10) {
                    interact(clientShip());
                    return false;
                }
                if (t < 30) return false;

                int[] after = callServer(() -> {
                    BasicEntityShip s = serverShip();
                    return new int[] {s.getFoodSaturation(), s.getFoodSaturationMax(), serverPlayer().getHeldItemMainhand().getCount(), s.isSitting() ? 1 : 0};
                });
                String detail = "food " + foodBefore + " -> " + after[0] + " (max " + after[1] + "), beef left " + after[2]
                        + ", sitting " + sitBefore + " -> " + (after[3] == 1);
                if (after[0] > foodBefore) pass(name, detail);
                else fail(name, "food did not increase: " + detail);

                setHeldItem(ItemStack.EMPTY);
                callServer(() -> {
                    serverShip().setEntitySit(false);
                    return null;
                });
                return true;
            }
        });

        steps.add(new Step("feed-ammo", 200) {
            private int ammoBefore;

            @Override
            boolean tick(int t) throws Exception {
                if (shipId == null) {
                    fail(name, "no ship from previous step");
                    return true;
                }
                if (t == 1) {
                    setHeldItem(new ItemStack(ModItems.Ammo, 4, 0));    //light ammo, fed through InteractHelper.interactFeed
                    ammoBefore = callServer(() -> serverShip().getStateMinor(ID.M.NumAmmoLight));
                    return false;
                }
                if (t == 10) {
                    interact(clientShip());
                    return false;
                }
                if (t < 30) return false;

                int after = callServer(() -> serverShip().getStateMinor(ID.M.NumAmmoLight));
                if (after > ammoBefore) pass(name, "light ammo " + ammoBefore + " -> " + after);
                else fail(name, "light ammo did not increase: " + ammoBefore + " -> " + after);
                setHeldItem(ItemStack.EMPTY);
                return true;
            }
        });

        steps.add(new Step("ship-attacks-husk", 800) {
            private UUID huskId;
            private boolean targetSeen;

            @Override
            boolean tick(int t) throws Exception {
                if (shipId == null) {
                    fail(name, "no ship from previous step");
                    return true;
                }
                if (t == 1) {
                    //step the player back so the fight is on screen; the ship picks its own target
                    huskId = callServer(() -> {
                        BasicEntityShip s = serverShip();
                        serverPlayer().connection.setPlayerLocation(s.posX, s.posY, s.posZ - 8D, 0F, 20F);
                        EntityHusk h = new EntityHusk(s.world);
                        h.setLocationAndAngles(s.posX + 6D, s.posY, s.posZ, 0F, 0F);
                        h.enablePersistence();
                        s.world.spawnEntity(h);
                        return h.getUniqueID();
                    });
                    return false;
                }
                if (t > 5) lookAt(clientShip());
                if (t == 100) screenshot("combat");
                if (t % 10 != 0) return false;

                String done = callServer(() -> {
                    if (serverShip().getEntityTarget() != null) targetSeen = true;
                    Entity h = server().getWorld(0).getEntityFromUuid(huskId);
                    if (h == null || !h.isEntityAlive()) return "husk dead";
                    EntityHusk husk = (EntityHusk) h;
                    if (husk.getHealth() < husk.getMaxHealth()) return "husk hp " + husk.getHealth() + "/" + husk.getMaxHealth();
                    return null;
                });
                if (done != null) {
                    pass(name, done + " after " + t + " ticks, ship target seen " + targetSeen);
                    return true;
                }
                if (t >= timeout - 10) {
                    String state = callServer(() -> {
                        BasicEntityShip s = serverShip();
                        Entity h = server().getWorld(0).getEntityFromUuid(huskId);
                        return "sitting=" + s.isSitting() + " target=" + s.getEntityTarget() + " targetSeen=" + targetSeen
                                + " ammoL=" + s.getStateMinor(ID.M.NumAmmoLight) + " ammoH=" + s.getStateMinor(ID.M.NumAmmoHeavy)
                                + " passive=" + s.getStateFlag(ID.F.PassiveAI) + " dist=" + (h == null ? "n/a" : s.getDistance(h));
                    });
                    fail(name, "husk undamaged after " + t + " ticks; " + state);
                    return true;
                }
                return false;
            }
        });

        steps.add(new Step("save-quit-reload", 1200) {
            private int rejoinTick = -1;

            @Override
            boolean tick(int t) throws Exception {
                if (shipId == null) {
                    fail(name, "no ship from previous step");
                    return true;
                }
                if (t == 1) {
                    mc.world.sendQuittingDisconnectingPacket();
                    mc.loadWorld(null);
                    mc.displayGuiScreen(new GuiMainMenu());
                    return false;
                }
                if (t == 5) {
                    mc.launchIntegratedServer(worldFolder, worldFolder, null);
                    return false;
                }
                if (t < 5 || mc.player == null || mc.getIntegratedServer() == null) return false;
                if (rejoinTick < 0) rejoinTick = t;
                if (t - rejoinTick < 60) return false;

                String detail = callServer(() -> {
                    Entity e = server().getWorld(0).getEntityFromUuid(shipId);
                    if (!(e instanceof BasicEntityShip)) return null;
                    BasicEntityShip s = (BasicEntityShip) e;
                    return "level=" + s.getLevel() + " ownerUID=" + s.getPlayerUID() + " hp=" + s.getHealth() + "/" + s.getMaxHealth();
                });
                if (detail == null) fail(name, "ship " + shipId + " not found after reload");
                else if (detail.startsWith("level=" + shipLevel + " ownerUID=" + shipUid + " ")) pass(name, detail);
                else fail(name, "ship changed: expected level=" + shipLevel + " ownerUID=" + shipUid + ", got " + detail);
                screenshot("reloaded");
                return true;
            }
        });

        steps.add(new Step("crafting-task-take", 1) {
            @Override
            boolean tick(int t) {
                info(name, "SKIP: not automated yet (issue #1 manual check)");
                return true;
            }
        });
    }

    /* ---------------- helpers ---------------- */

    private IntegratedServer server() {
        return mc.getIntegratedServer();
    }

    private EntityPlayerMP serverPlayer() {
        return server().getPlayerList().getPlayers().get(0);
    }

    private BasicEntityShip serverShip() {
        Entity e = server().getWorld(0).getEntityFromUuid(shipId);
        if (e instanceof BasicEntityShip) return (BasicEntityShip) e;
        throw new IllegalStateException("ship " + shipId + " not loaded on server");
    }

    private Entity clientShip() {
        for (Entity e : mc.world.loadedEntityList) {
            if (shipId.equals(e.getUniqueID())) return e;
        }
        throw new IllegalStateException("ship " + shipId + " not loaded on client");
    }

    /** runs on the integrated server thread and waits for the result */
    private <T> T callServer(Callable<T> task) throws Exception {
        return server().callFromMainThread(task).get(10, TimeUnit.SECONDS);
    }

    private ItemStack pickEgg() {
        int meta = Integer.getInteger("shincolle.gametest.eggMeta", -1);
        if (meta < 0) {
            NonNullList<ItemStack> eggs = NonNullList.create();
            ModItems.ShipSpawnEgg.getSubItems(ModItems.ShipSpawnEgg.getCreativeTab(), eggs);
            for (ItemStack s : eggs) {
                if (s.getMetadata() > 1 && s.getMetadata() <= 2000) return s.copy();
            }
            throw new IllegalStateException("no ship egg in creative list");
        }
        return new ItemStack(ModItems.ShipSpawnEgg, 1, meta);
    }

    private void setHeldItem(ItemStack stack) throws Exception {
        callServer(() -> {
            EntityPlayerMP p = serverPlayer();
            p.inventory.setInventorySlotContents(p.inventory.currentItem, stack.copy());
            p.inventoryContainer.detectAndSendChanges();
            return null;
        });
    }

    /** sends the sneak state directly, independent of keyboard focus */
    private void sneak(boolean on) {
        mc.player.connection.sendPacket(new CPacketEntityAction(mc.player,
                on ? CPacketEntityAction.Action.START_SNEAKING : CPacketEntityAction.Action.STOP_SNEAKING));
    }

    /** same packet sequence as Minecraft.rightClickMouse on an entity */
    private void interact(Entity target) {
        RayTraceResult ray = new RayTraceResult(target, new Vec3d(target.posX, target.posY + target.height * 0.5D, target.posZ));
        if (mc.playerController.interactWithEntity(mc.player, target, ray, EnumHand.MAIN_HAND) != EnumActionResult.SUCCESS) {
            mc.playerController.interactWithEntity(mc.player, target, EnumHand.MAIN_HAND);
        }
    }

    private void lookAt(Entity target) {
        double dx = target.posX - mc.player.posX;
        double dy = target.posY + target.height * 0.5D - (mc.player.posY + mc.player.getEyeHeight());
        double dz = target.posZ - mc.player.posZ;
        mc.player.rotationYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
        mc.player.rotationPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
    }

    private void screenshot(String name) {
        ScreenShotHelper.saveScreenshot(mc.gameDir, "gametest-" + name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        info("screenshot", "screenshots/gametest-" + name + ".png");
    }

    private void pass(String step, String detail) {
        passed++;
        record("PASS " + step + ": " + detail);
    }

    private void fail(String step, String detail) {
        failed++;
        record("FAIL " + step + ": " + detail);
    }

    private void info(String step, String detail) {
        record("INFO " + step + ": " + detail);
    }

    private void record(String line) {
        results.add(line);
        LOG.info("[GAMETEST] {}", line);
    }

    private void finish() {
        finished = true;
        record("SUMMARY passed=" + passed + " failed=" + failed);

        File dir = new File(mc.gameDir, "gametest");
        try {
            Files.createDirectories(dir.toPath());
            Files.write(new File(dir, "results.txt").toPath(), results, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("[GAMETEST] cannot write results", e);
        }

        if (!Boolean.getBoolean("shincolle.gametest.keepOpen")) mc.shutdown();
    }

}
