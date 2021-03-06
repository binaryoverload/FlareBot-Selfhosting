package stream.flarebot.flarebot.commands;

import net.dv8tion.jda.core.entities.User;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stream.flarebot.flarebot.permissions.PerGuildPermissions;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class CommandManager {

    private static CommandManager instance = null;

    private final List<Command> commands = new CopyOnWriteArrayList<>();
    private final Logger logger = LoggerFactory.getLogger("Command Manager");

    public CommandManager() {
        instance = this;

        long start = System.currentTimeMillis();
        try {
            for (Class<?> c : new Reflections("stream.flarebot.flarebot.commands.commands").getSubTypesOf(Command.class)) {
                commands.add((Command) c.newInstance());
            }
            logger.info("Loaded {} commands in {}ms.", commands.size(), (System.currentTimeMillis() - start));
        } catch (IllegalAccessException | InstantiationException e) {
            logger.error("Could not load commands!", e);
            System.exit(1);
        }
    }

    // https://bots.are-pretty.sexy/214501.png
    // New way to process commands, this way has been proven to be quicker overall.
    public Command getCommand(String s, User user) {
        if (PerGuildPermissions.isAdmin(user)) {
            for (Command cmd : getCommandsByType(CommandType.SECRET)) {
                if (cmd.getCommand().equalsIgnoreCase(s))
                    return cmd;
                for (String alias : cmd.getAliases())
                    if (alias.equalsIgnoreCase(s)) return cmd;
            }
        }
        for (Command cmd : getCommands()) {
            if (cmd.getType() == CommandType.SECRET) continue;
            if (cmd.getCommand().equalsIgnoreCase(s))
                return cmd;
            for (String alias : cmd.getAliases())
                if (alias.equalsIgnoreCase(s)) return cmd;
        }
        return null;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public Set<Command> getCommandsByType(CommandType type) {
        return commands.stream().filter(command -> command.getType() == type).collect(Collectors.toSet());
    }

    public static CommandManager getInstance() {
        return instance;
    }
}
