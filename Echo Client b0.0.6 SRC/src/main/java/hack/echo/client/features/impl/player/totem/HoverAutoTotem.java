package hack.echo.client.features.impl.player.totem;

import hack.echo.client.event.impl.EventTick;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.mixin.accessors.AbstractContainerScreenAccessor;
import hack.echo.client.api.InventoryClickCompat;
import hack.echo.client.api.InventoryClickType;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class HoverAutoTotem {
    private static final Minecraft mc = Minecraft.getInstance();

    public static final BoolSetting autoOpenHover = new BoolSetting(
        Concat.of("Auto Open"),
        false
    );
    public static final BoolSetting autoOffhandHover = new BoolSetting(
        Concat.of("Auto Offhand"),
        true
    );
    public static final BoolSetting autoHotbarSlotHover = new BoolSetting(
        Concat.of("Move to Hotbar"),
        false
    );
    public static final IntSetting hotbarSlotHover = new IntSetting(
        Concat.of("Hotbar Slot"),
        9, 1, 9
    );

    public HoverAutoTotem() {
    }

    public void onTick(EventTick e) {
        if (mc.player == null || mc.level == null) return;

        if (autoOpenHover.getValue()) {
            handleAutoOpen();
        }

        if (hack.echo.client.api.MinecraftCompat.getScreen() instanceof InventoryScreen inventoryScreen) {
            handleHoverTotem(inventoryScreen);
        } else if (hack.echo.client.api.MinecraftCompat.getScreen() instanceof CreativeModeInventoryScreen creativeScreen) {
            handleHoverTotemCreative(creativeScreen);
        }
    }

    private void handleAutoOpen() {
        if (hack.echo.client.api.MinecraftCompat.getScreen() instanceof InventoryScreen || hack.echo.client.api.MinecraftCompat.getScreen() instanceof CreativeModeInventoryScreen) return;

        int hbIdx = Math.max(0, Math.min(8, hotbarSlotHover.getValue() - 1));
        boolean offhandHas = mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
        var inventory = mc.player.getInventory();
        boolean hotbarHas = inventory.getItem(hbIdx).is(Items.TOTEM_OF_UNDYING);

        if (offhandHas && (!autoHotbarSlotHover.getValue() || hotbarHas)) return;

        boolean hasTotemInInventory = false;
        int containerSize = inventory.getContainerSize();
        for (int i = 0; i < containerSize; i++) {
            if (i == 40 || i == hbIdx) continue;
            if (inventory.getItem(i).is(Items.TOTEM_OF_UNDYING)) {
                hasTotemInInventory = true;
                break;
            }
        }

        if (hasTotemInInventory) {
            hack.echo.client.api.MinecraftCompat.setScreen(new InventoryScreen(mc.player));
        }
    }

    private void handleHoverTotem(InventoryScreen inventoryScreen) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) inventoryScreen;
        Slot focusedSlot = accessor.getHoveredSlot();

        if (focusedSlot == null) return;

        ItemStack stack = focusedSlot.getItem();
        if (!stack.is(Items.TOTEM_OF_UNDYING)) return;

        int slotIndex = getMenuSlotId(inventoryScreen.getMenu(), focusedSlot);
        if (slotIndex < 0) return;
        int containerSlot = focusedSlot.container instanceof Inventory ? focusedSlot.getContainerSlot() : -1;
        if (containerSlot == 40) return;
        int hbIdx = Math.max(0, Math.min(8, hotbarSlotHover.getValue() - 1));
        if (containerSlot == hbIdx && autoHotbarSlotHover.getValue()) return; // Already in target hotbar slot

        boolean needOffhand = autoOffhandHover.getValue() && !mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
        
        boolean needHotbar = autoHotbarSlotHover.getValue() && !mc.player.getInventory().getItem(hbIdx).is(Items.TOTEM_OF_UNDYING);

        if (!needOffhand && !needHotbar) return;

        if (mc.gameMode == null) return;

        if (needOffhand) {
            InventoryClickCompat.handleClick(
                inventoryScreen.getMenu().containerId,
                slotIndex,
                40,
                InventoryClickType.SWAP,
                mc.player
            );
        } else if (needHotbar) {
            InventoryClickCompat.handleClick(
                inventoryScreen.getMenu().containerId,
                slotIndex,
                hbIdx,
                InventoryClickType.SWAP,
                mc.player
            );
        }
    }

    private void handleHoverTotemCreative(CreativeModeInventoryScreen creativeScreen) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) creativeScreen;
        Slot focusedSlot = accessor.getHoveredSlot();

        if (focusedSlot == null) return;

        ItemStack stack = focusedSlot.getItem();
        if (!stack.is(Items.TOTEM_OF_UNDYING)) return;

        int slotIndex = getMenuSlotId(creativeScreen.getMenu(), focusedSlot);
        if (slotIndex < 0) return;
        int containerSlot = focusedSlot.container instanceof Inventory ? focusedSlot.getContainerSlot() : -1;
        if (containerSlot == 40) return; // Already in offhand

        int hbIdx = Math.max(0, Math.min(8, hotbarSlotHover.getValue() - 1));
        if (containerSlot == hbIdx && autoHotbarSlotHover.getValue()) return; // Already in target hotbar slot

        boolean needOffhand = autoOffhandHover.getValue() && !mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
        
        boolean needHotbar = autoHotbarSlotHover.getValue() && !mc.player.getInventory().getItem(hbIdx).is(Items.TOTEM_OF_UNDYING);

        if (!needOffhand && !needHotbar) return;

        if (mc.gameMode == null) return;

        if (needOffhand) {
            InventoryClickCompat.handleClick(
                creativeScreen.getMenu().containerId,
                slotIndex,
                40,
                InventoryClickType.SWAP,
                mc.player
            );
        } else if (needHotbar) {
            InventoryClickCompat.handleClick(
                creativeScreen.getMenu().containerId,
                slotIndex,
                hbIdx,
                InventoryClickType.SWAP,
                mc.player
            );
        }
    }

    private int getMenuSlotId(AbstractContainerMenu menu, Slot slot) {
        return menu.slots.indexOf(slot);
    }
}
