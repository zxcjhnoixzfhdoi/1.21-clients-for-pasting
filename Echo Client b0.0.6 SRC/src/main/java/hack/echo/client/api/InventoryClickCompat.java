package hack.echo.client.api;

import hack.echo.client.utils.Imports;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.entity.player.Player;
//? if >=26.1 {
/*import net.minecraft.world.inventory.ContainerInput;
*///?} else {
import net.minecraft.world.inventory.ClickType;
//?}

public final class InventoryClickCompat implements Imports {

    private InventoryClickCompat() {
    }

    public static void handleClick(int containerId, int slot, int button, InventoryClickType clickType) {
        if (mc.player == null) return;
        handleClick(containerId, slot, button, clickType, mc.player);
    }

    public static void handleClick(int containerId, int slot, int button, InventoryClickType clickType, Player player) {
        if (player == null) return;
        if (mc.gameMode == null) return;

        //? if >=26.1 {
        /*mc.gameMode.handleContainerInput(containerId, slot, button, toContainerInput(clickType), player);
        *///?} else {
        mc.gameMode.handleInventoryMouseClick(containerId, slot, button, toClickType(clickType), player);
        //?}
    }

    public static void sendClickPacket(int containerId, int slot, int button, InventoryClickType clickType) {
        if (mc.getConnection() == null) return;

        //? if >=26.1 {
        /*mc.getConnection().send(new ServerboundContainerClickPacket(
            containerId,
            0,
            (short) slot,
            (byte) button,
            toContainerInput(clickType),
            Int2ObjectMaps.emptyMap(),
            HashedStack.EMPTY
        ));
        *///?} else {
        mc.getConnection().send(new ServerboundContainerClickPacket(
            containerId,
            0,
            (short) slot,
            (byte) button,
            toClickType(clickType),
            Int2ObjectMaps.emptyMap(),
            HashedStack.EMPTY
        ));
        //?}
    }

    public static InventoryClickType fromVanilla(Object clickType) {
        if (!(clickType instanceof Enum<?> enumValue)) return null;

        try {
            return InventoryClickType.valueOf(enumValue.name());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static String describePacketClickType(ServerboundContainerClickPacket clickPacket) {
        //? if >=26.1 {
        /*return clickPacket.containerInput().name();
        *///?} else {
        return clickPacket.clickType().name();
        //?}
    }

    //? if >=26.1 {
    /*private static ContainerInput toContainerInput(InventoryClickType clickType) {
        return switch (clickType) {
            case PICKUP -> ContainerInput.PICKUP;
            case QUICK_MOVE -> ContainerInput.QUICK_MOVE;
            case SWAP -> ContainerInput.SWAP;
            case CLONE -> ContainerInput.CLONE;
            case THROW -> ContainerInput.THROW;
            case QUICK_CRAFT -> ContainerInput.QUICK_CRAFT;
            case PICKUP_ALL -> ContainerInput.PICKUP_ALL;
        };
    }
    *///?} else {
    private static ClickType toClickType(InventoryClickType clickType) {
        return switch (clickType) {
            case PICKUP -> ClickType.PICKUP;
            case QUICK_MOVE -> ClickType.QUICK_MOVE;
            case SWAP -> ClickType.SWAP;
            case CLONE -> ClickType.CLONE;
            case THROW -> ClickType.THROW;
            case QUICK_CRAFT -> ClickType.QUICK_CRAFT;
            case PICKUP_ALL -> ClickType.PICKUP_ALL;
        };
    }
    //?}
}
