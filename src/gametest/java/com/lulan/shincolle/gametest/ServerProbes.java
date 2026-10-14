package com.lulan.shincolle.gametest;

import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.network.C2SGUIPackets;
import com.lulan.shincolle.network.C2SInputPackets;
import com.lulan.shincolle.network.PacketAuthorization;
import com.lulan.shincolle.proxy.ServerProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.team.TeamData;
import com.lulan.shincolle.tileentity.TileEntitySmallShipyard;
import com.lulan.shincolle.tileentity.TileEntityWaypoint;
import com.lulan.shincolle.utility.InteractHelper;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Opt-in headless server probes (dev harness only, never shipped).
 * Enable with {@code -Dshincolle.gametest.probe=summon30,feed33,...} next to {@code -Dshincolle.gametest.server=true}.
 * The probes run one after the other from the dimension-0 world tick; each writes
 * {@code gametest-probe-<name>-results.txt} (PASS/FAIL/INFO lines with observed values) into the server working
 * directory. The server shuts down when the last probe is done.
 */
public final class ServerProbes {

    private static final String FLAG = System.getProperty("shincolle.gametest.probe", "");
    public static final boolean ACTIVE = !FLAG.trim().isEmpty();

    private static List<Probe> queue;
    private static int index;
    private static int idle;

    private ServerProbes() {
    }

    /** called from GameTestServerHooks.onWorldTick (END phase, dimension 0, server side) */
    public static void tick(WorldServer w) {
        if (queue == null) {
            queue = new ArrayList<>();
            for (String n : FLAG.split(",")) {
                switch (n.trim().toLowerCase(Locale.ROOT)) {
                    case "summon30": queue.add(new Summon30()); break;
                    case "feed33": queue.add(new Feed33()); break;
                    case "emote32": queue.add(new Emote32()); break;
                    case "auth29": queue.add(new Auth29()); break;
                    case "world31": queue.add(new World31()); break;
                    case "missile40": queue.add(new MoreProbes.Missile()); break;
                    case "addteam29": queue.add(new MoreProbes.AddTeam()); break;
                    case "guard37": queue.add(new MoreProbes.Guard()); break;
                    case "": break;
                    default: GameTestServerHooks.LOG.error("[GAMETEST-SRV] unknown probe '{}'", n);
                }
            }
        }
        if (index >= queue.size()) return;
        Probe p = queue.get(index);
        p.tick++;
        boolean done;
        try {
            done = p.step(w, p.tick);
        } catch (Throwable t) {
            p.fail("probe threw", t);
            done = true;
        }
        if (done) {
            p.finish(w);
            index++;
            if (index >= queue.size()) w.getMinecraftServer().initiateShutdown();
        }
    }

    /* ------------------------------------------------------------------ infrastructure */

    abstract static class Probe {
        final String name;
        final List<String> res = new ArrayList<>();
        int tick;
        int fails;
        int passes;

        Probe(String name) {
            this.name = name;
        }

        /** @return true when finished */
        abstract boolean step(WorldServer w, int t) throws Throwable;

        String resultFile() {
            return "gametest-probe-" + name + "-results.txt";
        }

        void info(String s) {
            res.add("INFO " + s);
        }

        void check(String what, boolean ok, String detail) {
            if (ok) passes++;
            else fails++;
            res.add((ok ? "PASS " : "FAIL ") + what + ": " + detail);
        }

        void fail(String what, Throwable t) {
            fails++;
            java.io.StringWriter sw = new java.io.StringWriter();
            t.printStackTrace(new java.io.PrintWriter(sw));
            res.add("FAIL " + what + ": " + t);
            res.add(sw.toString());
        }

        void finish(WorldServer w) {
            res.add("SUMMARY probe=" + name + " pass=" + passes + " fail=" + fails);
            for (String line : res) GameTestServerHooks.LOG.info("[GAMETEST-SRV] probe-{} {}", name, line);
            try {
                Files.write(Paths.get(resultFile()), res, StandardCharsets.UTF_8);
            } catch (java.io.IOException e) {
                GameTestServerHooks.LOG.error("[GAMETEST-SRV] cannot write {}", resultFile(), e);
            }
        }
    }

    static BlockPos spawnTop(WorldServer w) {
        return w.getTopSolidOrLiquidBlock(w.getSpawnPoint());
    }

