package we.devs.opium.client.modules.player;

import net.minecraft.item.Items;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.client.values.impl.ValueBoolean;

@RegisterModule(name = "FastPlace", description = "Sets your use cooldown to none.", category = Module.Category.PLAYER)
public class ModuleFastPlace extends Module {
    ValueBoolean exp = new ValueBoolean("EXP", "EXP", "Throws EXP fast.", false);
    ValueBoolean blocks = new ValueBoolean("Blocks", "Blocks", "Places blocks faster.", false);

    @Override
    public void onUpdate() {
        if (exp.getValue()) {
            if (nullCheck()) return;
            if (mc.player.isHolding(Items.EXPERIENCE_BOTTLE)) {
                mc.itemUseCooldown = 0;
            }
        }
        if (blocks.getValue()) {
            if (nullCheck()) return;
            mc.itemUseCooldown = 0;
        }
    }
}
