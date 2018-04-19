package stream.flarebot.flarebot.commands.commands.secret;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import stream.flarebot.flarebot.commands.*;
import stream.flarebot.flarebot.objects.GuildWrapper;
import stream.flarebot.flarebot.permissions.Permission;

public class QueryCommand implements Command {

    @Override
    public void onCommand(User sender, GuildWrapper guild, TextChannel channel, Message message, String[] args, Member member) {
//        try {
//            DatabaseManager.run(connection -> {
//                ResultSet set = connection.prepareStatement(MessageUtils.getMessage(args, 0)).executeQuery();
//                List<String> header = new ArrayList<>();
//                List<List<String>> table = new ArrayList<>();
//                int columnsCount = set.().size();
//                for (int i = 0; i < columnsCount; i++) {
//                    header.add(set.getColumnDefinitions().getName(i));
//                }
//                for (Row setRow : set) {
//                    List<String> row = new ArrayList<>();
//                    for (int i = 0; i < columnsCount; i++) {
//                        String value = setRow.getObject(i).toString();
//                        row.add(value.substring(0, Math.min(30, value.length())));
//                    }
//                    table.add(row);
//                }
//                String output = MessageUtils.makeAsciiTable(header, table, null);
//                if (output.length() < 2000) {
//                    channel.sendMessage(output).queue();
//                } else {
//                    MessageUtils.sendErrorMessage("The query result set was very large, it has been posted to paste [here](" + MessageUtils
//                            .paste(output) + ")", channel, sender);
//                }
//            });
//        } catch (QueryExecutionException | QueryValidationException e) {
//            EmbedBuilder eb = new EmbedBuilder();
//            eb.setTitle("Failed to execute query");
//            eb.addField("Error", "```\n" + e.getMessage() + "\n```", false);
//            channel.sendMessage(eb.build()).queue();
//        }
    }

    @Override
    public String getCommand() {
        return "query";
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getUsage() {
        return "{%}query <sql>";
    }


    @Override
    public Permission getPermission() {
        return null;
    }

    @Override
    public CommandType getType() {
        return CommandType.SECRET;
    }

}
