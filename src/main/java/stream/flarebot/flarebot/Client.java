package stream.flarebot.flarebot;

import com.arsenarsen.lavaplayerbridge.PlayerManager;
import com.arsenarsen.lavaplayerbridge.libraries.LibraryFactory;
import com.arsenarsen.lavaplayerbridge.libraries.UnknownBindingException;
import com.arsenarsen.lavaplayerbridge.utils.JDAMultiShard;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lavalink.client.io.Lavalink;
import lavalink.client.io.Link;
import lavalink.client.player.IPlayer;
import lavalink.client.player.LavaplayerPlayerWrapper;
import lavalink.client.player.event.AudioEventAdapterWrapped;
import lavalink.client.player.event.PlayerEvent;
import lavalink.client.player.event.TrackEndEvent;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.SelfUser;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.utils.SessionControllerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stream.flarebot.flarebot.database.RedisController;
import stream.flarebot.flarebot.mod.nino.NINOListener;
import stream.flarebot.flarebot.music.PlayerListener;
import stream.flarebot.flarebot.scheduler.Scheduler;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.IntFunction;

public class Client {

    private final Logger logger = LoggerFactory.getLogger(Client.class);

    private static Client instance;

    private DefaultShardManagerBuilder builder;
    private ShardManager shardManager;

    private Set<EventListener> listeners = ConcurrentHashMap.newKeySet();

    // Command - reason
    private Map<String, String> disabledCommands = new ConcurrentHashMap<>();
    private Events events;

    private PlayerManager musicManager;

    private Map<String, IPlayer> players = new HashMap<>();
    private Map<String, List<AudioTrack>> tracks = new HashMap<>();

    private Lavalink lavalink;

    private boolean lavalinkEnabled;


    Client() {
        instance = this;

        builder = new DefaultShardManagerBuilder()
                .setToken(Config.INS.getToken())
                .setShardsTotal(Config.INS.getNumShards())
                .setAutoReconnect(true)
                .setBulkDeleteSplittingEnabled(false)
                .setContextEnabled(true)
                .addEventListeners(setupMusic())
                .setSessionController(new SessionControllerAdapter());
    }

    protected void start() throws LoginException {
        shardManager = builder.build();

        while (isNotReady()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                logger.error("Some bad bad stuff happened while waiting for " +
                        "FlareBot to be ready.", e);
            }
        }
        registerListener((events = new Events()));
        registerListener(new ModlogEvents());
        registerListener(new NINOListener());
        setGame();

        new RedisController();

        logger.info("FlareBot started!");
    }

    protected void stop() {
        DataHandler.getGuilds().invalidateAll();
        for (ScheduledFuture<?> scheduledFuture : Scheduler.getTasks().values())
            scheduledFuture.cancel(false);
        shardManager.removeEventListener(listeners.toArray(new Object[]{}));
        shardManager.shutdown();
    }

    private Lavalink setupMusic() {
        lavalink = new Lavalink(Config.INS.getUserId(),
                Config.INS.getNumShards(),
                shardId -> Client.instance().getShardManager().getShardById(shardId));
        lavalinkEnabled = Config.INS.getNodes() != null && Config.INS.getNodes().size() > 0;

        if(lavalinkEnabled) {
            for (LinkedHashMap<String, Object> node : Config.INS.getNodes()) {
                try {
                    lavalink.addNode(new URI("http://" + node.get("address") + ":" + node.get("port")), String.valueOf(node.get("password")));
                } catch (URISyntaxException e) {
                    logger.error("Wrong Uri syntax " + node.get("address") + ":" + node.get("port"), e);
                }
            }
        } else {
            try {
                musicManager =
                        PlayerManager.getPlayerManager(LibraryFactory.getLibrary(new JDAMultiShard(Getters.getShardManager())));
            } catch (UnknownBindingException e) {
                logger.error("Error building lavaplayer music manager", e);
            }
        }

        return lavalink;
    }

    public void registerListener(EventListener listener) {
        this.listeners.add(listener);
        shardManager.addEventListener(listener);
        logger.debug("Added listener '" + listener.getClass().getSimpleName() + "'");
    }

    private void setGame() {
        setGame(shard -> Game.streaming(Config.INS.getPresence()
                .replace("{shard.id}", String.valueOf(shard))
                .replace("{shard.total}", String.valueOf(shardManager.getShardsTotal())), "https://www.twitch.tv/discordflarebot"))
        ;
    }

    private void setGame(IntFunction<Game> game) {
        shardManager.setGameProvider(game);
    }

    private boolean isNotReady() {
        for (JDA shard : shardManager.getShards())
            if (shard.getStatus() != JDA.Status.CONNECTED)
                return true;
        return false;
    }

    public ShardManager getShardManager() {
        return shardManager;
    }

    public IPlayer getPlayer(String guildId) {
        if(!players.containsKey(guildId)) {
            players.put(guildId, createPlayer(guildId));
        }
        return players.get(guildId);
    }

    private IPlayer createPlayer(String guildId) {
        IPlayer player = lavalinkEnabled
                ? lavalink.getLink(guildId).getPlayer()
                : new LavaplayerPlayerWrapper(musicManager.getPlayer(guildId).getPlayer());
        tracks.put(guildId, new ArrayList<>());
        player.addListener(new PlayerListener(guildId));
        return player;
    }

    public static Client instance() {
        return instance;
    }

    @Nonnull
    public SelfUser getSelfUser() {
        for (JDA jda : shardManager.getShardCache())
            if (jda.getStatus() == JDA.Status.CONNECTED || jda.getStatus() == JDA.Status.LOADING_SUBSYSTEMS)
                return jda.getSelfUser();
        throw new IllegalStateException("Tried to get SelfUSer with no shards connected!");
    }

    @Nonnull
    public JDA getJDA() {
        for (JDA jda : shardManager.getShardCache())
            if (jda.getStatus() == JDA.Status.CONNECTED || jda.getStatus() == JDA.Status.LOADING_SUBSYSTEMS)
                return jda;
        throw new IllegalStateException("Tried to get JDA with no shards connected!");
    }

    public boolean isCommandDisabled(String command) {
        return disabledCommands.containsKey(command);
    }

    public String getDisabledCommandReason(String command) {
        return this.disabledCommands.get(command);
    }

    public boolean toggleCommand(String command, String reason) {
        return disabledCommands.containsKey(command) ? disabledCommands.remove(command) != null :
                disabledCommands.put(command, reason) != null;
    }

    public Map<String, String> getDisabledCommands() {
        return disabledCommands;
    }

    public Events getEvents() {
        return events;
    }

    public Link getLink(String guildId) {
        return lavalink.getLink(guildId);
    }

    public List<AudioTrack> getTracks(String guildId) {
        if(!tracks.containsKey(guildId)) {
            tracks.put(guildId, new ArrayList<>());
        }
        return tracks.get(guildId);
    }
}
