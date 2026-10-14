package com.lulan.shincolle.gametest;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.init.ModItems;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Dedicated-server side of the harness. Only registered with -Dshincolle.gametest.server=true.
 * Provides a read-only state probe (and a few setup shortcuts) that run on the SERVER thread, so
 * the harness reads real server state instead of client-synced values. Never shipped.
 */
public final class GameTestServerHooks {

    static final Logger LOG = LogManager.getLogger("GameTestServer");
    static final UUID FAKE_OWNER_UUID = UUID.fromString("00000000-0000-4000-8000-000000009999");
    static final int FAKE_OWNER_UID = 9999;

    /** Logs every /ship* command with the sender's real server-side permission, before it executes. */
    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        String name = event.getCommand().getName();
        if (!name.startsWith("ship")) return;
        ICommandSender s = event.getSender();
        String perm = "n/a";
        if (s instanceof EntityPlayerMP) {
            EntityPlayerMP p = (EntityPlayerMP) s;
            perm = "canSendCommands=" + p.getServer().getPlayerList().canSendCommands(p.getGameProfile())
                    + " gamemode=" + p.interactionManager.getGameType();
        }
        LOG.info("[GAMETEST-SRV] command /{} {} by {} ({})", name, String.join(" ", event.getParameters()), s.getName(), perm);
    }

    private final java.util.Map<UUID, String> lastSeen = new java.util.HashMap<>();
    private int tickCounter;
    private final java.util.Map<BasicEntityShip, Integer> watched = new java.util.HashMap<>();

    /** logs ship joins, and the last known state of a ship that disappears (diagnosis of ships vanishing on the server) */
    @SubscribeEvent
    public void onJoin(net.minecraftforge.event.entity.EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof BasicEntityShip && !event.getWorld().isRemote) {
            BasicEntityShip s = (BasicEntityShip) event.getEntity();
            LOG.info("[GAMETEST-SRV] ship join {} uuid={} canceled={} hp={}", s.getClass().getSimpleName(), s.getUniqueID(), event.isCanceled(), s.getHealth());
            watched.put(s, 40);
        }
    }

    @SubscribeEvent
    public void onWorldTick(net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent event) {
        if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END || event.world.isRemote
                || event.world.provider.getDimension() != 0) return;
        if (SHIPLOAD) shipLoadTick((net.minecraft.world.WorldServer) event.world);
        for (java.util.Iterator<java.util.Map.Entry<BasicEntityShip, Integer>> it = watched.entrySet().iterator(); it.hasNext(); ) {
            java.util.Map.Entry<BasicEntityShip, Integer> en = it.next();
            BasicEntityShip w = en.getKey();
            LOG.info("[GAMETEST-SRV] watch ship ticks={} isDead={} hp={} inLoadedList={} pos={}/{}/{} shipUID={}", w.ticksExisted, w.isDead, w.getHealth(),
                    event.world.loadedEntityList.contains(w), (int) w.posX, (int) w.posY, (int) w.posZ, w.getShipUID());
            if (en.getValue() <= 1) it.remove();
            else en.setValue(en.getValue() - 1);
        }
        if (++tickCounter % 5 != 0) return;
        java.util.Set<UUID> now = new java.util.HashSet<>();
        for (net.minecraft.entity.Entity e : new java.util.ArrayList<>(event.world.loadedEntityList)) {
            if (!(e instanceof BasicEntityShip)) continue;
            BasicEntityShip s = (BasicEntityShip) e;
            now.add(s.getUniqueID());
            lastSeen.put(s.getUniqueID(), String.format("ticks=%d hp=%.1f isDead=%s deathTime=%d pos=%.1f/%.1f/%.1f shipUID=%d ownerUID=%d",
                    s.ticksExisted, s.getHealth(), s.isDead, s.deathTime, s.posX, s.posY, s.posZ, s.getShipUID(), s.getPlayerUID()));
        }
        for (java.util.Iterator<java.util.Map.Entry<UUID, String>> it = lastSeen.entrySet().iterator(); it.hasNext(); ) {
            java.util.Map.Entry<UUID, String> en = it.next();
            if (!now.contains(en.getKey())) {
                LOG.info("[GAMETEST-SRV] ship {} no longer in loadedEntityList; last seen: {}", en.getKey(), en.getValue());
                it.remove();
            }
        }
    }

    /* ---------------- issue #11 / #7: ships loaded without (or with partial) ShipExtProps ----------------
     * Headless self-test, no client and no RCON. Only with -Dshincolle.gametest.shipload=true.
     * Summons ships the way /summon does (AnvilChunkLoader.readWorldEntityPos) at the world spawn, which stays
     * loaded and ticking on a dedicated server without players, then reads the state after the slow update
     * (BasicEntityShip calls calcShipAttributes every 128 ticks) and shuts the server down.
     */
    private static final boolean SHIPLOAD = Boolean.getBoolean("shincolle.gametest.shipload");
    private static final int SHIPLOAD_SPAWN_TICK = 40;
    private static final int SHIPLOAD_WAIT_TICKS = 300;    //> 2 slow updates
    private final java.util.Map<String, BasicEntityShip> loadShips = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, String> loadBefore = new java.util.HashMap<>();
    private final java.util.List<String> loadResults = new java.util.ArrayList<>();
    private final java.util.Set<String> spawnFull = new java.util.HashSet<>();
    private int loadTick;
    private boolean loadDone;

    private static net.minecraft.nbt.NBTTagCompound stubExt(String... sections) {
        net.minecraft.nbt.NBTTagCompound ext = new net.minecraft.nbt.NBTTagCompound();
        for (String sec : sections) ext.setTag(sec, new net.minecraft.nbt.NBTTagCompound());
        return ext;
    }

    private void spawnLoadCase(net.minecraft.world.WorldServer w, String name, double x, double y, double z, net.minecraft.nbt.NBTTagCompound ext) {
        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
        tag.setString("id", "shincolle:EntityDestroyerI");
        tag.setString("CustomName", name);
        if (ext != null) tag.setTag("ShipExtProps", ext);
        try {
            net.minecraft.entity.Entity e = net.minecraft.world.chunk.storage.AnvilChunkLoader.readWorldEntityPos(tag, w, x, y, z, true);
            if (e instanceof BasicEntityShip) {
                BasicEntityShip s = (BasicEntityShip) e;
                loadShips.put(name, s);
                loadBefore.put(name, describe(s));
                if (Math.abs(s.getHealth() - s.getMaxHealth()) < 0.01F) spawnFull.add(name);
                loadResults.add("INFO " + name + " spawned: " + describe(s));
            } else {
                loadResults.add("FAIL " + name + ": summon returned " + e);
            }
        } catch (Throwable t) {
            LOG.error("[GAMETEST-SRV] shipload spawn threw for {}", name, t);
            loadResults.add("FAIL " + name + ": spawn threw " + t);
        }
    }

    private static String describe(BasicEntityShip s) {
        return String.format("ticks=%d level=%d hp=%.2f maxHp=%.2f baseMaxHp=%.2f isDead=%s pos=%.1f/%.1f/%.1f", s.ticksExisted, s.getLevel(),
                s.getHealth(), s.getMaxHealth(), s.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.MAX_HEALTH).getBaseValue(),
                s.isDead, s.posX, s.posY, s.posZ);
    }

    private void shipLoadTick(net.minecraft.world.WorldServer w) {
        if (loadDone) return;
        loadTick++;
        if (loadTick == SHIPLOAD_SPAWN_TICK) {
            net.minecraft.util.math.BlockPos sp = w.getTopSolidOrLiquidBlock(w.getSpawnPoint());
            double y = sp.getY();
            spawnLoadCase(w, "GTLOAD-bare", sp.getX() + 0.5, y, sp.getZ() + 0.5, null);
            spawnLoadCase(w, "GTLOAD-stub5", sp.getX() + 3.5, y, sp.getZ() + 0.5, stubExt("Minor", "Display", "Point", "ShipFlags", "Timer"));
            spawnLoadCase(w, "GTLOAD-minor", sp.getX() + 6.5, y, sp.getZ() + 0.5, stubExt("Minor"));
            LOG.info("[GAMETEST-SRV] shipload spawned {} ships at spawn {}", loadShips.size(), sp);
        }
        if (loadTick == SHIPLOAD_SPAWN_TICK + SHIPLOAD_WAIT_TICKS) {
            int fails = 0;
            for (java.util.Map.Entry<String, BasicEntityShip> en : loadShips.entrySet()) {
                BasicEntityShip s = en.getValue();
                String name = en.getKey();
                String after = describe(s);
                boolean inWorld = w.loadedEntityList.contains(s);
                boolean ticked = s.ticksExisted >= 2 * 128 - 20;    //two slow updates (every 128 ticks) have run
                boolean alive = !s.isDead && s.isEntityAlive();
                boolean levelOk = s.getLevel() == 1;
                //no Health tag in the summon NBT: the ship must start at full HP, both right after the summon and after the slow updates
                boolean hpOk = s.getMaxHealth() > 4.0F && Math.abs(s.getHealth() - s.getMaxHealth()) < 0.01F && spawnFull.contains(name);
                String detail = "before[" + loadBefore.get(name) + "] after[" + after + "] inLoadedList=" + inWorld + " ticked=" + ticked;
                if (alive && inWorld && ticked && levelOk && hpOk) loadResults.add("PASS " + name + ": " + detail);
                else {
                    fails++;
                    loadResults.add("FAIL " + name + " (alive=" + alive + " inWorld=" + inWorld + " ticked=" + ticked + " level1=" + levelOk
                            + " hpEqualsMax=" + hpOk + "): " + detail);
                }
            }
            if (loadShips.size() != 3) fails++;
            loadResults.add("SUMMARY cases=" + loadShips.size() + " failed=" + fails);
            for (String line : loadResults) LOG.info("[GAMETEST-SRV] shipload {}", line);
            try {
                java.nio.file.Files.write(java.nio.file.Paths.get("gametest-shipload-results.txt"), loadResults, java.nio.charset.StandardCharsets.UTF_8);
            } catch (java.io.IOException e) {
                LOG.error("[GAMETEST-SRV] cannot write shipload results", e);
            }
            loadDone = true;
            w.getMinecraftServer().initiateShutdown();
        }
    }

    public static final class ProbeCommand extends CommandBase {
        @Override
        public String getName() {
            return "gtprobe";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "/gtprobe state|egg|fakeowner|reset|stop (dev harness only)";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        /** vanilla refuses every command for non-OP players except a few; the probe must work for them too (dev harness only) */
        @Override
        public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
            return sender instanceof EntityPlayerMP;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (!(sender instanceof EntityPlayerMP) || args.length < 1) return;
            EntityPlayerMP p = (EntityPlayerMP) sender;

            switch (args[0]) {
                case "egg":
                    p.inventory.setInventorySlotContents(p.inventory.currentItem, new ItemStack(ModItems.ShipSpawnEgg, 1, 2));
                    p.inventoryContainer.detectAndSendChanges();
                    reply(p, "egg given slot=" + p.inventory.currentItem);
                    return;
                case "stop":
                    reply(p, "stopping server");
                    server.initiateShutdown();
                    return;
                default:
                    break;
            }

            //all loaded ships in the player's world (not only near the player), so a misplaced spawn is still found
            List<BasicEntityShip> ships = new java.util.ArrayList<>();
            for (net.minecraft.entity.Entity e : p.world.loadedEntityList) {
                if (e instanceof BasicEntityShip && e.isEntityAlive()) ships.add((BasicEntityShip) e);
            }
            BasicEntityShip s = ships.isEmpty() ? null : ships.get(0);

            switch (args[0]) {
                case "fakeowner":
                    if (s == null) {
                        reply(p, "fakeowner ships=0");
                        return;
                    }
                    int prev = s.getPlayerUID();
                    s.setPlayerUID(FAKE_OWNER_UID);
                    s.setOwnerId(FAKE_OWNER_UUID);
                    s.sendSyncPacketAll();
                    reply(p, "fakeowner prevOwnerUID=" + prev + " nowOwnerUID=" + s.getPlayerUID());
                    return;
                case "reset":
                    if (s == null) {
                        reply(p, "reset ships=0");
                        return;
                    }
                    for (int i = 0; i < 6; i++) s.getAttrs().setAttrsBonus(i, 0);
                    s.setShipLevel(1, true);
                    reply(p, "reset level=" + s.getLevel());
                    return;
                case "state":
                    StringBuilder sb = new StringBuilder("state ships=").append(ships.size());
                    if (s != null) {
                        byte[] b = s.getAttrs().getAttrsBonus();
                        sb.append(" uuid=").append(s.getUniqueID())
                                .append(" shipPos=").append(String.format("%.1f/%.1f/%.1f", s.posX, s.posY, s.posZ))
                                .append(" eid=").append(s.getEntityId())
                                .append(" shipUID=").append(s.getShipUID())
                                .append(" ownerUID=").append(s.getPlayerUID())
                                .append(" ownerUUID=").append(s.getOwnerId())
                                .append(" level=").append(s.getLevel())
                                .append(" bonus=");
                        for (int i = 0; i < b.length; i++) sb.append(i == 0 ? "" : ",").append(b[i]);
                    }
                    sb.append(" playerPos=").append(String.format("%.1f/%.1f/%.1f", p.posX, p.posY, p.posZ))
                            .append(" playerUID=").append(com.lulan.shincolle.utility.EntityHelper.getPlayerUID(p))
                            .append(" op=").append(server.getPlayerList().canSendCommands(p.getGameProfile()))
                            .append(" gm=").append(p.interactionManager.getGameType());
                    reply(p, sb.toString());
                    return;
                default:
                    reply(p, "unknown subcommand " + args[0]);
            }
        }

        private static void reply(EntityPlayerMP p, String msg) {
            LOG.info("[GAMETEST-SRV] {}", msg);
            p.sendMessage(new TextComponentString("[GT] " + msg));
        }
    }
}
