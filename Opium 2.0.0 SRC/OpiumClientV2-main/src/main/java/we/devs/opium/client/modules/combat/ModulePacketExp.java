package we.devs.opium.client.modules.combat;

import net.minecraft.item.Items;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.FindItemResult;
import we.devs.opium.api.utilities.InventoryUtil;

@RegisterModule(name = "PacketExp", description = "Throws Exp.", category = Module.Category.COMBAT)
public class ModulePacketExp extends Module {

    @Override
    public void onTick() {
        FindItemResult exp = InventoryUtil.findInHotbar(Items.EXPERIENCE_BOTTLE);
        if (!exp.found()) return;
        if (exp.getHand() != null) {
            mc.interactionManager.interactItem(mc.player, exp.getHand());
        }
        else {
            InventoryUtil.swap(exp.slot(), true);
            mc.interactionManager.interactItem(mc.player, exp.getHand());
            InventoryUtil.swapBack();
        }
    }
}
