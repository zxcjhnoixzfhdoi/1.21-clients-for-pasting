package we.devs.opium.api.utilities;

import net.minecraft.block.BlockState;
import net.minecraft.item.Items;
import we.devs.opium.Opium;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

import java.util.function.Predicate;

public class InventoryUtils implements IMinecraft {

    // Improved getTargetSlot method with more flexibility
    public static int getTargetSlot(String input) {
        int obsidianSlot = findBlock(Blocks.OBSIDIAN, 0, 9);
        int chestSlot = findBlock(Blocks.ENDER_CHEST, 0, 9);
        if (obsidianSlot == -1 && chestSlot == -1) {
            return -1;
        }
        if (obsidianSlot != -1 && chestSlot == -1) {
            return obsidianSlot;
        }
        if (obsidianSlot == -1) {
            return chestSlot;
        }
        if (input.equals("Obsidian")) {
            return obsidianSlot;
        }
        return chestSlot;
    }

    // Check if an item is in the offhand
    public static boolean testInOffHand(Item item) {
        assert mc.player != null;
        Item offhandItem = mc.player.getOffHandStack().getItem();
        return offhandItem == item;
    }

    // Silent switch with improved anticheat bypass
    public static void switchSlot(int slot, boolean silent) {
        Opium.PLAYER_MANAGER.setSwitching(true);
        assert mc.player != null;
        if (silent) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        } else {
            mc.player.getInventory().selectedSlot = slot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
        Opium.PLAYER_MANAGER.setSwitching(false);
    }

    // Find an item within a range
    public static int findItem(Item item, int minimum, int maximum) {
        for (int i = minimum; i <= maximum; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    public static void switchToSlot(int slot, boolean silent, Runnable runnable) {
        assert mc.player != null;

        // Save the original slot
        int originalSlot = mc.player.getInventory().selectedSlot;

        // Switch to the specified slot
        switchSlot(slot, silent);

        // Execute the provided code
        try {
            runnable.run();
        } catch (Exception e) {
            Opium.LOGGER.error("[Inventory Util] { switchToSlot() } : Error during execution: ", e);
        }

        // Swap back to the original slot
        switchSlot(originalSlot, silent);
    }

    // Interact with an item in hand
    public static void itemUsage(Hand hand) {
        assert mc.interactionManager != null;
        mc.interactionManager.interactItem(mc.player, hand);
    }

    // Find the best tool for a block
    public static int findBestTool(BlockState block, boolean onlyHotbar) {
        float bestMultiplier = Float.MIN_VALUE;
        int bestSlot = -1;
        for (int i = 0; i < (onlyHotbar ? 9 : 36); i++) {
            assert mc.player != null;
            ItemStack stack = mc.player.getInventory().getStack(i);
            float mul = stack.getMiningSpeedMultiplier(block);
            if (stack.isSuitableFor(block) && mul > bestMultiplier) {
                bestMultiplier = mul;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    // Get a slot by class
    public static int getSlotByClass(Class<?> clss) {
        for (int i = 45; i > 0; --i) {
            assert mc.player != null;
            if (mc.player.getInventory().getStack(i).getItem().getClass() == clss) {
                return i;
            }
        }
        return -1;
    }

    // Get an item stack from a slot
    public static ItemStack get(int slot) {
        if (slot == -2) {
            return mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot);
        }
        return mc.player.getInventory().getStack(slot);
    }

    // Find an item in the entire inventory
    public static int findItem(Item item) {
        for (int i = 9; i < 45; ++i) {
            if (get(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    // Move an item to the offhand
    public static void offhandItem(Item item) {
        int slot = findItem(item);
        if (slot != -1) {
            assert mc.interactionManager != null;
            assert mc.player != null;
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.tick();
        }
    }

    // Find a block within a range
    public static int findBlock(Block block, int minimum, int maximum) {
        for (int i = minimum; i <= maximum; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem) {
                BlockItem item = (BlockItem) stack.getItem();
                if (item.getBlock() == block) {
                    return i;
                }
            }
        }
        return -1;
    }

    // Silent switch with delay for better anticheat bypass
    public static void silentSwitch(int slot, int delay) {
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                switchSlot(slot, true);
            } catch (InterruptedException e) {
                Opium.LOGGER.error("[Inventory Util] { silentSwitch() } : ", e);
            }
        }).start();
    }

    // Check if the player has a specific item in their inventory
    public static boolean hasItem(Item item) {
        for (int i = 0; i < 45; ++i) {
            if (get(i).getItem() == item) {
                return true;
            }
        }
        return false;
    }

    // Drop an item from a specific slot
    public static void dropItem(int slot) {
        assert mc.interactionManager != null;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 1, SlotActionType.THROW, mc.player);
    }

    // Drop all items of a specific type
    public static void dropAllItems(Item item) {
        for (int i = 0; i < 45; ++i) {
            if (get(i).getItem() == item) {
                dropItem(i);
            }
        }
    }

    // Swap items between two slots
    public static void swapSlots(int slot1, int slot2) {
        assert mc.interactionManager != null;
        assert mc.player != null;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot1, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot2, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot1, 0, SlotActionType.PICKUP, mc.player);
    }

    public static void useItemInOffhand(Item item) {
        assert mc.player != null;

        // Find the item in the inventory
        int itemSlot = findItem(item);
        if (itemSlot == -1) {
            return; // Item not found
        }

        // Swap the found item to the offhand
        swapSlots(itemSlot, 45);

        // Use the item in the offhand
        itemUsage(Hand.OFF_HAND);

        // Swap the items back to their original positions
        swapSlots(45, itemSlot);
    }

    // Enum for item modes
    public enum ItemModes {
        Obsidian,
        Chest
    }

    // Enum for switch modes
    public enum SwitchModes {
        Normal,
        Silent,
        Strict
    }
}