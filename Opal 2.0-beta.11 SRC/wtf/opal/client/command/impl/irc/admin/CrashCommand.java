package wtf.opal.client.command.impl.irc.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import wtf.opal.client.command.Command;
import wtf.opal.client.socket.ClientSocket;
import wtf.opal.client.socket.packet.impl.c2s.C2SCrashPacket;
import wtf.opal.utility.misc.chat.ChatUtility;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class CrashCommand extends Command {

    public CrashCommand() {
        super("crash", "Crashes the specified user.");
    }

    @Override
    protected void onCommand(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("user", StringArgumentType.word()).executes(context -> {
            if (ClientSocket.getInstance().isAuthenticated()) {
                final String user = context.getArgument("user", String.class);
                ClientSocket.getInstance().sendPacket(new C2SCrashPacket(user));
            } else {
                ChatUtility.error("You are not connected to the IRC server!");
            }
            return SINGLE_SUCCESS;
        }));
    }
}
