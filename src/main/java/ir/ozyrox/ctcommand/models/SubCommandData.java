package ir.ozyrox.ctcommand.models;

import lombok.Value;

import java.lang.reflect.Method;
import java.util.Set;

@Value
public class SubCommandData {
    String value;

    Method method;
    Method completer;

    Set<String> permissions;

    boolean playerOnly;
    boolean consoleOnly;
    boolean opOnly;

    int cooldown;

    int minArgs;

    String usage;
}
