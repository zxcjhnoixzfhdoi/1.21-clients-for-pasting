package we.devs.opium.client.modules.movement;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.client.values.impl.ValueBoolean;

@RegisterModule(name = "NoSlow", description = "NoSlow", tag = "NoSlow", category = Module.Category.MOVEMENT)
public class ModuleNoSlow extends Module {
    public static ModuleNoSlow INSTANCE;
    public ValueBoolean items = new ValueBoolean("Items", "Items", "Items", true);
    public ValueBoolean gui = new ValueBoolean("Gui", "Gui", "Gui", true);

    public ModuleNoSlow() {
        INSTANCE = this;
    }

    @Override
    public void onTick() {
        if (gui.getValue() && mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen)) {
            KeyBinding.setKeyPressed(mc.options.forwardKey.getDefaultKey(), InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.forwardKey.getDefaultKey().getCode()));
            KeyBinding.setKeyPressed(mc.options.backKey.getDefaultKey(), InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.backKey.getDefaultKey().getCode()));
            KeyBinding.setKeyPressed(mc.options.leftKey.getDefaultKey(), InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.leftKey.getDefaultKey().getCode()));
            KeyBinding.setKeyPressed(mc.options.rightKey.getDefaultKey(), InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.rightKey.getDefaultKey().getCode()));
            KeyBinding.setKeyPressed(mc.options.jumpKey.getDefaultKey(), InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.jumpKey.getDefaultKey().getCode()));
            KeyBinding.setKeyPressed(mc.options.sprintKey.getDefaultKey(), InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.sprintKey.getDefaultKey().getCode()));
            KeyBinding.setKeyPressed(mc.options.sneakKey.getDefaultKey(), InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.sneakKey.getDefaultKey().getCode()));
        }
    }

    public boolean getGui() {
        return gui.getValue() && isToggled();
    }

    public boolean getItems() {
        return items.getValue() && isToggled();
    }
}
