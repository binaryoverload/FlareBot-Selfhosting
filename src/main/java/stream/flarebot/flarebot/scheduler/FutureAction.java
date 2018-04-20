package stream.flarebot.flarebot.scheduler;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import stream.flarebot.flarebot.DataHandler;
import stream.flarebot.flarebot.FlareBot;
import stream.flarebot.flarebot.Getters;
import stream.flarebot.flarebot.database.DatabaseManager;
import stream.flarebot.flarebot.mod.modlog.ModAction;
import stream.flarebot.flarebot.mod.modlog.ModlogHandler;
import stream.flarebot.flarebot.objects.GuildWrapper;
import stream.flarebot.flarebot.util.MessageUtils;
import stream.flarebot.flarebot.util.general.FormatUtils;
import stream.flarebot.flarebot.util.general.GuildUtils;

import java.sql.PreparedStatement;
import java.sql.Timestamp;

public class FutureAction {

    /**
     * Guild ID it was executed in
     */
    private long guildId;
    /**
     * Channel ID it was executed in
     */
    private long channelId;
    /**
     * ID of user who ran the command - Could be a mod or normal user.
     */
    private long responsible;
    /**
     * ID of the target user - if applicable
     */
    private long target;
    /**
     * Content - This could be a reason or just a general message.
     */
    private String content;
    /**
     * How long until the action will be executed.
     */
    private Period delay;
    /**
     * When the task expires. This is calculated from the delay, it will be added onto the current time.
     */
    private DateTime expires;
    /**
     * When the task was created
     */
    private DateTime created;
    /**
     * Rest action - This could be a role remove, a message being sent etc.
     */
    private Action action;

    public FutureAction(long guildId, long channelId, long responsible, long target, String content,
                        DateTime expires, DateTime created, Action action) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.responsible = responsible;
        this.target = target;
        this.content = content;
        this.expires = expires;
        this.created = created;
        this.action = action;

