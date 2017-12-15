package stream.flarebot.flarebot.commands.secret;

import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import stream.flarebot.flarebot.FlareBot;
import stream.flarebot.flarebot.commands.Command;
import stream.flarebot.flarebot.commands.CommandType;
import stream.flarebot.flarebot.objects.GuildWrapper;
import stream.flarebot.flarebot.util.ButtonRunnable;
import stream.flarebot.flarebot.util.ButtonUtil;
import stream.flarebot.flarebot.util.objects.ButtonGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestCommand implements Command {

    @Override
    public void onCommand(User sender, GuildWrapper guild, TextChannel channel, Message message, String[] args, Member member) {
        ButtonGroup buttons = new ButtonGroup();
        buttons.addButton(FlareBot.getInstance().getOfficialGuild().getEmoteById("368861419602575364"), new ButtonRunnable() {
            @Override
            public void run(User user) {
                channel.sendMessage(user.getAsMention() + " ban hammer clicked").queue();
            }
        });
        buttons.addButton(FlareBot.getInstance().getOfficialGuild().getEmoteById("355776056092917761"), new ButtonRunnable() {
            @Override
            public void run(User user) {
                channel.sendMessage(user.getAsMention() + " tick clicked").queue();
            }
        });
        buttons.addButton(FlareBot.getInstance().getOfficialGuild().getEmoteById("355776081384570881"), new ButtonRunnable() {
            @Override
            public void run(User user) {
                channel.sendMessage(user.getAsMention() + " I think you're addicted").queue();
            }
        });
        ButtonUtil.sendButtonedMessage(channel, "Buttons test", buttons);
        // Served it's purpose once again :) Now it's free to be reused... no memes pls
    }

    @Override
    public String getCommand() {
        return "test";
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getUsage() {
        return "{%}test";
    }

    @Override
    public CommandType getType() {
        return CommandType.SECRET;
    }
}
