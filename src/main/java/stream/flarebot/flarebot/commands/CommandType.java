package stream.flarebot.flarebot.commands;

import stream.flarebot.flarebot.FlareBot;

import java.util.Arrays;
import java.util.Set;

public enum CommandType {

    GENERAL(true),
    MODERATION(true),
    MUSIC(true),
    USEFUL(false),
    CURRENCY(false),
    RANDOM(false),
    INFORMATIONAL(false),
    SECRET(false, true);

    private static final CommandType[] values = values();
    private static final CommandType[] defaultTypes = fetchDefaultTypes();
    
    // If it shows up in help
    private boolean defaultType;
    // If it is restricted to staff
    private boolean admin = false;

    CommandType(boolean defaultType) {
        this.defaultType = defaultType;
    }
    
    CommandType(boolean defaultType, boolean admin) {
        this.defaultType = defaultType;
        this.admin = admin;
    }

    public String toString() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
    
    public boolean isAdmin() {
        return admin;
    }

    public static CommandType[] getTypes() {
        return defaultTypes;
    }
    
    public static CommandType[] fetchDefaultTypes() {
        return Arrays.stream(values).filter(type -> type.defaultType).toArray(CommandType[]::new);
    }

    public Set<Command> getCommands() {
        return FlareBot.getCommandManager().getCommandsByType(this);
    }
}
