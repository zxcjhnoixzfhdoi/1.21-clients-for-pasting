package hack.echo.client.features.impl.render.swing;

import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.ViewModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;

public abstract class AbstractMode {
    private final String name;

    public AbstractMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract void apply(float swingProgress, PoseStack matrices, int armX, HumanoidArm arm);

    public static void applySwingOffset(PoseStack matrices, HumanoidArm arm, float swingProgress) {
        ViewModel viewModel = Echo.featureManager.getFeatureByClass(ViewModel.class);
        float strength = 1.0f;
        if (viewModel != null && viewModel.isEnabled()) {
            if (Minecraft.getInstance().player != null) {
                strength = arm == Minecraft.getInstance().player.getMainArm() ? viewModel.strengthMain.getValue() : viewModel.strengthOff.getValue();
            } else {
                strength = viewModel.strengthMain.getValue();
            }
        }

        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        float f = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.mulPose(Axis.YP.rotationDegrees((float) i * (45.0F + f * -20.0F)));
        float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        matrices.mulPose(Axis.ZP.rotationDegrees((float) i * g * (-2.0F * strength)));
        matrices.mulPose(Axis.XP.rotationDegrees(g * (-8.0F * strength)));
        matrices.mulPose(Axis.YP.rotationDegrees((float) i * -45.0F));
    }
}