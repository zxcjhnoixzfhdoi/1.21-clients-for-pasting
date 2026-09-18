package we.devs.opium.client.modules.client;

import we.devs.opium.Opium;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;

@RegisterModule(name="Life Statisfaction", tag="Live Statisfaction", description="Manage Your Life Great Again", category=Module.Category.CLIENT)
public class ModuleLifeSatisfaction extends Module {
    public static ModuleLifeSatisfaction INSTANCE;

    public ModuleLifeSatisfaction() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player == null || mc.world == null) {
            this.disable(false);
            return;
        }
        mc.setScreen(Opium.GAMBLING_SCREEN);
        this.disable(false);
    }
}
