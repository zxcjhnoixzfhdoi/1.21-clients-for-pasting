package hack.echo.client.features.impl.player.totem;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import hack.echo.client.event.impl.EventSetScreen;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.event.impl.MouseUpdateEvent;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.features.settings.impl.RangeSetting;
import hack.echo.client.api.InventoryClickCompat;
import hack.echo.client.api.InventoryClickType;
import hack.echo.client.utils.Imports;
import hack.echo.client.utils.MathUtil;
import hack.echo.client.utils.inventory.CursorUtil;
import hack.echo.client.utils.inventory.CursorUtil.ScreenInfo;
import hack.echo.client.utils.inventory.CursorUtil.SlotResult;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.Items;

public class CursorModeTotem implements Imports {

    private enum SwapPhase { NONE, OFFHAND, HOTBAR }

    // --- Settings ---

    public static final BoolSetting autoOpenCursor = new BoolSetting(Concat.of("Auto Open"), false);
    public static final BoolSetting autoCloseCursor = new BoolSetting(Concat.of("Auto Close"), true);
    public static final BoolSetting autoSwapCursor = new BoolSetting(Concat.of("Auto Offhand"), true);
    public static final BoolSetting autoHotbarSlotCursor = new BoolSetting(Concat.of("Move to Hotbar"), false);
    public static final IntSetting hotbarSlotCursor = new IntSetting(Concat.of("Hotbar Slot"), 9, 1, 9);
    public static final RangeSetting cursorTimeMS = new RangeSetting(Concat.of("Cursor Time"), 50, 250, 25, 500, 1);

    // --- Animation state ---

    private boolean animating = false;
    private long animStartMs = 0L;
    private long animDurationMs = 200L;
    private double startX, startY, endX, endY;
    private int targetSlotId = -1;
    private boolean shouldSwap = false;
    private SwapPhase pendingSwap = SwapPhase.NONE;
    private int pendingHotbarIndex = -1;

    // --- Event handlers ---

    public void onTick(EventTick e) {
        if (mc.player == null || mc.level == null) return;
        if (mc.player.isDeadOrDying()) {
            resetAllState();
            return;
        }

        if (shouldSwap && targetSlotId >= 0 && hack.echo.client.api.MinecraftCompat.getScreen() != null) {
            executeSwap();
            return;
        }

        tryRefillTotem();
    }

    public void onScreenOpen(EventSetScreen e) {
        if (!CursorUtil.isInventoryScreen(e.getScreen())) return;
        if (mc.player == null || mc.player.isCreative() || mc.player.isDeadOrDying()) return;

        int hbIdx = hotbarIndex();
        boolean needOffhand = autoSwapCursor.getValue() && !mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
        boolean needHotbar = autoHotbarSlotCursor.getValue() && !mc.player.getInventory().getItem(hbIdx).is(Items.TOTEM_OF_UNDYING);
        if (!needOffhand && !needHotbar) return;

        SwapPhase phase = needOffhand ? SwapPhase.OFFHAND : SwapPhase.HOTBAR;

        mc.execute(() -> {
            mc.mouseHandler.releaseMouse();
            startAnimationToNearestTotem(e.getScreen(), phase, hbIdx);
        });
    }

    public void onRender2D(MouseUpdateEvent e) {
        if (!animating) return;
        if (mc.player == null || mc.player.isDeadOrDying() || !CursorUtil.isInventoryScreen(hack.echo.client.api.MinecraftCompat.getScreen())) {
            resetAllState();
            return;
        }

        long elapsed = System.currentTimeMillis() - animStartMs;
        double t = Math.min(1.0, Math.max(0.0, (double) elapsed / animDurationMs));
        double eased = MathUtil.smoothStepLerp(t, 0.0, 1.0);

        double x = startX + (endX - startX) * eased;
        double y = startY + (endY - startY) * eased;
        InputConstants.grabOrReleaseMouse(mc.getWindow(), InputConstants.CURSOR_NORMAL, x, y);

        if (t < 1.0) return;

        animating = false;
        boolean actionEnabled = (pendingSwap == SwapPhase.OFFHAND && autoSwapCursor.getValue())
                || (pendingSwap == SwapPhase.HOTBAR && autoHotbarSlotCursor.getValue());

        if (actionEnabled && targetSlotId >= 0) {
            shouldSwap = true;
        } else {
            targetSlotId = -1;
            pendingSwap = SwapPhase.NONE;
        }
    }

    // --- Private helpers ---

