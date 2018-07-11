package stream.flarebot.flarebot.util;

import stream.flarebot.flarebot.music.VideoThread;

import java.util.regex.Pattern;

public enum SourceProvider {
    YOUTUBE(VideoThread.YOUTUBE_PATTERN),
    SOUNDCLOUD(VideoThread.SOUNDCLOUD_PATTERN),
    MIXER(VideoThread.MIXER_PATTERN),
    TWITCH(VideoThread.TWITCH_PATTERN);

    public static final SourceProvider[] values = values();

    private Pattern pattern;
    SourceProvider(Pattern pattern) {
        this.pattern = pattern;
    }

    public Pattern getPattern() {
        return pattern;
    }

    @Override
    public String toString() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
