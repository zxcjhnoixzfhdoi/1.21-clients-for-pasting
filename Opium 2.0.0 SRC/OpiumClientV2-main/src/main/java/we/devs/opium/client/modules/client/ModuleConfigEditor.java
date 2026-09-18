package we.devs.opium.client.modules.client;

import we.devs.opium.Opium;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;

@RegisterModule(name="Config Manager", tag="Config Manager", description="Manage Your Configs", category=Module.Category.CLIENT)
public class ModuleConfigEditor extends Module {
    public static ModuleConfigEditor INSTANCE;

    public ModuleConfigEditor() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player == null || mc.world == null) {
            this.disable(false);
            return;
        }
        mc.setScreen(Opium.CONFIG_MANAGER_SCREEN);
        this.disable(false);
    }
}
