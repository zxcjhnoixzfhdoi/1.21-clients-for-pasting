package hack.echo.client.features.impl.render.swing;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;

public class SmoothMode extends AbstractMode {

    public SmoothMode() {
        super("Smooth");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int armX, HumanoidArm arm) {
        float n = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        applyEquipOffset(matrices, arm, n);
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        float f1 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.mulPose(Axis.YP.rotationDegrees((float) i * (45.0F + f1 * -20.0F)));
        float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        matrices.mulPose(Axis.ZP.rotationDegrees((float) i * g * -20.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(g * 0.0F));
        matrices.mulPose(Axis.YP.rotationDegrees((float) i * -45.0F));
    }

    private void applyEquipOffset(PoseStack matrices, HumanoidArm arm, float equipProgress) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        matrices.translate((float) i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }
}
