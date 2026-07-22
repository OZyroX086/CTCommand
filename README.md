# ctcommand

A lightweight annotation-based command framework for Paper/Folia/Spigot plugins. Define commands with simple annotations instead of writing boilerplate `CommandExecutor` classes by hand.

## Features

- Class-based command registration
- Simple commands using `@DefaultCommand`
- Subcommands using `@SubCommand`
- Automatic tab-completion per command/subcommand
- Permission checks with `@HasPermission` (customizable messages)
- Player-only (`@PlayerOnly`), console-only (`@ConsoleOnly`), and op-only (`@OpOnly`) restrictions
- Per-command / per-subcommand cooldowns
- Usage validation with `minArgs`
- Fully overridable messages (no permission, invalid usage, cooldown, player-only, console-only)

## Installation

### Maven

Add the JitPack repository:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Add the dependency:

```xml
<dependency>
    <groupId>com.github.ozyrox086</groupId>
    <artifactId>ctcommand</artifactId>
    <version>v1.2.0</version>
</dependency>
```

Since `ctcommand` needs to be bundled inside your plugin's final jar (the server doesn't know about it), add the Shade plugin to `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.5.1</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Then build with:

```bash
mvn clean package
```

## Getting started

### 1. Register the manager in your plugin's `onEnable`

```java
@Override
public void onEnable() {
    CommandManager manager = new CommandManager(this);
    manager.register(new TeleportCommand());
    manager.register(new EconomyCommand());
}
```

### 2. Declare each command in `plugin.yml`

```yaml
commands:
  tp:
    description: Teleport to a player
  eco:
    description: Manage economy
```

## Simple command (no subcommands)

Put `@Command` on the **class** for both simple commands and subcommand-based commands.

```java
@Command(name = "tp", cooldown = 3)
@PlayerOnly
@HasPermission("myplugin.tp")
public class TeleportCommand extends CommandBase {

    @DefaultCommand
    public void teleport(CommandSender sender, String[] args) {

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /tp <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return;
        }

        ((Player) sender).teleport(target);
    }
}
```

## Command with subcommands

Put `@Command` on the **class**. Each method inside becomes a subcommand via `@SubCommand`. Class-level `@HasPermission` and `@PlayerOnly` apply to the entire root command.

```java
@Command(name = "eco")
@HasPermission("myplugin.eco")
public class EconomyCommand extends CommandBase {

    @SubCommand(value = "give", minArgs = 2, usage = "/eco give <player> <amount>", cooldown = 10)
    @HasPermission("myplugin.eco.give")
    public void give(CommandSender sender, String[] args) {
        // minArgs already validated by the manager before this runs
        sender.sendMessage("§a" + args[1] + " coins given to " + args[0]);
    }

    @Completer("give")
    public List<String> giveCompleter(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return List.of("10", "50", "100", "1000");
        }
        return List.of();
    }

    @SubCommand("take")
    @HasPermission("myplugin.eco.take")
    @ConsoleOnly
    public void take(CommandSender sender, String[] args) {
        sender.sendMessage("§cCoins taken.");
    }
}
```

Running `/eco` with no arguments, or an unknown subcommand, triggers `onInvalidUsage`.

## Annotation reference

### `@Command`

| Parameter | Default | Description |
|---|---|---|
| `name` | — (required) | Command name, must match `plugin.yml` |
| `cooldown` | `0` | Cooldown in seconds (players only) |

### `@SubCommand`

| Parameter | Default | Description |
|---|---|---|
| `value` | — (required) | Subcommand name (e.g. `"give"`) |
| `usage` | `""` | Usage string passed to `onInvalidUsage` when `minArgs` isn't met |
| `minArgs` | `0` | Minimum number of arguments required (checked automatically) |
| `cooldown` | `0` | Cooldown in seconds for this subcommand |

### `@Completer`

| Parameter | Description |
|---|---|
| `value` | Name of the command or subcommand this completer provides suggestions for |

Completer methods must have the signature `List<String> method(CommandSender sender, String[] args)`.

### `@DefaultCommand`

| Parameter | Default | Description |
|-|-|-|
| - | - | Marks the method as the main executor for a simple command |

Used for simple commands without subcommands.

Example:

```java
@Command(name = "spawn")
public class SpawnCommand extends CommandBase {

    @DefaultCommand
    public void execute(CommandSender sender, String[] args) {

    }
}
```

The command method will be executed when the root command is called.

### Access & permission annotations

These can be placed on both classes and methods.
Class-level restrictions apply to the entire command.
Method-level restrictions apply to the selected command handler or subcommand.

| Annotation | Target | Description |
|---|---|---|
| `@HasPermission("...")` | Method / Class | Requires the sender to have the given permission node. Repeatable — all specified permissions must be held. |
| `@PlayerOnly` | Method / Class | Restricts the command to players only. Console senders are rejected with `onPlayerOnly`. |
| `@ConsoleOnly` | Method / Class | Restricts the command to console only. Players are rejected with `onConsoleOnly`. |
| `@OpOnly` | Method / Class | Restricts the command to server operators only. Non-ops are rejected with `onNoPermission`. |

For subcommand-style commands (class-level `@Command`), `@HasPermission` and `@PlayerOnly` can be placed on the class itself to gate the entire command before any subcommand logic runs.

## Customizing messages

Override any of these in your command class to change the default behavior:

```java
public class EconomyCommand extends CommandBase {

    @Override
    public void onNoPermission(CommandSender sender) {
        sender.sendMessage("§4You don't have permission to use this command.");
    }

    @Override
    public void onInvalidUsage(CommandSender sender, String usage) {
        sender.sendMessage("§cUsage: " + usage);
    }

    @Override
    public void onPlayerOnly(CommandSender sender) {
        sender.sendMessage("§cThis command is for players only.");
    }

    @Override
    public void onConsoleOnly(CommandSender sender) {
        sender.sendMessage("§cThis command is for console only.");
    }

    @Override
    public void onCooldown(CommandSender sender, long secondsLeft) {
        sender.sendMessage("§6Wait " + secondsLeft + "s before using this again.");
    }
}
```

If you don't override them, sensible defaults from `CommandBase` are used automatically.

## How it works

- `@Command` is placed on command classes.
- `@DefaultCommand` marks the executor for simple commands.
- `@SubCommand` creates child commands under a root command.
- Class-level annotations are inherited by all handlers inside the command.
- Method-level annotations override or extend handler restrictions.
- Cooldowns are tracked per player UUID and command key.
- Cooldowns are not persisted and reset after server restart.
- Console senders are ignored for cooldown checks.

A command class can either contain:
- One `@DefaultCommand` method for a simple command
- One or more `@SubCommand` methods for grouped commands