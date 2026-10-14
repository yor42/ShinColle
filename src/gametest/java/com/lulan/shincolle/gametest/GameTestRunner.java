package com.lulan.shincolle.gametest;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.capability.CapaShipInventory;
import com.lulan.shincolle.network.C2SInputPackets;
import com.lulan.shincolle.proxy.CommonProxy;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.tileentity.TileEntityWaypoint;
import com.lulan.shincolle.utility.TargetHelper;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
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
import net.minecraft.tileentity.TileEntity;
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
                if (!CONNECT.isEmpty()) buildDedicatedSteps();
                else if (FramePerf.ENABLED) buildRenderPerfSteps();    //opt-in: -Dshincolle.gametest.renderperf=true replaces the normal suite
                else buildSteps();
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

        //Task B diagnosis: baseline (same conditions as run 3), then variations that each change one thing
        //morale NORMAL_MORALE (2100 < m <= 3900, BuffHelper.updateBuffMorale) so the tier gives no HIT penalty
        steps.add(combatStep("ship-attacks-husk", 800, true, 6D, NORMAL_MORALE, true));
        steps.add(feedGrudgeStep());
        steps.add(combatStep("ship-attacks-husk-after-grudge", 800, true, 6D, NORMAL_MORALE, true));
        steps.add(combatStep("ship-attacks-husk-close-no-teleport", 800, false, 3D, NORMAL_MORALE, true));
        steps.add(combatStep("ship-attacks-husk-inside-range", 800, true, 1.2D, NORMAL_MORALE, true));
        //AI-10 characterization: at morale 0 (Exhausted tier, HIT -4) a husk 6 blocks away is expected to be ignored
        steps.add(combatStep("ai10-morale0-ignores-far-husk", 200, true, 6D, 0, false));

        //Task A: issue #1 crafting task conservation
        //planks 5 in one stack: partial takes from a stack larger than 1
        steps.add(craftStep("crafting-task-stack-gt1", new int[][] {{3, 5}}));
        //planks as three single-item stacks: whole-stack takes of a stack of exactly 1
        steps.add(craftStep("crafting-task-stack-eq1", new int[][] {{0, 1}, {5, 1}, {9, 1}}));

        steps.add(new Step("save-quit-reload", 1200) {
            private int rejoinTick = -1;
            private int expectedLevel;

            @Override
            boolean tick(int t) throws Exception {
                if (shipId == null) {
                    fail(name, "no ship from previous step");
                    return true;
                }
                if (t == 1) {
                    //the ship can level up during the combat steps (run 9d: level 2); compare against the level at quit time
                    expectedLevel = callServer(() -> serverShip().getLevel());
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
                else if (detail.startsWith("level=" + expectedLevel + " ownerUID=" + shipUid + " ")) pass(name, detail);
                else fail(name, "ship changed: expected level=" + expectedLevel + " ownerUID=" + shipUid + ", got " + detail);
                screenshot("reloaded");
                return true;
            }
        });

    }

    /* ---------------- Task B: combat probe ---------------- */

    /**
     * Spawns a husk huskDx blocks from the ship and waits for damage. Probes the server state every 8 ticks.
     * teleportPlayer: move the player 8 blocks behind the ship first (as run 3 did).
     */
    private static final int NORMAL_MORALE = 3000;

    /**
     * Server thread. A ship that actually fights burns Grudge; at 0 it gets NoFuel, which by design clears its AI and sets
     * morale 0 (BasicEntityShip.updateFuelState). Steps after the first fight must therefore start with fuel, or they
     * measure the starvation rule instead of the behaviour under test (the run 6 diagnosis).
     */
    private static void refuel(BasicEntityShip s) {
        //Unconditional reset so every step starts from the same ship state (runs 7-8 diagnosis): a threshold such as
        //"grudge < 100" let a run start a fight at ~150 and starve mid-fight, and HP/ammo carried over between steps
        //until the husk killed the ship ("ship not loaded on server").
        s.addGrudge(1000 - s.getGrudge());
        s.setHealth(s.getMaxHealth());
        s.deathTime = 0;
        s.extinguish();
        s.hurtResistantTime = 0;
        s.setAmmoLight(60);
        s.decrGrudgeNum(0);    //recompute NoFuel and re-add the AI tasks, as the game does
    }

    /**
     * morale: value set on the ship before the husk spawns (-1 = leave as is).
     * expectEngage: true = PASS when the husk is damaged; false = PASS when nothing happens until the timeout.
     */
    private Step combatStep(String stepName, int stepTimeout, boolean teleportPlayer, double huskDx, int morale, boolean expectEngage) {
        return new Step(stepName, stepTimeout) {
            private UUID huskId;
            private boolean targetSeen;

            @Override
            boolean tick(int t) throws Exception {
                if (shipId == null) {
                    fail(name, "no ship from previous step");
                    return true;
                }
                if (t == 1) {
                    huskId = callServer(() -> {
                        BasicEntityShip s = serverShip();
                        for (Entity e : new ArrayList<>(s.world.loadedEntityList)) {
                            if (e instanceof EntityHusk) e.setDead();
                        }
                        s.setEntitySit(false);
                        refuel(s);
                        if (morale >= 0) {
                            s.setMorale(morale);
                            s.calcShipAttributes(4, false);    //recalc the morale buff, as the game's own morale update does (flag 4)
                        }
                        if (teleportPlayer) serverPlayer().connection.setPlayerLocation(s.posX, s.posY, s.posZ - 8D, 0F, 20F);
                        EntityHusk h = new EntityHusk(s.world);
                        h.setLocationAndAngles(s.posX + huskDx, s.posY, s.posZ, 0F, 0F);
                        h.enablePersistence();
                        //Keep the husk where it was placed in every combat step. A moving husk walks into the ship and
                        //melees it, and the ship's shots miss with up to 50% chance (CombatHelper.calcMissRate), so a few misses
                        //left the husk undamaged while the ship was killed (runs 8, attempts 1 and 3). The steps test whether the
                        //ship ENGAGES (target set, shots fired), not husk pathing. Also, for the control case, a husk that
                        //walks into aiRange is legitimately engaged.
                        h.setNoAI(true);
                        s.world.spawnEntity(h);
                        return h.getUniqueID();
                    });
                    return false;
                }
                if (t > 5) lookAt(clientShip());
                if (t == 100) screenshot(name);
                if (t % 8 != 0) return false;

                String[] out = callServer(() -> {
                    BasicEntityShip s = serverShip();
                    Entity h = server().getWorld(0).getEntityFromUuid(huskId);
                    if (s.getEntityTarget() != null) targetSeen = true;
                    String probe = probe(s, h, serverPlayer());
                    String done = null;
                    if (h == null || !h.isEntityAlive()) done = "husk dead";
                    else if (((EntityHusk) h).getHealth() < ((EntityHusk) h).getMaxHealth())
                        done = "husk hp " + ((EntityHusk) h).getHealth() + "/" + ((EntityHusk) h).getMaxHealth();
                    return new String[] {probe, done};
                });
                info(name, "probe t=" + t + " " + out[0]);

                if (out[1] != null) {
                    if (expectEngage) pass(name, out[1] + " after " + t + " ticks, ship target seen " + targetSeen);
                    else fail(name, "expected the husk to be ignored but: " + out[1] + " after " + t + " ticks; last probe: " + out[0]);
                    killHusks();
                    return true;
                }
                if (t >= timeout - 8) {
                    if (expectEngage) fail(name, "husk undamaged after " + t + " ticks, ship target seen " + targetSeen + "; last probe: " + out[0]);
                    else if (targetSeen) fail(name, "ship set a target although the husk was expected to be ignored; last probe: " + out[0]);
                    else pass(name, "husk ignored for " + t + " ticks (expected at this morale); last probe: " + out[0]);
                    killHusks();
                    return true;
                }
                return false;
            }
        };
    }

    private void killHusks() throws Exception {
        callServer(() -> {
            for (Entity e : new ArrayList<>(server().getWorld(0).loadedEntityList)) {
                if (e instanceof EntityHusk) e.setDead();
            }
            return null;
        });
    }

    /** one line with everything that decides whether the range-target AI can pick the husk (server thread only) */
    private String probe(BasicEntityShip s, Entity husk, EntityPlayerMP p) {
        StringBuilder sb = new StringBuilder();
        float attrRange = s.getAttrs().getAttackRange();
        int range = (int) attrRange;
        if (range < 2) range = Math.max(2, s.getStateMinor(ID.M.FollowMax) + 2);    //same rule as EntityAIShipRangeTarget.updateRange
        sb.append("morale=").append(s.getMorale()).append(" attrRange=").append(attrRange).append(" aiRange=").append(range)
                .append(" followMin/Max=").append(s.getStateMinor(ID.M.FollowMin)).append('/').append(s.getStateMinor(ID.M.FollowMax));
        if (husk != null) {
            boolean inBox = s.getEntityBoundingBox().grow(range, range * 0.75D, range).intersects(husk.getEntityBoundingBox());
            sb.append(" dist=").append(String.format("%.1f", s.getDistance(husk)))
                    .append(" inAABB=").append(inBox)
                    .append(" selector(real TargetHelper.Selector)=").append(new TargetHelper.Selector(s).apply(husk))
                    .append(" canSee=").append(s.canEntityBeSeen(husk));
        } else {
            sb.append(" husk=null");
        }
        sb.append(" | NoFuel=").append(s.getStateFlag(ID.F.NoFuel))
                .append(" grudge=").append(s.getStateMinor(ID.M.NumGrudge))
                .append(" PassiveAI=").append(s.getStateFlag(ID.F.PassiveAI))
                .append(" AntiAir=").append(s.getStateFlag(ID.F.AntiAir))
                .append(" AntiSS=").append(s.getStateFlag(ID.F.AntiSS))
                .append(" PVPFirst=").append(s.getStateFlag(ID.F.PVPFirst))
                .append(" ammoL/H=").append(s.getStateMinor(ID.M.NumAmmoLight)).append('/').append(s.getStateMinor(ID.M.NumAmmoHeavy))
                .append(" | hp=").append(String.format("%.1f/%.1f", s.getHealth(), s.getMaxHealth()))
                .append(" isDead=").append(s.isDead).append(" deathTime=").append(s.deathTime)
                .append(" | sitting=").append(s.isSitting())
                .append(" CanFollow=").append(s.getStateFlag(ID.F.CanFollow))
                .append(" ownerDist=").append(String.format("%.1f", s.getDistance(p)))
                .append(" | attackTarget=").append(s.getAttackTarget())
                .append(" entityTarget=").append(s.getEntityTarget())
                .append(" | targetTasks=").append(describeTasks(s.targetTasks))
                .append(" tasks=").append(describeTasks(s.tasks));
        return sb.toString();
    }

    /** entry list; '*' marks entries in EntityAITasks.executingTaskEntries (private, read by reflection) */
    private String describeTasks(EntityAITasks tasks) {
        Set<?> running = Collections.emptySet();
        try {
            for (Field f : EntityAITasks.class.getDeclaredFields()) {
                //EntityAITasks has two Set fields (taskEntries, executingTaskEntries); take the second declared or the named one
                if (Set.class.isAssignableFrom(f.getType()) && f.getName().toLowerCase().contains("execut")) {
                    f.setAccessible(true);
                    running = (Set<?>) f.get(tasks);
                    break;
                }
            }
        } catch (Exception e) {
            return "(reflection failed: " + e + ")";
        }
        StringBuilder sb = new StringBuilder("[");
        for (EntityAITasks.EntityAITaskEntry e : tasks.taskEntries) {
            sb.append(e.action.getClass().getSimpleName()).append('@').append(e.priority);
            if (running.contains(e)) sb.append('*');
            sb.append(' ');
        }
        return sb.append("](").append(tasks.taskEntries.size()).append(" entries, ").append(running.size()).append(" running)").toString();
    }

    /** feeds Grudge items by right-click (the player path), then waits for NoFuel to clear */
    private Step feedGrudgeStep() {
        return new Step("feed-grudge", 500) {
            private int grudgeBefore;
            private boolean forced;

            @Override
            boolean tick(int t) throws Exception {
                if (shipId == null) {
                    fail(name, "no ship from previous step");
                    return true;
                }
                if (t == 1) {
                    setHeldItem(new ItemStack(ModItems.Grudge, 4, 0));
                    grudgeBefore = callServer(() -> serverShip().getStateMinor(ID.M.NumGrudge));
                    return false;
                }
                if (t == 10) {
                    interact(clientShip());
                    return false;
                }
                if (t < 30 || t % 8 != 0) return false;

                Object[] r = callServer(() -> {
                    BasicEntityShip s = serverShip();
                    return new Object[] {s.getStateMinor(ID.M.NumGrudge), s.getStateFlag(ID.F.NoFuel),
                            s.targetTasks.taskEntries.size(), s.tasks.taskEntries.size()};
                });
                String detail = "grudge " + grudgeBefore + " -> " + r[0] + ", NoFuel=" + r[1] + ", targetTasks=" + r[2] + ", tasks=" + r[3]
                        + (forced ? " (decrGrudgeNum(0) was forced)" : "");
                if ((int) r[0] > 0 && !(boolean) r[1] && (int) r[2] > 0) {
                    setHeldItem(ItemStack.EMPTY);
                    pass(name, detail);
                    return true;
                }
                if (t >= 240 && (int) r[0] > 0 && !forced) {
                    //NoFuel did not clear on its own; record that and recompute the flag so later steps can run
                    forced = true;
                    info(name, "NoFuel still set 240 ticks after feeding; forcing decrGrudgeNum(0): " + detail);
                    callServer(() -> {
                        serverShip().decrGrudgeNum(0);
                        return null;
                    });
                }
                if (t >= timeout - 8) {
                    setHeldItem(ItemStack.EMPTY);
                    fail(name, detail);
                    return true;
                }
                return false;
            }
        };
    }

    /* ---------------- Task A: issue #1 crafting task ---------------- */

    private BlockPos wpPos;
    private BlockPos chestPos;

    /**
     * Shortcut setup (server thread): waypoint block + chest, pairing, recipe paper in ship slot 22, task 4 (crafting),
     * guard on the waypoint. Then the ship's own TaskHelper.onUpdateCrafting runs; the step only counts items.
     * planksStacks: {chest slot, count} of oak planks. Recipe on the paper: sticks from planks in grid slots 1 and 4.
     */
    private Step craftStep(String stepName, int[][] planksStacks) {
        return new Step(stepName, 400) {
            private int[] before;

            @Override
            boolean tick(int t) throws Exception {
                if (shipId == null) {
                    fail(name, "no ship from previous step");
                    return true;
                }
                if (t == 1) {
                    before = callServer(() -> {
                        BasicEntityShip s = serverShip();
                        WorldServer w = server().getWorld(0);
                        EntityPlayerMP p = serverPlayer();
                        s.setEntitySit(false);
                        refuel(s);
                        p.connection.setPlayerLocation(s.posX, s.posY, s.posZ - 3D, 0F, 0F);

                        if (wpPos == null) {
                            wpPos = new BlockPos(MathHelper.floor(s.posX) + 2, MathHelper.floor(s.posY), MathHelper.floor(s.posZ) + 2);
                            chestPos = wpPos.east();
                            w.setBlockState(wpPos, ModBlocks.BlockWaypoint.getDefaultState());
                            w.setBlockState(chestPos, Blocks.CHEST.getDefaultState());
                        }
                        TileEntityWaypoint wp = (TileEntityWaypoint) w.getTileEntity(wpPos);
                        TileEntityChest chest = (TileEntityChest) w.getTileEntity(chestPos);
                        wp.setPlayerUID(s.getPlayerUID());
                        wp.setPairedChest(chestPos);

                        for (int i = 0; i < chest.getSizeInventory(); i++) chest.setInventorySlotContents(i, ItemStack.EMPTY);
                        for (int[] st : planksStacks) chest.setInventorySlotContents(st[0], new ItemStack(Blocks.PLANKS, st[1], 0));
                        //distractors: same chest, not in the recipe
                        chest.setInventorySlotContents(20, new ItemStack(Blocks.COBBLESTONE, 1));
                        chest.setInventorySlotContents(21, new ItemStack(Blocks.COBBLESTONE, 7));

                        CapaShipInventory inv = s.getCapaShipInventory();
                        for (int i = 12; i <= 20; i++) inv.setInventorySlotContents(i, ItemStack.EMPTY);

                        ItemStack paper = new ItemStack(ModItems.RecipePaper);
                        NBTTagCompound nbt = new NBTTagCompound();
                        NBTTagList list = new NBTTagList();
                        for (int slot : new int[] {1, 4}) {
                            NBTTagCompound tag = new NBTTagCompound();
                            tag.setInteger("Slot", slot);
                            new ItemStack(Blocks.PLANKS, 1, 0).writeToNBT(tag);
                            list.appendTag(tag);
                        }
                        NBTTagCompound res = new NBTTagCompound();
                        res.setInteger("Slot", 9);
                        new ItemStack(Items.STICK, 4).writeToNBT(res);
                        list.appendTag(res);
                        nbt.setTag("Recipe", list);
                        paper.setTagCompound(nbt);
                        inv.setInventorySlotContents(22, paper);

                        for (EntityItem ei : w.getEntitiesWithinAABB(EntityItem.class, s.getEntityBoundingBox().grow(12D))) ei.setDead();

                        s.setStateMinor(ID.M.Task, 4);
                        s.setStateMinor(ID.M.TaskSide, 1 << 18);    //bit 18 = check metadata
                        s.setGuardedPos(wpPos.getX(), wpPos.getY(), wpPos.getZ(), 0, 1);

                        return new int[] {count(s, chest, Item.getItemFromBlock(Blocks.PLANKS), 0)[3], count(s, chest, Items.STICK, -1)[3],
                                count(s, chest, Item.getItemFromBlock(Blocks.COBBLESTONE), -1)[3]};
                    });
                    info(name, "setup planks=" + before[0] + " sticks=" + before[1] + " cobble=" + before[2]
                            + " (shortcut: blocks, pairing, paper, task and guard set on the server thread)");
                    return false;
                }
                if (t % 40 != 0) return false;

                String[] out = callServer(() -> {
                    BasicEntityShip s = serverShip();
                    TileEntityChest chest = (TileEntityChest) server().getWorld(0).getTileEntity(chestPos);
                    int[] pl = count(s, chest, Item.getItemFromBlock(Blocks.PLANKS), 0);
                    int[] st = count(s, chest, Items.STICK, -1);
                    int[] cb = count(s, chest, Item.getItemFromBlock(Blocks.COBBLESTONE), -1);
                    String d = "planks chest/ship/ground=" + pl[0] + "/" + pl[1] + "/" + pl[2] + " sticks=" + st[0] + "/" + st[1] + "/" + st[2]
                            + " cobble=" + cb[0] + "/" + cb[1] + "/" + cb[2] + " | Task=" + s.getStateMinor(ID.M.Task)
                            + " NoFuel=" + s.getStateFlag(ID.F.NoFuel) + " held=" + s.getHeldItemMainhand().getItem().getRegistryName()
                            + " distToChest=" + String.format("%.1f", Math.sqrt(s.getDistanceSq(chestPos)));
                    return new String[] {d, pl[3] + "," + st[3] + "," + cb[3] + "," + cb[0]};
                });
                info(name, "t=" + t + " " + out[0]);
                if (t < 200) return false;

                String[] tot = out[1].split(",");
                int planks = Integer.parseInt(tot[0]);
                int sticks = Integer.parseInt(tot[1]);
                int cobble = Integer.parseInt(tot[2]);
                int cobbleChest = Integer.parseInt(tot[3]);
                int crafts = sticks / 4;
                boolean ok = sticks % 4 == 0 && sticks == 4 * (before[0] / 2) && planks + 2 * crafts == before[0]
                        && cobble == before[2] && cobbleChest == before[2];
                String detail = "before planks=" + before[0] + " sticks=" + before[1] + " cobble=" + before[2]
                        + "; after planks(total)=" + planks + " sticks=" + sticks + " cobble=" + cobble + " (chest " + cobbleChest + ")"
                        + "; expected sticks=" + 4 * (before[0] / 2) + ", planks + 2*crafts=" + (planks + 2 * crafts) + " vs " + before[0]
                        + "; " + out[0];
                if (ok) pass(name, detail);
                else fail(name, detail);
                return true;
            }
        };
    }

    /** {chest, ship slots 0..23, ground items near ship, total} of one item type (meta < 0 = any) */
    private int[] count(BasicEntityShip s, TileEntityChest chest, Item item, int meta) {
        int c = 0, sh = 0, g = 0;
        for (int i = 0; i < chest.getSizeInventory(); i++) c += countStack(chest.getStackInSlot(i), item, meta);
        CapaShipInventory inv = s.getCapaShipInventory();
        for (int i = 0; i < inv.getSizeInventory(); i++) sh += countStack(inv.getStackInSlot(i), item, meta);
        for (EntityItem ei : s.world.getEntitiesWithinAABB(EntityItem.class, s.getEntityBoundingBox().grow(12D))) {
            g += countStack(ei.getItem(), item, meta);
        }
        return new int[] {c, sh, g, c + sh + g};
    }

    private static int countStack(ItemStack st, Item item, int meta) {
        if (st.isEmpty() || st.getItem() != item) return 0;
        return (meta < 0 || st.getMetadata() == meta) ? st.getCount() : 0;
    }

    /* ---------------- dedicated server mode (issue #2 / NET-02) ---------------- */

    /** host:port of the dedicated server to join; empty = normal single-player run */
    private static final boolean SPAWN_OFF = Boolean.getBoolean("shincolle.gametest.spawnoff");
    private static final String CONNECT = System.getProperty("shincolle.gametest.connect", "");
    private final List<String[]> chat = new ArrayList<>();    //{json, plain text} of every chat line received
    private RconClient rcon;

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        chat.add(new String[] {ITextComponent.Serializer.componentToJson(event.getMessage()), event.getMessage().getUnformattedText()});
        if (!CONNECT.isEmpty()) LOG.info("[GAMETEST] chat: {}", event.getMessage().getUnformattedText());
    }

    private void sendChat(String msg) {
        info("client", "chat> " + msg);
        mc.player.sendChatMessage(msg);
    }

    private String rconCmd(String cmd) throws Exception {
        String reply = rcon.command(cmd);
        info("rcon", cmd + " -> " + reply.trim());
        return reply;
    }

    /** latest "[GT] state ..." line received at or after chat index since, as key=value map; null if none */
    private Map<String, String> parseState(int since) {
        for (int i = chat.size() - 1; i >= since; i--) {
            String t = chat.get(i)[1];
            if (t.startsWith("[GT] state")) {
                Map<String, String> m = new HashMap<>();
                for (String tok : t.substring(5).split(" ")) {
                    int eq = tok.indexOf('=');
                    if (eq > 0) m.put(tok.substring(0, eq), tok.substring(eq + 1));
                }
                return m;
            }
        }
        return null;
    }

    private static String stateKey(Map<String, String> m) {
        return "ownerUID=" + m.get("ownerUID") + " level=" + m.get("level") + " bonus=" + m.get("bonus");
    }

    private static boolean sameShipState(Map<String, String> a, Map<String, String> b) {
        return Objects.equals(a.get("ownerUID"), b.get("ownerUID")) && Objects.equals(a.get("ownerUUID"), b.get("ownerUUID"))
                && Objects.equals(a.get("level"), b.get("level")) && Objects.equals(a.get("bonus"), b.get("bonus"));
    }

    private int countChat(int since, String needle) {
        int n = 0;
        for (int i = since; i < chat.size(); i++) if (chat.get(i)[0].contains(needle)) n++;
        return n;
    }

    /**
     * One case: reads the server state (via /gtprobe, a server-thread command that replies in chat),
     * performs the action, reads the state again after a delay, then checks.
     */
    private abstract class DedicatedCase extends Step {
        Map<String, String> before;
        int chatAtAct;
        private int mark;
        private int afterMark;

        DedicatedCase(String name) {
            super(name, 900);
        }

        /** ticks 1..9, before the baseline is read */
        void prep(int t) throws Exception {
        }

        /** u = 1, 2, ... after the baseline was read */
        abstract void act(int u) throws Exception;

        abstract int actTicks();

        /** @return null when the case passed, otherwise the failure detail */
        abstract String check(Map<String, String> after);

        @Override
        boolean tick(int t) throws Exception {
            if (shipId != null) lookAt(clientShip());    //crosshair on the ship: the /ship* commands read the mouse-over target
            if (t < 10) {
                prep(t);
                return false;
            }
            if (t == 10) {
                mark = chat.size();
                sendChat("/gtprobe state");
                return false;
            }
            if (t == 20) {
                before = parseState(mark);
                if (before == null) {
                    fail(name, "no baseline state received");
                    return true;
                }
                if (!"1".equals(before.get("ships")) || before.get("ownerUID") == null) {
                    fail(name, "precondition failed: expected exactly 1 ship on the server, got ships=" + before.get("ships"));
                    return true;
                }
                info(name, "baseline " + stateKey(before) + " op=" + before.get("op") + " gm=" + before.get("gm"));
                chatAtAct = chat.size();
                return false;
            }
            int actEnd = 20 + actTicks();
            if (t > 20 && t <= actEnd) {
                act(t - 20);
                return false;
            }
            if (t == actEnd + 10) {
                afterMark = chat.size();
                sendChat("/gtprobe state");
                return false;
            }
            if (t == actEnd + 30) {
                Map<String, String> after = parseState(afterMark);
                if (after == null) {
                    fail(name, "no final state received");
                    return true;
                }
                info(name, "final " + stateKey(after) + " op=" + after.get("op") + " gm=" + after.get("gm"));
                String problem = check(after);
                if (problem == null) pass(name, "server state " + stateKey(before) + " -> " + stateKey(after)
                        + " (op=" + after.get("op") + " gm=" + after.get("gm") + ")");
                else fail(name, problem);
                return true;
            }
            return false;
        }
    }

    private void buildDedicatedSteps() {
        final String playerName = mc.getSession().getUsername();
        final String[] hostPort = System.getProperty("shincolle.gametest.rcon", "127.0.0.1:25631").split(":");

        steps.add(new Step("dedi-connect", 1800) {
            @Override
            boolean tick(int t) {
                if (t == 1) {
                    mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(), mc, new ServerData("gametest", CONNECT, false)));
                    return false;
                }
                if (mc.player == null || mc.world == null || mc.player.ticksExisted < 80) return false;
                pass(name, "joined " + CONNECT + " as " + mc.player.getName() + ", " + Loader.instance().getActiveModList().size() + " mods");
                return true;
            }
        });

        steps.add(new Step("dedi-rcon-setup", 200) {
            @Override
            boolean tick(int t) throws Exception {
                rcon = new RconClient(hostPort[0], Integer.parseInt(hostPort[1]), System.getProperty("shincolle.gametest.rconpass", ""));
                String list = rconCmd("list");
                rconCmd("deop " + playerName);
                rconCmd("gamemode survival " + playerName);
                pass(name, "RCON " + hostPort[0] + ":" + hostPort[1] + " ok; " + list.trim());
                return true;
            }
        });

        steps.add(new Step("dedi-spawn-ship-with-egg", 400) {
            private int m1, m2, m3;
            private int playerUid = -1;

            @Override
            boolean tick(int t) throws Exception {
                if (t >= 11 && t < 60) mc.player.rotationPitch = 90F;
                if (t == 1) {
                    m1 = chat.size();
                    sendChat("/gtprobe state");
                    return false;
                }
                if (t == 10) {
                    Map<String, String> s = parseState(m1);
                    if (s == null) {
                        fail(name, "no state reply");
                        return true;
                    }
                    info(name, "precondition op=" + s.get("op") + " gm=" + s.get("gm") + " ships=" + s.get("ships"));
                    if (!"false".equals(s.get("op")) || !"SURVIVAL".equals(s.get("gm"))) {
                        fail(name, "player must be non-OP survival at this point: op=" + s.get("op") + " gm=" + s.get("gm"));
                        return true;
                    }
                    playerUid = Integer.parseInt(s.get("playerUID"));
                    sendChat("/gtprobe egg");
                    return false;
                }
                if (t == 30) {
                    if (mc.player.getHeldItemMainhand().getItem() != ModItems.ShipSpawnEgg) {
                        fail(name, "egg did not arrive in the main hand: " + mc.player.getHeldItemMainhand());
                        return true;
                    }
                    EnumActionResult r = mc.playerController.processRightClick(mc.player, mc.world, EnumHand.MAIN_HAND);
                    info(name, "client processRightClick -> " + r);
                    return false;
                }
                if (t == 70) {
                    m2 = chat.size();
                    sendChat("/gtprobe fakeowner");    //setup shortcut: give the ship a foreign owner so a hijack is visible
                    return false;
                }
                if (t == 85) {
                    m3 = chat.size();
                    sendChat("/gtprobe state");
                    return false;
                }
                if (t == 100) {
                    Map<String, String> s = parseState(m3);
                    String fake = null;
                    for (int i = m2; i < chat.size(); i++) if (chat.get(i)[1].startsWith("[GT] fakeowner")) fake = chat.get(i)[1];
                    if (s == null || !"1".equals(s.get("ships")) || fake == null) {
                        fail(name, "ship not found or fakeowner failed: ships=" + (s == null ? "?" : s.get("ships")) + " fake=" + fake);
                        return true;
                    }
                    boolean eggOwned = fake.contains("prevOwnerUID=" + playerUid);
                    shipId = UUID.fromString(s.get("uuid"));
                    if (eggOwned && "9999".equals(s.get("ownerUID"))) {
                        pass(name, "egg ship owned by player UID " + playerUid + " at spawn, then set to foreign owner 9999 (shortcut); " + stateKey(s));
                    } else {
                        fail(name, "unexpected owner: " + fake + " " + stateKey(s));
                    }
                    return true;
                }
                return false;
            }
        });

        //1. non-OP, survival: forged C2S packets for the player's own ship must not change the server state
        steps.add(new DedicatedCase("dedi-nonop-survival-forged-packets") {
            @Override
            int actTicks() {
                return 5;
            }

            @Override
            void act(int u) {
                if (u != 1) return;
                int eid = Integer.parseInt(before.get("eid"));
                int dim = mc.world.provider.getDimension();
                info(name, "forging CmdChOwner(owner eid " + mc.player.getEntityId() + ", ship eid " + eid + ") and CmdShipAttr(level 77, bonus 9 x6)");
                CommonProxy.channelI.sendToServer(new C2SInputPackets(C2SInputPackets.PID.CmdChOwner, mc.player.getEntityId(), eid, dim));
                CommonProxy.channelI.sendToServer(new C2SInputPackets(C2SInputPackets.PID.CmdShipAttr, eid, dim, 77, 9, 9, 9, 9, 9, 9));
            }

            @Override
            String check(Map<String, String> after) {
                if (!"false".equals(before.get("op")) || !"SURVIVAL".equals(before.get("gm")))
                    return "precondition failed: op=" + before.get("op") + " gm=" + before.get("gm");
                if (!sameShipState(before, after)) return "server state CHANGED by forged packets: " + stateKey(before) + " -> " + stateKey(after);
                return null;
            }
        });

        //2. non-OP, creative (RCON /gamemode): the /ship commands must be refused
        steps.add(new DedicatedCase("dedi-nonop-creative-commands") {
            @Override
            int actTicks() {
                return 80;
            }

            @Override
            void act(int u) throws Exception {
                if (u == 1) rconCmd("gamemode creative " + playerName);
                if (u == 15) sendChat("/shipchangeowner " + playerName);
                if (u == 45) sendChat("/shipattrs 42 10 11 12 13 14 15");
            }

            @Override
            String check(Map<String, String> after) {
                int notop = countChat(chatAtAct, "command.notop");
                if (!"false".equals(after.get("op")) || !"CREATIVE".equals(after.get("gm")))
                    return "precondition failed: op=" + after.get("op") + " gm=" + after.get("gm");
                if (notop != 2) return "expected 2 notop refusals, got " + notop;
                if (!sameShipState(before, after)) return "server state CHANGED: " + stateKey(before) + " -> " + stateKey(after);
                return null;
            }
        });

        //3. OP (RCON /op): both commands must work end to end (command -> S2C -> client mouse-over -> C2S -> server)
        steps.add(new DedicatedCase("dedi-op-commands") {
            @Override
            int actTicks() {
                return 110;
            }

            @Override
            void act(int u) throws Exception {
                if (u == 1) {
                    rconCmd("op " + playerName);
                    rconCmd("gamemode survival " + playerName);
                }
                if (u == 20) sendChat("/shipchangeowner " + playerName);
                if (u == 65) sendChat("/shipattrs 42 10 11 12 13 14 15");
            }

            @Override
            String check(Map<String, String> after) {
                if (!"9999".equals(before.get("ownerUID"))) return "precondition failed: owner " + before.get("ownerUID");
                if (!"true".equals(after.get("op"))) return "player is not OP on the server: op=" + after.get("op");
                int notop = countChat(chatAtAct, "command.notop");
                if (notop != 0) return "unexpected notop refusal(s): " + notop;
                String problem = "";
                if (!after.get("playerUID").equals(after.get("ownerUID"))) problem += " owner not changed (" + after.get("ownerUID") + ", player " + after.get("playerUID") + ");";
                if (!"42".equals(after.get("level"))) problem += " level not changed (" + after.get("level") + ");";
                if (!"10,11,12,13,14,15".equals(after.get("bonus"))) problem += " bonus not set (" + after.get("bonus") + ");";
                return problem.isEmpty() ? null : problem.trim();
            }
        });

        //4. control: the same forged packets as case 1, now as OP, must take effect (shows the forged format is valid)
        steps.add(new DedicatedCase("dedi-op-forged-packets-control") {
            @Override
            void prep(int t) {
                if (t == 1) sendChat("/gtprobe fakeowner");
                if (t == 5) sendChat("/gtprobe reset");
            }

            @Override
            int actTicks() {
                return 5;
            }

            @Override
            void act(int u) {
                if (u != 1) return;
                int eid = Integer.parseInt(before.get("eid"));
                int dim = mc.world.provider.getDimension();
                CommonProxy.channelI.sendToServer(new C2SInputPackets(C2SInputPackets.PID.CmdChOwner, mc.player.getEntityId(), eid, dim));
                CommonProxy.channelI.sendToServer(new C2SInputPackets(C2SInputPackets.PID.CmdShipAttr, eid, dim, 77, 9, 9, 9, 9, 9, 9));
            }

            @Override
            String check(Map<String, String> after) {
                if (!"9999".equals(before.get("ownerUID")) || !"1".equals(before.get("level")))
                    return "precondition failed (reset): " + stateKey(before);
                if (!"true".equals(after.get("op"))) return "player is not OP: op=" + after.get("op");
                String problem = "";
                if (!after.get("playerUID").equals(after.get("ownerUID"))) problem += " owner not changed (" + after.get("ownerUID") + ");";
                if (!"77".equals(after.get("level"))) problem += " level not changed (" + after.get("level") + ");";
                if (!"9,9,9,9,9,9".equals(after.get("bonus"))) problem += " bonus not set (" + after.get("bonus") + ");";
                return problem.isEmpty() ? null : problem.trim();
            }
        });

        steps.add(new Step("dedi-stop-server", 200) {
            @Override
            boolean tick(int t) throws Exception {
                if (t == 1) {
                    rconCmd("deop " + playerName);
                    sendChat("/gtprobe stop");
                    return false;
                }
                if (t < 60) return false;
                pass(name, "stop requested through the server-thread /gtprobe stop");
                return true;
            }
        });
        //issue #10 (ENT-13): server runs with spawn-animals=false / spawn-npcs=false (-Pgametest_spawnoff=true);
        //vanilla would setDead() the friendly ship on its first tick. Only this case runs after the egg step in that mode.
        final Step spawnOff = new Step("dedi-spawnoff-ship-survives", 300) {
            private int m1, m2;
            private String uuid1;

            @Override
            boolean tick(int t) throws Exception {
                if (t == 1) {
                    m1 = chat.size();
                    sendChat("/gtprobe state");
                    return false;
                }
                if (t == 10) {
                    Map<String, String> s = parseState(m1);
                    if (s == null || !"1".equals(s.get("ships"))) {
                        fail(name, "ship is gone right after spawn: ships=" + (s == null ? "no reply" : s.get("ships")));
                        return true;
                    }
                    uuid1 = s.get("uuid");
                    info(name, "ship present at start: uuid=" + uuid1 + " shipUID=" + s.get("shipUID"));
                    return false;
                }
                if (t == 50) {
                    m2 = chat.size();
                    sendChat("/gtprobe state");
                    return false;
                }
                if (t == 60) {
                    Map<String, String> s = parseState(m2);
                    if (s == null || !"1".equals(s.get("ships")) || !Objects.equals(uuid1, s.get("uuid"))) {
                        fail(name, "ship was removed with spawn-animals=false: ships=" + (s == null ? "no reply" : s.get("ships")));
                        return true;
                    }
                    int uid = Integer.parseInt(s.get("shipUID"));
                    if (uid <= 0) {
                        fail(name, "ship alive but has no shipUID: " + uid);
                        return true;
                    }
                    pass(name, "ship still alive after ~50 ticks with spawn-animals=false/spawn-npcs=false: uuid=" + uuid1 + " shipUID=" + uid);
                    return true;
                }
                return false;
            }
        };
        if (SPAWN_OFF) {
            steps.removeIf(st -> st.name.startsWith("dedi-nonop") || st.name.startsWith("dedi-op"));
            steps.add(steps.size() - 1, spawnOff);    //before dedi-stop-server
        }
    }

    /* ---------------- opt-in client render profiling (-Dshincolle.gametest.renderperf=true) ---------------- */

    private static final int PERF_FRAMES = Integer.getInteger("shincolle.gametest.renderperf.frames", 600);
    private static final int PERF_WARMUP = Integer.getInteger("shincolle.gametest.renderperf.warmup", 300);
    //copy of MulitBlockHelper.PATTERN[0] [x][y][z]; 1 polymetal, 2 heavy grudge, -1 anything
    private static final int[][][] PERF_PATTERN = {
            {{1, 1, 1}, {1, -1, 1}, {-1, -1, -1}},
            {{1, 1, 1}, {-1, -1, -1}, {-1, 2, -1}},
            {{1, 1, 1}, {1, -1, 1}, {-1, -1, -1}}};

    private FramePerf perf;
    private int perfBaseX, perfBaseZ, perfGroundY;
    private float perfYaw, perfPitch;
    private final List<ItemStack> eggPool = new ArrayList<>();
    private final List<String> perfCsv = new ArrayList<>();
    private BlockPos shipyardMaster;
    private final List<BlockPos> shipyardBlocks = new ArrayList<>();
    private volatile int perfEngaged;
    private volatile int perfHuskAlive;
    private volatile int perfHuskSpawned;
    private final List<String> combatSamples = new ArrayList<>();
    private final java.util.Random perfRnd = new java.util.Random(42L);

    /** a measured window: setup, settle ticks, warm-up frames (discarded), then frames recorded by FramePerf */
    private abstract class PerfStep extends Step {
        private final int settle;
        private final int warm;
        private final int frames;
        private boolean armed;
        private String rendBefore = "";

        PerfStep(String name, int settle, int warm, int frames) {
            super(name, 30000);
            this.settle = settle;
            this.warm = warm;
            this.frames = frames;
        }

        abstract void setup() throws Exception;

        void during(int t) throws Exception {
        }

        void after() throws Exception {
        }

        @Override
        boolean tick(int t) throws Exception {
            if (t == 1) {
                perfYaw = 0F;
                perfPitch = 5F;
                setup();
            }
            mc.player.rotationYaw = mc.player.prevRotationYaw = perfYaw;
            mc.player.rotationPitch = mc.player.prevRotationPitch = perfPitch;
            during(t);

            if (!armed) {
                if (t >= settle) {
                    armed = true;
                    rendBefore = mc.renderGlobal.getDebugInfoEntities();
                    perf.begin(warm, frames);
                }
                return false;
            }
            if (!perf.isDone()) return false;

            String rendAfter = mc.renderGlobal.getDebugInfoEntities();
            int clientShips = 0;
            int clientEntities = mc.world.loadedEntityList.size();
            for (Entity e : mc.world.loadedEntityList) if (e instanceof BasicEntityShip) clientShips++;
            pass(name, perf.summary() + " | clientShips=" + clientShips + " clientEntities=" + clientEntities
                    + " clientTileEntities=" + mc.world.loadedTileEntityList.size() + " | renderer before=[" + rendBefore + "] after=[" + rendAfter + "]");
            perfCsv.add(name + "," + perf.csv());
            after();
            return true;
        }
    }

    private void perfSettings() {
        mc.gameSettings.enableVsync = false;
        mc.gameSettings.limitFramerate = 260;    //GameSettings.Options.FRAMERATE_LIMIT max = unlimited
        org.lwjgl.opengl.Display.setVSyncEnabled(false);
    }

    private void buildRenderPerfSteps() {
        perf = new FramePerf();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(perf);

        steps.add(new Step("perf-enter-world", 1200) {
            @Override
            boolean tick(int t) throws Exception {
                if (t == 1) {
                    perfSettings();
                    WorldSettings settings = new WorldSettings(20261013L, GameType.CREATIVE, false, false, WorldType.FLAT);
                    settings.enableCommands();
                    mc.launchIntegratedServer(worldFolder, worldFolder, settings);
                    return false;
                }
                if (mc.player == null || mc.getIntegratedServer() == null) return false;
                if (mc.player.ticksExisted < 100) return false;

                String ground = callServer(() -> {
                    WorldServer w = server().getWorld(0);
                    w.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
                    w.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
                    w.setWorldTime(6000L);
                    EntityPlayerMP p = serverPlayer();
                    perfBaseX = MathHelper.floor(p.posX);
                    perfBaseZ = MathHelper.floor(p.posZ);
                    perfGroundY = MathHelper.floor(p.posY) - 1;
                    return w.getBlockState(new BlockPos(perfBaseX, perfGroundY, perfBaseZ)).getBlock().getRegistryName() + " at y=" + perfGroundY
                            + ", player " + p.posX + "," + p.posY + "," + p.posZ;
                });
                NonNullList<ItemStack> eggs = NonNullList.create();
                ModItems.ShipSpawnEgg.getSubItems(ModItems.ShipSpawnEgg.getCreativeTab(), eggs);
                for (ItemStack s : eggs) if (s.getMetadata() > 1 && s.getMetadata() <= 2000) eggPool.add(s.copy());
                info(name, "egg pool " + eggPool.size() + " ship types; ground block " + ground);
                info(name, "settings: renderDistance=" + mc.gameSettings.renderDistanceChunks + " fancyGraphics=" + mc.gameSettings.fancyGraphics
                        + " vsync=" + mc.gameSettings.enableVsync + " fpsLimit=" + mc.gameSettings.limitFramerate + " window=" + mc.displayWidth + "x" + mc.displayHeight
                        + " fov=" + mc.gameSettings.fovSetting + " particles=" + mc.gameSettings.particleSetting + " entityShadows=" + mc.gameSettings.entityShadows
                        + " guiScale=" + mc.gameSettings.guiScale);
                info(name, "gl renderer=" + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER) + " vendor=" + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VENDOR)
                        + " java=" + System.getProperty("java.version") + " vm=" + System.getProperty("java.vm.name") + " cpus=" + Runtime.getRuntime().availableProcessors()
                        + " maxHeapMB=" + Runtime.getRuntime().maxMemory() / 1048576L + " mods=" + Loader.instance().getActiveModList().size());
                pass(name, "world " + worldFolder);
                return true;
            }
        });

        //S0: idle world, nothing in view (control). Longer warm-up: this is the first window, the JIT is coldest here.
        steps.add(new PerfStep("S0-idle-control", 100, PERF_WARMUP * 3, PERF_FRAMES) {
            @Override
            void setup() {
            }
        });

        //S1: formed Large Shipyard in view
        steps.add(new PerfStep("S1-large-shipyard", 120, PERF_WARMUP, PERF_FRAMES) {
            @Override
            void setup() throws Exception {
                final int mx = perfBaseX;
                final int mz = perfBaseZ + 6;
                final int my = perfGroundY + 3;
                shipyardMaster = new BlockPos(mx, my, mz);
                int type = callServer(() -> {
                    WorldServer w = server().getWorld(0);
                    for (int x = 0; x < 3; x++) {
                        for (int y = 0; y < 3; y++) {
                            for (int z = 0; z < 3; z++) {
                                int v = PERF_PATTERN[x][y][z];
                                BlockPos pos = new BlockPos(mx - 1 + x, my - 2 + y, mz - 1 + z);
                                if (v == 1) w.setBlockState(pos, ModBlocks.BlockPolymetal.getDefaultState());
                                else if (v == 2) w.setBlockState(pos, ModBlocks.BlockGrudgeHeavy.getDefaultState());
                                else continue;
                                shipyardBlocks.add(pos);
                            }
                        }
                    }
                    int ty = com.lulan.shincolle.utility.MulitBlockHelper.checkMultiBlockForm(w, mx, my, mz);
                    if (ty > 0) com.lulan.shincolle.utility.MulitBlockHelper.setupStructure(w, mx, my, mz, ty);
                    return ty;
                });
                info(name, "built " + shipyardBlocks.size() + " blocks, master " + shipyardMaster + ", checkMultiBlockForm=" + type);
                if (type <= 0) fail(name, "structure did not form (check returned " + type + ")");
                diagTile("t=1(setup)");
            }

            @Override
            void during(int t) throws Exception {
                //tile state for the first 5 s (100 ticks) after the structure was formed
                if (t > 1 && t <= 111 && t % 10 == 1) diagTile("t=" + t);
            }
        });

        //S1b: not a measurement. What happens on the client if the tile did have structType > 0 (forced by reflection, then restored)
        steps.add(new Step("S1b-forced-client-structType", 600) {
            private java.lang.reflect.Field f;

            @Override
            boolean tick(int t) throws Exception {
                mc.player.rotationYaw = mc.player.prevRotationYaw = perfYaw;
                mc.player.rotationPitch = mc.player.prevRotationPitch = perfPitch;
                if (t == 1) {
                    f = com.lulan.shincolle.tileentity.BasicTileMulti.class.getDeclaredField("structType");
                    f.setAccessible(true);
                    TileEntity te = findClientTile();
                    if (te == null) {
                        info(name, "no client tile at " + shipyardMaster);
                    } else {
                        f.setInt(te, 1);
                        info(name, "forced client tile structType=1 (reflection)");
                    }
                    diagTile("forced t=1");
                    return false;
                }
                if (t <= 101 && t % 10 == 1) diagTile("forced t=" + t);
                if (t == 102) {
                    TileEntity te = findClientTile();
                    if (te != null) f.setInt(te, 0);
                    info(name, "restored client tile structType=0");
                    callServer(() -> {
                        WorldServer w = server().getWorld(0);
                        for (BlockPos bp : shipyardBlocks) w.setBlockState(bp, Blocks.AIR.getDefaultState());
                        return null;
                    });
                    info(name, "removed shipyard blocks on the server");
                    return false;
                }
                if (t < 160) return false;
                pass(name, "diagnostic only");
                return true;
            }
        });

        //S2: idle ships on screen
        steps.add(new PerfStep("S2-ships-10-idle", 120, PERF_WARMUP, PERF_FRAMES) {
            @Override
            void setup() throws Exception {
                int n = callServer(() -> spawnPerfShips(0, 10));
                info(name, "spawn requested 10, server ships now " + n);
            }
        });
        steps.add(new PerfStep("S2-ships-30-idle", 120, PERF_WARMUP, PERF_FRAMES) {
            @Override
            void setup() throws Exception {
                int n = callServer(() -> spawnPerfShips(10, 30));
                info(name, "spawn requested 20 more, server ships now " + n);
            }
        });

        //S3: same 30 ships, husks without AI placed among them, topped up to 8 every second; ships refuelled
        steps.add(new PerfStep("S3-ships-30-combat", 120, PERF_WARMUP, PERF_FRAMES) {
            @Override
            void setup() throws Exception {
                perfHuskSpawned = 0;
                callServer(() -> {
                    maintainCombat();
                    return 0;
                });
            }

            @Override
            void during(int t) {
                if (t % 20 == 0) {
                    //non-blocking: callServer would stall the client frame loop for up to one server tick
                    server().addScheduledTask(GameTestRunner.this::maintainCombat);
                    if (t % 100 == 0) combatSamples.add("t=" + t + " engagedShips=" + perfEngaged + " huskAlive=" + perfHuskAlive + " huskSpawnedTotal=" + perfHuskSpawned);
                }
            }

            @Override
            void after() throws Exception {
                for (String s : combatSamples) info(name, "combat sample " + s);
                callServer(() -> {
                    for (Entity e : new ArrayList<>(server().getWorld(0).loadedEntityList)) if (e instanceof EntityHusk) e.setDead();
                    return null;
                });
            }
        });

        //S0b: control again after everything is removed (drift check)
        steps.add(new PerfStep("S0b-idle-control-after", 120, PERF_WARMUP, PERF_FRAMES) {
            @Override
            void setup() throws Exception {
                callServer(() -> {
                    for (Entity e : new ArrayList<>(server().getWorld(0).loadedEntityList)) {
                        if (e instanceof EntityHusk || e instanceof BasicEntityShip) e.setDead();
                    }
                    return null;
                });
            }
        });

        steps.add(new Step("perf-csv", 100) {
            @Override
            boolean tick(int t) {
                info(name, "csv header: scenario,interval_mean_ms,interval_p95_ms,cpu_mean_ms,cpu_p95_ms,alloc_mean_KB,alloc_p95_KB");
                for (String l : perfCsv) info(name, "csv " + l);
                pass(name, perfCsv.size() + " windows");
                return true;
            }
        });
    }

    /** server thread: spawns ships [from,to) through the real egg item (onItemRightClick) in a 7-column grid in front of the player */
    private int spawnPerfShips(int from, int to) {
        EntityPlayerMP p = serverPlayer();
        ItemStack prev = p.inventory.getStackInSlot(p.inventory.currentItem).copy();
        double ox = p.posX, oy = p.posY, oz = p.posZ;
        float oyaw = p.rotationYaw, opitch = p.rotationPitch;
        for (int i = from; i < to; i++) {
            ItemStack egg = eggPool.get((i * 7 + 3) % eggPool.size()).copy();
            p.inventory.setInventorySlotContents(p.inventory.currentItem, egg);
            double x = perfBaseX + 0.5D + (i % 7 - 3) * 2.5D;
            double z = perfBaseZ + 0.5D + 6D + (i / 7) * 2.5D;
            p.setPositionAndRotation(x, perfGroundY + 1D, z, 0F, 90F);
            ModItems.ShipSpawnEgg.onItemRightClick(p.world, p, EnumHand.MAIN_HAND);
        }
        p.setPositionAndRotation(ox, oy, oz, oyaw, opitch);
        p.inventory.setInventorySlotContents(p.inventory.currentItem, prev);
        int n = 0;
        for (Entity e : p.world.loadedEntityList) if (e instanceof BasicEntityShip) n++;
        return n;
    }

    /** server thread: keep ships fuelled and healthy, keep 8 husks (no AI) among the ships, count engaged ships */
    private void maintainCombat() {
        WorldServer w = server().getWorld(0);
        int engaged = 0;
        int husks = 0;
        for (Entity e : new ArrayList<>(w.loadedEntityList)) {
            if (e instanceof BasicEntityShip) {
                BasicEntityShip s = (BasicEntityShip) e;
                if (s.getEntityTarget() != null) engaged++;
                s.setHealth(s.getMaxHealth());
                s.setAmmoLight(60);
                if (s.getGrudge() < 500) {
                    s.addGrudge(1000 - s.getGrudge());
                    s.decrGrudgeNum(0);
                }
                if (s.getMorale() < 2200) {
                    s.setMorale(NORMAL_MORALE);
                    s.calcShipAttributes(4, false);
                }
            } else if (e instanceof EntityHusk && e.isEntityAlive()) {
                husks++;
            }
        }
        for (; husks < 8; husks++) {
            EntityHusk h = new EntityHusk(w);
            h.setLocationAndAngles(perfBaseX + 0.5D + perfRnd.nextInt(15) - 7, perfGroundY + 1D, perfBaseZ + 0.5D + 6D + perfRnd.nextInt(10), 0F, 0F);
            h.enablePersistence();
            h.setNoAI(true);
            w.spawnEntity(h);
            perfHuskSpawned++;
        }
        perfEngaged = engaged;
        perfHuskAlive = husks;
    }

    private TileEntity findClientTile() {
        for (TileEntity te : new ArrayList<>(mc.world.loadedTileEntityList)) {
            if (te.getPos().equals(shipyardMaster)) return te;
        }
        return null;
    }

    /** logs the client (and server) tile at the shipyard master position without creating tiles as a side effect */
    private void diagTile(String label) throws Exception {
        TileEntity te = findClientTile();
        TileEntity inChunk = mc.world.getChunk(shipyardMaster).getTileEntityMap().get(shipyardMaster);
        String client;
        if (te == null) {
            client = "client tile NOT in loadedTileEntityList; chunkMap=" + (inChunk == null ? "none" : inChunk.getClass().getSimpleName()
                    + "@" + System.identityHashCode(inChunk) + " isInvalid=" + inChunk.isInvalid());
        } else {
            client = te.getClass().getSimpleName() + "@" + System.identityHashCode(te) + " isInvalid=" + te.isInvalid()
                    + " structType=" + (te instanceof com.lulan.shincolle.tileentity.BasicTileMulti ? ((com.lulan.shincolle.tileentity.BasicTileMulti) te).getStructType() : -1)
                    + " hasMaster=" + (te instanceof com.lulan.shincolle.tileentity.BasicTileMulti && ((com.lulan.shincolle.tileentity.BasicTileMulti) te).hasMaster())
                    + " inLoadedTileEntityList=true inTickableTileEntities=" + mc.world.tickableTileEntities.contains(te)
                    + " sameAsChunkMap=" + (inChunk == te) + " blockState=" + mc.world.getBlockState(shipyardMaster);
        }
        final BlockPos mp = shipyardMaster;
        String srv = callServer(() -> {
            TileEntity st = server().getWorld(0).getTileEntity(mp);
            if (!(st instanceof com.lulan.shincolle.tileentity.BasicTileMulti)) return "server tile=" + st;
            com.lulan.shincolle.tileentity.BasicTileMulti m = (com.lulan.shincolle.tileentity.BasicTileMulti) st;
            return "server " + st.getClass().getSimpleName() + " structType=" + m.getStructType() + " hasMaster=" + m.hasMaster() + " isInvalid=" + st.isInvalid();
        });
        info("tile-diag", label + " | CLIENT " + client + " | " + srv + " | clientLoadedTileEntities=" + mc.world.loadedTileEntityList.size());
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
            Files.write(new File(dir, !CONNECT.isEmpty() ? "dedicated-results.txt" : FramePerf.ENABLED ? "renderperf-results.txt" : "results.txt").toPath(), results, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("[GAMETEST] cannot write results", e);
        }

        if (rcon != null) rcon.close();
        if (!Boolean.getBoolean("shincolle.gametest.keepOpen")) mc.shutdown();
    }

}
