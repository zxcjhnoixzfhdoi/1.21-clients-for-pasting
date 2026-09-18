package we.devs.opium.client.commands;

import we.devs.opium.Opium;
import we.devs.opium.api.manager.command.Command;
import we.devs.opium.api.manager.command.RegisterCommand;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.utilities.ChatUtils;
import we.devs.opium.client.modules.client.ModuleCommands;
import net.minecraft.util.Formatting;

@RegisterCommand(name="toggle", description="Let's you toggle a module by name.", syntax="toggle <module>", aliases={"t"})
public class CommandToggle extends Command {
    @Override
    public void onCommand(String[] args) {
        if (args.length == 1) {
            boolean found = false;
            for (Module module : Opium.MODULE_MANAGER.getModules()) {
                if (!module.getName().equalsIgnoreCase(args[0])) continue;
                module.toggle(false);
                ChatUtils.sendMessage(ModuleCommands.getSecondColor() + "" + Formatting.BOLD + module.getTag() + ModuleCommands.getFirstColor() + (module.isToggled() ? Formatting.GREEN + "ON" : Formatting.RED + "OFF") + ModuleCommands.getFirstColor(), "Toggle");
                found = true;
                break;
            }
            if (!found) {
                ChatUtils.sendMessage("Could not find module.", "Toggle");
            }
        } else {
            this.sendSyntax();
        }
    }
}
