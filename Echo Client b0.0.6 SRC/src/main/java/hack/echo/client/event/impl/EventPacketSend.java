package hack.echo.client.event.impl;

import hack.echo.client.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.network.protocol.Packet;

@Getter
@AllArgsConstructor
public class EventPacketSend extends Event {
    private Packet<?> packet;
}
