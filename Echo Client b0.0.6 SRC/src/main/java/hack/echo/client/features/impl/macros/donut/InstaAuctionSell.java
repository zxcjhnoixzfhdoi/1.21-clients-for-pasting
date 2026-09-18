package hack.echo.client.features.impl.macros.donut;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventPacketReceive;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.ItemPickerSetting;
import hack.echo.client.utils.inventory.ContainerUtils;
import hack.echo.client.utils.strings.Concat;
import hack.echo.client.utils.strings.TextStyleUtil;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;

public class InstaAuctionSell extends Feature {
    public InstaAuctionSell() {
        super(new FeatureInfo(
                Concat.of("Instant Auction"),
                Concat.of("Instantly confirms auction listing"),
                Category.MACROS,
                false
        ));
    }

    private final ItemPickerSetting whitelistedItems = new ItemPickerSetting(Concat.of("Whitelist"));

    private int savedContainerId = -1;


    @EventSubscribe
    private void onTick(EventTick e) {
        if (mc.player == null || savedContainerId == -1) return;

        var container = mc.player.containerMenu;
        if (container.containerId == savedContainerId) {
            System.out.println(container.containerId);
            var item = container.slots.get(13).getItem();
            if (!item.isEmpty() && whitelistedItems.isSelected(item.getItem())) {
                hack.echo.client.api.MinecraftCompat.setScreen(null);
                ContainerUtils.pickup(15);
                ContainerUtils.closeContainer(savedContainerId);
            }
            savedContainerId = -1;
        }
    }

    @EventSubscribe
    private void onPacketReceive(EventPacketReceive e) {
        Packet<?> packet = e.getPacket();

        if (packet instanceof ClientboundOpenScreenPacket screenPacket) {
            String title = TextStyleUtil.translate(screenPacket.getTitle().getString());
            if (title.contains("confirm listing")) {
                savedContainerId = screenPacket.getContainerId();
            }
        }
    }
}
