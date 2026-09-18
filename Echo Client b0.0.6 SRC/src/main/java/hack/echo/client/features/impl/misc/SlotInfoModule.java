package hack.echo.client.features.impl.misc;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventPacketSend;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.api.InventoryClickCompat;
import hack.echo.client.utils.ChatUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;

public class SlotInfoModule extends Feature {
    public SlotInfoModule() {
        super(new FeatureInfo(Concat.of("Debug slots"), Concat.of("test"), Category.INTERNALS));
    }

    @EventSubscribe
    private void onPacketSend(EventPacketSend e) {
        Packet<?> packet = e.getPacket();

        if (packet instanceof ServerboundContainerClickPacket clickPacket) {
            ChatUtils.chat(Concat.of(
                    "Slot clicked: ", Concat.ofInt(clickPacket.slotNum()),
                    " | Container: ", Concat.ofInt(clickPacket.containerId()),
                    " | ClickType: ", InventoryClickCompat.describePacketClickType(clickPacket),
                    " | Button: ", Concat.ofInt(clickPacket.buttonNum())
            ));
        }
    }
}
