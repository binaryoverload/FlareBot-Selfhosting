package stream.flarebot.flarebot;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import org.joda.time.DateTime;
import stream.flarebot.flarebot.commands.*;
import stream.flarebot.flarebot.database.DatabaseManager;
import stream.flarebot.flarebot.objects.GuildWrapper;
import stream.flarebot.flarebot.scheduler.FutureAction;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DataHandler {

    public static final char[] ALLOWED_SPECIAL_CHARACTERS = {'$', '_', ' ', '&', '%', '£', '!', '*', '@', '#', ':'};
    public static final Pattern ALLOWED_CHARS_REGEX = Pattern.compile("[\\w" + new String(ALLOWED_SPECIAL_CHARACTERS) + "\\p{Ll}\\p{Lu}\\p{Lt}\\p{Lm}\\p{Lo}]{3,32}");

    public static final Gson gson = new GsonBuilder().create();

    private static final Cache<Long, GuildWrapper> guilds = Caffeine.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .recordStats()
            .removalListener(new GuildSaveListener())
            .build();

    public void init() {
        DatabaseManager.run(conn -> {
            conn.prepareCall("CREATE TABLE IF NOT EXISTS guilds (guild_id BIGINT, guild_data JSON, PRIMARY KEY (guild_id))").execute();
            conn.prepareCall("CREATE TABLE IF NOT EXISTS playlists (playlist_name TEXT, guild_id BIGINT, owner BIGINT, songs TEXT, PRIMARY KEY (playlist_name, guild_id))").execute();
            conn.prepareCall("CREATE TABLE IF NOT EXISTS future_tasks (guild_id BIGINT, channel_id BIGINT, responsible BIGINT, content TEXT, expires_at TIMESTAMP, created_at TIMESTAMP, action TEXT)").execute();
        });
        loadFutureTasks();
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

            if (set.isBeforeFirst()) {
                set.next();
                data.set(gson.fromJson(set.getString("guid_data"), GuildWrapper.class));
            } else {
                data.set(new GuildWrapper(guildId));
            }
        });

        guilds.put(guildId, data.get());
        return data.get();
    }

    public static void savePlaylist(Command command, TextChannel channel, String ownerId, boolean overwriteAllowed, String name, List<String> songs) {
        DatabaseManager.run(connection -> {
            PreparedStatement savePlaylistStatement = connection.prepareStatement("SELECT * FROM playlists " +
                    "WHERE playlist_name = ? AND guild_id = ?");

            savePlaylistStatement.setString(1, name);
            savePlaylistStatement.setLong(2, channel.getGuild().getIdLong());
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
                        " (playlist_name, guild_id, owner, songs) VALUES (?, ?, ?, ?) ON CONFLICT (playlist_name, guild_id) DO UPDATE " +
                    "SET songs = EXCLUDED.songs, owner = EXCLUDED.owner");

            insertPlaylistStatement.setString(1, name);
            insertPlaylistStatement.setLong(2, channel.getGuild().getIdLong());
            insertPlaylistStatement.setLong(3, Long.parseLong(ownerId));
            insertPlaylistStatement.setString(4, songs.toString());
            insertPlaylistStatement.executeUpdate();
            channel.sendMessage(MessageUtils.getEmbed(Getters.getUserById(ownerId))
                    .setDescription("Successfully saved the playlist: " + MessageUtils.escapeMarkdown(name)).build()).queue();
        });
    }

    public static ArrayList<String> loadPlaylist(TextChannel channel, User sender, String name) {
        AtomicReference<ArrayList<String>> list = new AtomicReference<>(new ArrayList<>());
        DatabaseManager.run(connection -> {
            PreparedStatement savePlaylistStatement = connection.prepareStatement("SELECT * FROM playlists " +
                    "WHERE playlist_name = ? AND guild_id = ?");

            savePlaylistStatement.setString(1, name);
            savePlaylistStatement.setLong(2, channel.getGuild().getIdLong());
            ResultSet set = savePlaylistStatement.executeQuery();
            if (set.isBeforeFirst()) {
                set.next();
                String songs = set.getString("songs");
                songs = songs.substring(1, songs.length() - 1);
                list.get().addAll(Arrays.asList(songs.split(", ")));
            } else
                channel.sendMessage(MessageUtils.getEmbed(sender)
                        .setDescription("That playlist does not exist!").build()).queue();
        });
        return list.get();
    }

    private void loadFutureTasks() {
        AtomicReference<Integer> loaded = new AtomicReference<>(0);
        DatabaseManager.run(connection -> {
            ResultSet set = connection.prepareCall("SELECT * FROM future_tasks").executeQuery();
            while (set.next()) {
                FutureAction fa =
                        new FutureAction(set.getLong("guild_id"), set.getLong("channel_id"),
                                set.getLong("responsible"),
                                set.getLong("target"),
                                set.getString("content"),
                                new DateTime(set.getTimestamp("expires_at")),
                                new DateTime(set.getTimestamp("created_at")),
                                FutureAction.Action.valueOf(set.getString("action").toUpperCase())
                        );

                try {
                    if (new DateTime().isAfter(fa.getExpires()))
                        fa.execute();
                    else {
                        fa.queue();
                        loaded.getAndUpdate(i -> i++);
                    }
                } catch (NullPointerException e) {
                    FlareBot.LOGGER.error("Failed to execute/queue future task"
                            + "\nAction: " + fa.getAction() + "\nResponsible: " + fa.getResponsible()
                            + "\nTarget: " + fa.getTarget() + "\nContent: " + fa.getContent(), e);
                }
            }
        });

        FlareBot.LOGGER.info("Loaded " + loaded.get() + " future tasks");
    }

    public static Cache<Long, GuildWrapper> getGuilds() {
        return guilds;
    }
}
