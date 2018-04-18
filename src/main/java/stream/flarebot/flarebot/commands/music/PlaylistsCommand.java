package stream.flarebot.flarebot.commands.music;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import stream.flarebot.flarebot.commands.Command;
import stream.flarebot.flarebot.commands.CommandType;
import stream.flarebot.flarebot.objects.GuildWrapper;
import stream.flarebot.flarebot.permissions.Permission;
import stream.flarebot.flarebot.util.MessageUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlaylistsCommand implements Command {

    @Override
    public void onCommand(User sender, GuildWrapper guild, TextChannel channel, Message message, String[] args, Member member) {
        if (args.length >= 1) {

            channel.sendTyping().complete();
            CassandraController.runTask(session -> {
                ResultSet set = session.execute("SELECT playlist_name, scope FROM flarebot.playlist WHERE " +
                        "scope = 'global' ALLOW FILTERING");
                ArrayList<Row> tmp = new ArrayList<>(set.all());
                set = session.execute("SELECT playlist_name, scope FROM flarebot.playlist WHERE " +
                        "guild_id = '" + channel.getGuild().getId() + "' AND scope = 'local' ALLOW FILTERING");
                tmp.addAll(set.all());
                StringBuilder sb = new StringBuilder();
                StringBuilder globalSb = new StringBuilder();
                List<String> songs = new ArrayList<>();
                int i = 1;
                boolean loopingGlobal = true;

                Iterator<Row> rows = tmp.iterator();
                while (rows.hasNext() && songs.size() < 25) {
                    Row row = rows.next();
                    String toAppend;
                    if (row.getString("scope").equalsIgnoreCase("global")) {
                        toAppend = String.format("%s. %s\n", i++,
                                MessageUtils.escapeMarkdown(row.getString("playlist_name")));
                        globalSb.append(toAppend);
                    } else {
                        if (loopingGlobal) {
                            loopingGlobal = false;
                            i = 1;
                        }
                        toAppend = String.format("%s. %s\n", i++,
                                MessageUtils.escapeMarkdown(row.getString("playlist_name")));
                        if (sb.length() + toAppend.length() > 1024) {
                            songs.add(sb.toString());
                            sb = new StringBuilder();
                        }
                        sb.append(toAppend);
                    }
                }
                songs.add(sb.toString());
                EmbedBuilder builder = MessageUtils.getEmbed(sender);
                i = 1;
                builder.addField("Global Playlists", (globalSb.toString()
                        .isEmpty() ? "No global playlists!" : globalSb
                        .toString()), false);
                for (String s : songs) {
                    builder.addField("Page " + i++, s.isEmpty() ? "**No playlists!**" : s, false);
                }
                channel.sendMessage(builder.build()).queue();
            });
        }
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
        // Admin command:
        // {%}playlists mark <global/local> <playlist>
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
