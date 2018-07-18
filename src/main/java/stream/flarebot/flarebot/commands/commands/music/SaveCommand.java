package stream.flarebot.flarebot.commands.commands.music;

import com.arsenarsen.lavaplayerbridge.player.Track;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import stream.flarebot.flarebot.Client;
import stream.flarebot.flarebot.DataHandler;
import stream.flarebot.flarebot.FlareBot;
import stream.flarebot.flarebot.commands.Command;
import stream.flarebot.flarebot.commands.CommandType;
import stream.flarebot.flarebot.database.DatabaseManager;
import stream.flarebot.flarebot.objects.GuildWrapper;
import stream.flarebot.flarebot.permissions.Permission;
import stream.flarebot.flarebot.util.MessageUtils;

import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;


public class SaveCommand implements Command {

    @Override
    public void onCommand(User sender, GuildWrapper guild, TextChannel channel, Message message, String[] args, Member member) {
        if (args.length == 0) {
            MessageUtils.sendUsage(this, channel, sender, args);
            return;
        }

        String name = MessageUtils.getMessage(args, 0);
        if (name.length() > 20) {
            MessageUtils.sendErrorMessage("Name can only be a maximum of 20 characters!", channel);
            return;
        }
        if (Client.instance().getTracks(guild.getGuildId()).size() == 0) {
            MessageUtils.sendErrorMessage("Your playlist is empty!", channel);
            return;
        }
        List<AudioTrack> playlist = Client.instance().getTracks(guild.getGuildId());
        AudioTrack currentlyPlaying =
                Client.instance().getPlayer(guild.getGuildId()).getPlayingTrack();

        channel.sendTyping().complete();

        List<String> tracks = playlist.stream()
                .map(track -> track.getInfo().uri).collect(Collectors.toList());
        if (currentlyPlaying != null) {
            tracks.add(0, currentlyPlaying.getInfo().uri);
        }

        if (tracks.isEmpty()) {
            MessageUtils.sendErrorMessage("Your playlist is empty!", channel);
            return;
        }

        DataHandler.savePlaylist(this,
                channel,
                sender.getId(),
                this.getPermissions(channel).hasPermission(member, Permission.SAVE_OVERWRITE),
                name,
                tracks);
    }

    @Override
    public String getCommand() {
        return "save";
    }

    @Override
    public String getDescription() {
        return "Save the current playlist!";
    }

    @Override
    public String getUsage() {
        return "`{%}save <name>` - Saves a playlist.";
    }

    @Override
    public Permission getPermission() {
        return Permission.SAVE_COMMAND;
    }

    @Override
    public CommandType getType() {
        return CommandType.MUSIC;
    }
}
