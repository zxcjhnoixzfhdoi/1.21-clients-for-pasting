package hack.echo.client.utils.inventory;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import hack.echo.client.handlers.InputHandler;
import hack.echo.client.utils.Imports;
import hack.echo.client.utils.network.NetworkUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;


public final class InventoryUtils implements Imports {

    public static void setInvSlot(int slot) {
        if (mc.player.getInventory().getSelectedSlot() == slot) return;
        mc.player.getInventory().setSelectedSlot(slot);
    }

    public static void setInvSlot(int slot, boolean inputSimulation) {
        if (slot < 0 || slot > 8) return;

        if (mc.player.getInventory().getSelectedSlot() == slot) return;

        if (inputSimulation) {
            if (mc.options != null && mc.options.keyHotbarSlots != null) {
                KeyMapping hotbarKey = mc.options.keyHotbarSlots[slot];
                if (hotbarKey != null) {
                    InputHandler.incrementClickCountForKeyMapping(hotbarKey);
                }
            }
        } else {
            setInvSlot(slot);
        }
    }

    public static void swapToOffhand(){
        NetworkUtil.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
    }

    public static boolean selectItemFromHotbar(Predicate<Item> item) {
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; ++i) {
            ItemStack itemStack = inv.getItem(i);
            if (!item.test(itemStack.getItem())) continue;
            inv.setSelectedSlot(i);
            return true;
        }
        return false;
    }

    public static boolean selectItemFromHotbar(Item item) {
        return InventoryUtils.selectItemFromHotbar((Item i) -> i == item);
    }

    public static boolean hasItemInHotbar(Predicate<Item> item) {
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; ++i) {
            ItemStack itemStack = inv.getItem(i);
            if (!item.test(itemStack.getItem())) continue;
            return true;
        }
        return false;
    }

    public static int findItemWithPredicateInHotbar(Predicate<ItemStack> predicate) {
        Inventory inventory = mc.player.getInventory();
        for (int i = 0; i <= 8; i++) {
            if (predicate.test(inventory.getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    public static int countItem(Predicate<Item> item) {
        Inventory inv = mc.player.getInventory();
        int count = 0;
        for (int i = 0; i < 36; ++i) {
            ItemStack itemStack = inv.getItem(i);
            if (!item.test(itemStack.getItem())) continue;
            count += itemStack.getCount();
        }
        return count;
    }

    public static int countItemExceptHotbar(Predicate<Item> item) {
        Inventory inv = mc.player.getInventory();
        int count = 0;
        for (int i = 9; i < 36; ++i) {
            ItemStack itemStack = inv.getItem(i);
            if (!item.test(itemStack.getItem())) continue;
            count += itemStack.getCount();
        }
        return count;
    }

    public static int getSwordSlot() {
        Inventory playerInventory = mc.player.getInventory();
        for (int itemIndex = 0; itemIndex < 9; ++itemIndex) {
            Item item = playerInventory.getItem(itemIndex).getItem();
            if (item == Items.WOODEN_SWORD || item == Items.STONE_SWORD || item == Items.IRON_SWORD || item == Items.GOLDEN_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD || item == Items.COPPER_SWORD) {
                return itemIndex;
            }
        }
        return -1;
    }

    public static boolean isSword(Item item) {
        return item == Items.WOODEN_SWORD || item == Items.STONE_SWORD || item == Items.IRON_SWORD || item == Items.GOLDEN_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD;
    }

    public static boolean isFood(ItemStack item) {
        return item.getComponents().has(DataComponents.FOOD);
    }

    public static boolean selectSword() {
        int itemIndex = InventoryUtils.getSwordSlot();
        if (itemIndex != -1) {
            InventoryUtils.setInvSlot(itemIndex);
            return true;
        }
        return false;
    }

    public static int findTotemSlot() {
        assert (mc.player != null);
        Inventory inv = mc.player.getInventory();
        for (int index = 9; index < 36; ++index) {
            if (inv.getItem(index).getItem() != Items.TOTEM_OF_UNDYING) continue;
            return index;
        }
        return -1;
    }

    public static boolean selectAxe() {
        int itemIndex = InventoryUtils.getAxeSlot();
        if (itemIndex != -1) {
            mc.player.getInventory().setSelectedSlot(itemIndex);
            return true;
        }
        return false;
    }

    public static int findRandomTotemSlot() {
        Inventory inventory = mc.player.getInventory();
        Random random = new Random();
        ArrayList<Integer> totemIndexes = new ArrayList<Integer>();
        for (int i = 9; i < 36; ++i) {
            if (inventory.getItem(i).getItem() != Items.TOTEM_OF_UNDYING) continue;
            totemIndexes.add(i);
        }
        if (!totemIndexes.isEmpty()) {
            return totemIndexes.get(random.nextInt(totemIndexes.size()));
        }
        return -1;
    }

    public static int findRandomPot(String potion) {
        Inventory inventory = mc.player.getInventory();
        Random random = new Random();
        int slotIndex = random.nextInt(27) + 9;
        for (int i = 0; i < 27; ++i) {
            int index = (slotIndex + i) % 36;
            ItemStack itemStack = inventory.getItem(index);
            if (!(itemStack.getItem() instanceof SplashPotionItem)) continue;
            if (!itemStack.get(DataComponents.POTION_CONTENTS).getAllEffects().toString().contains(potion)) {
                return -1;
            }
            return index;
        }
        return -1;
    }

    public static List<Integer> getEmptyHotbarSlots() {
        Inventory inventory = mc.player.getInventory();
        ArrayList<Integer> slots = new ArrayList<Integer>();
        for (int i = 0; i < 9; ++i) {
            if (inventory.getItem(i).isEmpty()) {
                slots.add(i);
            }
        }
        return slots;
    }

    public static int getAxeSlot() {
        Inventory playerInventory = mc.player.getInventory();
        for (int itemIndex = 0; itemIndex < 9; ++itemIndex) {
            if (!(playerInventory.getItem(itemIndex).getItem() instanceof AxeItem)) continue;
            return itemIndex;
        }
        return -1;
    }

    public static int countItem(Item item) {
        return InventoryUtils.countItem((Item i) -> i == item);
    }

    public static boolean isHoldingChestplate() {
        var mainHand = mc.player.getMainHandItem().getItem();
        return mainHand == Items.NETHERITE_CHESTPLATE ||
                mainHand == Items.CHAINMAIL_CHESTPLATE ||
                mainHand == Items.IRON_CHESTPLATE ||
                mainHand == Items.GOLDEN_CHESTPLATE ||
                mainHand == Items.DIAMOND_CHESTPLATE ||
                mainHand == Items.LEATHER_CHESTPLATE;
    }

    public static boolean isOffhandHoldingChestplate() {
        var offhand = mc.player.getOffhandItem().getItem();
        return offhand == Items.NETHERITE_CHESTPLATE ||
                offhand == Items.CHAINMAIL_CHESTPLATE ||
                offhand == Items.IRON_CHESTPLATE ||
                offhand == Items.GOLDEN_CHESTPLATE ||
                offhand == Items.DIAMOND_CHESTPLATE ||
                offhand == Items.LEATHER_CHESTPLATE;
    }

    public static int findChestplateSlot() {
        final Inventory inv = mc.player.getInventory();
        for (int i = 0; i <= 8; i++) {
            Item item = inv.getItem(i).getItem();
            if (item == Items.NETHERITE_CHESTPLATE ||
                    item == Items.CHAINMAIL_CHESTPLATE ||
                    item == Items.IRON_CHESTPLATE ||
                    item == Items.GOLDEN_CHESTPLATE ||
                    item == Items.DIAMOND_CHESTPLATE ||
                    item == Items.LEATHER_CHESTPLATE) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isWearingElytra() {
        return mc.player.getInventory().getItem(38).getItem() == Items.ELYTRA;
    }

    public static ItemStack getMainHandItem() {
        return mc.player.getMainHandItem();
    }

    public static boolean isHoldingItem(Item item) {
        return mc.player.getMainHandItem().getItem() == item;
    }

    public static boolean isHoldingAnyHand(Item item) {
        return isHoldingItem(item) || isOffhandHoldingItem(item);
    }

    public static boolean isOffhandHoldingItem(Item item) {
        return mc.player.getOffhandItem().getItem() == item;
    }

    public static boolean isHoldingSwordItem() {
        Item mainHand = mc.player.getMainHandItem().getItem();
        return mainHand == Items.WOODEN_SWORD ||
                mainHand == Items.STONE_SWORD ||
                mainHand == Items.COPPER_SWORD ||
                mainHand == Items.IRON_SWORD ||
                mainHand == Items.GOLDEN_SWORD ||
                mainHand == Items.DIAMOND_SWORD ||
                mainHand == Items.NETHERITE_SWORD;
    }

    public static int findSwordInHotbar() {
        return findItemWithPredicateInHotbar(stack -> isSword(stack.getItem()));
    }

    public static boolean isHoldingSpearItem(){
        Item mainHand = mc.player.getMainHandItem().getItem();
        return mainHand == Items.WOODEN_SPEAR ||
                mainHand == Items.STONE_SPEAR ||
                mainHand == Items.COPPER_SPEAR ||
                mainHand == Items.IRON_SPEAR ||
                mainHand == Items.GOLDEN_SPEAR ||
                mainHand == Items.DIAMOND_SPEAR ||
                mainHand == Items.NETHERITE_SPEAR;
    }

    public static int findSpearInHotbar() {
        return findItemWithPredicateInHotbar(stack -> isItemSpear(stack.getItem()));
    }

    public static boolean isItemSpear(Item item){
        return item == Items.WOODEN_SPEAR ||
                item == Items.STONE_SPEAR ||
                item == Items.COPPER_SPEAR ||
                item == Items.IRON_SPEAR ||
                item == Items.GOLDEN_SPEAR ||
                item == Items.DIAMOND_SPEAR ||
                item == Items.NETHERITE_SPEAR;
    }

    public static boolean isHoldingAxeItem() {
        Item mainHand = mc.player.getMainHandItem().getItem();
        return mainHand == Items.WOODEN_AXE ||
                mainHand == Items.STONE_AXE ||
                mainHand == Items.COPPER_AXE ||
                mainHand == Items.IRON_AXE ||
                mainHand == Items.GOLDEN_AXE ||
                mainHand == Items.DIAMOND_AXE ||
                mainHand == Items.NETHERITE_AXE;
    }

    public static boolean isHoldingPickaxeItem() {
        Item mainHand = mc.player.getMainHandItem().getItem();
        return mainHand == Items.WOODEN_PICKAXE ||
                mainHand == Items.STONE_PICKAXE ||
                mainHand == Items.COPPER_PICKAXE ||
                mainHand == Items.IRON_PICKAXE ||
                mainHand == Items.GOLDEN_PICKAXE ||
                mainHand == Items.DIAMOND_PICKAXE ||
                mainHand == Items.NETHERITE_PICKAXE;
    }

    public static boolean isHoldingShovelItem() {
        Item mainHand = mc.player.getMainHandItem().getItem();
        return mainHand == Items.WOODEN_SHOVEL ||
                mainHand == Items.STONE_SHOVEL ||
                mainHand == Items.COPPER_SHOVEL ||
                mainHand == Items.IRON_SHOVEL ||
                mainHand == Items.GOLDEN_SHOVEL ||
                mainHand == Items.DIAMOND_SHOVEL ||
                mainHand == Items.NETHERITE_SHOVEL;
    }

    public static boolean isHoldingHoeItem() {
        Item mainHand = mc.player.getMainHandItem().getItem();
        return mainHand == Items.WOODEN_HOE ||
                mainHand == Items.STONE_HOE ||
                mainHand == Items.COPPER_HOE ||
                mainHand == Items.IRON_HOE ||
                mainHand == Items.GOLDEN_HOE ||
                mainHand == Items.DIAMOND_HOE ||
                mainHand == Items.NETHERITE_HOE;
    }

    public static int findItemWithEnchantmentInHotbar(Predicate<ItemStack> predicate, ResourceKey<Enchantment> enchantKey, boolean allowNoEnch) {
        if (enchantKey != null) {
            Inventory inventory = mc.player.getInventory();
            for (int i = 0; i <= 8; i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack != null && !stack.isEmpty() && predicate.test(stack)) {
                    if (mc.level != null) {
                        var opt = mc.level.registryAccess().get(enchantKey);
                        if (opt.isPresent() && EnchantmentHelper.getItemEnchantmentLevel(opt.get(), stack) > 0) {
                            return i;
                        }
                    }
                }
            }
        }

        if (allowNoEnch) {
            Inventory inventory = mc.player.getInventory();
            for (int i = 0; i <= 8; i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack != null && !stack.isEmpty() && predicate.test(stack)) {
                    return i;
                }
            }
        }

        return -1;
    }

    public static int findMaceWithEnchantmentInHotbar(ResourceKey<Enchantment> enchantKey, boolean allowNoEnch) {
        return findItemWithEnchantmentInHotbar(stack -> stack.getItem() instanceof MaceItem, enchantKey, allowNoEnch);
    }

    public static void silentSwapTo(int slot) {
        mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }

    public static void silentSwapBack() {
        mc.player.connection.send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot()));
    }

}
