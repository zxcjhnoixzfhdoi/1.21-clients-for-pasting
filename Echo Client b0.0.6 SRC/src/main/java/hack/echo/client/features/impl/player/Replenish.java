package hack.echo.client.features.impl.player;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.event.impl.MouseUpdateEvent;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.FloatSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.api.InventoryClickCompat;
import hack.echo.client.api.InventoryClickType;
import hack.echo.client.utils.MathUtil;
import hack.echo.client.utils.strings.Concat;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public class Replenish extends Feature {

    private final FloatSetting minPercent = new FloatSetting(
            Concat.of("Min %"), 25f, 0f, 100f, 1f
    );

    private final IntSetting cursorSpeed = new IntSetting(
            Concat.of("Cursor Speed"), 20, 1, 100
    );

    private boolean isAnimating = false;
    private long animStartTime = 0L;
    private long animDuration = 200L;
    private double startX = 0.0;
    private double startY = 0.0;
    private double endX = 0.0;
    private double endY = 0.0;
    private int targetSlot = -1;

    public Replenish() {
        super(new FeatureInfo(
                Concat.of("Replenish"),
                Concat.of("Automatically replenishes items"),
                Category.UTILITY
        ));
    }

    @EventSubscribe
    private void onTick(EventHandleInput.Early event) {
        if (isNull()) return;
        if (!(hack.echo.client.api.MinecraftCompat.getScreen() instanceof InventoryScreen)) return;

        for (int hotbar = 0; hotbar < 9; hotbar++) {
            ItemStack stack = mc.player.getInventory().getItem(hotbar);
            if (stack.isEmpty() || !stack.isStackable()) continue;

            float pct = (stack.getCount() / (float) stack.getMaxStackSize()) * 100f;

            if (stack.getCount() == 1 || pct <= Math.max(minPercent.getValue(), 5f)) {
                mergeStack(stack);
            }
        }
    }

    private void mergeStack(ItemStack target) {

        for (int invSlot = 9; invSlot < 36; invSlot++) {
            ItemStack stack = mc.player.getInventory().getItem(invSlot);
            if (stack.isEmpty()) continue;
            if (!canMerge(target, stack)) continue;

            int invSlotIndex = getSlotIndex(invSlot);

            startAnimation(invSlot);
            quickMoveItem(invSlotIndex);

            return;
        }
    }

    private boolean canMerge(ItemStack a, ItemStack b) {
        if (!a.getItem().equals(b.getItem())) return false;

        if (a.getItem() instanceof BlockItem ba && b.getItem() instanceof BlockItem bb) {
            return ba.getBlock() == bb.getBlock();
        }

        return true;
    }

    private int getSlotIndex(int index) {
        return mc.player.containerMenu.slots.get(index).getContainerSlot();
    }

    private void quickMoveItem(int slot) {
        InventoryClickCompat.handleClick(
            mc.player.containerMenu.containerId,
            slot,
            0,
            InventoryClickType.QUICK_MOVE,
            mc.player
        );
    }

    private void startAnimation(int targetSlotIndex) {
        if (hack.echo.client.api.MinecraftCompat.getScreen() == null) return;

        AbstractContainerMenu handler = ((InventoryScreen) hack.echo.client.api.MinecraftCompat.getScreen()).getMenu();
        List<Slot> slots = handler.slots;
        Window window = mc.getWindow();
        double mouseX = mc.mouseHandler.xpos();
        double mouseY = mc.mouseHandler.ypos();

        int scaledWidth = window.getGuiScaledWidth();
        int scaledHeight = window.getGuiScaledHeight();
        int screenX = (scaledWidth - 176) / 2;
        int screenY = (scaledHeight - 166) / 2;

        Slot targetSlotObj = slots.get(targetSlotIndex);
        double targetX = screenX + targetSlotObj.x + 8.0;
        double targetY = screenY + targetSlotObj.y + 8.0;

        startX = mouseX;
        startY = mouseY;

        animDuration = 200 - (cursorSpeed.getValue() * 2L);

        endX = targetX;
        endY = targetY;

        animStartTime = System.currentTimeMillis();
        isAnimating = true;
        targetSlot = targetSlotIndex;
    }

    @EventSubscribe
    private void onRender2D(MouseUpdateEvent e) {
        if (!isAnimating) return;
        if (mc == null || hack.echo.client.api.MinecraftCompat.getScreen() == null) {
            isAnimating = false;
            return;
        }
        if (!(hack.echo.client.api.MinecraftCompat.getScreen() instanceof InventoryScreen)) {
            isAnimating = false;
            return;
        }

        long elapsed = System.currentTimeMillis() - animStartTime;
        double t = Math.max(0.0, Math.min(1.0, (double) elapsed / (double) animDuration));
        double eased = MathUtil.smoothStepLerp(t, 0.0, 1.0);
        double x = startX + (endX - startX) * eased;
        double y = startY + (endY - startY) * eased;

        Window window = mc.getWindow();

        double scaledX = (x * window.getScreenWidth()) / window.getGuiScaledWidth();
        double scaledY = (y * window.getScreenHeight()) / window.getGuiScaledHeight();

        InputConstants.grabOrReleaseMouse(window, InputConstants.CURSOR_NORMAL, scaledX, scaledY);

        if (t >= 1.0) {
            isAnimating = false;
            if (targetSlot >= 0) {
                quickMoveItem(targetSlot);
                targetSlot = -1;
            }
        }
    }
}
