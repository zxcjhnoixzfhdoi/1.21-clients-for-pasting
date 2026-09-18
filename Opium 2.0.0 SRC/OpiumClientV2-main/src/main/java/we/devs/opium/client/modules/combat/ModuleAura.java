package we.devs.opium.client.modules.combat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.*;
import we.devs.opium.client.events.EventMotion;
import we.devs.opium.client.values.impl.ValueBoolean;
import we.devs.opium.client.values.impl.ValueEnum;
import we.devs.opium.client.values.impl.ValueNumber;

@RegisterModule(name="Aura", tag="Aura", description="Automatically kills people with a sword.", category=Module.Category.COMBAT)
public class ModuleAura extends Module {
    ValueNumber range = new ValueNumber("Range", "Range", "Range", 5.0f, 1.0f, 6.0f);
    ValueEnum weapon = new ValueEnum("Weapon", "Weapon", "Weapon", Weapon.Require);
    ValueBoolean rotate = new ValueBoolean("Rotate", "Rotate", "Will rotate you to the pos", true);
    PlayerEntity target;

    @Override
    public void onMotion(EventMotion event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }
        target = TargetUtils.getTarget(range.getValue().floatValue());
        if (target == null) {
            return;
        }

        switch (weapon.getValue()) {
            case Weapon.Require -> {
                if (!(mc.player.getMainHandStack().getItem() instanceof SwordItem)) {
                    return;
                }
            }
            case Weapon.Swap -> {
                int slot = InventoryUtils.getSlotByClass(SwordItem.class);
                if (mc.player.getInventory().selectedSlot != slot) {
                    if (slot == -1) {
                        return;
                    }
                    InventoryUtils.switchSlot(slot, false);
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + weapon.getValue());
        }

        float factor = 20.0f - TPSUtils.getTickRate();
        if (mc.player.getAttackCooldownProgress(factor) >= 1.0f) {
            if (rotate.getValue()) RotationsUtil.rotateToBlockPos(target.getBlockPos(), false);
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    enum Weapon {
        Require,
        Swap
    }

    @Override
    public void onDisable() {
        super.onDisable();
        target = null;
    }
}
