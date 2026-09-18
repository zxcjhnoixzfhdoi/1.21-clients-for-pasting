package wtf.opal.client.socket.packet.impl.s2c.config;

import net.minecraft.util.Formatting;
import wtf.opal.client.socket.packet.api.S2CPacket;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.data.SaveUtility;
import wtf.opal.utility.misc.chat.ChatUtility;
import wtf.opal.utility.socket.buffer.BufferReader;

@NativeInclude
public final class S2CConfigLoadPacket implements S2CPacket {

    private final String configName, configData;

    public S2CConfigLoadPacket(final BufferReader reader) throws Exception {
        this.configName = reader.readString();
        this.configData = reader.readString();
    }

    @Override
    public void handle() throws Exception {
        if (SaveUtility.loadConfig(configData)) {
            ChatUtility.success(Formatting.YELLOW + configName + Formatting.GRAY + " has been successfully loaded!");
        } else {
            ChatUtility.error("Your config could not be loaded.");
        }
    }

    @Override
    public int id() {
        return 8;
    }
}