    private void executeSwap() {
        ScreenInfo info = CursorUtil.getScreenInfo(hack.echo.client.api.MinecraftCompat.getScreen());
        if (info == null || mc.gameMode == null) {
            resetSwapState();
            return;
        }

        // Totem was consumed
        var slot = info.handler().slots.get(targetSlotId);
        if (!slot.getItem().is(Items.TOTEM_OF_UNDYING)) {
            resetSwapState();
            return;
        }

        int swapButton = (pendingSwap == SwapPhase.OFFHAND) ? 40 : pendingHotbarIndex;
        InventoryClickCompat.handleClick(
            info.handler().containerId,
            targetSlotId,
            swapButton,
            InventoryClickType.SWAP,
            mc.player
        );

        // chain a hotbar swap
        int hbIdx = hotbarIndex();
        boolean needsHotbar = autoHotbarSlotCursor.getValue()
                && !mc.player.getInventory().getItem(hbIdx).is(Items.TOTEM_OF_UNDYING);
        if (pendingSwap == SwapPhase.OFFHAND && needsHotbar) {
            if (startAnimationToNearestTotem(hack.echo.client.api.MinecraftCompat.getScreen(), SwapPhase.HOTBAR, hbIdx)) {
                shouldSwap = false;
                return;
            }
        }

        if (autoCloseCursor.getValue()) {
            mc.player.closeContainer();
        }
        resetSwapState();
    }

    private void tryRefillTotem() {
        int hbIdx = hotbarIndex();
        boolean offhandHas = mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
        boolean hotbarHas = mc.player.getInventory().getItem(hbIdx).is(Items.TOTEM_OF_UNDYING);

        if (offhandHas && (!autoHotbarSlotCursor.getValue() || hotbarHas)) return;
        if (animating) return;
        if (!hasTotemInInventory(hbIdx)) return;

        boolean needOffhand = autoSwapCursor.getValue() && !offhandHas;
        SwapPhase phase = needOffhand ? SwapPhase.OFFHAND : SwapPhase.HOTBAR;

        // Inventory already open - start animation directly on current screen
        if (CursorUtil.isInventoryScreen(hack.echo.client.api.MinecraftCompat.getScreen())) {
            startAnimationToNearestTotem(hack.echo.client.api.MinecraftCompat.getScreen(), phase, hbIdx);
            return;
        }

        /**
         * Only auto open inventory if the setting is enabled
         *  This fixes the issue krin mentioned
         */
        if (!autoOpenCursor.getValue()) return;
        hack.echo.client.api.MinecraftCompat.setScreen(new InventoryScreen(mc.player));
    }

    private boolean startAnimationToNearestTotem(Object screen, SwapPhase phase, int hbIdx) {
        ScreenInfo info = CursorUtil.getScreenInfo(screen);
        if (info == null) return false;

        Window window = mc.getWindow();
        int[] origin = CursorUtil.getScreenOrigin(info, window);
        double mouseX = mc.mouseHandler.xpos();
        double mouseY = mc.mouseHandler.ypos();
        boolean isCreative = screen instanceof CreativeModeInventoryScreen;

        double maxDist = CursorUtil.maxSlotDistance(info.handler().slots, isCreative, origin[0], origin[1], window, mouseX, mouseY);
        SlotResult nearest = CursorUtil.findNearestSlotWithItem(info.handler().slots, Items.TOTEM_OF_UNDYING, isCreative, origin[0], origin[1], window, mouseX, mouseY);
        if (nearest == null) return false;

        startX = mouseX;
        startY = mouseY;
        endX = nearest.absX();
        endY = nearest.absY();
        animDurationMs = CursorUtil.computeAnimDuration(nearest.distance(), maxDist, cursorTimeMS.getMinValue(), cursorTimeMS.getMaxValue());
        animStartMs = System.currentTimeMillis();
        animating = true;
        targetSlotId = nearest.slotIndex();
        pendingHotbarIndex = hbIdx;
        pendingSwap = phase;
        return true;
    }

    private boolean hasTotemInInventory(int excludeHotbarSlot) {
        var inventory = mc.player.getInventory();
        int size = inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            if (i == 40 || i == excludeHotbarSlot) continue;
            if (inventory.getItem(i).is(Items.TOTEM_OF_UNDYING)) return true;
        }
        return false;
    }

    private void resetSwapState() {
        shouldSwap = false;
        targetSlotId = -1;
        pendingSwap = SwapPhase.NONE;
    }

    private void resetAllState() {
        animating = false;
        resetSwapState();
    }

    private static int hotbarIndex() {
        return hotbarSlotCursor.getValue() - 1;
    }
}
