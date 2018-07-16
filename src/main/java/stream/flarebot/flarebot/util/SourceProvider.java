package stream.flarebot.flarebot.util;

import stream.flarebot.flarebot.util.general.MusicUtils;

import java.util.regex.Pattern;

public enum SourceProvider {
    YOUTUBE(MusicUtils.YOUTUBE_PATTERN),
    SOUNDCLOUD(MusicUtils.SOUNDCLOUD_PATTERN),
    MIXER(MusicUtils.MIXER_PATTERN),
    TWITCH(MusicUtils.TWITCH_PATTERN);

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
