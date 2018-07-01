package stream.flarebot.flarebot.music;

import com.arsenarsen.lavaplayerbridge.PlayerManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import stream.flarebot.flarebot.Config;
import stream.flarebot.flarebot.FlareBot;
import stream.flarebot.flarebot.music.extractors.Extractor;
import stream.flarebot.flarebot.music.extractors.RandomExtractor;
import stream.flarebot.flarebot.music.extractors.SavedPlaylistExtractor;
import stream.flarebot.flarebot.music.extractors.YouTubeExtractor;
import stream.flarebot.flarebot.music.extractors.YouTubeSearchExtractor;
import stream.flarebot.flarebot.util.MessageUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class VideoThread extends Thread {

    private static PlayerManager manager;
    private static final List<Class<? extends Extractor>> extractors = Arrays.asList(YouTubeExtractor.class,
            SavedPlaylistExtractor.class, RandomExtractor.class);
    private static final Set<Class<? extends AudioSourceManager>> managers = new HashSet<>();
    public static final ThreadGroup VIDEO_THREADS = new ThreadGroup("Video Threads");
    private User user;
    private TextChannel channel;
    private String url;
    private Extractor extractor;

    public static final Pattern YOUTUBE_PATTERN = Pattern.compile("(https?://)?(www\\.|m\\.)?" +
            "(youtube\\.com|youtu\\.be)?/(watch\\?v=|playlist\\?list=)(\\w+)(&list=(\\w+))?");
    public static final Pattern SOUNDCLOUD_PATTERN = Pattern.compile("(?:https?://)(?:www\\.|m\\.)?soundcloud\\.com/" +
            "([a-zA-Z0-9_-]{4,25})/([\\w_-]{4,})");
    public static final Pattern TWITCH_PATTERN = Pattern.compile("(?:https?://)(?:www\\.)?twitch\\.tv/" +
            "([\\w_-]{4,25})");
    public static final Pattern MIXER_PATTERN = Pattern.compile("(?:https?://)(?:www\\.)?mixer\\.com/" +
            "([\\w_-]{4,25})");

    private static final Pattern YOUTUBE_SONG_OR_PLAYLIST = Pattern
            .compile("(?:https?://)?(?:www\\.|m\\.)?(?:youtube\\.com|youtu\\.be)?" +
                    "/(?:watch\\?v=|playlist\\?list=)([\\w_-]+)(?:&list=([\\w_-]+))?");

    private static final String YOUTUBE_SONG = "https://youtube.com/watch?v=%s";
    private static final String YOUTUBE_PLAYLIST = "https://youtube.com/playlist?list=%s";

    private static AudioPlayerManager playerManager = initManager();

    private static AudioPlayerManager initManager() {
        playerManager = new DefaultAudioPlayerManager();
        // TODO all of this
        if (Config.INS.isYouTubeEnabled()) {
            YoutubeAudioSourceManager youtubeAudioSourceManager = new YoutubeAudioSourceManager(false);
            youtubeAudioSourceManager.configureRequests(config -> RequestConfig.copy(config)
                    .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                    .setConnectTimeout(5000)
                    .build());
            playerManager.registerSourceManager(youtubeAudioSourceManager);
        }
        if (Config.INS.isMixerEnabled())
            playerManager.registerSourceManager(new BeamAudioSourceManager());
        if (Config.INS.isTwitchEnabled())
            playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());

        return playerManager;
    }

    public static AudioPlayerManager getPlayerManager() {
        return playerManager;
    }

    private VideoThread() {
        setName("Video Thread " + VIDEO_THREADS.activeCount());
    }

    @Override
    public void run() {
        Message message = channel.sendMessage("Processing..").complete();
        try {
            if (extractor == null)
                for (Class<? extends Extractor> clazz : extractors) {
                    Extractor extractor = clazz.newInstance();
                    if (!extractor.valid(url))
                        continue;
                    this.extractor = extractor;
                    break;
                }
            if (extractor == null) {
                MessageUtils.editMessage(message, "Could not find a way to process that..");
                return;
            }
            if (managers.add(extractor.getSourceManagerClass()))
                manager.getManager().registerSourceManager(extractor.newSourceManagerInstance());
            extractor.process(url, manager.getPlayer(channel.getGuild().getId()), message, user);
        } catch (Exception e) {
            FlareBot.LOGGER.warn(("Could not init extractor for '{}'. Guild ID: " + channel.getGuild().getId()).replace("{}", url), e);
            FlareBot.reportError(channel, "Something went wrong while searching for the video!", e);
        }
    }

    @Override
    public void start() {
        if (url == null)
            throw new IllegalStateException("URL Was not set!");
        super.start();
    }

    public static VideoThread getThread(String url, TextChannel channel, User user) {
        VideoThread thread = new VideoThread();
        thread.url = url;
        thread.channel = channel;
        thread.user = user;
        return thread;
    }

    public static VideoThread getSearchThread(String term, TextChannel channel, User user) {
        VideoThread thread = new VideoThread();
        thread.url = term;
        thread.channel = channel;
        thread.user = user;
        thread.extractor = new YouTubeSearchExtractor();
        return thread;
    }
}
