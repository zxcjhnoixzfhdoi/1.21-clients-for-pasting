package we.devs.opium.client.commands;

import we.devs.opium.Opium;
import we.devs.opium.api.manager.command.Command;
import we.devs.opium.api.manager.command.RegisterCommand;
import we.devs.opium.api.utilities.ChatUtils;
import we.devs.opium.client.modules.client.ModuleCommands;
import net.minecraft.util.Formatting;

@RegisterCommand(name="Friend", description="Let's you add friends.", syntax="friend <add/del> <name>", aliases={"friend", "f"})
public class CommandFriend extends Command {
    @Override
    public void onCommand(String[] args) {
        if (args.length == 1) {
            ChatUtils.sendMessage("You have " + (Opium.FRIEND_MANAGER.getFriends().size() + 1) + " friends");
            return;
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add")) {
                if (Opium.FRIEND_MANAGER.isFriend(args[1])) {
                    ChatUtils.sendMessage(ModuleCommands.getSecondColor() + args[1] + ModuleCommands.getFirstColor() + " is already a friend!");
                    return;
                }
                if (!Opium.FRIEND_MANAGER.isFriend(args[1])) {
                    Opium.FRIEND_MANAGER.addFriend(args[1]);
                    ChatUtils.sendMessage(Formatting.GREEN + "Added " + ModuleCommands.getSecondColor() + args[1] + ModuleCommands.getFirstColor() + " to friends list");
                }
            }
            if (args[0].equalsIgnoreCase("del") || args[0].equalsIgnoreCase("remove")) {
                if (!Opium.FRIEND_MANAGER.isFriend(args[1])) {
                    ChatUtils.sendMessage(ModuleCommands.getSecondColor() + args[1] + ModuleCommands.getFirstColor() + " is not a friend!");
                    return;
                }
                if (Opium.FRIEND_MANAGER.isFriend(args[1])) {
                    Opium.FRIEND_MANAGER.removeFriend(args[1]);
                    ChatUtils.sendMessage(Formatting.RED + "Removed " + ModuleCommands.getSecondColor() + args[1] + ModuleCommands.getFirstColor() + " from friends list");
                }
            }
        } else {
            this.sendSyntax();
        }
    }
}