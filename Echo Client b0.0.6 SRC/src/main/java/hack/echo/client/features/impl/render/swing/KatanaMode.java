package hack.echo.client.features.impl.render.swing;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;

public class KatanaMode extends AbstractMode {

    public KatanaMode() {
        super("Katana");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int armX, HumanoidArm arm) {
        float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        applyEquipOffset(matrices, arm, 0);
        matrices.mulPose(Axis.XP.rotationDegrees(50f));
        matrices.mulPose(Axis.YP.rotationDegrees(-30f * (1f - g) - 30f));
        matrices.mulPose(Axis.ZP.rotationDegrees(110f));
    }

    private void applyEquipOffset(PoseStack matrices, HumanoidArm arm, float equipProgress) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        matrices.translate((float) i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }
}
