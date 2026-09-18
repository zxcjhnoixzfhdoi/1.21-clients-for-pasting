package hack.echo.client.features.impl.macros.donut;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventPacketReceive;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.utils.inventory.ContainerUtils;
import hack.echo.client.utils.strings.Concat;
import hack.echo.client.utils.strings.TextStyleUtil;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;

public class InstaCrate extends Feature {
    public InstaCrate() {
        super(new FeatureInfo(
                Concat.of("Instant Crate"),
                Concat.of("Instant crate claiming"),
                Category.MACROS,
                false
        ));
    }

    private final IntSetting containerSlot = new IntSetting(Concat.of("Container Slot"), 0, 0, 26);

    private int savedContainerId = -1;
    private int stage = 0; // 0 = nada, 1 = choose item, 2 = confirm container
    // adds 'natural' tick delay with how events work

    @EventSubscribe
    private void onTickEvent(EventTick e) {
        if (isNull() || savedContainerId == -1) return;

        var container = mc.player.containerMenu;
        if (container.containerId != savedContainerId) return;

        if (stage == 1) {
            ContainerUtils.pickup(containerSlot.getValue());
            savedContainerId = -1;
            stage = 2;
        } else if (stage == 3) {
            ContainerUtils.pickup(15);
            savedContainerId = -1;
            stage = 0;
        }
    }

    @EventSubscribe
    private void onPacketReceive(EventPacketReceive e) {
        if (isNull()) return;
        Packet<?> packet = e.getPacket();

        if (packet instanceof ClientboundOpenScreenPacket screenPacket) {
            String title = TextStyleUtil.translate(screenPacket.getTitle().getString());
            if (stage == 0 && title.contains("choose")) {
                savedContainerId = screenPacket.getContainerId();
                stage = 1;
            } else if (stage == 2) {
                savedContainerId = screenPacket.getContainerId();
                stage = 3;
            }
        }
    }
}
