package we.devs.opium.client.modules.combat;

import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.DamageUtils;
import we.devs.opium.api.utilities.TargetUtils;
import we.devs.opium.api.utilities.TimerUtils;
import we.devs.opium.client.modules.miscellaneous.ModuleMiddleClick;
import we.devs.opium.client.values.impl.ValueBoolean;
import we.devs.opium.client.values.impl.ValueNumber;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.*;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;

@RegisterModule(name="AutoArmor", tag="Auto Armor", description="Automatically put armor on.", category=Module.Category.COMBAT)
public class ModuleAutoArmor extends Module {
    // Configuration values for armor management
    private final ValueNumber delay = new ValueNumber("Delay", "Delay", "Delay to put armor on.", 100, 0, 500);
    private final ValueBoolean elytraPriority = new ValueBoolean("ElytraPriority", "Elytra Priority", "Prioritize elytra if it is equipped.", false);
    private final ValueBoolean autoMend = new ValueBoolean("AutoMend", "Auto Mend", "Automatically remove armor when mending.", false);
    private final ValueNumber armorHP = new ValueNumber("ArmorHealth", "Armor Health", "Health for armor to remove it when mending.", 70.0f, 20.0f, 100.0f);
    private final ValueNumber enemyRange = new ValueNumber("EnemyRange", "Enemy Range", "Put on armor if enemy is near.", 7.0f, 0.0f, 20.0f);
    private final ValueNumber pearlRange = new ValueNumber("PearlRange", "Pearl Range", "Put on armor if pearl is near.", 7.0f, 0.0f, 20.0f);

    private final TimerUtils cooldown = new TimerUtils();

    @Override
    public void onUpdate() {
        super.onUpdate();

        // Return early if conditions are not met (e.g., GUI open or player null)
        if (nullCheck() || mc.currentScreen instanceof Screen) {
            return;
        }

        // Fetch current armor items
        ItemStack helmet = mc.player.getInventory().getStack(39);
        ItemStack chestplate = mc.player.getInventory().getStack(38);
        ItemStack pants = mc.player.getInventory().getStack(37);
        ItemStack boots = mc.player.getInventory().getStack(36);

        // Check if all armor pieces are in good condition (health above threshold)
        boolean allGood = isArmorHealthGood(helmet, chestplate, pants, boots);

        // If AutoMend is enabled and conditions are met, try to mend the armor
        if (autoMend.getValue() && isSafe() && isMendingActive() && !allGood) {
            saveArmor(helmet, 5);
            saveArmor(chestplate, 6);
            saveArmor(pants, 7);
            saveArmor(boots, 8);
        } else {
            // If armor is not good, attempt to equip new armor from inventory
            updateArmor(helmet, EquipmentSlot.HEAD, 5);
            updateArmor(chestplate, EquipmentSlot.CHEST, 6);
            updateArmor(pants, EquipmentSlot.LEGS, 7);
            updateArmor(boots, EquipmentSlot.FEET, 8);
        }
    }

    /**
     * Checks if all the armor pieces' health are above the defined threshold.
     */
    private boolean isArmorHealthGood(ItemStack... armorItems) {
        for (ItemStack armorItem : armorItems) {
            // If any armor's health is below threshold, return false
            if (DamageUtils.getRoundedDamage(armorItem) < armorHP.getValue().floatValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if mending is active by looking for XP bottles or a custom hotkey.
     */
    private boolean isMendingActive() {
        return mc.player.getMainHandStack().getItem().equals(Items.EXPERIENCE_BOTTLE) && mc.options.useKey.isPressed()
                || ModuleMiddleClick.INSTANCE.throwingXP;
    }

    /**
     * Attempts to save an armor piece by moving it to an empty slot if its health is below threshold.
     */
    private void saveArmor(ItemStack stack, int armorSlot) {
        if (stack.isEmpty() || DamageUtils.getRoundedDamage(stack) >= armorHP.getValue().floatValue()) {
            return; // No need to save if the armor is good or the slot is empty
        }

        ArrayList<Integer> emptySlots = emptySlots();
        if (!emptySlots.isEmpty()) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, armorSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
        }
    }

    /**
     * Attempts to update (equip) a new armor piece from the inventory to the correct slot.
     */
    private void updateArmor(ItemStack item, EquipmentSlot type, int newSlot) {
        return; // Skip if the item is not armor

    }

    /**
     * Moves an item from one slot to another with cooldown.
     */
    private void moveItem(int slot, int newSlot) {
        if (cooldown.hasTimeElapsed(delay.getValue().intValue())) {
            // Pickup the item from the original slot, move to the new slot, and return the item
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot < 9 ? slot + 36 : slot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, newSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot < 9 ? slot + 36 : slot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.tick();
            cooldown.reset(); // Reset cooldown after moving items
        }
    }

    /**
     * Gets the inventory slot of an armor item that matches the given equipment slot type.
     */
    private int getArmorSlot(EquipmentSlot type) {
        for (int i = 0; i < 36; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof ArmorItem && ((ArmorItem) stack.getItem()).getSlotType().equals(type)) {
                return i; // Return the inventory slot index
            }
        }
        return -1; // No matching armor found
    }

    /**
     * Returns a list of empty slots in the player's inventory.
     */
    private ArrayList<Integer> emptySlots() {
        ArrayList<Integer> emptySlots = new ArrayList<>();
        for (int i = 0; i < 36; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                emptySlots.add(i); // Add empty slots to the list
            }
        }
        return emptySlots;
    }

    /**
     * Determines if the player is in a safe state, i.e., no enemies or Ender Pearls nearby.
     */
    private boolean isSafe() {
        return TargetUtils.getTarget(enemyRange.getValue().floatValue()) == null && getPearl(pearlRange.getValue().floatValue()) == null;
    }

    /**
     * Gets the closest Ender Pearl within a specified range.
     */
    private EnderPearlEntity getPearl(float range) {
        EnderPearlEntity closestPearl = null;
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EnderPearlEntity && mc.player.distanceTo(entity) <= range) {
                if (closestPearl == null || mc.player.distanceTo(entity) < mc.player.distanceTo(closestPearl)) {
                    closestPearl = (EnderPearlEntity) entity;
                }
            }
        }
        return closestPearl;
    }
}
