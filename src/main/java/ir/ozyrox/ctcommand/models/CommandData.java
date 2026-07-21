package ir.ozyrox.ctcommand.models;

import lombok.Value;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

@Value
public class CommandData {
    String name;

    Method method;
    Method completer;

    Set<String> permissions;

    boolean playerOnly;
    boolean consoleOnly;
    boolean opOnly;

    int cooldown;

    Map<String, SubCommandData> subCommands;
}
