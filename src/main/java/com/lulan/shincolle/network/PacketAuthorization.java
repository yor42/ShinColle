package com.lulan.shincolle.network;

import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipOwner;
import com.lulan.shincolle.utility.TeamHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Server-side authorization for client command packets (NET-02, issue #2).
 * The permission query is injected so the decision can be tested without a server.
 */
public final class PacketAuthorization {

    private PacketAuthorization() {
    }

    /**
     * @param sender        the player handling the packet on the server (from the message context), never from the payload
     * @param serverOpQuery server-side OP query; creative mode must not be part of it
     */
    public static <P> boolean mayEditShips(P sender, Predicate<P> serverOpQuery) {
        return sender != null && serverOpQuery.test(sender);
    }

    /**
     * Production check: same rule the /ship commands use (PlayerList.canSendCommands), without the creative shortcut.
     */
    public static boolean mayEditShips(EntityPlayerMP sender) {
        return mayEditShips(sender, p -> FMLCommonHandler.instance().getMinecraftServerInstance()
                .getPlayerList().canSendCommands(p.getGameProfile()));
    }

    /** Vanilla container reach: 8 blocks (squared) and a loaded chunk. */
    public static final double REACH_SQ = 64.0D;

    /**
     * Pure core of the ship-command rule (ADR-0004, issue #29).
     * The actor may act when its UID is a real player UID (> 0) and it is the owner, a server OP,
     * or the owner's ally. Ownerless (-1) and mob-owned (&lt; -1) ships accept server OPs only.
     *
     * @param isTeammate (ownerUid, actorUid): true if the owner's ally list contains the actor
     */
    public static boolean mayAct(int actorUid, int ownerUid, boolean isServerOp, BiPredicate<Integer, Integer> isTeammate) {
        if (actorUid <= 0) return false;
        if (ownerUid > 0) {
            return actorUid == ownerUid || isServerOp || isTeammate.test(ownerUid, actorUid);
        }
        return isServerOp;
    }

    /**
     * Pure owner-only variant (no ally access): owner or server OP.
     */
    public static boolean mayActAsOwner(int actorUid, int ownerUid, boolean isServerOp) {
        return mayAct(actorUid, ownerUid, isServerOp, (o, a) -> false);
    }

    /**
     * Pure reach rule for batch 2: within 8 blocks (squared distance &lt;= 64) and chunk loaded.
     */
    public static boolean withinReach(double distSq, boolean chunkLoaded) {
        return chunkLoaded && distSq <= REACH_SQ;
    }

    /**
     * Pure tile/container rule (batch 2): the {@link #mayAct} owner/OP/ally rule plus vanilla reach.
     * Used for tiles that have an owner (IShipOwner).
     */
    public static boolean mayUseOwnedTile(int actorUid, int ownerUid, boolean isServerOp,
                                          BiPredicate<Integer, Integer> isTeammate, double distSq, boolean chunkLoaded) {
        return withinReach(distSq, chunkLoaded) && mayAct(actorUid, ownerUid, isServerOp, isTeammate);
    }

    /**
     * Pure pairing rule (Request_WpSet / Request_ChestSet): owner or server OP, no ally access.
     * An unloaded chunk or no endpoint within reach rejects. The wrench is used on the second block and the first
     * one may be up to the configured pairing distance away, so one endpoint within reach is enough.
     */
    public static boolean mayPairTiles(int actorUid, int ownerUid, boolean isServerOp,
                                       double distSqA, boolean loadedA, double distSqB, boolean loadedB) {
        if (!loadedA || !loadedB) return false;
        if (!withinReach(distSqA, true) && !withinReach(distSqB, true)) return false;
        return mayActAsOwner(actorUid, ownerUid, isServerOp);
    }

    /** Array guard: true when the array exists and has at least {@code min} elements. */
    public static boolean hasLength(int[] arr, int min) {
        return arr != null && arr.length >= min;
    }

    /** Array guard for byte payloads. */
    public static boolean hasLength(byte[] arr, int min) {
        return arr != null && arr.length >= min;
    }

    /**
     * True when the chunk holding {@code pos} is already loaded in {@code world}. Never loads or generates a chunk
     * (World#getTileEntity would), so call this before resolving a client-supplied position.
     */
    public static boolean isChunkLoaded(World world, BlockPos pos) {
        return world != null && pos != null && world.getChunkProvider().getLoadedChunk(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    /** Squared distance from the player to the block center. */
    public static double distSqToBlock(EntityPlayer player, BlockPos pos) {
        return player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    /**
     * Production check for TileBtn / Desk_FuncSync: sender-bound reach (8 blocks, loaded chunk) and, for tiles with an
     * owner (IShipOwner), owner / ally / non-Creative OP. Tiles without an owner concept (e.g. the desk) are reach-only.
     *
     * @param tile the tile already resolved from the sender's world; may be null
     */
    public static boolean mayUseTile(EntityPlayerMP sender, TileEntity tile) {
        if (sender == null || tile == null || tile.isInvalid() || tile.getWorld() != sender.world) return false;

        BlockPos pos = tile.getPos();
        boolean loaded = isChunkLoaded(sender.world, pos);
        double distSq = distSqToBlock(sender, pos);

        if (!(tile instanceof IShipOwner)) {
            return withinReach(distSq, loaded);
        }

        CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(sender);
        if (capa == null) return false;
        return mayUseOwnedTile(capa.getPlayerUID(), ((IShipOwner) tile).getPlayerUID(), isServerOp(sender),
                TeamHelper::checkIsAlly, distSq, loaded);
    }

    /** Container variant: only a server-side player can be checked; the client side is not authoritative. */
    public static boolean isServerOp(EntityPlayer player) {
        return player instanceof EntityPlayerMP && isServerOp((EntityPlayerMP) player);
    }

    /**
     * Container check for a ship GUI: ship alive and the player may command it, within 8 blocks.
     * A non-server player (client side) is not authoritative and passes.
     */
    public static boolean mayUseShipContainer(EntityPlayer player, BasicEntityShip ship) {
        if (!(player instanceof EntityPlayerMP)) return true;
        if (ship == null || !ship.isEntityAlive() || ship.world != player.world) return false;
        return withinReach(player.getDistanceSq(ship), true) && mayCommandShip((EntityPlayerMP) player, ship);
    }

    /**
     * Server OP (PlayerList.canSendCommands); creative mode deliberately does not count. Packets only.
     */
    public static boolean isServerOp(EntityPlayerMP sender) {
        return mayEditShips(sender);
    }

    /**
     * Production check: sender may command the ship (owner, owner's ally, or server OP).
     */
    public static boolean mayCommandShip(EntityPlayerMP sender, BasicEntityShip ship) {
        if (sender == null || ship == null) return false;
        CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(sender);
        if (capa == null) return false;
        return mayAct(capa.getPlayerUID(), ship.getPlayerUID(), isServerOp(sender), TeamHelper::checkIsAlly);
    }

    /**
     * Production check: sender owns the ship or is a server OP (no ally access).
     */
    public static boolean mayOwnShip(EntityPlayerMP sender, BasicEntityShip ship) {
        if (sender == null || ship == null) return false;
        CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(sender);
        if (capa == null) return false;
        return mayActAsOwner(capa.getPlayerUID(), ship.getPlayerUID(), isServerOp(sender));
    }
}
