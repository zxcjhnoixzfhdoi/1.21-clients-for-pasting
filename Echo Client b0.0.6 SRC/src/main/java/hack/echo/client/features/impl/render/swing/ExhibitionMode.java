package hack.echo.client.features.impl.render.swing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class ExhibitionMode extends AbstractMode {

    public ExhibitionMode() {
        super("Exhibition");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int armX, HumanoidArm arm) {
        if (arm != Minecraft.getInstance().player.getMainArm()) {
            applySwingOffset(matrices, arm, swingProgress);
            return;
        }

        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        matrices.translate(i * -0.1f, 0.2f, 0.0f);
        float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        matrices.mulPose(Axis.ZP.rotationDegrees(i * g * 10.0f));
        matrices.mulPose(Axis.XP.rotationDegrees(g * -35.0f));
        matrices.mulPose(Axis.XP.rotationDegrees(-102.25f));
        matrices.mulPose((arm == HumanoidArm.RIGHT ? Axis.YP : Axis.YN).rotationDegrees(13.365f));
        matrices.mulPose((arm == HumanoidArm.RIGHT ? Axis.ZP : Axis.ZN).rotationDegrees(78.05f));
    }
}
