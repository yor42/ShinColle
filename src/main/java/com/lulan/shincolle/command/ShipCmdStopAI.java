package com.lulan.shincolle.command;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.utility.EntityHelper;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Command: /shipstopai
 * <p>
 * stop all ship AI (stop onUpdate, onLivingUpdate) for debug
 * <p>
 * authority: OP only
 * <p>
 * type:
 * 1. /shipstopai
 * <p>
 * process:
 * 1. check sender is OP (server)
 * 3. set stop flag
 */
public class ShipCmdStopAI extends CommandBase {

    //command name list
    private static final ArrayList<String> Aliases = new ArrayList() {{
        add("shipstopai");
        add("shipstop");
    }};


    public ShipCmdStopAI() {
    }

    /**
     * command name
     */
    @Override
    public String getName() {
        return Aliases.get(0);
    }

    /**
     * command alias
     */
    @Override
    public List<String> getAliases() {
        return Aliases;
    }

    /**
     * command guide text
     */
    @Override
    public String getUsage(ICommandSender sender) {
        return "/shipstopai";
    }

    /**
     * command authority
     */
    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender instanceof EntityPlayer;
    }

    /**
     * command process, SERVER SIDE ONLY
     */
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        World world = sender.getEntityWorld();
        EntityPlayer op = null;
        int senderEID = -1;
        boolean isOP = false;

        if (!world.isRemote) {
            //check sender is player
            if (sender instanceof EntityPlayer) {
                op = (EntityPlayer) sender;
                isOP = EntityHelper.checkOP(op);

                if (isOP) {
                    BasicEntityShip.stopAI = !BasicEntityShip.stopAI;
                    BasicEntityShipHostile.stopAI = !BasicEntityShipHostile.stopAI;
                    BasicEntityMount.stopAI = !BasicEntityMount.stopAI;
                    sender.sendMessage(new TextComponentTranslation("chat.shincolle:command.command")
                            .appendSibling(new TextComponentString(" shipstopai: " + BasicEntityShip.stopAI)));
                }
            }//is player
        }//end server side
    }


}