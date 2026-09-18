package we.devs.opium.api.utilities;

import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.function.Predicate;
import java.util.HashMap;
import java.util.Map;

public class InventoryUtil implements IMinecraft {
    private static final Action ACTION = new Action();
    public static int previousSlot = -1;

    private InventoryUtil() {}

    // Predicates

    public static boolean testInMainHand(Predicate<ItemStack> predicate) {
        return predicate.test(mc.player.getMainHandStack());
    }

    public static boolean testInMainHand(Item... items) {
        return testInMainHand(itemStack -> {
            for (var item : items) {
                if (itemStack.isOf(item)) return true;
            }
            return false;
        });
    }

    public static boolean testInOffHand(Predicate<ItemStack> predicate) {
        return predicate.test(mc.player.getOffHandStack());
    }

    public static boolean testInOffHand(Item... items) {
        return testInOffHand(itemStack -> {
            for (var item : items) {
                if (itemStack.isOf(item)) return true;
            }
            return false;
        });
    }

    public static boolean testInHands(Predicate<ItemStack> predicate) {
        return testInMainHand(predicate) || testInOffHand(predicate);
    }

    public static boolean testInHands(Item... items) {
        return testInMainHand(items) || testInOffHand(items);
    }

    public static boolean testInHotbar(Predicate<ItemStack> predicate) {
        if (testInHands(predicate)) return true;

        for (int i = SlotUtils.HOTBAR_START; i < SlotUtils.HOTBAR_END; i++) {
            if (predicate.test(mc.player.getInventory().getStack(i))) return true;
        }
        return false;
    }

    public static boolean testInHotbar(Item... items) {
        return testInHotbar(itemStack -> {
            for (var item : items) {
                if (itemStack.isOf(item)) return true;
            }
            return false;
        });
    }

    public static Map<Integer, ItemStack> getInventoryAndHotbarSlots() {
        Map<Integer, ItemStack> slots = new HashMap<>();
        for (int i = 0; i <= 44; i++) {
            slots.put(i, mc.player.getInventory().getStack(i));
        }
        return slots;
    }

    // Finding Items

    public static FindItemResult findEmpty() {
        return find(ItemStack::isEmpty);
    }

    public static FindItemResult findInHotbar(Predicate<ItemStack> predicate) {
        if (testInOffHand(predicate)) {
            return new FindItemResult(SlotUtils.OFFHAND, mc.player.getOffHandStack().getCount());
        }
        if (testInMainHand(predicate)) {
            return new FindItemResult(mc.player.getInventory().selectedSlot, mc.player.getMainHandStack().getCount());
        }
        return find(predicate, 0, 8);
    }

    public static FindItemResult findInHotbar(Item... items) {
        return findInHotbar(itemStack -> {
            for (var item : items) {
                if (itemStack.isOf(item)) return true;
            }
            return false;
        });
    }

    public static FindItemResult find(Predicate<ItemStack> predicate) {
        if (mc.player == null) return new FindItemResult(-1, 0);
        return find(predicate, 0, mc.player.getInventory().size());
    }

    public static FindItemResult find(Predicate<ItemStack> predicate, int start, int end) {
        if (mc.player == null) return new FindItemResult(-1, 0);

        int slot = -1, count = 0;

        for (int i = start; i <= end; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (predicate.test(stack)) {
                if (slot == -1) slot = i;
                count += stack.getCount();
            }
        }
        return new FindItemResult(slot, count);
    }

    public static FindItemResult find(Item... items) {
        return find(itemStack -> {
            for (var item : items) {
                if (itemStack.isOf(item)) return true;
            }
            return false;
        });
    }

    public static FindItemResult findFastestTool(BlockState state) {
        float bestScore = 1.0f;
        int slot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isSuitableFor(state)) continue;

            float score = stack.getMiningSpeedMultiplier(state);
            if (score > bestScore) {
                bestScore = score;
                slot = i;
            }
        }
        return new FindItemResult(slot, 1);
    }

    // Interactions

    public static boolean swap(int slot, boolean swapBack) {
        if (slot == SlotUtils.OFFHAND) return true;
        if (slot < 0 || slot > 8) return false;

        if (swapBack && previousSlot == -1) previousSlot = mc.player.getInventory().selectedSlot;
        else if (!swapBack) previousSlot = -1;

        mc.player.getInventory().selectedSlot = slot;
        return true;
    }

    public static boolean swapBack() {
        if (previousSlot == -1) return false;

        boolean success = swap(previousSlot, false);
        previousSlot = -1;
        return success;
    }

    public static Action move() {
        return ACTION.setType(SlotActionType.PICKUP).setTwo(true);
    }

    public static Action click() {
        return ACTION.setType(SlotActionType.PICKUP);
    }

    public static Action quickSwap() {
        return ACTION.setType(SlotActionType.SWAP);
    }

    public static Action shiftClick() {
        return ACTION.setType(SlotActionType.QUICK_MOVE);
    }

    public static Action drop() {
        return ACTION.setType(SlotActionType.THROW).setData(1);
    }

    public static void dropHand() {
        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, ScreenHandler.EMPTY_SPACE_SLOT_INDEX, 0, SlotActionType.PICKUP, mc.player);
        }
    }

    public static class Action {
        private SlotActionType type;
        private boolean two;
        private int from;
        private int to;
        private int data;
        private boolean isRecursive;

        private Action() {}

        public Action setType(SlotActionType type) {
            this.type = type;
            return this;
        }

        public Action setTwo(boolean two) {
            this.two = two;
            return this;
        }

        public Action setData(int data) {
            this.data = data;
            return this;
        }

        public void fromId(int id) {
            this.from = id;
        }

        public void toId(int id) {
            this.to = id;
            execute();
        }

        private void execute() {
            // Click logic omitted for brevity...
        }
    }
}
