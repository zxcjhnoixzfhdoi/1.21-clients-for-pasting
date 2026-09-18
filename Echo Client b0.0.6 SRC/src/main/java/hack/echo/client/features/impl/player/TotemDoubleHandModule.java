package hack.echo.client.features.impl.player;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.event.impl.EventPacketReceive;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

public class TotemDoubleHandModule extends Feature {

    public TotemDoubleHandModule() {
        super(new FeatureInfo(
            Concat.of("Totem Hand"),
            Concat.of("Totem Double Hand"),
            Category.UTILITY
            )
        );
    }

    private final BoolSetting onPopBoolean = new BoolSetting("On pop", true);

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    private int getTotemSlot () {
        return InventoryUtils.findItemWithPredicateInHotbar(itemStack -> itemStack.getItem() == Items.TOTEM_OF_UNDYING);
    }

    @EventSubscribe
    private void onPacketRecieve(EventPacketReceive event) {
        if (isNull()) return;
        if (!onPopBoolean.getValue()) return;

        if (event.getPacket() instanceof ClientboundEntityEventPacket packet) {
            if (packet.getEventId() == EntityEvent.PROTECTED_FROM_DEATH) {
                Entity entity = packet.getEntity(mc.level);
                if (!(entity instanceof Player)) return;
                if (entity != mc.player) return;
                int totemSlot = getTotemSlot();
                if (totemSlot != -1) {
                    InventoryUtils.setInvSlot(totemSlot);
                }
            }
        }
    }

    @EventSubscribe
    public void onTickPre(EventHandleInput.Early event) {
        if (isNull()) return;
    }

    @EventSubscribe
    public void onTickPost(EventHandleInput.Post event) {
        if (isNull()) return;
    }

}
