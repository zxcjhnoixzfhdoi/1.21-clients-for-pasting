package hack.echo.client.utils.inventory;

import hack.echo.client.api.InventoryClickCompat;
import hack.echo.client.api.InventoryClickType;
import hack.echo.client.utils.Imports;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.item.Item;

public final class ContainerUtils implements Imports {

    public static void click(int slot, InventoryClickType clickType) {
        if (mc.player == null || mc.gameMode == null) return;
        var container = mc.player.containerMenu;
        InventoryClickCompat.handleClick(container.containerId, slot, 0, clickType, mc.player);
    }

    public static void clickPacket(int containerId, int slot, InventoryClickType clickType) {
        InventoryClickCompat.sendClickPacket(containerId, slot, 0, clickType);
    }

    public static void closeContainer(int containerId) {
        if (mc.getConnection() == null) return;
        mc.getConnection().send(new ServerboundContainerClosePacket(containerId));
    }

    public static void quickMove(int slot) {
        click(slot, InventoryClickType.QUICK_MOVE);
    }

    public static void pickup(int slot) {
        click(slot, InventoryClickType.PICKUP);
    }

    public static void quickMoveAll(Item item) {
        if (mc.player == null) return;
        var container = mc.player.containerMenu;
        for (int i = 0; i < container.slots.size(); i++) {
            if (container.slots.get(i).getItem().getItem() == item) {
                quickMove(i);
            }
        }
    }

    public static int getContainerSize() {
        if (mc.player == null) return 0;
        return mc.player.containerMenu.slots.size() - 36;
    }
}
