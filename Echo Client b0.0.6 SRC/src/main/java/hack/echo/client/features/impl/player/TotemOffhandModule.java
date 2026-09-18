package hack.echo.client.features.impl.player;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.api.InventoryClickCompat;
import hack.echo.client.api.InventoryClickType;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.world.item.Items;

public class TotemOffhandModule extends Feature {

    private final BoolSetting mustHold = new BoolSetting(Concat.of("Must Hold"), true);
    private final BoolSetting hotbarOnly = new BoolSetting(Concat.of("Hotbar Only"), false);

    private int lastSwapTick = -1000;

    public TotemOffhandModule() {
        super(new FeatureInfo(
                Concat.of("Totem Offhand"),
                Concat.of("Equips a totem in your offhand"),
                Category.UTILITY
        ));
    }

    @Override
    public void onEnable() {
        super.onEnable();
        lastSwapTick = -1000;
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventSubscribe
    private void onTickPre(EventHandleInput.Early event) {
        if (isNull() || mc.gameMode == null) return;
        if (InventoryUtils.isOffhandHoldingItem(Items.TOTEM_OF_UNDYING)) return;

        int age = mc.player.tickCount;
        if ((age - lastSwapTick) < 2) return;

        if (mustHold.getValue()) {
            if (!InventoryUtils.isHoldingItem(Items.TOTEM_OF_UNDYING)) return;

            swapSlot(mc.player.getInventory().getSelectedSlot() + 36, age);
            return;
        }

        int hotbarSlot = InventoryUtils.findItemWithPredicateInHotbar(
                stack -> stack.getItem() == Items.TOTEM_OF_UNDYING
        );

        if (hotbarSlot != -1) {
            swapSlot(hotbarSlot + 36, age);
            return;
        }

        if (hotbarOnly.getValue()) return;

        int invSlot = InventoryUtils.findTotemSlot();
        if (invSlot != -1) {
            swapSlot(invSlot, age);
        }
    }

    private void swapSlot(int containerSlot, int tick) {
        InventoryClickCompat.handleClick(
            mc.player.containerMenu.containerId,
            containerSlot,
            40,
            InventoryClickType.SWAP,
            mc.player
        );
        lastSwapTick = tick;
    }
}
