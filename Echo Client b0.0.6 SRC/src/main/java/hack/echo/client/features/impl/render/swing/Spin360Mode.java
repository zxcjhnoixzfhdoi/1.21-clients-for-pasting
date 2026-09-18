package hack.echo.client.features.impl.render.swing;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.HumanoidArm;
import com.mojang.math.Axis;

public class Spin360Mode extends AbstractMode {

    private boolean flip = false;

    public Spin360Mode() {
        super("Spin360");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int armX, HumanoidArm arm) {
        applyEquipOffset(matrices, arm, 0);
        matrices.mulPose(Axis.XP.rotationDegrees(swingProgress * (flip ? 360.0F : -360.0F)));

        if (swingProgress < 0.01f && swingProgress > 0) {
            flip = !flip;
        }
    }

    private void applyEquipOffset(PoseStack matrices, HumanoidArm arm, float equipProgress) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        matrices.translate((float) i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }
}
