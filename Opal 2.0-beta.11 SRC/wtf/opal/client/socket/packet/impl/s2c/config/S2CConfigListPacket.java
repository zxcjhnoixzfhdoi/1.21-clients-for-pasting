package wtf.opal.client.socket.packet.impl.s2c.config;

import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import wtf.opal.client.socket.ClientSocket;
import wtf.opal.client.socket.packet.api.S2CPacket;
import wtf.opal.client.socket.packet.impl.c2s.config.C2SConfigLoadPacket;
import wtf.opal.client.socket.packet.types.ConfigListRequestType;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.data.Config;
import wtf.opal.utility.misc.RunnableClickEvent;
import wtf.opal.utility.misc.chat.ChatUtility;
import wtf.opal.utility.socket.buffer.BufferReader;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@NativeInclude
public final class S2CConfigListPacket implements S2CPacket {

    private final int packetType;

    private final int configAmount;
    private final List<Config> configList;

    public S2CConfigListPacket(final BufferReader reader) throws Exception {
        this.packetType = reader.readInt();

        this.configList = new ArrayList<>();

        this.configAmount = reader.readInt();

        for (int i = 0; i < configAmount; i++) {
            if (packetType == ConfigListRequestType.CHAT) {
                final String configName = reader.readString();

                final String description = reader.readString();
                final boolean pinned = reader.readBoolean();
                final Date date = new Date(reader.readLong());

                configList.add(new Config(configName, description, pinned, date));
            } else if (packetType == ConfigListRequestType.SUGGESTION) {
                final String configName = reader.readString();
                configList.add(new Config(configName));
            }
        }
    }

    @Override
    public void handle() throws Exception {
        ClientSocket.getInstance().getConfigCache().setConfigs(configList);

        if (packetType == ConfigListRequestType.CHAT) {
            final MutableText text = Text.literal("§lConfigs §r").formatted(Formatting.YELLOW)
                    .append(Text.literal("(" + configAmount + "): ").formatted(Formatting.GRAY));

            final String pinnedStar = " " + Formatting.GOLD + "⭐";

            configList.forEach(config -> {
                final HoverEvent hoverEvent = new HoverEvent.ShowText(
                        Text.literal(Formatting.YELLOW + config.getName() + (config.isPinned() ? pinnedStar : ""))
                                .append("\n")
                                .append(config.getDescription()).formatted(Formatting.GRAY)
                                .append("\n")
                                .append(Formatting.GRAY + "Last updated " + Formatting.AQUA + DateFormat.getDateTimeInstance().format(config.getUpdatedAt()))
                                .append("\n\n")
                                .append(Formatting.GREEN + "" + Formatting.UNDERLINE + Formatting.ITALIC + "Click to load this config!"));

                final RunnableClickEvent runnableClickEvent = new RunnableClickEvent(() -> ClientSocket.getInstance().sendPacket(new C2SConfigLoadPacket(config.getName())));

                text.append("\n")
                        .append(Text.literal(Formatting.GRAY + " • " + config.getName() + (config.isPinned() ? pinnedStar : ""))
                                .styled(style -> style.withHoverEvent(hoverEvent))
                                .styled(style -> style.withClickEvent(runnableClickEvent)));
            });

            ChatUtility.display(text);
        }
    }

    @Override
    public int id() {
        return 7;
    }

}
