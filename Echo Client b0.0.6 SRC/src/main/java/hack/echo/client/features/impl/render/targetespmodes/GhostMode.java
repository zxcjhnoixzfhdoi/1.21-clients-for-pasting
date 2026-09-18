package hack.echo.client.features.impl.render.targetespmodes;

import hack.echo.client.Echo;
import hack.echo.client.event.impl.EventRender3D;
import hack.echo.client.features.settings.impl.ColorSetting;
import hack.echo.client.particle.impl.GhostParticle;
import hack.echo.client.utils.combat.TargetUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

public class GhostMode {
    private final ColorSetting colorSetting;
    private LivingEntity lastTarget;
    private static final Minecraft mc = Minecraft.getInstance();

    public GhostMode(ColorSetting colorSetting) {
        this.colorSetting = colorSetting;
    }

    public void reset() { lastTarget = null; }

    public void onRender3D(EventRender3D e) {
        if (mc.player == null || mc.level == null) return;

        LivingEntity target = TargetUtils.getLastAttackedTarget(400);
        if (target == null) {
            lastTarget = null;
            return;
        }

        if (target != lastTarget) {
            lastTarget = target;
            for (int layer = 0; layer < 3; layer++) {
                for (int i = 0; i <= 14; i++) {
                    Echo.particleManager.addParticle(new GhostParticle(target, layer, i, colorSetting));
                }
            }
        }
    }
}
