package we.devs.opium.client.modules.movement;

import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.client.values.impl.ValueNumber;
import net.minecraft.util.math.Vec3d;

@RegisterModule(name = "ReverseStep", tag = "ReverseStep", description = "Makes you step down Blocks", category = Module.Category.MOVEMENT)
public class ModuleReverseStep extends Module {
    public static ValueNumber height = new ValueNumber("Height", "Height", "Changes the amount of blocks you can step down", 1.0, 1.0, 3.0);
    public static ValueNumber fallSpeed = new ValueNumber("FallSpeed", "Fall Speed", "Changes How quickly you step down blocks", 3.0, 0.0, 3.0);

    @Override
    public void onTick() {
        if (mc.player != null && mc.player.isOnGround() && mc.player.fallDistance <= height.getValue().doubleValue() && !mc.player.isTouchingWater() && !mc.player.isInLava()) {
            Vec3d currentVelocity = mc.player.getVelocity();
            mc.player.setVelocity(currentVelocity.x, -fallSpeed.getValue().doubleValue(), currentVelocity.z);
        }
    }
}


