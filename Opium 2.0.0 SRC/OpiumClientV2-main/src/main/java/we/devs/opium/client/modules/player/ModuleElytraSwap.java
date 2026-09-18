package we.devs.opium.client.modules.player;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.ChatUtils;
import we.devs.opium.api.utilities.InventoryUtils;
import we.devs.opium.client.values.impl.ValueBoolean;
import we.devs.opium.client.values.impl.ValueNumber;
import we.devs.opium.client.values.impl.ValueEnum;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RegisterModule(name = "Elytra Swap", tag = "Elytra Swap", description = "Automatically swap elytra and chestplate", category = Module.Category.PLAYER)
public class ModuleElytraSwap extends Module {

    private final ValueBoolean autoFirework = new ValueBoolean("AutoFirework", "Auto Firework", "Automatically uses a Firework when switching to an Elytra", true);
    private final ValueEnum<SwitchModes> switchMode = new ValueEnum<>("SwitchMode", "Switch Mode", "Switch Modes", SwitchModes.Silent);
    private final ValueNumber delay = new ValueNumber("FireworkDelay", "Firework Delay", "Delay between jumping and using fireworks", 4, 0, 8);

    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    @Override
    public void onEnable() {
        if (nullCheck()) {
            disable(false);
            return;
        }

        int fireworkSlot;
        if (switchMode.getValue().equals(SwitchModes.InvSwitch)) {
            fireworkSlot = InventoryUtils.findItem(Items.FIREWORK_ROCKET, 0, 36);
        } else fireworkSlot = InventoryUtils.findItem(Items.FIREWORK_ROCKET, 0, 8);

        if (fireworkSlot == -1) {
            ChatUtils.sendMessage("No Fireworks Found", "Elytra Swap");
            disable(true);
            return;
        }

        // Find the best chestplate and elytra slots
        int bestChestplateSlot = findBestChestplateSlot();
        int elytraSlot = InventoryUtils.findItem(Items.ELYTRA, 0, 39);

        // Swap logic
        if (elytraSlot == 38 && bestChestplateSlot != -1) {
            // Swap chestplate to armor slot
            InventoryUtils.swapSlots(bestChestplateSlot < 9 ? bestChestplateSlot + 36 : bestChestplateSlot, 6);
        } else if (bestChestplateSlot == 38 && elytraSlot != -1) {
            // Swap elytra to armor slot
            InventoryUtils.swapSlots(elytraSlot < 9 ? elytraSlot + 36 : elytraSlot, 6);

            // Handle auto firework logic
            if (autoFirework.getValue()) {
                // Schedule Elytra flight after delay
                int delayTicks = delay.getValue().intValue();

                assert mc.player != null;
                if (mc.player.isOnGround()) mc.player.jump();
                if(delayTicks == 0) useFirework(fireworkSlot);
                else scheduler.schedule(() -> mc.execute(() -> useFirework(fireworkSlot)), delayTicks * 50L, TimeUnit.MILLISECONDS);
            }
        } else if (elytraSlot != 38 && elytraSlot != -1) {
            // Swap elytra to armor slot
            InventoryUtils.swapSlots(elytraSlot < 9 ? elytraSlot + 36 : elytraSlot, 6);
        } else if (elytraSlot == -1 && bestChestplateSlot != -1 && bestChestplateSlot != 38) {
            // Swap chestplate to armor slot
            InventoryUtils.swapSlots(bestChestplateSlot < 9 ? bestChestplateSlot + 36 : bestChestplateSlot, 6);
        }

        disable(true); // Disable the module after completing the task
    }

    private void useFirework(int fireworkSlot) {
        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));

        if (!switchMode.getValue().equals(SwitchModes.InvSwitch) && switchMode.getValue().equals(SwitchModes.Silent)) {
            // Use firework from hotbar
            InventoryUtils.switchToSlot(fireworkSlot, true, () -> {
                InventoryUtils.itemUsage(Hand.MAIN_HAND);
            });
        } else if (switchMode.getValue().equals(SwitchModes.Strict)){
            InventoryUtils.switchToSlot(fireworkSlot, false, () -> {
                InventoryUtils.itemUsage(Hand.MAIN_HAND);
            });
        } else {
            // Use firework in offhand
            InventoryUtils.useItemInOffhand(Items.FIREWORK_ROCKET);
        }
    }

    private int findBestChestplateSlot() {
        int bestSlot = -1;
        int bestLevel = -1;

        for (int i = 0; i <= 39; i++) {
            assert mc.player != null;
            Item item = mc.player.getInventory().getStack(i).getItem();
            int itemLevel = getChestplateLevel(item);
            if (itemLevel > bestLevel) {
                bestSlot = i;
                bestLevel = itemLevel;
            }
        }

        return bestSlot;
    }

    private int getChestplateLevel(Item item) {
        if (item.equals(Items.LEATHER_CHESTPLATE)) return 1;
        else if (item.equals(Items.CHAINMAIL_CHESTPLATE)) return 2;
        else if (item.equals(Items.GOLDEN_CHESTPLATE)) return 3;
        else if (item.equals(Items.IRON_CHESTPLATE)) return 4;
        else if (item.equals(Items.DIAMOND_CHESTPLATE)) return 5;
        else if (item.equals(Items.NETHERITE_CHESTPLATE)) return 6;
        return -1;
    }

    public enum SwitchModes {
        Silent,
        Strict,
        InvSwitch
    }
}