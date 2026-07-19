package ir.ozyrox.ctcommand;

import org.bukkit.command.CommandSender;

public abstract class CommandBase {

    public void onNoPermission(CommandSender sender){
        sender.sendMessage("You don't have permission to use this command");
    }

    public void onInvalidUsage(CommandSender sender, String usage){
        sender.sendMessage("Invalid usage: " + usage);
    }

    public void onPlayerOnly(CommandSender sender){
        sender.sendMessage("Player Only");
    }

    public void onCooldown(CommandSender sender, long secondsLeft){
        sender.sendMessage("wait " + secondsLeft + "s before use again");
    }
}
