package we.devs.opium.client.modules.movement;

import net.minecraft.entity.attribute.EntityAttributes;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.client.values.impl.ValueNumber;

@RegisterModule(name = "Step", tag = "Step", description = "Makes you Step up Blocks", category = Module.Category.MOVEMENT)
public class ModuleStep extends Module {
    public static ValueNumber height = new ValueNumber("Height", "Height", "Changes the amount of blocks you can step up", 1.0, 1.0, 3.0);

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (mc.player.getStepHeight() != height.getValue().doubleValue()) {
            mc.player.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT).setBaseValue(height.getValue().doubleValue());
        }
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            mc.player.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT).setBaseValue(0.6f);
        }
    }
}

