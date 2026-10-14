package com.lulan.shincolle.network;

import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.utility.TeamHelper;
import net.minecraft.entity.player.EntityPlayerMP;
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
