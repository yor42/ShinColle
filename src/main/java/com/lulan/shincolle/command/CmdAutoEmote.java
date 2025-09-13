package com.lulan.shincolle.command;

import com.lulan.shincolle.utility.AutoEmoteHelper;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

public class CmdAutoEmote extends CommandBase {

    private static final List<String> ALIASES = new ArrayList<>() {{
        add("autoem");
        add("autoemo");
        add("aemote");
        add("aemotes");
    }};

    @Override
    public String getName() {
        return "autoemote";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/autoemote";
    }

    @Override
    public List<String> getAliases() {
        return ALIASES;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(sender instanceof EntityPlayer player){
            AutoEmoteHelper.AUTO_EMOTE_MANAGER.toggleAutoEmote(player);
        }
    }
}
