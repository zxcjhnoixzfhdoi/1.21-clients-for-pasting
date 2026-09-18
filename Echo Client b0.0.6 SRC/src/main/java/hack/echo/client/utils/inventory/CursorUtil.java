package hack.echo.client.utils.inventory;

import com.mojang.blaze3d.platform.Window;
import hack.echo.client.utils.Imports;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Predicate;

public final class CursorUtil implements Imports {

    public record ScreenInfo(AbstractContainerMenu handler, int bgWidth, int bgHeight) {}

    public record SlotResult(int slotIndex, double absX, double absY, double distance) {}

    /**
     * Returns screen info for InventoryScreen or CreativeModeInventoryScreen
     */
    public static ScreenInfo getScreenInfo(Object screen) {
        if (screen instanceof InventoryScreen inv) {
            return new ScreenInfo(inv.getMenu(), 176, 166);
        }
        if (screen instanceof CreativeModeInventoryScreen cre) {
            return new ScreenInfo(cre.getMenu(), 195, 136);
        }
        return null;
    }

    public static boolean isInventoryScreen(Object screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
    }

    /**
     * Converts a slot position to absolute screen pixel coordinates.
     */
    public static double[] slotToAbsPos(Slot slot, int screenX, int screenY, Window window) {
        double scaledX = screenX + slot.x + 8.0;
        double scaledY = screenY + slot.y + 8.0;
        double absX = scaledX * window.getScreenWidth() / window.getGuiScaledWidth();
        double absY = scaledY * window.getScreenHeight() / window.getGuiScaledHeight();
        return new double[]{absX, absY};
    }

    /**
     * Returns the top-left corner of the centered inventory background in GUI-scaled coordinates.
     */
    public static int[] getScreenOrigin(ScreenInfo info, Window window) {
        int x = (window.getGuiScaledWidth() - info.bgWidth()) / 2;
        int y = (window.getGuiScaledHeight() - info.bgHeight()) / 2;
        return new int[]{x, y};
    }

    /**
     * Returns the maximum distance from the mouse to any valid inventory slot (for duration scaling).
     */
    public static double maxSlotDistance(List<Slot> slots, boolean isCreative, int screenX, int screenY, Window window, double mouseX, double mouseY) {
        return maxSlotDistance(slots, isCreative, screenX, screenY, window, mouseX, mouseY, null);
    }

    public static double maxSlotDistance(List<Slot> slots, boolean isCreative, int screenX, int screenY, Window window, double mouseX, double mouseY, Predicate<Slot> slotFilter) {
        double max = 0.0;
        for (Slot slot : slots) {
            if (!isValidSlot(slot, isCreative)) continue;
            if (slotFilter != null && !slotFilter.test(slot)) continue;
            double[] pos = slotToAbsPos(slot, screenX, screenY, window);
            double d = Math.hypot(pos[0] - mouseX, pos[1] - mouseY);
            if (d > max) max = d;
        }
        return max;
    }

    /**
     * Finds the nearest slot containing the given item.
     */
    public static SlotResult findNearestSlotWithItem(List<Slot> slots, Item item, boolean isCreative, int screenX, int screenY, Window window, double mouseX, double mouseY) {
        return findNearestSlotWithItem(slots, item, isCreative, screenX, screenY, window, mouseX, mouseY, null);
    }

    public static SlotResult findNearestSlotWithItem(List<Slot> slots, Item item, boolean isCreative, int screenX, int screenY, Window window, double mouseX, double mouseY, Predicate<Slot> slotFilter) {
        double minDist = Double.MAX_VALUE;
        SlotResult best = null;

        for (int menuSlot = 0; menuSlot < slots.size(); menuSlot++) {
            Slot slot = slots.get(menuSlot);
            if (!isValidSlot(slot, isCreative)) continue;
            if (slotFilter != null && !slotFilter.test(slot)) continue;
            if (!slot.getItem().is(item)) continue;

            double[] pos = slotToAbsPos(slot, screenX, screenY, window);
            double dist = Math.hypot(pos[0] - mouseX, pos[1] - mouseY);
            if (dist < minDist) {
                minDist = dist;
                best = new SlotResult(menuSlot, pos[0], pos[1], dist);
            }
        }
        return best;
    }

    /**
     * Computes cursor animation duration in ms, scaled by how far the target is relative to the farthest slot.
     */
    public static long computeAnimDuration(double distance, double maxDistance, float minMs, float maxMs) {
        if (maxDistance <= 0.0) return Math.max(1L, Math.round(minMs));
        double ratio = Math.max(0.0, Math.min(1.0, distance / maxDistance));
        double mapped = minMs + ratio * (maxMs - minMs);
        return Math.max(1L, Math.round(mapped));
    }

    private static boolean isValidSlot(Slot slot, boolean isCreative) {
        if (isCreative && !(slot.container instanceof Inventory)) return false;
        if (slot.container instanceof Inventory && slot.getContainerSlot() == 40) return false;
        return true;
    }
}
