package ir.ozyrox.ctcommand;

import org.bukkit.command.CommandSender;

public abstract class CommandBase {

    protected void onNoPermission(CommandSender sender) {
        sender.sendMessage("You don't have permission to use this command");
    }

    protected void onInvalidUsage(CommandSender sender, String usage){
        sender.sendMessage("Invalid usage: " + usage);
    }

    protected void onPlayerOnly(CommandSender sender) {
        sender.sendMessage("Player Only");
    }

    protected void onConsoleOnly(CommandSender sender) {
        sender.sendMessage("This is for console senders only");
    }

    protected void onCooldown(CommandSender sender, long secondsLeft){
        sender.sendMessage("wait " + secondsLeft + "s before use again");
    }
}
