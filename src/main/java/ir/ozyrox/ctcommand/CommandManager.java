package ir.ozyrox.ctcommand;

import ir.ozyrox.ctcommand.annotation.Command;
import ir.ozyrox.ctcommand.annotation.Completer;
import ir.ozyrox.ctcommand.annotation.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class CommandManager {
    private final JavaPlugin plugin;

    private final Map<String, Long> cooldowns = new HashMap<>();

    public CommandManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(CommandBase instance) {
        Class<?> clazz = instance.getClass();

        if (clazz.isAnnotationPresent(Command.class)) {
            registerWithSubCommand(instance, clazz.getAnnotation(Command.class));
        } else {
            registerSimple(instance);
        }
    }

    private void registerSimple(CommandBase instance) {
        for (Method method : instance.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Command.class)) continue;
            Command cmd = method.getAnnotation(Command.class);

            PluginCommand pc = plugin.getCommand(cmd.name());
            if (pc == null) {
                plugin.getLogger().warning("Command " + cmd.name() + " is not defined in plugin.yml");
                continue;
            }

            Method completerMethod = findCompleter(instance.getClass(), cmd.name());

            pc.setExecutor((sender, command, label, args) -> {

                if (cmd.playerOnly() && !(sender instanceof Player)) {
                    instance.onPlayerOnly(sender);
                    return true;
                }

                if (!cmd.permission().isEmpty() && !sender.hasPermission(cmd.permission())) {
                    instance.onNoPermission(sender);
                    return true;
                }

                if (isOnCooldown(sender, cmd.name(), cmd.cooldown(), instance)) {
                    return true;
                }

                invoke(instance, method, sender, args);
                return true;
            });

            if (completerMethod != null) {
                pc.setTabCompleter((sender, command, alias, args) -> invokeCompleter(instance, completerMethod, sender, args));
            }
        }
    }

    private void registerWithSubCommand(CommandBase instance, Command rootCmd) {
        PluginCommand pc = plugin.getCommand(rootCmd.name());
        if (pc == null) {
            plugin.getLogger().warning("Command " + rootCmd.name() + " is not defined in plugin.yml");
            return;
        }

        Map<String, Method> subCommand = new HashMap<>();
        Map<String, Method> completers = new HashMap<>();

        for (Method method : instance.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(SubCommand.class)) {
                SubCommand sub = method.getAnnotation(SubCommand.class);
                subCommand.put(sub.value().toLowerCase(), method);
            }
            if (method.isAnnotationPresent(Completer.class)) {
                Completer comp = method.getAnnotation(Completer.class);
                completers.put(comp.value().toLowerCase(), method);
            }
        }

        pc.setExecutor((sender, command, label, args) -> {
            if (rootCmd.playerOnly() && !(sender instanceof Player)) {
                instance.onPlayerOnly(sender);
                return true;
            }
            if (!rootCmd.permission().isEmpty() && !sender.hasPermission(rootCmd.permission())) {
                instance.onNoPermission(sender);
                return true;
            }

            if (args.length == 0) {
                instance.onInvalidUsage(sender, "");
                return true;
            }

            Method method = subCommand.get(args[0].toLowerCase());
            if (method == null) {
                instance.onInvalidUsage(sender, "");
                return true;
            }

            SubCommand sub = method.getAnnotation(SubCommand.class);

            if (sub.playerOnly() && !(sender instanceof Player)) {
                instance.onPlayerOnly(sender);
                return true;
            }

            if (!sub.permission().isEmpty() && !sender.hasPermission(sub.permission())) {
                instance.onNoPermission(sender);
                return true;
            }

            String cooldownKey = rootCmd.name() + ":" + sub.value();
            if (isOnCooldown(sender, cooldownKey, sub.cooldown(), instance)) {
                return true;
            }

            String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);

            if (remainingArgs.length < sub.minArgs()) {
                instance.onInvalidUsage(sender, sub.usage());
                return true;
            }

            invoke(instance, method, sender, remainingArgs);
            return true;
        });

        pc.setTabCompleter((sender, command, alias, args) -> {
            if (args.length == 1) {
                return subCommand.keySet().stream()
                        .filter(s -> s.startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }

            Method completerMethod = completers.get(args[0].toLowerCase());
            if (completerMethod == null) return Collections.emptyList();

            String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);
            return invokeCompleter(instance, completerMethod, sender, remainingArgs);
        });

    }

    private Method findCompleter(Class<?> clazz, String name) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Completer.class) && method.getAnnotation(Completer.class).value().equalsIgnoreCase(name)) {
                return method;
            }
        }
        return null;
    }

    private List<String> invokeCompleter(CommandBase instance, Method method, CommandSender sender, String[] args) {
        try {
            Object result = method.invoke(instance, sender, args);
            return (List<String>) result;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private void invoke(CommandBase instance, Method method, CommandSender sender, String[] args) {
        try {
            method.invoke(instance, sender, args);
        } catch (Exception e) {
            sender.sendMessage("Fail to execute command");
            e.printStackTrace();
        }
    }

    private boolean isOnCooldown(CommandSender sender, String cooldownKey, int cooldownSeconds, CommandBase instance) {
        if (cooldownSeconds <= 0) return false;
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;

        String key = player.getUniqueId() + ":" + cooldownKey;
        long now = System.currentTimeMillis();
        long lastUse = cooldowns.getOrDefault(key, 0L);
        long elapsedSeconds = (now - lastUse) / 1000;

        if (elapsedSeconds < cooldownSeconds) {
            long secondsLeft = cooldownSeconds - elapsedSeconds;
            instance.onCooldown(sender, secondsLeft);
            return true;
        }

        cooldowns.put(key, now);
        return false;

    }
}
