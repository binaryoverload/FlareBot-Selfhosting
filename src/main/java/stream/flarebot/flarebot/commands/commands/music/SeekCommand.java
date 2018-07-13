package stream.flarebot.flarebot.commands.commands.music;

import com.arsenarsen.lavaplayerbridge.player.Track;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import org.joda.time.Duration;
import stream.flarebot.flarebot.Client;
import stream.flarebot.flarebot.FlareBot;
import stream.flarebot.flarebot.commands.Command;
import stream.flarebot.flarebot.commands.CommandType;
import stream.flarebot.flarebot.objects.GuildWrapper;
import stream.flarebot.flarebot.permissions.Permission;
import stream.flarebot.flarebot.util.MessageUtils;
import stream.flarebot.flarebot.util.general.FormatUtils;
import stream.flarebot.flarebot.util.general.GeneralUtils;


public class SeekCommand implements Command {

    @Override
    public void onCommand(User sender, GuildWrapper guild, TextChannel channel, Message message, String[] args, Member member) {
        if (args.length == 1) {
            Long millis = GeneralUtils.parseTime(args[0]);
            if (millis == null || millis > Integer.MAX_VALUE) {
                MessageUtils.sendErrorMessage("You have entered an invalid duration to skip to!\n" + getExtraInfo(), channel);
                return;
            }
            AudioTrack t = Client.instance().getPlayer(guild.getGuildId()).getPlayingTrack();
            if (t == null) {
                MessageUtils.sendErrorMessage("There is no song currently playing!", channel);
                return;
            } else {
                if (t.getInfo().isStream) {
                    MessageUtils.sendErrorMessage("Cannot seek on a livestream!", channel);
                    return;
                } else if (!t.isSeekable()) {
                    MessageUtils.sendErrorMessage("Cannot seek on this track!", channel);
                    return;
                } else {
                    if (millis >= t.getDuration()) {
                        MessageUtils.sendErrorMessage("The duration specified is bigger than the length of the video!", channel);
                        return;
                    } else {
                        Client.instance().getPlayer(guild.getGuildId()).seekTo(millis);
                        MessageUtils.sendSuccessMessage("The track has been skipped to: " + FormatUtils.formatJodaTime(new Duration(millis).toPeriod()), channel);
                        return;
                    }
                }
            }
        }
        MessageUtils.sendUsage(this, channel, sender, args);
    }

    @Override
    public String getCommand() {
        return "seek";
    }

    @Override
    public String getDescription() {
        return "Allows users to skip to a certain point in a video";
    }

    @Override
    public String getUsage() {
        return "`{%}seek <time>` - Seeks to a specific time in the currently playing video.";
    }

    @Override
    public Permission getPermission() {
        return Permission.SEEK_COMMAND;
    }

    @Override
    public String getExtraInfo() {
        return "**Time formatting**\n" +
                "s\n" +
                "m:s\n" +
                "h:m:s\n\n" +
                "XhXmXs\n\n" +
                "Examples:\n" +
                "5 - Would be 5 seconds\n" +
                "5s - Would also be 5 seconds\n" +
                "1:10 - Would be 1 minute and 10 seconds\n" +
                "1m10s - Would also be 1 minute and 10 seconds\n" +
                "1:00:00 - Would be an hour\n" +
                "1h - Would also be an hour";
    }

    @Override
    public CommandType getType() {
        return CommandType.MUSIC;
    }
}
