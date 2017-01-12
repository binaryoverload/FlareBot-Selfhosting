package com.bwfcwalshy.flarebot.commands.secret;

import com.bwfcwalshy.flarebot.FlareBot;
import com.bwfcwalshy.flarebot.MessageUtils;
import com.bwfcwalshy.flarebot.commands.Command;
import com.bwfcwalshy.flarebot.commands.CommandType;
import com.bwfcwalshy.flarebot.scheduler.FlarebotTask;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class UpdateCommand implements Command {

    public static final AtomicBoolean UPDATING = new AtomicBoolean(false);
    private static AtomicBoolean queued = new AtomicBoolean(false);
    public static final AtomicBoolean NOVOICE_UPDATING = new AtomicBoolean(false);

    private FlareBot flareBot = FlareBot.getInstance();

    @Override
    public void onCommand(IUser sender, IChannel channel, IMessage message, String[] args) {
        if (getPermissions(channel).isCreator(sender)) {
            if (args.length == 0) {
                update(false, channel);
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("force")) {
                    update(true, channel);
                } else if (args[0].equalsIgnoreCase("no-active-channels")) {
                    MessageUtils.sendMessage("I will now update to the latest version when no channels are playing music!", channel);
                    if (flareBot.getClient().getConnectedVoiceChannels().size() == 0) {
                        update(true, channel);
                    } else {
                        if (!queued.getAndSet(true)) {
                            NOVOICE_UPDATING.set(true);
                        } else
                            MessageUtils.sendMessage("There is already an update queued!", channel);
                    }
                } else {
                    if (!queued.getAndSet(true)) {
                        Period p;
                        try {
                            PeriodFormatter formatter = new PeriodFormatterBuilder()
                                    .appendDays().appendSuffix("d")
                                    .appendHours().appendSuffix("h")
                                    .appendMinutes().appendSuffix("m")
                                    .appendSeconds().appendSuffix("s")
                                    .toFormatter();
                            p = formatter.parsePeriod(args[0]);

                            new FlarebotTask("Scheduled-Update") {
                                @Override
                                public void run() {
                                    update(true, channel);
                                }
                            }.delay(p.toStandardSeconds().getSeconds() * 1000);
                        } catch (IllegalArgumentException e) {
                            MessageUtils.sendMessage("That is an invalid time option!", channel);
                            return;
                        }
                        MessageUtils.sendMessage("I will now update to the latest version in " + p.toStandardSeconds().getSeconds() + " seconds.", channel);
                    } else
                        MessageUtils.sendMessage("There is already an update queued!", channel);
                }
            }
        }
    }

    /**
     * Update to the newest version of FlareBot!
     *
     * @param force   If the version number has not changed this will need to be true in order to update it.
     * @param channel Channel the command was sent in.
     */
    public static void update(boolean force, IChannel channel) {
        try {
            URL url = new URL("https://raw.githubusercontent.com/ArsenArsen/FlareBot/master/pom.xml");
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while (true) {
                line = br.readLine();
                if (line != null && (line.contains("<version>") && line.contains("</version>"))) {
                    String latestVersion = line.replace("<version>", "").replace("</version>", "").replaceAll(" ", "").replaceAll("\t", "");
                    String currentVersion = FlareBot.getInstance().getVersion();
                    if (force || isHigher(latestVersion, currentVersion)) {
                        FlareBot.getInstance().setStatus("Updating..");
                        if (channel != null)
                            MessageUtils.sendMessage("Updating to version `" + latestVersion + "` from `" + currentVersion + "`", channel);
                        UPDATING.set(true);
                        FlareBot.getInstance().quit(true);
                    } else {
                        if (channel != null)
                            MessageUtils.sendMessage("I am currently up to date! Current version: `" + currentVersion + "`", channel);
                    }
                    break;
                }
            }
        } catch (IOException e) {
            FlareBot.LOGGER.error("Could not update!", e);
        }
    }


    /**
     * Check if a string is higher than another.
     *
     * @param s1 This is the string that will be checked. Use this for things like latest version.
     * @param s2 This is the string being compared with. Use this for things like current version.
     * @return If s1 is greater than s2.
     */
    private static boolean isHigher(String s1, String s2) {
        String[] split = s1.split("\\.");
        int s1Major = Integer.parseInt(split[0]);
        int s1Minor = Integer.parseInt(split[1]);
        int s1Build = 0;
        if (split.length == 3)
            s1Build = Integer.parseInt(split[2]);

        String[] split2 = s2.split("\\.");
        int s2Major = Integer.parseInt(split2[0]);
        int s2Minor = Integer.parseInt(split2[1]);
        int s2Build = 0;
        if (split2.length == 3)
            s2Build = Integer.parseInt(split2[2]);

        return s1Major > s2Major || s1Minor > s2Minor || s1Build > s2Build;
    }

    @Override
    public String getCommand() {
        return "update";
    }

    @Override
    public String getDescription() {
        return "Update the bot.";
    }

    @Override
    public CommandType getType() {
        return CommandType.HIDDEN;
    }
}