    static BasicEntityShip spawnShip(WorldServer w, String id, String name, double x, double y, double z, NBTTagCompound ext) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", id);
        if (name != null) tag.setString("CustomName", name);
        if (ext != null) tag.setTag("ShipExtProps", ext);
        Entity e = AnvilChunkLoader.readWorldEntityPos(tag, w, x, y, z, true);
        return e instanceof BasicEntityShip ? (BasicEntityShip) e : null;
    }

    static NBTTagCompound section(NBTTagCompound ext, String sec) {
        if (!ext.hasKey(sec)) ext.setTag(sec, new NBTTagCompound());
        return ext.getCompoundTag(sec);
    }

    /**
     * A real EntityPlayerMP (not a Forge FakePlayer, which the mod deliberately never gives a CapaTeitoku) with a stub
     * connection that records outbound packets instead of writing to a channel. Registered in the world player list so
     * entity-id lookups find it. Not added to the loaded entity list, so it is not ticked.
     */
    static final class ProbePlayer extends EntityPlayerMP {
        final List<Packet<?>> sent = new ArrayList<>();

        ProbePlayer(WorldServer w, GameProfile gp) {
            super(w.getMinecraftServer(), w, gp, new PlayerInteractionManager(w));
            new NetHandlerPlayServer(w.getMinecraftServer(), new NetworkManager(EnumPacketDirection.SERVERBOUND), this) {
                @Override
                public void sendPacket(Packet<?> packetIn) {
                    sent.add(packetIn);
                }
            };
        }
    }

    static EntityPlayerMP player(WorldServer w, String name, int uid, double x, double y, double z) {
        ProbePlayer fp = new ProbePlayer(w, new GameProfile(UUID.nameUUIDFromBytes(("gtprobe-" + name).getBytes(StandardCharsets.UTF_8)), name));
        fp.setPosition(x, y, z);
        fp.interactionManager.setGameType(GameType.SURVIVAL);
        fp.capabilities.isCreativeMode = false;
        w.playerEntities.add(fp);
        CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(fp);
        if (capa != null) {
            capa.init(fp);
            capa.setPlayerUID(uid);
        }
        return fp;
    }

    static void unregister(WorldServer w, EntityPlayerMP fp) {
        w.playerEntities.remove(fp);
    }

    static String minor(BasicEntityShip s) {
        return String.format("level=%d morale=%d numAmmoL=%d numAmmoH=%d numGrudge=%d airL=%d airH=%d food=%d foodMax=%d guardType=%d guardId=%d guardX=%d "
                        + "followMin=%d followMax=%d fleeHP=%d task=%d formatType=%d formatPos=%d autoCR=%d crane=%d wpStay=%d kills=%d exp=%d | canDrop=%s passiveAI=%s melee=%s ammoL=%s ammoH=%s "
                        + "airLflag=%s onSight=%s pvpFirst=%s aa=%s asm=%s pickItem=%s wedEffect=%s canFollow=%s married=%s hp=%.1f/%.1f",
                s.getStateMinor(ID.M.ShipLevel), s.getStateMinor(ID.M.Morale), s.getStateMinor(ID.M.NumAmmoLight), s.getStateMinor(ID.M.NumAmmoHeavy),
                s.getStateMinor(ID.M.NumGrudge), s.getStateMinor(ID.M.NumAirLight), s.getStateMinor(ID.M.NumAirHeavy), s.getStateMinor(ID.M.Food),
                s.getStateMinor(ID.M.FoodMax), s.getStateMinor(ID.M.GuardType), s.getStateMinor(ID.M.GuardID), s.getStateMinor(ID.M.GuardX),
                s.getStateMinor(ID.M.FollowMin), s.getStateMinor(ID.M.FollowMax), s.getStateMinor(ID.M.FleeHP), s.getStateMinor(ID.M.Task),
                s.getStateMinor(ID.M.FormatType), s.getStateMinor(ID.M.FormatPos), s.getStateMinor(ID.M.UseCombatRation), s.getStateMinor(ID.M.CraneState),
                s.getStateMinor(ID.M.WpStay), s.getStateMinor(ID.M.Kills), s.getStateMinor(ID.M.ExpCurrent),
                s.getStateFlag(ID.F.CanDrop), s.getStateFlag(ID.F.PassiveAI), s.getStateFlag(ID.F.UseMelee), s.getStateFlag(ID.F.UseAmmoLight),
                s.getStateFlag(ID.F.UseAmmoHeavy), s.getStateFlag(ID.F.UseAirLight), s.getStateFlag(ID.F.OnSightChase), s.getStateFlag(ID.F.PVPFirst),
                s.getStateFlag(ID.F.AntiAir), s.getStateFlag(ID.F.AntiSS), s.getStateFlag(ID.F.PickItem), s.getStateFlag(ID.F.UseRingEffect),
                s.getStateFlag(ID.F.CanFollow), s.getStateFlag(ID.F.IsMarried), s.getHealth(), s.getMaxHealth());
    }

    /* ------------------------------------------------------------------ A. summon30 (#30/#45) */

    static final class Summon30 extends Probe {
        static final String ID_DD = "shincolle:EntityDestroyerI";
        final Map<String, BasicEntityShip> ships = new LinkedHashMap<>();
        final Map<String, String> before = new LinkedHashMap<>();
        final Map<String, int[]> immediate = new LinkedHashMap<>();   // level, morale, canDrop
        BasicEntityShip ref;
        int spawnAt = 40;

        Summon30() {
            super("summon30");
        }

        private void spawn(WorldServer w, String name, int dx, NBTTagCompound ext) {
            BlockPos sp = spawnTop(w);
            BasicEntityShip s = spawnShip(w, ID_DD, name, sp.getX() + 0.5 + dx, sp.getY(), sp.getZ() + 0.5, ext);
            if (s == null) {
                check(name + " spawned", false, "readWorldEntityPos returned no ship");
                return;
            }
            ships.put(name, s);
            before.put(name, minor(s));
            immediate.put(name, new int[]{s.getLevel(), s.getMorale(), s.getStateFlag(ID.F.CanDrop) ? 1 : 0});
            info(name + " immediately after load: " + minor(s));
        }

        @Override
        boolean step(WorldServer w, int t) {
            if (t == spawnAt) {
                ref = new com.lulan.shincolle.entity.destroyer.EntityDestroyerI(w);   // never added to the world: constructor defaults
                info("constructor defaults (reference, not in world): " + minor(ref));
                spawn(w, "S-bare", 0, null);
                spawn(w, "S-emptyExt", 3, new NBTTagCompound());
                NBTTagCompound minorOnly = new NBTTagCompound();
                section(minorOnly, "Minor");
                spawn(w, "S-minorStub", 6, minorOnly);
                NBTTagCompound partial = new NBTTagCompound();
                for (String sec : new String[]{"Minor", "Display", "Point", "ShipFlags", "Timer"}) section(partial, sec);
                spawn(w, "S-partialFlags5", 9, partial);
                NBTTagCompound flagsOnly = new NBTTagCompound();
                section(flagsOnly, "ShipFlags");
                spawn(w, "S-flagsStubOnly", 12, flagsOnly);
                NBTTagCompound explicit = new NBTTagCompound();
                NBTTagCompound mn = section(explicit, "Minor");
                mn.setInteger("Level", 7);
                mn.setInteger("Morale", 0);
                mn.setInteger("FMin", 0);
                mn.setInteger("GuardType", 1);
                mn.setInteger("NumAmmoL", 0);
                section(explicit, "ShipFlags").setBoolean("CanDrop", false);
                section(explicit, "ShipFlags").setBoolean("PassiveAI", true);
                spawn(w, "S-explicitValues", 15, explicit);
                return false;
            }
            if (t == spawnAt + 30) {
                int[] cmp = {ID.M.NumAmmoLight, ID.M.NumAmmoHeavy, ID.M.Food, ID.M.GuardType, ID.M.GuardID, ID.M.FollowMin, ID.M.FollowMax, ID.M.FleeHP,
                        ID.M.Task, ID.M.NumGrudge, ID.M.NumAirLight, ID.M.NumAirHeavy, ID.M.UseCombatRation, ID.M.FormatType, ID.M.FormatPos, ID.M.WpStay};
                String[] cmpN = {"NumAmmoL", "NumAmmoH", "Food", "GuardType", "GuardID", "FollowMin", "FollowMax", "FleeHP", "Task", "NumGrudge", "NumAirL", "NumAirH",
                        "AutoCR", "FormatType", "FormatPos", "WpStay"};
                for (Map.Entry<String, BasicEntityShip> en : ships.entrySet()) {
                    String n = en.getKey();
                    BasicEntityShip s = en.getValue();
                    int[] im = immediate.get(n);
                    info(n + " after ticks=" + s.ticksExisted + ": " + minor(s));
                    boolean alive = s.isEntityAlive() && w.loadedEntityList.contains(s);
                    if (n.equals("S-explicitValues")) {
                        check(n + " explicit Level kept (immediate)", im[0] == 7, "level=" + im[0]);
                        check(n + " explicit Morale=0 kept (immediate)", im[1] == 0, "morale=" + im[1]);
                        check(n + " explicit CanDrop=false kept (immediate)", im[2] == 0, "canDrop=" + (im[2] == 1));
                        check(n + " explicit FMin=0 / GuardType=1 / PassiveAI kept", s.getStateMinor(ID.M.FollowMin) == 0
                                && s.getStateMinor(ID.M.GuardType) == 1 && s.getStateFlag(ID.F.PassiveAI),
                                "followMin=" + s.getStateMinor(ID.M.FollowMin) + " guardType=" + s.getStateMinor(ID.M.GuardType) + " passiveAI=" + s.getStateFlag(ID.F.PassiveAI));
                        check(n + " explicit CanDrop=false kept after initAI (ticks=" + s.ticksExisted + ")", !s.getStateFlag(ID.F.CanDrop), "canDrop=" + s.getStateFlag(ID.F.CanDrop));
                        info(n + " morale after " + s.ticksExisted + " ticks = " + s.getMorale() + " (explicit 0 saved; morale ticks naturally)");
                        continue;
                    }
                    check(n + " alive and in world", alive, "alive=" + alive);
                    check(n + " Level==1 (immediate)", im[0] == 1, "level=" + im[0]);
                    check(n + " Level==1 (+30 ticks)", s.getLevel() == 1, "level=" + s.getLevel());
                    check(n + " Morale==60 (immediate)", im[1] == 60, "morale=" + im[1]);
                    info(n + " morale at +30 ticks = " + s.getMorale() + " noFuel=" + s.getStateFlag(ID.F.NoFuel) + " grudge=" + s.getStateMinor(ID.M.NumGrudge)
                            + " (updateFuelState(true) sets morale 0 for a ship with no grudge; not a load-default issue)");
                    check(n + " CanDrop==true (immediate)", im[2] == 1, "canDrop=" + (im[2] == 1));
                    check(n + " CanDrop==true (+30 ticks, after initAI)", s.getStateFlag(ID.F.CanDrop), "canDrop=" + s.getStateFlag(ID.F.CanDrop));
                    StringBuilder diffs = new StringBuilder();
                    for (int i = 0; i < cmp.length; i++) {
                        if (s.getStateMinor(cmp[i]) != ref.getStateMinor(cmp[i])) {
                            diffs.append(cmpN[i]).append(" ctor=").append(ref.getStateMinor(cmp[i])).append(" got=").append(s.getStateMinor(cmp[i])).append("; ");
                        }
                    }
                    check(n + " constructor-default fields unchanged (NumAmmo*/Food/Guard*/Follow*/FleeHP/Task/...)", diffs.length() == 0, diffs.length() == 0 ? "all equal reference" : diffs.toString());
                    check(n + " HP == max HP", Math.abs(s.getHealth() - s.getMaxHealth()) < 0.01F, String.format("hp=%.2f max=%.2f", s.getHealth(), s.getMaxHealth()));
                }
                for (BasicEntityShip s : ships.values()) s.setDead();
                return true;
            }
            return false;
        }
    }

    /* ------------------------------------------------------------------ B. feed33 (#33) */

    static final class Feed33 extends Probe {
        /** vanilla soups have NO containerItem in 1.12.2 (ItemSoup returns the bowl from onItemUseFinish), so use a food that really has one */
        static final net.minecraft.item.ItemFood SOUP = new net.minecraft.item.ItemFood(6, 0.6F, false);

        static {
            SOUP.setContainerItem(Items.BOWL);
        }

        Feed33() {
            super("feed33");
        }

        private static int count(EntityPlayerMP fp, net.minecraft.item.Item item) {
            int n = 0;
            for (ItemStack st : fp.inventory.mainInventory) if (!st.isEmpty() && st.getItem() == item) n += st.getCount();
            return n;
        }

        private static List<EntityItem> drops(WorldServer w, EntityPlayerMP fp) {
            return w.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(fp.getPosition()).grow(4), e -> !e.isDead);
        }

        private static void clearDrops(WorldServer w, EntityPlayerMP fp) {
            for (EntityItem e : drops(w, fp)) e.setDead();
        }

        private static void fillInventory(EntityPlayerMP fp, int fromSlot) {
            for (int i = fromSlot; i < 36; i++) fp.inventory.mainInventory.set(i, new ItemStack(Items.WOODEN_SWORD));
        }

        @Override
        boolean step(WorldServer w, int t) {
            if (t != 40) return false;
            BlockPos sp = spawnTop(w);
            double x = sp.getX() + 0.5, y = sp.getY(), z = sp.getZ() + 0.5;
            BasicEntityShip ship = spawnShip(w, "shincolle:EntityDestroyerI", "GT-feedship", x + 2, y, z, null);
            check("ship spawned", ship != null, String.valueOf(ship));
            if (ship == null) return true;
            EntityPlayerMP fp = player(w, "GT-feeder", 7001, x, y, z);

            //vanilla mushroom stew: informational, what the container rule does with a real vanilla soup
            fp.inventory.clear();
            ship.setFoodSaturation(0);
            fp.inventory.setInventorySlotContents(0, new ItemStack(Items.MUSHROOM_STEW, 1));
            info("vanilla mushroom stew: getContainerItem=" + Items.MUSHROOM_STEW.getContainerItem(new ItemStack(Items.MUSHROOM_STEW)) + " hasContainerItem="
                    + Items.MUSHROOM_STEW.hasContainerItem(new ItemStack(Items.MUSHROOM_STEW)));
            net.minecraft.util.EnumActionResult rv = ship.applyPlayerInteraction(fp, Vec3d.ZERO, EnumHand.MAIN_HAND);
            info("vanilla mushroom stew fed: result=" + rv + " stewLeft=" + count(fp, Items.MUSHROOM_STEW) + " bowls=" + count(fp, Items.BOWL) + " drops=" + drops(w, fp).size());

            //real entry point: BasicEntityShip.applyPlayerInteraction (server side) -> InteractHelper.interactFeed
            //1. survival, soup x1, otherwise empty inventory
            fp.inventory.clear();
            clearDrops(w, fp);
            ship.setFoodSaturation(0);
            fp.inventory.setInventorySlotContents(0, new ItemStack(SOUP, 1));
            fp.inventory.currentItem = 0;
            net.minecraft.util.EnumActionResult r = ship.applyPlayerInteraction(fp, Vec3d.ZERO, EnumHand.MAIN_HAND);
            check("survival x1 soup: feed accepted", r == net.minecraft.util.EnumActionResult.SUCCESS, "result=" + r + " foodSat=" + ship.getFoodSaturation());
            check("survival x1 soup: soup consumed", count(fp, SOUP) == 0, "soup=" + count(fp, SOUP));
            check("survival x1 soup: bowl in inventory", count(fp, Items.BOWL) == 1, "bowl=" + count(fp, Items.BOWL) + " slot0=" + fp.inventory.getStackInSlot(0));
            check("survival x1 soup: no bowl dropped", drops(w, fp).isEmpty(), "drops=" + drops(w, fp));

            //2. survival, soup x2 (stack), full inventory -> stack shrinks to 1, bowl must drop as item entity
            fp.inventory.clear();
            clearDrops(w, fp);
            ship.setFoodSaturation(0);
            fillInventory(fp, 1);
            fp.inventory.setInventorySlotContents(0, new ItemStack(SOUP, 2));
            r = ship.applyPlayerInteraction(fp, Vec3d.ZERO, EnumHand.MAIN_HAND);
            List<EntityItem> d = drops(w, fp);
            check("survival x2 soup, full inventory: feed accepted", r == net.minecraft.util.EnumActionResult.SUCCESS, "result=" + r);
            check("survival x2 soup, full inventory: soup stack shrinks to 1", count(fp, SOUP) == 1, "soup=" + count(fp, SOUP));
            check("survival x2 soup, full inventory: bowl NOT in inventory", count(fp, Items.BOWL) == 0, "bowl=" + count(fp, Items.BOWL));
            check("survival x2 soup, full inventory: bowl dropped as item entity", d.size() == 1 && d.get(0).getItem().getItem() == Items.BOWL,
                    "drops=" + d.size() + " " + (d.isEmpty() ? "" : d.get(0).getItem().toString()));

            //3. survival, soup x1, full inventory: the emptied hand slot takes the bowl (informational on slot reuse)
            fp.inventory.clear();
            clearDrops(w, fp);
            ship.setFoodSaturation(0);
            fillInventory(fp, 1);
            fp.inventory.setInventorySlotContents(0, new ItemStack(SOUP, 1));
            r = ship.applyPlayerInteraction(fp, Vec3d.ZERO, EnumHand.MAIN_HAND);
            check("survival x1 soup, full other slots: bowl lands in inventory (hand slot freed)", count(fp, Items.BOWL) == 1 && drops(w, fp).isEmpty(),
                    "result=" + r + " bowl=" + count(fp, Items.BOWL) + " drops=" + drops(w, fp).size() + " slot0=" + fp.inventory.getStackInSlot(0));

            //4. creative: nothing consumed, no bowl
            fp.inventory.clear();
            clearDrops(w, fp);
            ship.setFoodSaturation(0);
            fp.capabilities.isCreativeMode = true;
            fp.interactionManager.setGameType(GameType.CREATIVE);
            fp.inventory.setInventorySlotContents(0, new ItemStack(SOUP, 1));
            r = ship.applyPlayerInteraction(fp, Vec3d.ZERO, EnumHand.MAIN_HAND);
            check("creative soup: feed accepted", r == net.minecraft.util.EnumActionResult.SUCCESS, "result=" + r + " foodSat=" + ship.getFoodSaturation());
            check("creative soup: soup not consumed", count(fp, SOUP) == 1, "soup=" + count(fp, SOUP));
            check("creative soup: no bowl given or dropped", count(fp, Items.BOWL) == 0 && drops(w, fp).isEmpty(), "bowl=" + count(fp, Items.BOWL) + " drops=" + drops(w, fp).size());

            //5. control: bread has no container
            fp.capabilities.isCreativeMode = false;
            fp.interactionManager.setGameType(GameType.SURVIVAL);
            fp.inventory.clear();
            clearDrops(w, fp);
            ship.setFoodSaturation(0);
            fp.inventory.setInventorySlotContents(0, new ItemStack(Items.BREAD, 2));
            r = ship.applyPlayerInteraction(fp, Vec3d.ZERO, EnumHand.MAIN_HAND);
            check("control survival bread x2: shrinks to 1, no container", r == net.minecraft.util.EnumActionResult.SUCCESS && count(fp, Items.BREAD) == 1 && count(fp, Items.BOWL) == 0
                    && drops(w, fp).isEmpty(), "result=" + r + " bread=" + count(fp, Items.BREAD));

            //6. ship full of food rejects: nothing consumed, no container
            fp.inventory.clear();
            clearDrops(w, fp);
            ship.setFoodSaturation(ship.getFoodSaturationMax());
            fp.inventory.setInventorySlotContents(0, new ItemStack(SOUP, 1));
            boolean fed = InteractHelper.interactFeed(ship, fp, fp.inventory.getStackInSlot(0));
            check("full ship rejects soup: nothing consumed, no bowl", !fed && count(fp, SOUP) == 1 && count(fp, Items.BOWL) == 0,
                    "fed=" + fed + " soup=" + count(fp, SOUP) + " bowl=" + count(fp, Items.BOWL));

            clearDrops(w, fp);
            ship.setDead();
            unregister(w, fp);
            return true;
        }
    }

    /* ------------------------------------------------------------------ C. emote32 (#32) */

    static final class Emote32 extends Probe {
        Emote32() {
            super("emote32");
        }

        @Override
        boolean step(WorldServer w, int t) {
            if (t != 40) return false;
            EntityPlayerMP fp = player(w, "GT-emote", 7101, 0, 70, 0);
            CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(fp);
            check("capability present on fake player", capa != null, String.valueOf(capa));
            if (capa == null) return true;

            check("default autoEmoteOff == false", !capa.isAutoEmoteOff(), "autoEmoteOff=" + capa.isAutoEmoteOff());
            capa.setAutoEmoteOff(true);
            NBTTagCompound saved = capa.saveNBTData(new NBTTagCompound());
            NBTTagCompound ext = saved.getCompoundTag(CapaTeitoku.CAPA_KEY);
            check("saved NBT carries AutoEmoteOff=true", ext.hasKey(CapaTeitoku.AUTO_EMOTE_OFF_KEY) && ext.getBoolean(CapaTeitoku.AUTO_EMOTE_OFF_KEY),
                    "key present=" + ext.hasKey(CapaTeitoku.AUTO_EMOTE_OFF_KEY) + " value=" + ext.getBoolean(CapaTeitoku.AUTO_EMOTE_OFF_KEY));

            CapaTeitoku fresh = new CapaTeitoku();
            fresh.loadNBTData(saved.copy());
            check("fresh capability loads AutoEmoteOff=true from NBT", fresh.isAutoEmoteOff(), "autoEmoteOff=" + fresh.isAutoEmoteOff());

            //absent key (old save) -> false, even if the target capability was true before the load
            NBTTagCompound old = saved.copy();
            old.getCompoundTag(CapaTeitoku.CAPA_KEY).removeTag(CapaTeitoku.AUTO_EMOTE_OFF_KEY);
            CapaTeitoku fresh2 = new CapaTeitoku();
            fresh2.setAutoEmoteOff(true);
            fresh2.loadNBTData(old);
            check("absent AutoEmoteOff key loads as false (old save), overriding a true instance", !fresh2.isAutoEmoteOff(), "autoEmoteOff=" + fresh2.isAutoEmoteOff());

            //full player save path: EntityPlayer.writeToNBT -> new player readFromNBT (ForgeCaps)
            try {
                NBTTagCompound pl = fp.writeToNBT(new NBTTagCompound());
                info("player NBT has ForgeCaps=" + pl.hasKey("ForgeCaps") + " ; ForgeCaps keys=" + (pl.hasKey("ForgeCaps") ? pl.getCompoundTag("ForgeCaps").getKeySet() : "-"));
                EntityPlayerMP other = player(w, "GT-emote-load", 7102, 0, 70, 0);
                other.readFromNBT(pl);
                CapaTeitoku oc = CapaTeitoku.getTeitokuCapability(other);
                check("player writeToNBT/readFromNBT round trip keeps AutoEmoteOff=true", oc != null && oc.isAutoEmoteOff(), "other autoEmoteOff=" + (oc == null ? "no capa" : oc.isAutoEmoteOff()));
                NBTTagCompound pl2 = pl.copy();
                if (pl2.hasKey("ForgeCaps") && pl2.getCompoundTag("ForgeCaps").getKeySet().size() > 0) {
                    NBTTagCompound fc = pl2.getCompoundTag("ForgeCaps");
                    for (String k : new ArrayList<>(fc.getKeySet())) {
                        NBTTagCompound c = fc.getCompoundTag(k);
                        if (c.hasKey(CapaTeitoku.CAPA_KEY)) c.getCompoundTag(CapaTeitoku.CAPA_KEY).removeTag(CapaTeitoku.AUTO_EMOTE_OFF_KEY);
                    }
                    EntityPlayerMP other2 = player(w, "GT-emote-load2", 7103, 0, 70, 0);
                    CapaTeitoku c2 = CapaTeitoku.getTeitokuCapability(other2);
                    if (c2 != null) c2.setAutoEmoteOff(true);
                    other2.readFromNBT(pl2);
                    c2 = CapaTeitoku.getTeitokuCapability(other2);
                    check("player NBT without the key reads AutoEmoteOff=false", c2 != null && !c2.isAutoEmoteOff(), "autoEmoteOff=" + (c2 == null ? "no capa" : c2.isAutoEmoteOff()));
                    unregister(w, other2);
                }
                unregister(w, other);
            } catch (Throwable th) {
                fail("full player NBT round trip", th);
            }

            //toggle flow (messages need a connection): capture exceptions
            capa.setAutoEmoteOff(false);
            try {
                com.lulan.shincolle.utility.AutoEmoteHelper.AUTO_EMOTE_MANAGER.toggleAutoEmote(fp);
                check("toggleAutoEmote #1 flips false -> true", capa.isAutoEmoteOff(), "autoEmoteOff=" + capa.isAutoEmoteOff());
                com.lulan.shincolle.utility.AutoEmoteHelper.AUTO_EMOTE_MANAGER.toggleAutoEmote(fp);
                check("toggleAutoEmote #2 flips true -> false", !capa.isAutoEmoteOff(), "autoEmoteOff=" + capa.isAutoEmoteOff());
            } catch (Throwable th) {
                fail("toggleAutoEmote on a EntityPlayerMP threw (status message needs a connection?) ; flag now=" + capa.isAutoEmoteOff(), th);
            }
            unregister(w, fp);
            return true;
        }
    }

    /* ------------------------------------------------------------------ D. auth29 (#29) */

    static final class Auth29 extends Probe {
        static final int OWNER_UID = 8001, STRANGER_UID = 8002, ALLY_UID = 8003, OP_UID = 8004, CREATIVE_UID = 8005;

        Auth29() {
            super("auth29");
        }

        MessageContext ctxFor(EntityPlayerMP p) throws Exception {
            for (Constructor<?> c : MessageContext.class.getDeclaredConstructors()) {
                Class<?>[] pt = c.getParameterTypes();
                if (pt.length == 2 && pt[0] == INetHandler.class && pt[1] == Side.class) {
                    c.setAccessible(true);
                    return (MessageContext) c.newInstance(p.connection, Side.SERVER);
                }
            }
            throw new IllegalStateException("MessageContext(INetHandler, Side) constructor not found");
        }

        static C2SGUIPackets rt(C2SGUIPackets m) {
            ByteBuf b = Unpooled.buffer();
            m.toBytes(b);
            C2SGUIPackets o = new C2SGUIPackets();
            o.fromBytes(b);
            return o;
        }

        static C2SInputPackets rt(C2SInputPackets m) {
            ByteBuf b = Unpooled.buffer();
            m.toBytes(b);
            C2SInputPackets o = new C2SInputPackets();
            o.fromBytes(b);
            return o;
        }

        Method guiHandle, inputHandle;

        /** returns null on clean return, else the exception thrown by the handler */
        Throwable runGui(C2SGUIPackets msg, EntityPlayerMP sender) throws Exception {
            if (guiHandle == null) {
                guiHandle = C2SGUIPackets.class.getDeclaredMethod("handle", C2SGUIPackets.class, MessageContext.class);
                guiHandle.setAccessible(true);
            }
            try {
                guiHandle.invoke(null, rt(msg), ctxFor(sender));
                return null;
            } catch (InvocationTargetException e) {
                return e.getCause();
            }
        }

        Throwable runInput(C2SInputPackets msg, EntityPlayerMP sender) throws Exception {
            if (inputHandle == null) {
                inputHandle = C2SInputPackets.class.getDeclaredMethod("handle", C2SInputPackets.class, MessageContext.class);
                inputHandle.setAccessible(true);
            }
            try {
                inputHandle.invoke(null, rt(msg), ctxFor(sender));
                return null;
            } catch (InvocationTargetException e) {
                return e.getCause();
            }
        }

        void record(String packet, String actor, boolean accepted, boolean expected, String extra) {
            check(packet + " by " + actor, accepted == expected, (accepted ? "ACCEPTED" : "REJECTED") + " (expected " + (expected ? "accept" : "reject") + ") " + extra);
        }

        static String thr(Throwable t) {
            return t == null ? "" : " [handler threw " + t + " after/while processing]";
        }

        @Override
        boolean step(WorldServer w, int t) throws Throwable {
            if (t != 40) return false;
            MinecraftServer server = w.getMinecraftServer();
            BlockPos sp = spawnTop(w);
            double x = sp.getX() + 0.5, y = sp.getY(), z = sp.getZ() + 0.5;

            EntityPlayerMP owner = player(w, "GT-owner", OWNER_UID, x, y, z);
            EntityPlayerMP stranger = player(w, "GT-stranger", STRANGER_UID, x + 1, y, z);
            EntityPlayerMP ally = player(w, "GT-ally", ALLY_UID, x + 2, y, z);
            EntityPlayerMP op = player(w, "GT-op", OP_UID, x + 3, y, z);
            EntityPlayerMP creative = player(w, "GT-creative", CREATIVE_UID, x + 4, y, z);
            creative.capabilities.isCreativeMode = true;
            creative.interactionManager.setGameType(GameType.CREATIVE);

            //spike: connection and hooks
            info("spike: owner.connection=" + (owner.connection == null ? "null" : owner.connection.getClass().getName())
                    + " capaPresent=" + (CapaTeitoku.getTeitokuCapability(owner) != null));
            info("spike: MessageContext ctor via reflection OK=" + (ctxFor(owner) != null));

            //teams: owner team lists ally team as ally (ally direction: owner's allyList contains the actor's team id)
            ServerProxy.setTeamData(new TeamData(OWNER_UID, "GTOwnerTeam", "GT-owner"));
            ServerProxy.setTeamData(new TeamData(ALLY_UID, "GTAllyTeam", "GT-ally"));
            ServerProxy.setTeamData(new TeamData(STRANGER_UID, "GTStrangerTeam", "GT-stranger"));
            ServerProxy.teamAddAlly(OWNER_UID, ALLY_UID);
            info("teams: owner allies=" + ServerProxy.getTeamData(OWNER_UID).getTeamAllyList()
                    + " stranger allies=" + ServerProxy.getTeamData(STRANGER_UID).getTeamAllyList());

            //ops
            server.getPlayerList().addOp(op.getGameProfile());
            info("canSendCommands: owner=" + server.getPlayerList().canSendCommands(owner.getGameProfile())
                    + " stranger=" + server.getPlayerList().canSendCommands(stranger.getGameProfile())
                    + " ally=" + server.getPlayerList().canSendCommands(ally.getGameProfile())
                    + " op=" + server.getPlayerList().canSendCommands(op.getGameProfile())
                    + " creative(non-op)=" + server.getPlayerList().canSendCommands(creative.getGameProfile()));

            BasicEntityShip ship = spawnShip(w, "shincolle:EntityDestroyerI", "GT-authship", x, y, z + 3, null);
            BasicEntityShip noOwnerShip = spawnShip(w, "shincolle:EntityDestroyerI", "GT-noownership", x + 2, y, z + 3, null);
            check("ships spawned", ship != null && noOwnerShip != null, "ship=" + ship + " ownerless=" + noOwnerShip);
            if (ship == null || noOwnerShip == null) return true;
            ship.setPlayerUID(OWNER_UID);
            ship.setOwnerId(owner.getUniqueID());
            noOwnerShip.setPlayerUID(-1);

            BlockPos yardPos = new BlockPos(x + 4, y, z + 4);
            w.setBlockState(yardPos, ModBlocks.BlockSmallShipyard.getDefaultState(), 3);
            TileEntity te = w.getTileEntity(yardPos);
            check("small shipyard tile placed", te instanceof TileEntitySmallShipyard, String.valueOf(te));
            TileEntitySmallShipyard yard = (TileEntitySmallShipyard) te;
            yard.setPlayerUID(OWNER_UID);

            BlockPos wp1 = new BlockPos(x + 1, y, z + 5), wp2 = new BlockPos(x + 3, y, z + 5), chestPos = new BlockPos(x + 2, y, z + 6), chestPos2 = new BlockPos(x + 5, y, z + 6);
            w.setBlockState(wp1, ModBlocks.BlockWaypoint.getDefaultState(), 3);
            w.setBlockState(wp2, ModBlocks.BlockWaypoint.getDefaultState(), 3);
            w.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 3);
            w.setBlockState(chestPos2, Blocks.CHEST.getDefaultState(), 3);
            TileEntityWaypoint tw1 = (TileEntityWaypoint) w.getTileEntity(wp1), tw2 = (TileEntityWaypoint) w.getTileEntity(wp2);
            check("waypoint tiles and chest placed", tw1 != null && tw2 != null && w.getTileEntity(chestPos) != null, tw1 + " " + tw2 + " " + w.getTileEntity(chestPos));
            tw1.setPlayerUID(OWNER_UID);
            tw2.setPlayerUID(OWNER_UID);

            Object[][] actors = {
                    {"owner", owner}, {"ally(owner's ally list)", ally}, {"server OP (survival)", op}, {"stranger", stranger}, {"creative non-OP", creative}};
            // expected accept: [owner, ally, op, stranger, creative]
            boolean[] expShipCmd = {true, true, true, false, false};
            boolean[] expOwnerOnly = {true, false, true, false, false};
            boolean[] expTile = {true, true, true, false, false};

            //---- direct core checks as a cross-check
            for (int i = 0; i < actors.length; i++) {
                EntityPlayerMP a = (EntityPlayerMP) actors[i][1];
                String n = (String) actors[i][0];
                record("core mayCommandShip(owned ship)", n, PacketAuthorization.mayCommandShip(a, ship), expShipCmd[i], "");
                record("core mayOwnShip(owned ship)", n, PacketAuthorization.mayOwnShip(a, ship), expOwnerOnly[i], "");
                record("core mayUseTile(owned shipyard)", n, PacketAuthorization.mayUseTile(a, yard), expTile[i], "");
            }

            //---- ShipBtn (FollowMin button)
            for (int i = 0; i < actors.length; i++) {
                EntityPlayerMP a = (EntityPlayerMP) actors[i][1];
                ship.setStateMinor(ID.M.FollowMin, 3);
                ship.setStateMinor(ID.M.FollowMax, 12);
                Throwable th = runGui(new C2SGUIPackets(ship, C2SGUIPackets.PID.ShipBtn, ID.B.ShipInv_FollowMin, 5), a);
                record("ShipBtn(FollowMin=5)", (String) actors[i][0], ship.getStateMinor(ID.M.FollowMin) == 5, expShipCmd[i], "followMin=" + ship.getStateMinor(ID.M.FollowMin) + thr(th));
            }
            //ownerless ship: OP only
            for (int i = 0; i < actors.length; i++) {
                EntityPlayerMP a = (EntityPlayerMP) actors[i][1];
                noOwnerShip.setStateMinor(ID.M.FollowMin, 3);
                noOwnerShip.setStateMinor(ID.M.FollowMax, 12);
                Throwable th = runGui(new C2SGUIPackets(noOwnerShip, C2SGUIPackets.PID.ShipBtn, ID.B.ShipInv_FollowMin, 5), a);
                record("ShipBtn on ownerless ship", (String) actors[i][0], noOwnerShip.getStateMinor(ID.M.FollowMin) == 5, i == 2, "followMin=" + noOwnerShip.getStateMinor(ID.M.FollowMin) + thr(th));
            }

            //---- HitHeight
            for (int i = 0; i < actors.length; i++) {
                EntityPlayerMP a = (EntityPlayerMP) actors[i][1];
                ship.setHitHeight(0);
                ship.setHitAngle(0);
                Throwable th = runGui(new C2SGUIPackets(a, C2SGUIPackets.PID.HitHeight, ship.getEntityId(), 41, 42), a);
                record("HitHeight(41,42)", (String) actors[i][0], ship.getHitHeight() == 41 && ship.getHitAngle() == 42, expShipCmd[i], "hitHeight=" + ship.getHitHeight() + " angle=" + ship.getHitAngle() + thr(th));
            }
            //spoofed payload actor: sender=stranger, payload actor = owner player entity
            {
                ship.setHitHeight(0);
                ship.setHitAngle(0);
                Throwable th = runGui(new C2SGUIPackets(owner, C2SGUIPackets.PID.HitHeight, ship.getEntityId(), 41, 42), stranger);
                record("HitHeight, payload actor=owner", "stranger-as-sender (spoof)", ship.getHitHeight() == 41, false, "hitHeight=" + ship.getHitHeight() + thr(th));
            }

            //---- AddTeam (owner / OP only)
            for (int i = 0; i < actors.length; i++) {
                EntityPlayerMP a = (EntityPlayerMP) actors[i][1];
                CapaTeitoku c = CapaTeitoku.getTeitokuCapability(a);
                try {
                    c.removeShipCurrentTeam();
                } catch (Throwable ignored) {
                }
                Throwable th = runGui(new C2SGUIPackets(a, C2SGUIPackets.PID.AddTeam, ship.getEntityId()), a);
                boolean in = false;
                for (int s = 0; s < 6; s++) if (c.getShipEntityCurrentTeam(s) == ship) in = true;
                record("AddTeam(ship)", (String) actors[i][0], in, expOwnerOnly[i], "inActorTeam=" + in + thr(th));
                try {
                    c.removeShipCurrentTeam();
                } catch (Throwable ignored) {
                }
            }
            {
                CapaTeitoku cs = CapaTeitoku.getTeitokuCapability(stranger), co = CapaTeitoku.getTeitokuCapability(owner);
                try {
                    cs.removeShipCurrentTeam();
                    co.removeShipCurrentTeam();
                } catch (Throwable ignored) {
                }
                Throwable th = runGui(new C2SGUIPackets(owner, C2SGUIPackets.PID.AddTeam, ship.getEntityId()), stranger);
                boolean inS = false, inO = false;
                for (int s = 0; s < 6; s++) {
                    if (cs.getShipEntityCurrentTeam(s) == ship) inS = true;
                    if (co.getShipEntityCurrentTeam(s) == ship) inO = true;
                }
                record("AddTeam, payload actor=owner", "stranger-as-sender (spoof)", inS || inO, false, "inStrangerTeam=" + inS + " inOwnerTeam=" + inO + thr(th));
            }

            //---- OpenShipGUI: handler path cannot be observed with a EntityPlayerMP (FMLNetworkHandler.openGui ignores them), so check the GuiHandler server element
            com.lulan.shincolle.handler.GuiHandler gh = new com.lulan.shincolle.handler.GuiHandler();
            for (int i = 0; i < actors.length; i++) {
                EntityPlayerMP a = (EntityPlayerMP) actors[i][1];
                Object el = null;
                Throwable thrown = null;
                try {
                    el = gh.getServerGuiElement(ID.Gui.SHIPINVENTORY, a, w, ship.getEntityId(), 0, 0);
                } catch (Throwable th) {
                    thrown = th;
                }
                record("GuiHandler SHIPINVENTORY container (OpenShipGUI path)", (String) actors[i][0], el != null, expShipCmd[i], "container=" + (el == null ? "null" : el.getClass().getSimpleName()) + thr(thrown));
                //the stub connection has no netty channel, so FMLNetworkHandler.openGui throws an NPE on Channel.attr: reaching it proves the authorization passed
                Throwable th = runGui(new C2SGUIPackets(a, C2SGUIPackets.PID.OpenShipGUI, ship.getEntityId()), a);
                boolean reached = th != null && String.valueOf(th.getMessage()).contains("Channel.attr");
                record("OpenShipGUI packet handler reached FMLNetworkHandler.openGui", (String) actors[i][0], reached, expShipCmd[i], thr(th));
            }
            {
                Throwable th = runGui(new C2SGUIPackets(owner, C2SGUIPackets.PID.OpenShipGUI, ship.getEntityId()), stranger);
                record("OpenShipGUI packet, payload actor=owner", "stranger-as-sender (spoof)", th != null && String.valueOf(th.getMessage()).contains("Channel.attr"), false, thr(th));
            }
            //far owner: 12 blocks away -> container rejected
            {
                owner.setPosition(x + 40, y, z);
                Object el = gh.getServerGuiElement(ID.Gui.SHIPINVENTORY, owner, w, ship.getEntityId(), 0, 0);
                info("GuiHandler SHIPINVENTORY container for owner 40 blocks away (reach is enforced in PacketAuthorization.mayUseShipContainer, not getServerGuiElement): " + (el == null ? "null" : el.getClass().getSimpleName())
                        + " ; mayUseShipContainer=" + PacketAuthorization.mayUseShipContainer(owner, ship));
                record("mayUseShipContainer (owner 40 blocks away)", "owner", PacketAuthorization.mayUseShipContainer(owner, ship), false, "");
                owner.setPosition(x, y, z);
                record("mayUseShipContainer (owner in reach)", "owner", PacketAuthorization.mayUseShipContainer(owner, ship), true, "");
            }

            //---- TileBtn (small shipyard: build type)
            for (int i = 0; i < actors.length; i++) {
                EntityPlayerMP a = (EntityPlayerMP) actors[i][1];
                yard.setBuildType(0);
                Throwable th = runGui(new C2SGUIPackets(yard, C2SGUIPackets.PID.TileBtn, 0, 2, 0), a);
                record("TileBtn(buildType=2)", (String) actors[i][0], yard.getBuildType() == 2, expTile[i], "buildType=" + yard.getBuildType() + thr(th));
            }
            {
                yard.setBuildType(0);
                owner.setPosition(x + 40, y, z);
                Throwable th = runGui(new C2SGUIPackets(yard, C2SGUIPackets.PID.TileBtn, 0, 2, 0), owner);
                record("TileBtn(buildType=2)", "owner 40 blocks away (reach)", yard.getBuildType() == 2, false, "buildType=" + yard.getBuildType() + thr(th));
                owner.setPosition(x, y, z);
            }

            //---- Request_WpSet / Request_ChestSet (owner or OP only, reach)
            for (int i = 0; i < actors.length; i++) {
                EntityPlayerMP a = (EntityPlayerMP) actors[i][1];
                tw1.setNextWaypoint(BlockPos.ORIGIN);
                Throwable th = runInput(new C2SInputPackets(C2SInputPackets.PID.Request_WpSet, OWNER_UID,
                        wp1.getX(), wp1.getY(), wp1.getZ(), wp2.getX(), wp2.getY(), wp2.getZ()), a);
                record("Request_WpSet (payload uid=owner)", (String) actors[i][0], wp2.equals(tw1.getNextWaypoint()), expOwnerOnly[i], "next=" + tw1.getNextWaypoint() + thr(th));
            }
            for (int i = 0; i < actors.length; i++) {
                EntityPlayerMP a = (EntityPlayerMP) actors[i][1];
                tw1.setPairedChest(chestPos2);   // baseline: a different chest (setPairedChest ignores non-inventory positions)
                Throwable th = runInput(new C2SInputPackets(C2SInputPackets.PID.Request_ChestSet, OWNER_UID,
                        wp1.getX(), wp1.getY(), wp1.getZ(), chestPos.getX(), chestPos.getY(), chestPos.getZ()), a);
                record("Request_ChestSet (payload uid=owner)", (String) actors[i][0], chestPos.equals(tw1.getPairedChest()), expOwnerOnly[i], "pairedChest=" + tw1.getPairedChest() + " (baseline " + chestPos2 + ", target " + chestPos + ")" + thr(th));
            }
            {
                tw1.setNextWaypoint(BlockPos.ORIGIN);
                owner.setPosition(x + 40, y, z);
                Throwable th = runInput(new C2SInputPackets(C2SInputPackets.PID.Request_WpSet, OWNER_UID,
                        wp1.getX(), wp1.getY(), wp1.getZ(), wp2.getX(), wp2.getY(), wp2.getZ()), owner);
                record("Request_WpSet", "owner 40+ blocks from both endpoints (reach)", wp2.equals(tw1.getNextWaypoint()), false, "next=" + tw1.getNextWaypoint() + thr(th));
                owner.setPosition(x, y, z);
            }
            {
                //payload spoofs a different uid than the sender's: stranger sends uid=owner
                tw1.setNextWaypoint(BlockPos.ORIGIN);
                Throwable th = runInput(new C2SInputPackets(C2SInputPackets.PID.Request_WpSet, OWNER_UID,
                        wp1.getX(), wp1.getY(), wp1.getZ(), wp2.getX(), wp2.getY(), wp2.getZ()), stranger);
                record("Request_WpSet, payload uid=owner", "stranger-as-sender (spoof)", wp2.equals(tw1.getNextWaypoint()), false, "next=" + tw1.getNextWaypoint() + thr(th));
                //too far endpoints (> pairDistWp): exercises the 'too far' branch that sends a message through the packet's player
                tw1.setNextWaypoint(BlockPos.ORIGIN);
                th = runInput(new C2SInputPackets(C2SInputPackets.PID.Request_WpSet, OWNER_UID,
                        wp1.getX(), wp1.getY(), wp1.getZ(), wp1.getX() + 200, wp1.getY(), wp1.getZ()), owner);
                info("Request_WpSet owner, endpoints 200 blocks apart (too-far branch): handler " + (th == null ? "returned normally" : "THREW " + th));
            }

            //cleanup
            w.setBlockToAir(yardPos);
            w.setBlockToAir(wp1);
            w.setBlockToAir(wp2);
            w.setBlockToAir(chestPos);
            w.setBlockToAir(chestPos2);
            ship.setDead();
            noOwnerShip.setDead();
            server.getPlayerList().removeOp(op.getGameProfile());
            for (int uid : new int[]{OWNER_UID, ALLY_UID, STRANGER_UID}) ServerProxy.removeTeamData(uid);
            for (EntityPlayerMP f : new EntityPlayerMP[]{owner, stranger, ally, op, creative}) unregister(w, f);
            return true;
        }
    }

    /* ------------------------------------------------------------------ E. world31 (#31/#58) */

    static final class World31 extends Probe {
        static final int IDLE_FROM = 40, SPAWN_AT = 140, SETTLE_AT = 260, MOVE_AT = 560, MOVE2_AT = 760, END_AT = 960;
        final List<BasicEntityShip> ships = new ArrayList<>();
        final Map<String, int[]> windows = new LinkedHashMap<>();
        String window;
        final Map<String, List<Integer>> evt = new LinkedHashMap<>();
        boolean reload;
        Path dat;

        World31() {
            super("world31");
        }

        @Override
        String resultFile() {
            return reload ? "gametest-probe-world31-reload-results.txt" : super.resultFile();
        }

        private void poll() {
            if (window == null || ServerProxy.serverData == null) return;
            if (ServerProxy.serverData.isDirty()) {
                windows.get(window)[0]++;
                windows.get(window)[1] = tick;
                evt.computeIfAbsent(window, k -> new ArrayList<>()).add(tick);
                ServerProxy.serverData.setDirty(false);
            }
        }

        private void open(String name) {
            window = name;
            windows.put(name, new int[]{0, -1});
            if (ServerProxy.serverData != null) ServerProxy.serverData.setDirty(false);
        }

        @Override
        boolean step(WorldServer w, int t) throws Throwable {
            if (t == 1) {
                dat = w.getSaveHandler().getWorldDirectory().toPath().resolve("data").resolve("shincolle.dat");
                reload = Files.exists(dat);
                final boolean rl = reload;
                final Path datF = dat;
                info("mode=" + (reload ? "RELOAD (shincolle.dat existed at start, size=" + sizeOf(dat) + ")" : "FRESH world"));
                info("tick1: ShinWorldData.nbtData is " + (com.lulan.shincolle.server.ShinWorldData.nbtData == null ? "null" : "non-null (keys=" + com.lulan.shincolle.server.ShinWorldData.nbtData.getKeySet() + ")"));
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        List<String> l = new ArrayList<>();
                        l.add("INFO shutdown-hook: mode=" + (rl ? "reload" : "fresh") + " ShinWorldData.nbtData=" + (com.lulan.shincolle.server.ShinWorldData.nbtData == null ? "null (cleared)" : "NON-NULL (not cleared)"));
                        l.add("INFO shutdown-hook: shincolle.dat exists=" + Files.exists(datF) + " size=" + sizeOf(datF));
                        Files.write(Paths.get("gametest-probe-world31-shutdown" + (rl ? "-reload" : "") + ".txt"), l, StandardCharsets.UTF_8);
                    } catch (Throwable th) {
                        th.printStackTrace();
                    }
                }));
            }
            if (reload) {
                //second run: just observe retained static state and loaded ships, then stop
                if (t == 40) {
                    int n = 0;
                    for (Entity e : new ArrayList<>(w.loadedEntityList)) if (e instanceof BasicEntityShip) n++;
                    info("tick40: ShinWorldData.nbtData is " + (com.lulan.shincolle.server.ShinWorldData.nbtData == null ? "null" : "non-null (keys=" + com.lulan.shincolle.server.ShinWorldData.nbtData.getKeySet() + ")"));
                    info("tick40: ships loaded from saved world = " + n + " ; ServerProxy ship map size = " + ServerProxy.getAllShipWorldData().size()
                            + " ; nextShipID=" + ServerProxy.getNextShipID());
                    open("reload-idle-300");
                }
                poll();
                if (t == 340) {
                    int[] r = windows.get("reload-idle-300");
                    info("reload-idle-300 ticks: dirtied " + r[0] + " time(s) (last tick " + r[1] + ")");
                    return true;
                }
                return false;
            }

            poll();
            if (t == IDLE_FROM) open("idle-no-ships");
            if (t == SPAWN_AT) {
                close();
                BlockPos sp = spawnTop(w);
                open("spawn-3-ships(+first 120 ticks)");
                for (int i = 0; i < 3; i++) {
                    BasicEntityShip s = spawnShip(w, "shincolle:EntityDestroyerI", "GT-w31-" + i, sp.getX() + 0.5 + 3 * i, sp.getY(), sp.getZ() + 0.5, null);
                    if (s != null) ships.add(s);
                }
                check("3 ships spawned", ships.size() == 3, "count=" + ships.size());
            }
            if (t == SETTLE_AT) {
                close();
                open("settled-stationary-300");
            }
            if (t == MOVE_AT) {
                close();
                open("one-ship-moved-200");
                BasicEntityShip s = ships.get(0);
                s.setPositionAndUpdate(s.posX + 24, s.posY, s.posZ);
                info("moved ship0 to " + (int) s.posX + "/" + (int) s.posZ + " at probe tick " + t);
            }
            if (t == MOVE_AT + 5 || t == MOVE_AT + 10 || t == MOVE_AT + 15) {
                //direct cache refresh: +5 after the move (changed content -> expect a mark), +10 and +15 unchanged (expect none)
                info("probe tick " + t + ": direct updateShipCacheDataWithoutNewID() on ship0 (dirty before call=" + ServerProxy.serverData.isDirty() + ")");
                ships.get(0).updateShipCacheDataWithoutNewID();
                info("probe tick " + t + ": dirty after call=" + ServerProxy.serverData.isDirty());
            }
            if (t == MOVE2_AT) {
                close();
                open("second-ship-moved-200");
                BasicEntityShip s = ships.get(1);
                s.setPositionAndUpdate(s.posX, s.posY, s.posZ + 24);
                info("moved ship1 to " + (int) s.posX + "/" + (int) s.posZ + " at probe tick " + t);
            }
            if (t == END_AT) {
                close();
                for (Map.Entry<String, int[]> en : windows.entrySet()) {
                    info("window " + en.getKey() + ": ShinWorldData dirtied " + en.getValue()[0] + " time(s) (events at probe ticks " + evt.getOrDefault(en.getKey(), new ArrayList<>()));
                }
                check("no dirtying while idle (no ships)", windows.get("idle-no-ships")[0] == 0, "dirty count=" + windows.get("idle-no-ships")[0]);
                check("dirtied when ships appear (positive control)", windows.get("spawn-3-ships(+first 120 ticks)")[0] > 0, "dirty count=" + windows.get("spawn-3-ships(+first 120 ticks)")[0]);
                check("no dirtying while 3 ships sit unchanged for 300 ticks", windows.get("settled-stationary-300")[0] == 0, "dirty count=" + windows.get("settled-stationary-300")[0]);
                check("dirtied when a ship moves (positive control)", windows.get("one-ship-moved-200")[0] > 0, "dirty count=" + windows.get("one-ship-moved-200")[0]);
                check("dirtied when a second ship moves (positive control)", windows.get("second-ship-moved-200")[0] > 0, "dirty count=" + windows.get("second-ship-moved-200")[0]);
                int sz = ServerProxy.getAllShipWorldData().size();
                info("ServerProxy ship map size=" + sz + " nextShipID=" + ServerProxy.getNextShipID());
                //force a save now so the file size reflects 3 ships (the normal shutdown save also happens)
                ServerProxy.saveServerProxy();
                info("shincolle.dat size after explicit save with " + sz + " ships = " + sizeOf(dat) + " bytes");
                ServerProxy.saveServerFile = true;   //so the normal unload path clears the cache and saves again
                return true;
            }
            return false;
        }

        private void close() {
            poll();
            window = null;
        }

        static long sizeOf(Path p) {
            try {
                return Files.exists(p) ? Files.size(p) : -1;
            } catch (java.io.IOException e) {
                return -2;
            }
        }
    }
}
