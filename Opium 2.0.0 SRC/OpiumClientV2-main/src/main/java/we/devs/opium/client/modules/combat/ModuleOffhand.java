package we.devs.opium.client.modules.combat;

import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.HoleUtils;
import we.devs.opium.api.utilities.InventoryUtils;
import we.devs.opium.client.values.impl.ValueBoolean;
import we.devs.opium.client.values.impl.ValueEnum;
import we.devs.opium.client.values.impl.ValueNumber;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;

@RegisterModule(name = "Offhand", description = "Automatically switches items to your offhand.", category = Module.Category.COMBAT)
public class ModuleOffhand extends Module {
    ValueEnum mode = new ValueEnum("Mode", "Mode", "Mode for offhand.", Modes.Totem);
    ValueNumber hp = new ValueNumber("Health", "Health", "Health of player", 12.0f, 1.0f, 20.0f);
    ValueNumber fall = new ValueNumber("Fall", "Fall", "Fall distance.", 10, 5, 30);
    ValueNumber holeHp = new ValueNumber("HoleHp", "Hole HP", "Determines at what health it will switch to a totem, inside of a Hole", 6.0f, 1.0f, 20.0f);
    ValueBoolean swordGap = new ValueBoolean("SwordGap", "Sword Gap", "Automatically switch to gap when holding a sword and right clicking.", false);


    @Override
    public void onTick() {
        super.onTick();
        if (super.nullCheck()) {
            return;
        }
        if (mc.player.getHealth() + mc.player.getAbsorptionAmount() <= this.hp.getValue().floatValue() && !HoleUtils.isInHole(mc.player) ||
                (mc.player.fallDistance >= (float) this.fall.getValue().intValue() && !mc.player.isFallFlying() && !HoleUtils.isInHole(mc.player))) {
            if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                InventoryUtils.offhandItem(Items.TOTEM_OF_UNDYING);
            }
            return;
        }
        if (mc.player.getHealth() + mc.player.getAbsorptionAmount() <= this.holeHp.getValue().floatValue() && HoleUtils.isInHole(mc.player)) {
            if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                InventoryUtils.offhandItem(Items.TOTEM_OF_UNDYING);
            }
            return;
        }
        if (mc.player.getMainHandStack().getItem() instanceof SwordItem && this.swordGap.getValue() && mc.mouse.wasRightButtonClicked()) {
            if (mc.player.getOffHandStack().getItem() != Items.ENCHANTED_GOLDEN_APPLE) {
                InventoryUtils.offhandItem(Items.ENCHANTED_GOLDEN_APPLE);
            }
            return;
        }
        if (this.mode.getValue().equals(Modes.Totem)) {
            if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                InventoryUtils.offhandItem(Items.TOTEM_OF_UNDYING);
            }
        } else if (this.mode.getValue().equals(Modes.Crystal)) {
            if (mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL) {
                InventoryUtils.offhandItem(Items.END_CRYSTAL);
            }
        } else if (this.mode.getValue().equals(Modes.Gapple)) {
            if (mc.player.getOffHandStack().getItem() != Items.ENCHANTED_GOLDEN_APPLE) {
                InventoryUtils.offhandItem(Items.ENCHANTED_GOLDEN_APPLE);
            }
        }
    }


    @Override
    public String getHudInfo() {
        return this.mode.getValue().name();
    }

    public enum Modes {
        Totem,
        Crystal,
        Gapple
    }
}

