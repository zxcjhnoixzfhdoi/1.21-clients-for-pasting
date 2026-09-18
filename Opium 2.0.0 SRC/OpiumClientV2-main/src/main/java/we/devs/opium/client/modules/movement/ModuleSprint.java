package we.devs.opium.client.modules.movement;

import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.client.values.impl.ValueEnum;

@RegisterModule(name="Sprint", tag="Sprint", description="Always be sprinting.", category=Module.Category.MOVEMENT)
public class ModuleSprint extends Module {
    ValueEnum mode = new ValueEnum("Mode", "Mode", "", modes.Legit);

    @Override
    public void onTick() {
        super.onTick();
        if (this.mode.getValue().equals(modes.Legit)) {
            if (mc.player.forwardSpeed > 0.0f && !mc.player.horizontalCollision) {
                mc.player.setSprinting(true);
            }
        }
        if (this.mode.getValue().equals(modes.Omni)) {
            if(mc.options.forwardKey.isPressed()) {
                mc.player.setSprinting(true);
            }
            if(mc.options.rightKey.isPressed()) {
                mc.player.setSprinting(true);
            }
            if(mc.options.leftKey.isPressed()) {
                mc.player.setSprinting(true);
            }
            if(mc.options.backKey.isPressed()) {
                mc.player.setSprinting(true);
            }
        }
        if (this.mode.getValue().equals(modes.Rage)) {
            mc.player.setSprinting(true);
        }
    }

    public enum modes {
        Legit,
        Omni,
        Rage

    }
}
