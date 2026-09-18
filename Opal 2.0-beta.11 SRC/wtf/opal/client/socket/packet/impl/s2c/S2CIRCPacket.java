package wtf.opal.client.socket.packet.impl.s2c;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import wtf.opal.client.OpalClient;
import wtf.opal.client.feature.module.impl.utility.IRCModule;
import wtf.opal.client.socket.ClientSocket;
import wtf.opal.client.socket.packet.api.S2CPacket;
import wtf.opal.client.socket.packet.types.IRCPacketType;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.misc.chat.ChatUtility;
import wtf.opal.utility.socket.buffer.BufferReader;
import wtf.opal.utility.socket.user.ResolvedUser;
import wtf.opal.utility.socket.user.UserRole;

@NativeInclude
public final class S2CIRCPacket implements S2CPacket {

    private final int packetType;

    private ResolvedUser user;
    private String message;
    private ResolvedUser[] onlineUsers;

    public S2CIRCPacket(final BufferReader reader) throws Exception {
        this.packetType = reader.readInt();

        switch (this.packetType) {
            case IRCPacketType.BROADCAST,
                 IRCPacketType.WHISPER_SENT -> {
                this.user = new ResolvedUser(reader.readString(), UserRole.fromName(reader.readString()));
                this.message = reader.readString();
            }
            case IRCPacketType.WHISPER_RECEIVED -> {
                this.user = new ResolvedUser(reader.readString(), UserRole.fromName(reader.readString()));
                this.message = reader.readString();
                ClientSocket.getInstance().setLastReceivedWhisperUsername(this.user.getName());
            }
            case IRCPacketType.LIST_ONLINE -> {
                onlineUsers = new ResolvedUser[reader.readInt()];
                for (int i = 0; i < onlineUsers.length; i++) {
                    onlineUsers[i] = new ResolvedUser(reader.readString(), UserRole.fromName(reader.readString()));
                }
            }
        }
    }

    @Override
    public void handle() {
        switch (this.packetType) {
            case IRCPacketType.BROADCAST -> {
                if (OpalClient.getInstance().getModuleRepository().getModule(IRCModule.class).isEnabled()) {
                    ChatUtility.irc(0, this.user, this.message);
                }
            }
            case IRCPacketType.WHISPER_RECEIVED -> {
                ChatUtility.irc(1, this.user, this.message);
            }
            case IRCPacketType.WHISPER_SENT -> {
                ChatUtility.irc(2, this.user, this.message);
            }
            case IRCPacketType.LIST_ONLINE -> {
                final MutableText text = Text
                        .literal("§lOnline §r").formatted(Formatting.YELLOW)
                        .append(Text.literal("(" + onlineUsers.length + "): ").formatted(Formatting.GRAY));

                for (int i = 0; i < onlineUsers.length; i++) {
                    final ResolvedUser onlineUser = onlineUsers[i];
                    text.append(Text.literal(onlineUser.getName()).withColor(onlineUser.getRole().getColor().getRgb()));

                    if (i != onlineUsers.length - 1) {
                        text.append(Text.literal(", ").formatted(Formatting.GRAY));
                    }
                }

                ChatUtility.display(text);
            }
        }
    }

    @Override
    public int id() {
        return 6;
    }

}
