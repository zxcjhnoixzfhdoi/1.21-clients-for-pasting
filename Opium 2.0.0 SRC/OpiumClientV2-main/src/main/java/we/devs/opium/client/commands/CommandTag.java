package we.devs.opium.client.commands;

import we.devs.opium.Opium;
import we.devs.opium.api.manager.command.Command;
import we.devs.opium.api.manager.command.RegisterCommand;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.utilities.ChatUtils;
import we.devs.opium.client.modules.client.ModuleCommands;

@RegisterCommand(name="tag", description="Let's you customize any module's tag.", syntax="tag <module> <value>", aliases={"customname", "modtag", "moduletag", "mark"})
public class CommandTag extends Command {
    @Override
    public void onCommand(String[] args) {
        if (args.length == 2) {
            boolean found = false;
            for (Module module : Opium.MODULE_MANAGER.getModules()) {
                if (!module.getName().equalsIgnoreCase(args[0])) continue;
                found = true;
                module.setTag(args[1].replace("_", " "));
                ChatUtils.sendMessage(ModuleCommands.getSecondColor() + module.getName() + ModuleCommands.getFirstColor() + " is now marked as " + ModuleCommands.getSecondColor() + module.getTag() + ModuleCommands.getFirstColor() + ".", "Tag");
            }
            if (!found) {
                ChatUtils.sendMessage("Could not find module.", "Tag");
            }
        } else {
            this.sendSyntax();
        }
    }
}