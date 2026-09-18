package we.devs.opium.client.commands;

import we.devs.opium.Opium;
import we.devs.opium.api.manager.command.Command;
import we.devs.opium.api.manager.command.RegisterCommand;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.utilities.ChatUtils;
import we.devs.opium.client.modules.client.ModuleCommands;
import net.minecraft.util.Formatting;

@RegisterCommand(name="notify", description="Let's you disable or enable module toggle messages.", syntax="notify <module> <value>", aliases={"chatnotify", "togglemsg", "togglemsgs", "togglemessages"})
public class CommandNotify extends Command {
    @Override
    public void onCommand(String[] args) {
        if (args.length == 2) {
            boolean found = false;
            for (Module module : Opium.MODULE_MANAGER.getModules()) {
                if (!module.getName().equalsIgnoreCase(args[0])) continue;
                found = true;
                module.setChatNotify(Boolean.parseBoolean(args[1]));
                ChatUtils.sendMessage(ModuleCommands.getSecondColor() + module.getName() + ModuleCommands.getFirstColor() + " now has Toggle Messages " + (module.isChatNotify() ? Formatting.GREEN + "enabled" : Formatting.RED + "disabled") + ModuleCommands.getFirstColor() + ".", "Notify");
            }
            if (!found) {
                ChatUtils.sendMessage("Could not find module.", "Notify");
            }
        } else {
            this.sendSyntax();
        }
    }
}