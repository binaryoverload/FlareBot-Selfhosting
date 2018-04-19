package stream.flarebot.flarebot;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.requests.ErrorResponse;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.webhook.WebhookClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stream.flarebot.flarebot.commands.*;
import stream.flarebot.flarebot.database.DatabaseManager;
import stream.flarebot.flarebot.objects.PlayerCache;
import stream.flarebot.flarebot.scheduler.FlareBotTask;
import stream.flarebot.flarebot.scheduler.FutureAction;
import stream.flarebot.flarebot.tasks.VoiceChannelCleanup;
import stream.flarebot.flarebot.util.MessageUtils;
import stream.flarebot.flarebot.util.ShardUtils;
import stream.flarebot.flarebot.util.general.GeneralUtils;

import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FlareBot {

    public static final Logger LOGGER;
    public static final Gson GSON = new GsonBuilder().create();
    public static final AtomicBoolean EXITING = new AtomicBoolean(false);
    public static final AtomicBoolean UPDATING = new AtomicBoolean(false);
    public static final AtomicBoolean NOVOICE_UPDATING = new AtomicBoolean(false);
    private static final Map<String, Logger> LOGGERS;
    private static FlareBot instance;
    private static String version = null;

    private Client client;

    static {
        handleLogArchive("latest.log");
        handleLogArchive("debug-latest.log");
        LOGGERS = new ConcurrentHashMap<>();
        LOGGER = getLog(FlareBot.class.getName());
    }

    private Map<String, PlayerCache> playerCache = new ConcurrentHashMap<>();

    private long startTime;
    private Set<FutureAction> futureActions = Sets.newConcurrentHashSet();
    private Events events;
    private CommandManager commandManager;

    public static void main(String[] args) {
        (instance = new FlareBot()).init();
    }

    private void init() {
        instance = this;

        Thread.setDefaultUncaughtExceptionHandler(((t, e) -> LOGGER.error("Uncaught exception in thread " + t, e)));
        Thread.currentThread()
                .setUncaughtExceptionHandler(((t, e) -> LOGGER.error("Uncaught exception in thread " + t, e)));

        DatabaseManager.init();
        new DataHandler().init();

        client = new Client();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> client.stop()));

        LOGGER.info("Finished init, starting JDA");
        run();
    }


    // Disabled for now.
    // TODO: Make sure the API has a way to handle this and also update that page.
    public static void reportError(TextChannel channel, String s, Exception e) {
        JsonObject message = new JsonObject();
        message.addProperty("message", s);
        message.addProperty("exception", GeneralUtils.getStackTrace(e));
        MessageUtils.sendErrorMessage(s, channel);
        //String id = instance.postToApi("postReport", "error", message);
        //MessageUtils.sendErrorMessage(s + "\nThe error has been reported! You can follow the report on the website, https://flarebot.stream/report?id=" + id, channel);
    }

    private static Logger getLog(String name) {
        return LOGGERS.computeIfAbsent(name, LoggerFactory::getLogger);
    }

    public static Logger getLog(Class<?> clazz) {
        return getLog(clazz.getName());
    }

    private static void handleLogArchive(String file) {
        try {
            byte[] buffer = new byte[1024];
            String time = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss").format(new Date());

            File dir = new File("logs");
            if (!dir.exists() && !dir.mkdir())
                LOGGER.error("Failed to create directory for latest log!");
            File f = new File(dir, file + "@" + time + ".zip");
            File latestLog = new File(file);

            FileOutputStream fos = new FileOutputStream(f);
            ZipOutputStream zos = new ZipOutputStream(fos);
            ZipEntry entry = new ZipEntry(latestLog.getName());
            zos.putNextEntry(entry);
            FileInputStream in = new FileInputStream(latestLog);

            int len;
            while ((len = in.read(buffer)) > 0)
                zos.write(buffer, 0, len);

            in.close();
            zos.closeEntry();
            zos.close();
            fos.close();

            if (!latestLog.delete()) {
                throw new IllegalStateException("Failed to delete the old log file!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getVersion() {
        if (version == null) {
            Properties p = new Properties();
            try {
                p.load(FlareBot.class.getClassLoader().getResourceAsStream("version.properties"));
            } catch (IOException e) {
                LOGGER.error("There was an error trying to load the version!", e);
                return null;
            }
            version = (String) p.get("version");
        }
        return version;
    }

    public static String getInvite() {
        return String.format("https://discordapp.com/oauth2/authorize?client_id=%s&scope=bot&permissions=%s",
                Getters.getSelfUser().getId(), Permission.getRaw(Permission.MESSAGE_WRITE, Permission.MESSAGE_READ,
                        Permission.MANAGE_ROLES, Permission.MESSAGE_MANAGE, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK,
                        Permission.VOICE_MOVE_OTHERS, Permission.KICK_MEMBERS, Permission.BAN_MEMBERS,
                        Permission.MANAGE_CHANNEL, Permission.MESSAGE_EMBED_LINKS, Permission.NICKNAME_CHANGE,
                        Permission.MANAGE_PERMISSIONS, Permission.VIEW_AUDIT_LOGS, Permission.MESSAGE_HISTORY,
                        Permission.MANAGE_WEBHOOKS, Permission.MANAGE_SERVER, Permission.MESSAGE_ADD_REACTION));
    }

    public static FlareBot instance() {
        return instance;
    }

    public Events getEvents() {
        return events;
    }

    protected void run() {
        try {
            client.start();
            startTime = System.currentTimeMillis();
        } catch (LoginException e) {
            LOGGER.error("Looks like your token is invalid", e);
            System.exit(1);
        }

        commandManager = new CommandManager();
        client.registerListener((events = new Events()));

        RestAction.DEFAULT_FAILURE = GeneralUtils.getFailedRestActionHandler("Failed RestAction - {}",
                ErrorResponse.UNKNOWN_MESSAGE);

        GeneralUtils.methodErrorHandler(LOGGER, "Starting tasks!",
                "Started all tasks, run complete!", "Failed to start all tasks!",
                this::runTasks);
    }

    public void quit() {
        Client.instance().stop();
        System.exit(0);
    }

    public String getUptime() {
        long totalSeconds = (System.currentTimeMillis() - startTime) / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = (totalSeconds / 3600);
        return (hours < 10 ? "0" + hours : hours) + "h " + (minutes < 10 ? "0" + minutes : minutes) + "m " + (seconds < 10 ? "0" + seconds : seconds) + "s";
    }

    public PlayerCache getPlayerCache(String userId) {
        this.playerCache.computeIfAbsent(userId, k -> new PlayerCache(userId, null, null, null));
        return this.playerCache.get(userId);
    }

    public void runTasks() {

        new FlareBotTask("spam" + System.currentTimeMillis()) {
            @Override
            public void run() {
                events.getSpamMap().clear();
            }
        }.repeat(TimeUnit.SECONDS.toMillis(3), TimeUnit.SECONDS.toMillis(3));

        new FlareBotTask("DeadShard-Checker") {
            @Override
            public void run() {
                if (Config.INS.getImportantWebhook() == null) {
                    LOGGER.warn("No webhook for the important-log channel! Due to this the dead shard checker has been disabled!");
                    cancel();
                    return;
                }
                if (Getters.getShards().size() == 1) {
                    LOGGER.warn("Single sharded bot, the DeadShard-Checker has been disabled!");
                    cancel();
                    return;
                }
                // 10 mins without an event... this son bitch is dead.
                if (Getters.getShards().stream().anyMatch(shard -> ShardUtils.isDead(shard, TimeUnit.MINUTES.toMillis(10)))) {
                    Getters.getShards().stream().filter(shard -> ShardUtils.isDead(shard, TimeUnit.MINUTES.toMillis(10)))
                            .forEach(shard -> {
                                Config.INS.getImportantWebhook().send("Restarting " + ShardUtils.getShardId(shard)
                                        + " as it seems to be dead.");
                                Client.instance().getShardManager().restart(ShardUtils.getShardId(shard));
                            });
                }

                Set<Integer> deadShards =
                        Getters.getShards().stream().filter(ShardUtils::isDead).map(ShardUtils::getShardId)
                                .collect(Collectors.toSet());

                if (!deadShards.isEmpty()) {
                    Config.INS.getImportantWebhook().send("Found " + deadShards.size() + " possibly dead shards! Shards: " +
                            deadShards.toString());
                }
            }
        }.repeat(TimeUnit.MINUTES.toMillis(10), TimeUnit.MINUTES.toMillis(5));

        new VoiceChannelCleanup("VoiceChannelCleanup");
    }

    public static CommandManager getCommandManager() {
        return FlareBot.instance().commandManager;
    }

    public Set<FutureAction> getFutureActions() {
        return futureActions;
    }
}
