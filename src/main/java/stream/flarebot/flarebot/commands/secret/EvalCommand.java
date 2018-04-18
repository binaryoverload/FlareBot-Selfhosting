package stream.flarebot.flarebot.commands.secret;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import stream.flarebot.flarebot.commands.*;
import stream.flarebot.flarebot.objects.GuildWrapper;
import stream.flarebot.flarebot.util.MessageUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EvalCommand implements InternalCommand {

    private ScriptEngineManager manager = new ScriptEngineManager();
    private static final ThreadGroup EVAL_POOL = new ThreadGroup("EvalCommand Thread Pool");
    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> new Thread(EVAL_POOL, r,
            EVAL_POOL.getName() + EVAL_POOL.activeCount()));
    private static final List<String> IMPORTS = Arrays.asList("stream.flarebot.flarebot",
            "stream.flarebot.flarebot.music",
            "stream.flarebot.flarebot.util",
            "stream.flarebot.flarebot.util.general",
            "stream.flarebot.flarebot.util.currency",
            "stream.flarebot.flarebot.util.objects",
            "stream.flarebot.flarebot.objects",
            "stream.flarebot.flarebot.web",
            "stream.flarebot.flarebot.mod",
            "stream.flarebot.flarebot.mod.modlog",
            "stream.flarebot.flarebot.mod.automod",
            "stream.flarebot.flarebot.mod.events",
            "stream.flarebot.flarebot.scheduler",
            "stream.flarebot.flarebot.database",
            "stream.flarebot.flarebot.permissions",
            "stream.flarebot.flarebot.commands",
            "stream.flarebot.flarebot.music.extractors",
            "net.dv8tion.jda.core",
            "net.dv8tion.jda.core.managers",
            "net.dv8tion.jda.core.entities.impl",
            "net.dv8tion.jda.core.entities",
            "net.dv8tion.jda.core.utils",
            "java.util.streams",
            "java.util",
            "java.lang",
            "java.text",
            "java.lang",
            "java.math",
            "java.time",
            "java.io",
            "java.nio",
            "java.nio.files",
            "java.util.stream",
            "org.json");

    @Override
    public void onCommand(User sender, GuildWrapper guild, TextChannel channel, Message message, String[] args, Member member) {
        if (args.length == 0) {
            channel.sendMessage("Eval something at least smh!").queue();
            return;
        }
        String imports = IMPORTS.stream().map(s -> "import " + s + ".*;").collect(Collectors.joining("\n"));
        ScriptEngine engine = manager.getEngineByName("groovy");
        engine.put("channel", channel);
        engine.put("guild", guild);
        engine.put("message", message);
        engine.put("jda", sender.getJDA());
        engine.put("sender", sender);

        String msg = MessageUtils.getMessage(args);
        final String[] code = {getCode(args)};

        boolean silent = hasOption(Options.SILENT, msg);

        if (code[0] == null)
            return;
        final String finalCode = code[0];

        POOL.submit(() -> {
            try {
                String eResult = String.valueOf(engine.eval(imports + '\n' + finalCode));
                if (eResult.length() > 2000) {
                    eResult = String.format("Eval too large, result pasted: %s", MessageUtils.paste(eResult));
                }
                if (!silent)
                    channel.sendMessage(eResult).queue();
            } catch (Exception e) {
                //FlareBot.LOGGER.error("Error occurred in the evaluator thread pool! " + e.getMessage(), e, Markers.NO_ANNOUNCE);
                channel.sendMessage(MessageUtils.getEmbed(sender)
                        .addField("Result: ", "```bf\n" + e.getMessage() + "```", false).build()).queue();
            }
        });
    }

    @Override
    public String getCommand() {
        return "eval";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public CommandType getType() {
        return CommandType.SECRET;
    }

    @Override
    public boolean deleteMessage() {
        return false;
    }

    enum Options {
        SILENT("s");

        private String key;

        Options(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public String getAsArgument() {
            return "-" + key;
        }
    }

    private boolean hasOption(Options option, String message) {
        return Pattern.compile("(.^ *)*-\\b" + option.getKey() + "\\b( \\w+)?(.^ )*").matcher(message).find();
    }

    private Pattern getRegex(Options option) {
        return Pattern.compile("(.^ )*-\\b" + option.getKey() + "\\b( \\w+)?(.^ )*");
    }

    private String getCode(String[] args) {
        String code = MessageUtils.getMessage(args);
        for (Options option : Options.values()) {
            if (hasOption(option, code)) {
                Matcher matcher = getRegex(option).matcher(code);
                code = matcher.replaceAll("");
            }
        }
        return code;
    }
}
