package stream.flarebot.flarebot.commands.commands.music;

import com.arsenarsen.lavaplayerbridge.PlayerManager;
import com.arsenarsen.lavaplayerbridge.player.Player;
import com.arsenarsen.lavaplayerbridge.player.Track;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lavalink.client.player.IPlayer;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import stream.flarebot.flarebot.Client;
import stream.flarebot.flarebot.FlareBot;
import stream.flarebot.flarebot.Getters;
import stream.flarebot.flarebot.commands.Command;
import stream.flarebot.flarebot.commands.CommandType;
import stream.flarebot.flarebot.music.extractors.YouTubeExtractor;
import stream.flarebot.flarebot.objects.GuildWrapper;
import stream.flarebot.flarebot.permissions.Permission;
import stream.flarebot.flarebot.scheduler.Scheduler;
import stream.flarebot.flarebot.util.MessageUtils;
import stream.flarebot.flarebot.util.buttons.ButtonUtil;
import stream.flarebot.flarebot.util.buttons.ButtonGroupConstants;
import stream.flarebot.flarebot.util.general.FormatUtils;
import stream.flarebot.flarebot.util.general.GeneralUtils;
import stream.flarebot.flarebot.util.objects.ButtonGroup;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

public class SongCommand implements Command {


    private static final Map<Long, Long> songMessages = new ConcurrentHashMap<>();

    @Override
    public void onCommand(User sender, GuildWrapper guild, TextChannel channel, Message message, String[] args, Member member) {
        IPlayer player = Client.instance().getPlayer(guild.getGuildId());
        if (player.getPlayingTrack() != null) {
            AudioTrack track = player.getPlayingTrack();
            EmbedBuilder eb = MessageUtils.getEmbed(sender)
                    .addField("Current Song", track.getInfo().uri, false)
                    .setThumbnail("https://img.youtube.com/vi/" + track.getIdentifier() + "/hqdefault.jpg");
            if (track.getInfo().isStream)
                eb.addField("Amount Played", "Issa livestream ;)", false);
            else
                eb.addField("Amount Played", GeneralUtils.getProgressBar(track, Client.instance().getPlayer(guild.getGuildId())), true)
                        .addField("Time", String.format("%s / %s", FormatUtils.formatDuration(Client.instance().getPlayer(channel.getGuild().getId()).getTrackPosition()),
                                FormatUtils.formatDuration(track.getDuration())), false);
            ButtonGroup buttonGroup = new ButtonGroup(sender.getIdLong(), ButtonGroupConstants.SONG);
            buttonGroup.addButton(new ButtonGroup.Button("\u23EF", (owner, user, message1) -> {
                    if (player.isPaused()) {
                        if (getPermissions(channel).hasPermission(guild.getGuild().getMember(user), Permission.RESUME_COMMAND)) {
                            player.setPaused(false);
                        }
                    } else {
                        if (getPermissions(channel).hasPermission(guild.getGuild().getMember(user), Permission.PAUSE_COMMAND)) {
                            player.setPaused(true);
                        }
                    }
            }));
            buttonGroup.addButton(new ButtonGroup.Button("\u23F9", (owner, user, message1) -> {
                if (getPermissions(channel).hasPermission(guild.getGuild().getMember(user), Permission.STOP_COMMAND)) {
                    player.stopTrack();
                }
            }));
            buttonGroup.addButton(new ButtonGroup.Button("\u23ED", (owner, user, message1) -> {
                if (getPermissions(channel).hasPermission(guild.getGuild().getMember(user), Permission.SKIP_COMMAND)) {
                    Command cmd = FlareBot.getCommandManager().getCommand("skip", user);
                    if (cmd != null)
                        cmd.onCommand(user, guild, channel, message1, new String[0], guild.getGuild().getMember(user));
                }
            }));
            buttonGroup.addButton(new ButtonGroup.Button("\uD83D\uDD01", (ownerID, user, message1) -> {
                updateSongMessage(user, message1, message1.getTextChannel());
            }));
            Message message1 = ButtonUtil.sendReturnedButtonedMessage(channel, eb.build(), buttonGroup);
            songMessages.put(channel.getIdLong(), message1.getIdLong());
        } else {
            channel.sendMessage(MessageUtils.getEmbed(sender)
                    .addField("Current song", "**No song playing right now!**", false)
                    .build()).queue();
        }
    }

    @Override
    public String getCommand() {
        return "song";
    }

    @Override
    public String getDescription() {
        return "Get the current song playing.";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"playing"};
    }

    @Override
    public String getUsage() {
        return "`{%}song` - Displays info about the currently playing song.";
    }

    @Override
    public Permission getPermission() {
        return Permission.SONG_COMMAND;
    }

    @Override
    public CommandType getType() {
        return CommandType.MUSIC;
    }

    public static void updateSongMessage(User sender, Message message, TextChannel channel) {
        AudioTrack track = Client.instance().getPlayer(channel.getGuild().getId()).getPlayingTrack();
        if (track == null)
            return;
        EmbedBuilder eb = MessageUtils.getEmbed(sender)
                .addField("Current Song", track.getInfo().uri, false)
                .setThumbnail(GeneralUtils.getTrackPreview(track));
        if (track.getInfo().isStream)
            eb.addField("Amount Played", "Issa livestream ;)", false);
        else
            eb.addField("Amount Played", GeneralUtils.getProgressBar(track, Client.instance().getPlayer(channel.getGuild().getId())), true)
                    .addField("Time", String.format("%s / %s", FormatUtils.formatDuration(Client.instance().getPlayer(channel.getGuild().getId()).getTrackPosition()),
                            FormatUtils.formatDuration(track.getDuration())), false);
        message.editMessage(eb.build()).queue();
    }

    public static void updateMessages() {
        for (Map.Entry<Long, Long> pair : songMessages.entrySet()) {
            TextChannel channel = Getters.getChannelById(pair.getKey());
            if (channel == null) {
                songMessages.remove(pair.getKey());
                break;
            }
            IPlayer player = Client.instance().getPlayer(channel.getGuild().getId());
            AudioTrack track = player.getPlayingTrack();
            if (track != null) {
                if(!player.isPaused()) {
                    channel.getMessageById(pair.getValue()).queue(message -> updateSongMessage(Client.instance().getSelfUser(), message, channel));
                }
            }
        }
    }

}
