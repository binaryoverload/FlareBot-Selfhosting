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
import java.util.Map;
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
    private String errorLogChannel;
    private WebhookClient errorLogHook;
    private String importantLogChannel;
    private WebhookClient importantLogHook;
    private String statusLogChannel;
    private WebhookClient statusLogHook;

    private ArrayList<Long> admins;
    private Long officialGuild;

    // Err, not sure what to really name this.
    private int numShards;

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

            Object db = config.get("database");
            if (db instanceof Map) {
                Map<String, String> details = (Map<String, String>) db;

                if (details.containsKey("username") && details.containsKey("password")) {
                    databaseUsername = details.get("username");
                    databasePassword = details.get("password");
                } else {
                    log.error("You need to supply postgresql username and password!");
                    System.exit(1);
                }

                databaseHost = details.getOrDefault("host", "localhost");
                try {
                    databasePort = Integer.parseInt(details.getOrDefault("port", "5432"));
                } catch (NumberFormatException e) {
                    databasePort = 5432;
                }
                databaseName = details.getOrDefault("name", "flarebot");

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


            errorLogChannel = (String) config.getOrDefault("errorLogChannel", "");
            importantLogChannel = (String) config.getOrDefault("importantLogChannel", "");
            statusLogChannel = (String) config.getOrDefault("statusLogChannel", "");

            Object admins = config.getOrDefault("admins", new ArrayList<Long>());
            if (admins instanceof ArrayList && !((ArrayList) admins).isEmpty()) {
                this.admins = new ArrayList<>();
                this.admins.addAll(((ArrayList<String>) admins).stream().map(Long::parseLong).collect(Collectors.toSet()));
            } else {
                log.error("No admins were specified!");
                System.exit(1);
            }

            String officialGuild = (String) config.getOrDefault("officialGuild", "");
            if (officialGuild.isEmpty()) {
                log.error("You need to provide an official server!");
                System.exit(1);
            } else {
                try {
                    this.officialGuild = Long.parseLong(officialGuild);
                } catch (NumberFormatException e) {
                    log.error("Official guild is not valid!");
                    System.exit(1);
                }
            }

            numShards = (int) config.getOrDefault("numShards", -1);
        } catch (IOException e) {
            log.error("Unexpected error loading config file!", e);
        } catch (YAMLException | ClassCastException e) {
            log.error("Could not parse the config file, this is likely due to it being malformed!" +
                    "Check the file on an online YAML validator.", e);
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
        return errorLogChannel;
    }

    public String getImportantLogChannel() {
        return importantLogChannel;
    }

    public WebhookClient getImportantWebhook() {
        if (importantLogChannel.isEmpty())
            return null;
        if (importantLogHook == null)
            importantLogHook = new WebhookClientBuilder(importantLogChannel).build();
        return importantLogHook;
    }

    public WebhookClient getErrorWebhook() {
        if (errorLogChannel.isEmpty())
            return null;
        if (errorLogHook == null)
            errorLogHook = new WebhookClientBuilder(errorLogChannel).build();
        return errorLogHook;
    }

    public WebhookClient getStatusWebhook() {
        if (statusLogChannel.isEmpty())
            return null;
        if (statusLogHook == null)
            statusLogHook = new WebhookClientBuilder(statusLogChannel).build();
        return statusLogHook;
    }

    public int getNumShards() {
        return numShards;
    }

    public long getOfficialGuild() {
        return officialGuild;
    }
}
