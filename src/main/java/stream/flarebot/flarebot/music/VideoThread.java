package stream.flarebot.flarebot.music;

import com.arsenarsen.lavaplayerbridge.PlayerManager;
import com.google.gson.JsonObject;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import stream.flarebot.flarebot.Client;
import stream.flarebot.flarebot.Config;
import stream.flarebot.flarebot.FlareBot;
import stream.flarebot.flarebot.music.extractors.Extractor;
import stream.flarebot.flarebot.music.extractors.RandomExtractor;
import stream.flarebot.flarebot.music.extractors.SavedPlaylistExtractor;
import stream.flarebot.flarebot.music.extractors.YouTubeExtractor;
import stream.flarebot.flarebot.music.extractors.YouTubeSearchExtractor;
import stream.flarebot.flarebot.util.MessageUtils;
import stream.flarebot.flarebot.util.SourceProvider;
import stream.flarebot.flarebot.util.WebUtils;
import stream.flarebot.flarebot.util.buttons.ButtonRunnable;
import stream.flarebot.flarebot.util.buttons.ButtonUtil;
import stream.flarebot.flarebot.util.objects.ButtonGroup;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
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
    private boolean search = false;
    private Extractor extractor;

    public static final Pattern YOUTUBE_PATTERN = Pattern.compile("(?:https?:\\/\\/)?(?:www\\.|m\\.)?(?:youtube\\.com|youtu\\.be)?\\/(?:watch\\?v=|playlist\\?list=)([0-9A-z-_]+)(?:&list=([0-9A-z-_]+))?");
    public static final Pattern SOUNDCLOUD_PATTERN = Pattern.compile("(?:https?://)(?:www\\.|m\\.)?soundcloud\\.com/" +
            "([a-zA-Z0-9_-]{4,25})/([\\w_-]{4,})");
    public static final Pattern TWITCH_PATTERN = Pattern.compile("(?:https?://)(?:www\\.)?twitch\\.tv/" +
            "([\\w_-]{4,25})");
    public static final Pattern MIXER_PATTERN = Pattern.compile("(?:https?://)(?:www\\.)?mixer\\.com/" +
            "([\\w_-]{4,25})");

    private static final Pattern YOUTUBE_SONG_OR_PLAYLIST = Pattern
            .compile("(?:https?:\\/\\/)?(?:www\\.|m\\.)?(?:youtube\\.com|youtu\\.be)?\\/(?:watch\\?v=|playlist\\?list=)([0-9A-z-_]+)(?:&list=([0-9A-z-_]+))?");

    private static final String YOUTUBE_SONG = "https://youtube.com/watch?v=%s";
    private static final String YOUTUBE_PLAYLIST = "https://youtube.com/playlist?list=%s";

    private static AudioPlayerManager playerManager = initManager();

    private static AudioPlayerManager initManager() {
        playerManager = new DefaultAudioPlayerManager();
        YoutubeAudioSourceManager youtubeAudioSourceManager = new YoutubeAudioSourceManager(false);
        youtubeAudioSourceManager.configureRequests(config -> RequestConfig.copy(config)
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                .setConnectTimeout(5000)
                .build());
        playerManager.registerSourceManager(youtubeAudioSourceManager);
        if (Config.INS.isMixerEnabled())
            playerManager.registerSourceManager(new BeamAudioSourceManager());
        if (Config.INS.isTwitchEnabled())
            playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        if (Config.INS.isSoundCloudEnabled())
            playerManager.registerSourceManager(new SoundCloudAudioSourceManager());

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
        if(search) {
            boolean isUrl = url.startsWith("http");
            if (isUrl) {
                loadLink(url, channel, message);
                return;
            }
            List<Map.Entry<String, String>> searchResults = getSearchResults(url, message);
            if(searchResults != null) {
                loadLink(searchResults.get(0).getKey(), channel, message);
            }

        } else {
            //SourceProvider provider = getProviderFromURL(url);
            if (url.contains("\u200B")) {
                String[] split = url.split("\u200B");
                String name = split[0];
                String songs = split[1];
                songs = songs.replaceAll("\\[", "").replaceAll("\\]","");
                String[] items = songs.split("\\s*,\\s*");
                for (String item : items) {
                    loadLink(item, channel, null);
                }
                message.editMessage(new EmbedBuilder().setTitle("Loaded Playlist").addField("Playlist", name, true).addField("Song count", String.valueOf(items.length), true).build()).queue();
            } else {
                Matcher matcher = YOUTUBE_SONG_OR_PLAYLIST.matcher(url);
                if (matcher.matches()) {
                    ButtonGroup buttonGroup = new ButtonGroup(user.getIdLong(), "youtube-song/playlist");
                    buttonGroup.addButton(new ButtonGroup.Button("\u0031\u20E3", (ownerID, user, message1) -> {
                        if (user.getIdLong() == ownerID) {
                            message1.delete().queue();
                            loadLink(String.format(YOUTUBE_PLAYLIST, matcher.group(2)), channel, message);
                        }
                    }));
                    buttonGroup.addButton(new ButtonGroup.Button("\u0032\u20E3", (ownerID, user, message1) -> {
                        if (user.getIdLong() == ownerID) {
                            message1.delete().queue();
                            loadLink(String.format(YOUTUBE_SONG, matcher.group(1)), channel, message);
                        }
                    }));
                    ButtonUtil.sendButtonedMessage(channel, new EmbedBuilder().setTitle("How to load url?")
                            .appendDescription("\u0031\u20E3: Load as playlist\n" +
                                    "\u0032\u20E3: Load as song")
                            .build(), buttonGroup);
                } else {
                    loadLink(url, channel, message);
                }
            }
        }
        /*try {
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
        }*/
    }

    private void loadLink(String link, TextChannel channel, Message message) {
        playerManager.loadItem(link, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                track.setUserData(user.getId());
                if(Client.instance().getPlayer(channel.getGuild().getId()).getPlayingTrack() != null) {
                    Client.instance().getTracks(channel.getGuild().getId()).add(track);
                } else {
                    Client.instance().getPlayer(channel.getGuild().getId()).playTrack(track);
                }
                if(message != null) {
                    message.editMessage(new EmbedBuilder().setTitle("Loaded Song")
                            .appendDescription("[" + track.getInfo().title + "](" + track.getInfo().uri + ")")
                            .setFooter("Requested by " + MessageUtils.getTag(user), user.getAvatarUrl())
                            .build()).queue();
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                int size = playlist.getTracks().size();
                if(Client.instance()
                        .getPlayer(channel.getGuild().getId())
                        .getPlayingTrack() != null) {
                    Client.instance().getTracks(channel.getGuild().getId()).addAll(playlist.getTracks());
                } else {
                    List<AudioTrack> tracks = playlist.getTracks();
                    tracks.forEach(track -> {
                        track.setUserData(user.getId());
                    });
                    AudioTrack firstTrack = tracks.get(0);
                    tracks.remove(0);
                    Client.instance().getPlayer(channel.getGuild().getId()).playTrack(firstTrack);
                    Client.instance().getTracks(channel.getGuild().getId()).addAll(tracks);
                }
                if(message != null) {
                    message.editMessage(new EmbedBuilder().setTitle("Loaded Playlist")
                            .appendDescription("[" + playlist.getName() + "](" + link + ")")
                            .addField("Song Count", String.valueOf(size), false)
                            .setFooter("Requested by " + MessageUtils.getTag(user), user.getAvatarUrl())
                            .build()).queue();
                }
            }

            @Override
            public void noMatches() {
                message.editMessage("We couldn't find that song!").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                MessageUtils.sendException("Error loading song\n" + exception.getMessage(), exception, channel);
            }
        });
    }

    private static SourceProvider getProviderFromURL(String url) {
        for (SourceProvider provider : SourceProvider.values) {
            if (provider.getPattern().matcher(url).matches())
                return provider;
        }
        return null;
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
        thread.search = true;
        thread.extractor = new YouTubeSearchExtractor();
        return thread;
    }

    public static List<Map.Entry<String, String>> getSearchResults(String search, Message message) {
        Response response = null;
        try {
            response = WebUtils.get(new Request.Builder().url(String.format("https://www.googleapis.com/youtube/v3/" +
                            "search?q=%s&part=snippet&key=%s&type=video,playlist", URLEncoder.encode(search, "UTF-8"),
                    Config.INS.getYoutubeApi())));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(response != null) {
            if(!response.isSuccessful()) {
                message.editMessage("Error searching for video!").queue();
                return null;
            }

            ResponseBody body = response.body();
            if(body == null) {
                message.editMessage("No videos found!").queue();
                return null;
            }

            List<Map.Entry<String, String>> contentIds = new ArrayList<>();
            JSONArray results;
            try {
                results = new JSONObject(body.string()).getJSONArray("items");
            } catch (IOException e) {
                message.editMessage("Youtube failed to return video Ids!").queue();
                return null;
            }
            body.close();
            response.close();

            for(Object o : results) {
                if (o instanceof JSONObject) {
                    JSONObject snippet = ((JSONObject) o).getJSONObject("snippet");
                    JSONObject id = ((JSONObject) o).getJSONObject("id");
                    switch (id.getString("kind")) {
                        case "youtube#video":
                            contentIds.add(new Map.Entry<String, String>() {
                                String value = snippet.getString("title");

                                @Override
                                public String getKey() {
                                    return id.getString("videoId");
                                }

                                @Override
                                public String getValue() {
                                    return value;
                                }

                                @Override
                                public String setValue(String value) {
                                    this.value = value;
                                    return value;
                                }
                            });
                            break;
                        case "youtube#playlist":
                            contentIds.add(new Map.Entry<String, String>() {
                                String value = snippet.getString("title");

                                @Override
                                public String getKey() {
                                    return id.getString("playlistId");
                                }

                                @Override
                                public String getValue() {
                                    return value;
                                }

                                @Override
                                public String setValue(String value) {
                                    this.value = value;
                                    return value;
                                }
                            });
                            break;
                    }
                }
            }

            return contentIds;
        } else {
            message.editMessage("Got null response from youtube").queue();
            return null;
        }
    }
}
