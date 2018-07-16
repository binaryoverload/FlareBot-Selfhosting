package stream.flarebot.flarebot.util.general;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import stream.flarebot.flarebot.Client;
import stream.flarebot.flarebot.util.MessageUtils;

import java.util.regex.Pattern;

public class MusicUtils {
    public static final Pattern YOUTUBE_PATTERN = Pattern.compile("(?:https?:\\/\\/)?(?:www\\.|m\\.)?(?:youtube\\.com|youtu\\.be)?\\/(?:watch\\?v=|playlist\\?list=)([0-9A-z-_]+)(?:&list=([0-9A-z-_]+))?");
    public static final Pattern SOUNDCLOUD_PATTERN = Pattern.compile("(?:https?://)(?:www\\.|m\\.)?soundcloud\\.com/" +
            "([a-zA-Z0-9_-]{4,25})/([\\w_-]{4,})");
    public static final Pattern TWITCH_PATTERN = Pattern.compile("(?:https?://)(?:www\\.)?twitch\\.tv/" +
            "([\\w_-]{4,25})");
    public static final Pattern MIXER_PATTERN = Pattern.compile("(?:https?://)(?:www\\.)?mixer\\.com/" +
            "([\\w_-]{4,25})");

    public static final Pattern YOUTUBE_SONG_OR_PLAYLIST = Pattern
            .compile("(?:https?:\\/\\/)?(?:www\\.|m\\.)?(?:youtube\\.com|youtu\\.be)?\\/(?:watch\\?v=|playlist\\?list=)([0-9A-z-_]+)(?:&list=([0-9A-z-_]+))?");

    public static boolean joinChannel(TextChannel channel, Member member) {
        if (channel.getGuild().getSelfMember()
                .hasPermission(member.getVoiceState().getChannel(), Permission.VOICE_CONNECT) &&
                channel.getGuild().getSelfMember()
                        .hasPermission(member.getVoiceState().getChannel(), Permission.VOICE_SPEAK)) {
            if (member.getVoiceState().getChannel().getUserLimit() > 0 && member.getVoiceState().getChannel()
                    .getMembers().size()
                    >= member.getVoiceState().getChannel().getUserLimit() && !member.getGuild().getSelfMember()
                    .hasPermission(member.getVoiceState().getChannel(), Permission.MANAGE_CHANNEL)) {
                MessageUtils.sendErrorMessage("We can't join :(\n\nThe channel user limit has been reached and we don't have the 'Manage Channel' permission to " +
                        "bypass it!", channel);
                return false;
            }
            Client.instance().getLink(channel.getGuild().getId()).connect(member.getVoiceState().getChannel());
            return true;
        } else {
            MessageUtils.sendErrorMessage("I do not have permission to " + (!channel.getGuild().getSelfMember()
                    .hasPermission(member.getVoiceState()
                            .getChannel(), Permission.VOICE_CONNECT) ?
                    "connect" : "speak") + " in your voice channel!", channel);
            return false;
        }
    }

    public static void skip(String guildId) {
        if(Client.instance().getTracks(guildId).size() > 0) {
            AudioTrack nextTrack = Client.instance().getTracks(guildId).get(0);
            Client.instance().getPlayer(guildId).playTrack(nextTrack);
            Client.instance().getTracks(guildId).remove(0);
        } else {
            Client.instance().getPlayer(guildId).stopTrack();
        }
    }
}
