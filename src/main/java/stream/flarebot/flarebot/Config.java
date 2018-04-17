package stream.flarebot.flarebot;

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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    public static String DEFAULT_PREFIX = "_";

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
    @Nullable
    private String redisHost;
    private int redisPort;
    @Nullable
    private String redisPassword;
    private int hikariPoolSize;

    // Hooks and test bot
    private String errorLogWebhook;
    private String importantLogWebhook;
    private String testBotChannel;

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

            testBotChannel = (String) config.getOrDefault("testBotChannel", "");

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

    public class LavalinkHost {

        private String name;
        private URI uri;
        private String password;

        public LavalinkHost(String name, URI uri, String pass) {
            this.name = name;
            this.uri = uri;
            this.password = pass;
        }

        public String getName() {
            return name;
        }

        public URI getUri() {
            return uri;
        }

        public String getPassword() {
            return password;
        }
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

    public String getTestBotChannel() {
        return testBotChannel;
    }

    public boolean isTestBot() {
        return !testBotChannel.equals("");
    }

    public int getNumShards() {
        return numShards;
    }
}
