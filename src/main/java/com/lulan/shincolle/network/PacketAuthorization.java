package com.lulan.shincolle.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;

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
}
