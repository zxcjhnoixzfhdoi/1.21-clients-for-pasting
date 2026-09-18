package hack.echo.client.features.impl.render.swing;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.util.Mth;

public class ScaleMode extends AbstractMode {

    public ScaleMode() {
        super("Scale");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int armX, HumanoidArm arm) {
        applyEquipOffset(matrices, arm, 0);
        float a = -Mth.sin(swingProgress * 3f) / 2f + 1f;
        matrices.scale(a, a, a);
    }

    private void applyEquipOffset(PoseStack matrices, HumanoidArm arm, float equipProgress) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        matrices.translate((float) i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }
}
