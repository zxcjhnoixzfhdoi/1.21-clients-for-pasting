package hack.echo.client.features.impl.render.swing;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;

public class DefaultMode extends AbstractMode {

    public DefaultMode() {
        super("Default");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int armX, HumanoidArm arm) {
        float g = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        float h = 0.2F * Mth.sin(Mth.sqrt(swingProgress) * (float) (Math.PI * 2));
        float j = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
        matrices.translate(armX * g, h, j);
        applyItemArmAttackTransform(matrices, arm, swingProgress);
    }

    private void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm humanoidArm, float f) {
        int i = humanoidArm == HumanoidArm.RIGHT ? 1 : -1;
        float g = Mth.sin(f * f * (float) Math.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(i * (45.0F + g * -20.0F)));
        float h = Mth.sin(Mth.sqrt(f) * (float) Math.PI);
        poseStack.mulPose(Axis.ZP.rotationDegrees(i * h * -20.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(h * -80.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(i * -45.0F));
    }
}