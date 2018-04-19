package stream.flarebot.flarebot;

import com.arsenarsen.lavaplayerbridge.PlayerManager;
import com.arsenarsen.lavaplayerbridge.libraries.LibraryFactory;
import com.arsenarsen.lavaplayerbridge.libraries.UnknownBindingException;
import com.arsenarsen.lavaplayerbridge.utils.JDAMultiShard;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.SelfUser;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.utils.SessionControllerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stream.flarebot.flarebot.audio.PlayerListener;
import stream.flarebot.flarebot.music.QueueListener;
import stream.flarebot.flarebot.scheduler.Scheduler;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.IntFunction;

public class Client {

    private final Logger logger = LoggerFactory.getLogger(Client.class);

    private static Client instance;

    private PlayerManager musicManager;

    private DefaultShardManagerBuilder builder;
    private ShardManager shardManager;

    private Set<EventListener> listeners = ConcurrentHashMap.newKeySet();

    // Command - reason
    private Map<String, String> disabledCommands = new ConcurrentHashMap<>();

    Client() {
        instance = this;

        builder = new DefaultShardManagerBuilder()
                .setToken(Config.INS.getToken())
                .setShardsTotal(Config.INS.getNumShards())
                .setAutoReconnect(true)
                .setBulkDeleteSplittingEnabled(false)
                .setContextEnabled(true)
                .setSessionController(new SessionControllerAdapter());

        setupMusic();
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
        registerListener(new Events());
        setGame();

        logger.info("FlareBot started!");
    }

    protected void stop() {
        for (ScheduledFuture<?> scheduledFuture : Scheduler.getTasks().values())
            scheduledFuture.cancel(false);
        shardManager.removeEventListener(listeners.toArray(new Object[]{}));
        shardManager.shutdown();
    }

    private void setupMusic() {
        try {
            musicManager =
                    PlayerManager.getPlayerManager(LibraryFactory.getLibrary(new JDAMultiShard(Getters.getShardManager())));
        } catch (UnknownBindingException e) {
            logger.error("Failed to initialize musicManager", e);
        }
        musicManager.getPlayerCreateHooks()
                .register(player -> player.getQueueHookManager().register(new QueueListener()));

        musicManager.getPlayerCreateHooks().register(player -> player.addEventListener(new PlayerListener(player)));
    }

    public void registerListener(EventListener listener) {
        this.listeners.add(listener);
        shardManager.addEventListener(listener);
        logger.debug("Added listener '" + listener.getClass().getSimpleName() + "'");
    }

    private void setGame() {
        setGame(shard -> Game.playing(Config.INS.getPresence()
                .replace("{shard.id}", String.valueOf(shard))
                .replace("{shard.total}", String.valueOf(shardManager.getShardsTotal()))
        ));
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

    public PlayerManager getMusicManager() {
        return musicManager;
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

}
