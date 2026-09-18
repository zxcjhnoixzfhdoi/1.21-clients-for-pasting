package we.devs.opium.client.modules.visuals;

import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.client.values.impl.ValueBoolean;

@RegisterModule(name = "NoRender", description = "stops rendering selected Settings.", tag = "NoRender", category = Module.Category.VISUALS)
public class ModuleNoRender extends Module {
    public ValueBoolean fire = new ValueBoolean("Fire", "Fire", "dont render fire", true);
    public ValueBoolean block = new ValueBoolean("Block", "Block", "dont render block", true);

    public static ModuleNoRender INSTANCE;

    public ModuleNoRender() {
        INSTANCE = this;
    }

    public boolean noFire() {
        return fire.getValue() && isToggled();
    }

    public boolean noBlock() {
        return block.getValue() && isToggled();
    }
}
