package stream.flarebot.flarebot.commands.music;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import stream.flarebot.flarebot.commands.*;
import stream.flarebot.flarebot.database.DatabaseManager;
import stream.flarebot.flarebot.objects.GuildWrapper;
import stream.flarebot.flarebot.permissions.Permission;
import stream.flarebot.flarebot.util.MessageUtils;

import java.awt.Color;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DeleteCommand implements Command {

    @Override
    public void onCommand(User sender, GuildWrapper guild, TextChannel channel, Message message, String[] args, Member member) {
        if (args.length == 0) {
            MessageUtils.sendUsage(this, channel, sender, args);
            return;
        }
        channel.sendTyping().complete();
        String name = MessageUtils.getMessage(args, 0);
        DatabaseManager.run(connection -> {
            PreparedStatement select = connection.prepareStatement("SELECT playlist_name FROM playlists WHERE playlist_name = ? AND guild_id = ?");
            select.setString(1, name);
            select.setLong(2, channel.getGuild().getIdLong());
            ResultSet set = select.executeQuery();
            if (!set.isBeforeFirst()) {
                PreparedStatement delete = connection.prepareStatement("DELETE FROM playlists WHERE playlist_name = ? AND guild_id = ?");
                delete.setString(1, name);
                delete.setLong(2, channel.getGuild().getIdLong());
                delete.execute();
                channel.sendMessage(MessageUtils.getEmbed(sender)
                        .setDescription(String
                                .format("Removed the playlist '%s'", name)).setColor(Color.green)
                        .build()).queue();
            } else {
                MessageUtils.sendErrorMessage(String.format("The playlist '%s' does not exist!", name), channel, sender);
            }
        });
    }

    @Override
    public String getCommand() {
        return "delete";
    }

    @Override
    public String getDescription() {
        return "Deletes a playlist.";
    }

    @Override
    public String getUsage() {
        return "`{%}delete <playlist>` - Deletes a playlist.";
    }

    @Override
    public Permission getPermission() {
        return Permission.DELETE_COMMAND;
    }

    @Override
    public CommandType getType() {
        return CommandType.MUSIC;
    }

}
