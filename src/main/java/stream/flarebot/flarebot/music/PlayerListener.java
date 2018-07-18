package stream.flarebot.flarebot.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lavalink.client.player.event.AudioEventAdapterWrapped;
import lavalink.client.player.event.PlayerEvent;
import lavalink.client.player.event.TrackEndEvent;
import lavalink.client.player.event.TrackStartEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import stream.flarebot.flarebot.Client;
import stream.flarebot.flarebot.DataHandler;
import stream.flarebot.flarebot.Getters;
import stream.flarebot.flarebot.commands.commands.music.SkipCommand;
import stream.flarebot.flarebot.commands.commands.music.SongCommand;
import stream.flarebot.flarebot.objects.GuildWrapper;
import stream.flarebot.flarebot.util.MessageUtils;
import stream.flarebot.flarebot.util.general.FormatUtils;
import stream.flarebot.flarebot.util.general.GeneralUtils;
import stream.flarebot.flarebot.util.general.GuildUtils;
import stream.flarebot.flarebot.util.votes.VoteUtil;

import java.util.List;

public class PlayerListener extends AudioEventAdapterWrapped {

    private String guildId;
    public PlayerListener(String guildId) {
        this.guildId = guildId;
    }

    @Override
    public void onEvent(PlayerEvent event) {
        GuildWrapper wrapper = DataHandler.getGuild(Long.parseLong(guildId));
        super.onEvent(event);
        if(event instanceof TrackEndEvent) {
            TrackEndEvent endEvent = (TrackEndEvent) event;
            if(endEvent.getReason().equals(AudioTrackEndReason.FINISHED)) {
                VoteUtil.remove(SkipCommand.getSkipUUID(), wrapper.getGuild());
                if(Client.instance().getTracks(guildId).size() > 0) {
                    Client.instance().getPlayer(guildId).playTrack(Client.instance().getTracks(guildId).get(0));
                    Client.instance().getTracks(guildId).remove(0);
                }
            }

            if (wrapper.isSongnickEnabled()) {
                if (GuildUtils.canChangeNick(guildId)) {
                    Guild c = wrapper.getGuild();
                    if (c == null) {
                        wrapper.setSongnick(false);
                    } else {
                        if (Client.instance().getTracks(guildId).isEmpty())
                            c.getController().setNickname(c.getSelfMember(), null).queue();
                    }
                } else {
                    if (!GuildUtils.canChangeNick(guildId)) {
                        MessageUtils.sendPM(wrapper.getGuild().getOwner().getUser(),
                                "FlareBot can't change it's nickname so SongNick has been disabled!");
                    }
                }
            }
        }

        if(event instanceof TrackStartEvent) {
            if (wrapper.getMusicAnnounceChannelId() != null) {
                TextChannel c = Getters.getChannelById(wrapper.getMusicAnnounceChannelId());
                if (c != null) {
                    if (c.getGuild().getSelfMember().hasPermission(c,
                            Permission.MESSAGE_EMBED_LINKS,
                            Permission.MESSAGE_READ,
                            Permission.MESSAGE_WRITE)) {
                        AudioTrack track = Client.instance().getPlayer(guildId).getPlayingTrack();
                        List<AudioTrack> tracks = Client.instance().getTracks(guildId);
                        c.sendMessage(MessageUtils.getEmbed()
                                .addField("Now Playing", track.getInfo().uri, false)
                                .addField("Duration", FormatUtils
                                        .formatDuration(track.getDuration()), false)
                                .addField("Requested by",
                                        String.format("<@!%s>", track.getUserData()), false)
                                .addField("Next up", tracks.isEmpty() ? "Nothing" :
                                        tracks.get(1).getInfo().uri, false)
                                .setImage(GeneralUtils.getTrackPreview(track))
                                .build()).queue();
                    } else {
                        wrapper.setMusicAnnounceChannelId(null);
                    }
                } else {
                    wrapper.setMusicAnnounceChannelId(null);
                }
            }
            if (wrapper.isSongnickEnabled()) {
                Guild c = wrapper.getGuild();
                if (c == null || !GuildUtils.canChangeNick(guildId)) {
                    if (!GuildUtils.canChangeNick(guildId)) {
                        wrapper.setSongnick(false);
                        MessageUtils.sendPM(wrapper.getGuild().getOwner().getUser(),
                                "FlareBot can't change it's nickname so SongNick has been disabled!");
                    }
                } else {
                    AudioTrack track = Client.instance().getPlayer(guildId).getPlayingTrack();
                    String str = null;
                    if (track != null) {
                        str = track.getInfo().title;
                        if (str.length() > 32)
                            str = str.substring(0, 32);
                        str = str.substring(0, str.lastIndexOf(' ') + 1);
                    }
                    c.getController()
                            .setNickname(c.getSelfMember(), str)
                            .queue();
                }
            }
        }
    }
}
