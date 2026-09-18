package wtf.opal.client.command.impl.config;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import wtf.opal.client.command.Command;
import wtf.opal.client.command.arguments.ConfigArgumentType;
import wtf.opal.client.socket.ClientSocket;
import wtf.opal.client.socket.packet.impl.c2s.config.C2SConfigDeletePacket;
import wtf.opal.client.socket.packet.impl.c2s.config.C2SConfigListPacket;
import wtf.opal.client.socket.packet.impl.c2s.config.C2SConfigLoadPacket;
import wtf.opal.client.socket.packet.types.ConfigListRequestType;
import wtf.opal.utility.data.SaveUtility;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class ConfigCommand extends Command {

    public ConfigCommand() {
        super(ClientSocket.getInstance().getVariableCache().getString("Failed to initialize repository:"), "Interacts with configs.", "c");
    }

    @Override
    protected void onCommand(final LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("save").then(argument("config_name", ConfigArgumentType.create()).executes(context -> {
            final String configName = context.getArgument("config_name", String.class).toLowerCase();

            SaveUtility.saveConfig(configName);

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("list").executes(context -> {
            ClientSocket.getInstance().sendPacket(new C2SConfigListPacket(ConfigListRequestType.CHAT));

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("load").then(argument("config_name", ConfigArgumentType.create()).executes(context -> {
            final String configName = context.getArgument("config_name", String.class).toLowerCase();

            ClientSocket.getInstance().sendPacket(new C2SConfigLoadPacket(configName));

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("delete").then(argument("config_name", ConfigArgumentType.create()).executes(context -> {
            final String configName = context.getArgument("config_name", String.class).toLowerCase();

            ClientSocket.getInstance().sendPacket(new C2SConfigDeletePacket(configName));

            return SINGLE_SUCCESS;
        })));
    }
}
