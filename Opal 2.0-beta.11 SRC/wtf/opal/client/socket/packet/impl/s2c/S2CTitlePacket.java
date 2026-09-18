package wtf.opal.client.socket.packet.impl.s2c;

import net.minecraft.text.Text;
import wtf.opal.client.socket.packet.api.S2CPacket;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.misc.chat.ChatUtility;
import wtf.opal.utility.socket.buffer.BufferReader;

import static wtf.opal.client.Constants.mc;

@NativeInclude
public final class S2CTitlePacket implements S2CPacket {

    private final String message;
    private final int fadeInTicks, stayTicks, fadeOutTicks;

    public S2CTitlePacket(final BufferReader reader) throws Exception {
        this.message = reader.readString();
        this.fadeInTicks = reader.readInt();
        this.stayTicks = reader.readInt();
        this.fadeOutTicks = reader.readInt();
    }

    @Override
    public void handle() throws Exception {
        mc.inGameHud.setTitleTicks(fadeInTicks, stayTicks, fadeOutTicks);
        mc.inGameHud.setTitle(ChatUtility.translateAlternateColorCodes(message));
    }

    @Override
    public int id() {
        return 13;
    }
}
