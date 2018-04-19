package stream.flarebot.flarebot.commands;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import stream.flarebot.flarebot.DataHandler;
import stream.flarebot.flarebot.objects.GuildWrapper;
import stream.flarebot.flarebot.permissions.PerGuildPermissions;

import java.util.EnumSet;

public interface Command {

    void onCommand(User sender, GuildWrapper guild, TextChannel channel, Message message, String[] args, Member member);

    String getCommand();

    String getDescription();

    String getUsage();

    CommandType getType();

    default String getExtraInfo() {
        return null;
    }

    stream.flarebot.flarebot.permissions.Permission getPermission();

    default EnumSet<Permission> getDiscordPermission() {
        return getPermission().getDiscordPerm();
    }

    default String[] getAliases() {
        return new String[]{};
    }

    default PerGuildPermissions getPermissions(TextChannel chan) {
        return DataHandler.getGuild(chan.getGuild().getIdLong()).getPermissions();
    }

    default boolean isDefaultPermission() {
        return getPermission() != null && getPermission().isDefaultPerm() && !getType().isAdmin();
    }

    default boolean deleteMessage() {
        return true;
    }

    default boolean isBetaTesterCommand() {
        return false;
    }

    default char getPrefix(Guild guild) {
        return DataHandler.getGuild(guild.getIdLong()).getPrefix();
    }
}