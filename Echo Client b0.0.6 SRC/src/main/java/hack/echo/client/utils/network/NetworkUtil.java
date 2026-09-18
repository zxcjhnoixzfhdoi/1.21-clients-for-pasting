package hack.echo.client.utils.network;

import net.minecraft.network.protocol.Packet;
import hack.echo.client.utils.Imports;


public final class NetworkUtil implements Imports {

    public static void send(Packet<?> packet) {
        if (mc.getConnection() != null) return;
        mc.getConnection().send(packet);
    }
}
