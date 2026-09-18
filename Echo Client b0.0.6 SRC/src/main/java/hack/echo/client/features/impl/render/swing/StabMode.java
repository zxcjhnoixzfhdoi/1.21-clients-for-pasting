package hack.echo.client.features.impl.render.swing;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;

public class StabMode extends AbstractMode {

    public StabMode() {
        super("Stab");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int armX, HumanoidArm arm) {
        float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        applyEquipOffset(matrices, arm, 0);
        matrices.translate(0, 0, -g / 4f);
        matrices.mulPose(Axis.XP.rotationDegrees(-120f));
    }

    private void applyEquipOffset(PoseStack matrices, HumanoidArm arm, float equipProgress) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        matrices.translate((float) i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }
}
