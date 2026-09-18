package hack.echo.client.features.impl.misc;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventPacketReceive;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.api.ChatCompat;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.chat.Component;

public class Transactions extends Feature {

    public Transactions() {
        super(new FeatureInfo(
            Concat.of("Transactions"),
            Concat.of("Transaction packet logging. Useful for figuring out the anticheat of a server"),
            Category.INTERNALS
        ));
    }

    @EventSubscribe
    private void onPacket(EventPacketReceive event) {
        if (event.getPacket() instanceof ClientboundPingPacket) {
            mc.execute(() -> ChatCompat.addMessage(Component.nullToEmpty(
                    Concat.of("Transaction Packet: ", Concat.ofInt(((ClientboundPingPacket) event.getPacket()).getId())).toString()
            )));
//            System.out.println("Transaction Packet: " + event.getPacket());
        }
    }
}
