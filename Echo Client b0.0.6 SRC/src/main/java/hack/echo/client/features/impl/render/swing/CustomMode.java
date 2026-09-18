package hack.echo.client.features.impl.render.swing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class CustomMode extends AbstractMode {

    public CustomMode() {
        super("Custom");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int armX, HumanoidArm arm) {
        applySwingOffset(matrices, arm, swingProgress);
        float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);

        if (arm != Minecraft.getInstance().player.getMainArm()) {
            applySwingOffset(matrices, arm, swingProgress);
            return;
        }

        matrices.mulPose(Axis.XP.rotationDegrees(70f));
        matrices.mulPose(Axis.YP.rotationDegrees(-30f * (1f - g) - 30f));
        matrices.mulPose(Axis.ZP.rotationDegrees(110f));


    }
}