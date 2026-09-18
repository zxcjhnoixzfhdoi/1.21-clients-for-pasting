package hack.echo.client.command.impl;

import hack.echo.client.Echo;
import hack.echo.client.command.Command;
import hack.echo.client.utils.ChatUtils;
import hack.echo.client.utils.strings.Concat;

import java.util.List;

public class Config extends Command {

    public Config() {
        super("config", "Manage feature configurations.");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            showHelp();
            return;
        }

        switch (args[0].toLowerCase()) {
            case "save" -> {
                if (args.length < 2) {
                    ChatUtils.chat("usage: .config save <name>");
                    return;
                }
                boolean ok = Echo.featureConfig.saveProfile(args[1]);
                ChatUtils.chat(ok ? Concat.of("saved ", args[1]) : Concat.of("failed to save ", args[1]));
            }
            case "load" -> {
                if (args.length < 2) {
                    ChatUtils.chat("usage: .config load <name>");
                    return;
                }
                boolean ok = Echo.featureConfig.loadProfile(args[1]);
                ChatUtils.chat(ok ? Concat.of("loaded ", args[1]) : Concat.of(args[1], " not found"));
            }
            case "list" -> {
                List<String> profiles = Echo.featureConfig.listProfiles();
                ChatUtils.chat(profiles.isEmpty() ? "no profiles" : String.join(", ", profiles));
            }
            case "delete" -> {
                if (args.length < 2) {
                    ChatUtils.chat("usage: .config delete <name>");
                    return;
                }
                boolean ok = Echo.featureConfig.deleteProfile(args[1]);
                ChatUtils.chat(ok ? Concat.of("deleted ", args[1]) : Concat.of(args[1], " not found"));
            }
            case "help" -> showHelp();
            default -> ChatUtils.chat(Concat.of("unknown sub-command: ", args[0]));
        }
    }

    private void showHelp() {
        ChatUtils.chat(".config save <name>");
        ChatUtils.chat(".config load <name>");
        ChatUtils.chat(".config list");
        ChatUtils.chat(".config delete <name>");
    }
}
