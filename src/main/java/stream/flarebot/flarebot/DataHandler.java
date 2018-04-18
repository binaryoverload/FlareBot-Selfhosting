package stream.flarebot.flarebot;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import stream.flarebot.flarebot.commands.*;
import stream.flarebot.flarebot.database.DatabaseManager;
import stream.flarebot.flarebot.objects.GuildWrapper;
import stream.flarebot.flarebot.util.ConfirmUtil;
import stream.flarebot.flarebot.util.MessageUtils;
import stream.flarebot.flarebot.util.objects.RunnableWrapper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DataHandler {

    public static final Gson gson = new GsonBuilder().create();

    private static final Cache<Long, GuildWrapper> guilds = Caffeine.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .recordStats()
            .removalListener(new GuildSaveListener())
            .build();

    public void init() {
        DatabaseManager.run(conn -> {
            conn.prepareCall("CREATE TABLE IF NOT EXISTS guilds (guild_id BIGINT, guild_data JSON)").execute();
            conn.prepareCall("CREATE TABLE IF NOT EXISTS playlists (playlist_name TEXT, guild_id BIGINT, owner BIGINT, songs TEXT, PRIMARY KEY (playlist_name, guild_id))").execute();
            conn.prepareCall("CREATE TABLE IF NOT EXISTS future_tasks (guild_id BIGINT, channel_id BIGINT, responsible BIGINT, content TEXT, expires_at TIMESTAMP, created_at TIMESTAMP, action TEXT)").execute();
        });
    }

    public static void saveGuild(String guildId) {
        guilds.invalidate(guildId);
    }

    public static GuildWrapper getGuild(Long guildId) {
        String id = Long.toString(guildId);
        AtomicReference<GuildWrapper> data = new AtomicReference<>(guilds.getIfPresent(guildId));

        if (data.get() != null)
            return data.get();

        DatabaseManager.run(conn -> {
            PreparedStatement ps = conn.prepareStatement("SELECT guild_data FROM guilds WHERE guild_id = ?");
            ps.setLong(1, guildId);
            ResultSet set = ps.executeQuery();

            if (set.isFirst()) {
                data.set(gson.fromJson(set.getString("guid_data"), GuildWrapper.class));
            } else {
                data.set(new GuildWrapper(guildId));
            }
        });

        guilds.put(guildId, data.get());
        return data.get();
    }

    public void savePlaylist(Command command, TextChannel channel, String ownerId, boolean overwriteAllowed, String name, List<String> songs) {
        DatabaseManager.run(connection -> {
            PreparedStatement savePlaylistStatement = connection.prepareStatement("SELECT * FROM playlists " +
                    "WHERE playlist_name = ? AND guild_id = ?");

            savePlaylistStatement.setString(1, name);
            savePlaylistStatement.setString(2, channel.getGuild().getId());
            ResultSet set = savePlaylistStatement.executeQuery();
            if (set.isBeforeFirst()) {
                if (ConfirmUtil.checkExists(ownerId, command.getClass())) {
                    MessageUtils.sendWarningMessage("Overwriting playlist!", channel);
                } else if (!overwriteAllowed) {
                    MessageUtils.sendErrorMessage("That name is already taken! You need the `flarebot.queue.save.overwrite` permission to overwrite", channel);
                    return;
                } else {
                    MessageUtils.sendErrorMessage("That name is already taken! Do this again within 1 minute to overwrite!", channel);
                    ConfirmUtil.pushAction(ownerId, new RunnableWrapper(() -> {}, command.getClass()));
                    return;
                }
            }
            PreparedStatement insertPlaylistStatement = connection.prepareStatement("INSERT INTO playlists" +
                        " (playlist_name, guild_id, owner, songs) VALUES (?, ?, ?, ?)");

            insertPlaylistStatement.setString(1, name);
            insertPlaylistStatement.setString(2, channel.getGuild().getId());
            insertPlaylistStatement.setString(3, ownerId);
            insertPlaylistStatement.setString(4, songs.toString());
            channel.sendMessage(MessageUtils.getEmbed(Getters.getUserById(ownerId))
                    .setDescription("Successfully saved the playlist: " + MessageUtils.escapeMarkdown(name)).build()).queue();
        });
    }

    public ArrayList<String> loadPlaylist(TextChannel channel, User sender, String name) {
        AtomicReference<ArrayList<String>> list = new AtomicReference<>(new ArrayList<>());
        DatabaseManager.run(connection -> {
            PreparedStatement savePlaylistStatement = connection.prepareStatement("SELECT * FROM playlists " +
                    "WHERE playlist_name = ? AND guild_id = ?");

            savePlaylistStatement.setString(1, name);
            savePlaylistStatement.setString(2, channel.getGuild().getId());
            ResultSet set = savePlaylistStatement.executeQuery();
            if (!set.isBeforeFirst()) {
                String songs = set.getString("songs");
                songs = songs.substring(1, songs.length() - 1);
                list.get().addAll(Arrays.asList(songs.split(", ")));
            } else
                channel.sendMessage(MessageUtils.getEmbed(sender)
                        .setDescription("That playlist does not exist!").build()).queue();
        });
        return list.get();
    }


}