        if (expires.minus(created.getMillis()).getMillis() > Integer.MAX_VALUE) {
            delete();
            return;
        }
        this.delay = new Period(expires.minus(created.getMillis()).getMillis());
    }

    public FutureAction(long guildId, long channelId, long responsible, long target, String content,
                        Period delay, Action action) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.responsible = responsible;
        this.target = target;
        this.content = content;
        this.delay = delay;
        this.created = new DateTime(DateTimeZone.UTC);
        this.expires = created.plus(delay);
        this.action = action;

        if (delay.getMillis() <= 0)
            delete();
    }

    public FutureAction(long guildId, long channelId, long responsible, String content, Period delay, Action action) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.responsible = responsible;
        this.target = -1;
        this.content = content;
        this.delay = delay;
        this.created = new DateTime(DateTimeZone.UTC);
        this.expires = created.plus(delay);
        this.action = action;
    }

    public long getGuildId() {
        return guildId;
    }

    public long getChannelId() {
        return channelId;
    }

    public long getResponsible() {
        return responsible;
    }

    public long getTarget() {
        return this.target;
    }

    public String getContent() {
        return content;
    }

    public DateTime getExpires() {
        return expires;
    }

    public DateTime getCreated() {
        return created;
    }

    public Action getAction() {
        return action;
    }

    public void execute() {
        GuildWrapper gw = DataHandler.getGuild(guildId);
        if (gw == null || gw.getGuild() == null) return;
        switch (action) {
            case TEMP_MUTE:
                if (gw.getGuild().getTextChannelById(channelId) != null) {
                    ModlogHandler.getInstance().handleAction(gw,
                            gw.getGuild().getTextChannelById(channelId),
                            null,
                            GuildUtils.getUser(String.valueOf(target), String.valueOf(guildId), true),
                            ModAction.UNMUTE,
                            "Temporary mute expired, was muted for " + FormatUtils.formatJodaTime(delay)
                    );
                } else
                    gw.getModeration().unmuteUser(gw, gw.getGuild().getMemberById(target));
                break;
            case TEMP_BAN:
                if (gw.getGuild().getTextChannelById(channelId) != null) {
                    ModlogHandler.getInstance().handleAction(gw,
                            gw.getGuild().getTextChannelById(channelId),
                            null,
                            GuildUtils.getUser(String.valueOf(target), String.valueOf(guildId), true),
                            ModAction.UNBAN,
                            "Temporary ban expired, was banned for " + FormatUtils.formatJodaTime(delay)
                    );
                } else
                    gw.getGuild().getController().unban(String.valueOf(target)).queue();
                break;
            case REMINDER:
                if (Getters.getChannelById(channelId) != null)
                    Getters.getChannelById(channelId).sendMessage(Getters
                            .getUserById(responsible).getAsMention() + " You asked me to remind you " +
                            FormatUtils.formatJodaTime(delay).toLowerCase() + " ago about: `" + content.replaceAll("`", "'") + "`")
                            .queue();
                break;
            case DM_REMINDER:
                MessageUtils.sendPM(Getters.getChannelById(channelId), Getters.getUserById(responsible), Getters.getUserById(responsible).getAsMention() + " You asked me to remind you " +
                        FormatUtils.formatJodaTime(delay).toLowerCase() + " ago about: `" + content.replaceAll("`", "'") + "`");
                break;
            default:
                break;
        }
        delete();
    }

    public void queue() {
        // I have to minus here since this has the complete end time.
        Scheduler.delayTask(this::execute, "FutureTask-" + action.name() + "-" + expires.toString(),
                getExpires().minus(System.currentTimeMillis()).getMillis());
        DatabaseManager.run(connection -> {
            PreparedStatement query = connection.prepareStatement("SELECT * FROM future_tasks WHERE guild_id = ? AND channel_id = ? AND created_at = ?");
            query.setLong(1, guildId);
            query.setLong(2, channelId);
            query.setTimestamp(3, new Timestamp(created.getMillis()));
            if (query.executeQuery().isBeforeFirst()) {
                PreparedStatement update = connection.prepareStatement("UPDATE future_tasks SET responsible = ?, " +
                        "target = ?, content = ?, expires_at = ?, action = ? WHERE guild_id = ? AND channel_id = ? " +
                        "AND created_at = ?");
                update.setLong(1, responsible);
                update.setLong(2, target);
                update.setString(3, content);
                update.setTimestamp(4, new Timestamp(expires.getMillis()));
                update.setString(5, action.name());
                update.setLong(6, guildId);
                update.setLong(7, channelId);
                update.setTimestamp(8, new Timestamp(created.getMillis()));
                update.executeUpdate();
            } else {
                PreparedStatement insert = connection.prepareStatement("INSERT INTO future_tasks (guild_id, channel_id, responsible, target, content, expires_at, created_at, action) VALUES (?,? ,?, ?,? ,? ,?, ?)");
                insert.setLong(1, guildId);
                insert.setLong(2, channelId);
                insert.setLong(3, responsible);
                insert.setLong(4, target);
                insert.setString(5, content);
                insert.setTimestamp(6, new Timestamp(expires.getMillis()));
                insert.setTimestamp(7, new Timestamp(created.getMillis()));
                insert.setString(8, action.name());
                insert.executeUpdate();
            }

        });
        FlareBot.instance().getFutureActions().add(this);
    }

    public void delete() {
        FlareBot.instance().getFutureActions().remove(this);
        DatabaseManager.run(connection -> {
            PreparedStatement delete = connection.prepareStatement("DELETE FROM future_tasks WHERE guild_id = ? " +
                    "AND channel_id = ? AND created_at = ?");
            delete.setLong(1, guildId);
            delete.setLong(2, channelId);
            delete.setTimestamp(3, new Timestamp(created.getMillis()));
            delete.execute();
        });
        Scheduler.cancelTask("FutureTask-" + action.name() + "-" + expires.toString());
    }

    public enum Action {
        TEMP_MUTE,
        TEMP_BAN,
        REMINDER,
        DM_REMINDER
    }
}
