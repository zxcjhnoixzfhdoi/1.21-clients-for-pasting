package we.devs.opium.client.modules.client;

import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.ChatUtils;
import we.devs.opium.client.values.impl.*;

import static we.devs.opium.Opium.CONFIG_MANAGER;

@RegisterModule(name = "Config Auto Save", description = "Auto Saves Your Configs Based On A Delay", category = Module.Category.CLIENT)
public class ModuleAutoConfigSaving extends Module {
    ValueNumber delayV = new ValueNumber("Delay", "Delay", "Delay Between Automatic Config Saving", 120, 1, 30000);
    ValueString cfg = new ValueString("Save To Config", "Save To Config", "Which Config to save to", "OpiumCfg");
    int delay = 0;
    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onTick() {
        super.onTick();
        delay++;
        if (delay > delayV.getValue().intValue()) {
            CONFIG_MANAGER.save(cfg.getValue());
            delay = 0;
            ChatUtils.sendMessage("Automatically Saved Config", "Auto Config Save");
        }
    }
}
