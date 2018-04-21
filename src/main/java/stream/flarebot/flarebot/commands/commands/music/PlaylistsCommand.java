package stream.flarebot.flarebot.commands.commands.music;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import stream.flarebot.flarebot.commands.*;
import stream.flarebot.flarebot.database.DatabaseManager;
import stream.flarebot.flarebot.objects.GuildWrapper;
import stream.flarebot.flarebot.permissions.Permission;
import stream.flarebot.flarebot.util.MessageUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PlaylistsCommand implements Command {

    @Override
    public void onCommand(User sender, GuildWrapper guild, TextChannel channel, Message message, String[] args, Member member) {
        channel.sendTyping().complete();
        DatabaseManager.run(connection -> {
            PreparedStatement select = connection.prepareStatement("SELECT playlist_name FROM playlists WHERE guild_id=?");
            select.setLong(1, channel.getGuild().getIdLong());
            ResultSet resultSet = select.executeQuery();
            List<String> songs = new ArrayList<>();

            while (resultSet.next() && songs.size() <= 25) {
                songs.add(resultSet.getString("playlist_name"));
            }

            String playlists = songs.stream().collect(Collectors.joining("\n"));

            EmbedBuilder builder = MessageUtils.getEmbed(sender);
            builder.addField("Playlists", playlists.isEmpty() ? "**No playlists!**" : playlists, false);
            channel.sendMessage(builder.build()).queue();
        });
    }

    @Override
    public String getCommand() {
        return "playlists";
    }

    @Override
    public String getDescription() {
        return "Lists all playlists saved in the current guild";
    }

    @Override
    public String getUsage() {
        return "`{%}playlists` - Returns the playlists available.";
    }

    @Override
    public Permission getPermission() {
        return Permission.PLAYLISTS_COMMAND;
    }

    @Override
    public CommandType getType() {
        return CommandType.MUSIC;
    }
}
