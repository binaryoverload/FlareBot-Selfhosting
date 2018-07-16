package stream.flarebot.flarebot;

import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    public static char DEFAULT_PREFIX = '_';

    public static final Config INS;

    static {
        Config config;

        try {
            config = new Config("config");
            log.info("Loaded config!");
        } catch (final IOException e) {
            config = null;
            log.error("Could not load config file!", e);
            System.exit(1);
        }
        INS = config;
    }

    // Config
    @Nonnull
    private String presence;

    // Essential info
    @Nonnull
    private String token;
    @Nonnull
    private String youtubeApi;

    private boolean mixer;
    private boolean twitch;
    private boolean soundCloud;

    private String soundcloudApi;

    // DB
    @Nonnull
    private String databaseUsername;
    @Nonnull
    private String databasePassword;
    @Nonnull
    private String databaseName;
    @Nonnull
    private String databaseHost;
    @Nonnull
    private int databasePort;

    @Nullable
    private String redisHost;
    private int redisPort;
    @Nullable
    private String redisPassword;
    private int hikariPoolSize;

    // Hooks and test bot
    private String errorLogWebhook;
    private WebhookClient errorLogHook;
    private String importantLogWebhook;
    private WebhookClient importantLogHook;
    private String statusLogWebhook;
    private WebhookClient statusLogHook;
    private String hasteServer;

    private String userId;

    private ArrayList<Long> admins;

    // Err, not sure what to really name this.
    private int numShards;

    private List<LinkedHashMap<String, Object>> nodes;

    public Config(String fileName) throws FileNotFoundException {
        this(getConfig(fileName));
    }

    @SuppressWarnings("unchecked")
    public Config(File configFile) {
        try {
            Yaml yaml = new Yaml();

            String configStr = FileUtils.readFileToString(configFile, Charset.forName("UTF-8"));
            if (configStr.contains("\t")) {
                configStr = configStr.replace("\t", "  ");
                log.warn("{} contains a tab! Please look into replacing it with normal spaces!", configFile.getName());
            }

            Map<String, Object> config = (Map<String, Object>) yaml.load(configStr);

            presence = (String) config.getOrDefault("presence", DEFAULT_PREFIX + "help | " + DEFAULT_PREFIX + "invite");

            token = (String) config.getOrDefault("token", "");
            if (token.isEmpty()) {
                log.error("No bot token was specified! Please provide a token to start the bot.");
                System.exit(1);
            }

            youtubeApi = (String) config.getOrDefault("youtubeApi", "");
            if (youtubeApi.isEmpty()) {
                log.error("No YouTube API key was specified! Please provide a key to start the bot.");
                System.exit(1);
            }

            userId = (String) config.getOrDefault("userId", "");
            if (userId.isEmpty()) {
                log.error("No User Id was specified! Please provide a user id to start the bot.");
                System.exit(1);
            }

            mixer = (boolean) config.getOrDefault("mixer", false);
            twitch = (boolean) config.getOrDefault("twitch", false);
            soundCloud = (boolean) config.getOrDefault("soundCloud", false);

            if(soundCloud) {
                soundcloudApi = (String) config.getOrDefault("soundCloudClientId", "");
                if(soundcloudApi.isEmpty()) {
                    /*log.error("Sound cloud is enabled but no client id is present!");
                    System.exit(1);*/
                }
            }

            Object db = config.get("database");
            if (db instanceof Map) {
                Map<String, Object> details = (Map<String, Object>) db;

                if (details.containsKey("username") && details.containsKey("password")) {
                    databaseUsername = (String) details.get("username");
                    databasePassword = (String) details.get("password");
                } else {
                    log.error("You need to supply postgresql username and password!");
                    System.exit(1);
                }

                databaseHost = (String) details.getOrDefault("host", "localhost");
                try {
                    databasePort = (int) details.getOrDefault("port", 5432);
                } catch (NumberFormatException e) {
                    databasePort = 5432;
                }
                databaseName = (String) details.getOrDefault("name", "flarebot");

                log.info(String.format("Using Host: %s Port: %d Name: %s for PostegreSQL", databaseHost, databasePort, databaseName));
            } else {
                log.error("You need to supply postgresql database details!");
                System.exit(1);
            }

            redisHost = (String) config.getOrDefault("redisHost", null);
            redisPort = (int) config.getOrDefault("redisPort", 6379);
            redisPassword = (String) config.getOrDefault("redisPassword", null);

            if (redisHost == null || redisHost.isEmpty())
                log.warn("Redis details not provided! Modlog will not catch message edits and deletions!");

            hikariPoolSize = (int) config.getOrDefault("hikariPoolSize", 10);
            log.info("Hikari max pool size set to " + hikariPoolSize);


            errorLogWebhook = (String) config.getOrDefault("errorLogWebhook", "");
            importantLogWebhook = (String) config.getOrDefault("importantLogWebhook", "");
            statusLogWebhook = (String) config.getOrDefault("statusLogWebhook", "");
            hasteServer = (String) config.getOrDefault("hasteServer", null);

            nodes = (List<LinkedHashMap<String, Object>>) config.getOrDefault("nodes", null);

            Object admins = config.getOrDefault("admins", new ArrayList<Long>());
            if (admins instanceof ArrayList && !((ArrayList) admins).isEmpty()) {
                this.admins = new ArrayList<>();
                this.admins.addAll(((ArrayList<String>) admins).stream().map(Long::parseLong).collect(Collectors.toSet()));
            } else {
                log.error("No admins were specified!");
                System.exit(1);
            }

            numShards = (int) config.getOrDefault("numShards", -1);
        } catch (IOException e) {
            log.error("Unexpected error loading config file!", e);
            System.exit(2);
        } catch (YAMLException | ClassCastException e) {
            log.error("Could not parse the config file, this is likely due to it being malformed!" +
                    "Check the file on an online YAML validator.", e);
            System.exit(2);
        }
    }

    private static File getConfig(String fileName) throws FileNotFoundException {
        String path = "./" + fileName + ".yml";
        File file = new File(path);
        if (!file.exists() || file.isDirectory())
            throw new FileNotFoundException("Could not find " + path + " file!");
        return file;
    }

    public ArrayList<Long> getAdmins() {
        return admins;
    }

    @Nonnull
    public String getPresence() {
        return presence;
    }

    @Nonnull
    public String getToken() {
        return token;
    }

    @Nonnull
    public String getYoutubeApi() {
        return youtubeApi;
    }

    public boolean isMixerEnabled() {
        return  mixer;
    }

    public boolean isTwitchEnabled() {
        return  twitch;
    }

    public boolean isSoundCloudEnabled() {
        return  soundCloud;
    }

    @Nonnull
    public String getDatabaseUsername() {
        return databaseUsername;
    }

    @Nonnull
    public String getDatabasePassword() {
        return databasePassword;
    }

    @Nonnull
    public String getDatabaseHost() {
        return databaseHost;
    }

    @Nonnull
    public String getDatabaseName() {
        return databaseName;
    }

    public int getDatabasePort() {
        return databasePort;
    }

    @Nullable
    public String getRedisHost() {
        return redisHost;
    }

    public int getRedisPort() {
        return redisPort;
    }

    @Nullable
    public String getRedisPassword() {
        return redisPassword;
    }

    public int getHikariPoolSize() {
        return hikariPoolSize;
    }

    public String getErrorLogWebhook() {
        return errorLogWebhook;
    }

    public String getImportantLogWebhook() {
        return importantLogWebhook;
    }

    public WebhookClient getImportantLogWebhookClient() {
        if (importantLogWebhook.isEmpty())
            return null;
        if (importantLogHook == null)
            importantLogHook = new WebhookClientBuilder(importantLogWebhook).build();
        return importantLogHook;
    }

    public WebhookClient getErrorLogWebhookClient() {
        if (errorLogWebhook.isEmpty())
            return null;
        if (errorLogHook == null)
            errorLogHook = new WebhookClientBuilder(errorLogWebhook).build();
        return errorLogHook;
    }

    public WebhookClient getStatusLogWebhookClient() {
        if (statusLogWebhook.isEmpty())
            return null;
        if (statusLogHook == null)
            statusLogHook = new WebhookClientBuilder(statusLogWebhook).build();
        return statusLogHook;
    }

    public String getHasteServer() {
        return hasteServer;
    }

    public int getNumShards() {
        return numShards;
    }

    public List<LinkedHashMap<String, Object>> getNodes() {
        return nodes;
    }

    public String getUserId() {
        return userId;
    }

    /*public String getSouldcloudApi() {
        return soundcloudApi;
    }*/
}
