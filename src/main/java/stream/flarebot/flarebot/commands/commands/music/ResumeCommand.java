package stream.flarebot.flarebot.commands.commands.music;

import com.arsenarsen.lavaplayerbridge.player.Player;
import lavalink.client.player.IPlayer;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import stream.flarebot.flarebot.Client;
import stream.flarebot.flarebot.FlareBot;
import stream.flarebot.flarebot.commands.Command;
import stream.flarebot.flarebot.commands.CommandType;
import stream.flarebot.flarebot.objects.GuildWrapper;
import stream.flarebot.flarebot.permissions.Permission;
import stream.flarebot.flarebot.util.MessageUtils;

public class ResumeCommand implements Command {

    @Override
    public void onCommand(User sender, GuildWrapper guild, TextChannel channel, Message message, String[] args, Member member) {
        IPlayer player = Client.instance().getPlayer(guild.getGuildId());
        if (player.getPlayingTrack() == null) {
            MessageUtils.sendErrorMessage("There is no music playing!", channel);
        } else if (!player.isPaused()) {
            MessageUtils.sendErrorMessage("The music is already playing!", channel);
        } else {
            player.setPaused(false);
            MessageUtils.sendSuccessMessage("Resuming...!", channel);
        }
    }

    @Override
    public String getCommand() {
        return "resume";
    }

    @Override
    public String getDescription() {
        return "Resumes your playlist";
    }

    @Override
    public String getUsage() {
        return "`{%}resume` - Resumes the playlist.";
    }

    @Override
    public Permission getPermission() {
        return Permission.RESUME_COMMAND;
    }

    @Override
    public CommandType getType() {
        return CommandType.MUSIC;
    }

}
