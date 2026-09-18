package hack.echo.client.features.impl.render.targetespmodes;

import hack.echo.client.Echo;
import hack.echo.client.event.impl.EventRender3D;
import hack.echo.client.features.settings.impl.ColorSetting;
import hack.echo.client.particle.impl.MarkerParticle;
import hack.echo.client.utils.combat.TargetUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

public class MarkerMode {
    private final ColorSetting colorSetting;
    private LivingEntity lastTarget;
    private static final Minecraft mc = Minecraft.getInstance();

    public MarkerMode(ColorSetting colorSetting) {
        this.colorSetting = colorSetting;
    }

    public void reset() { lastTarget = null; }

    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.level == null) return;

        LivingEntity target = TargetUtils.getLastAttackedTarget(400);
        if (target == null) {
            lastTarget = null;
            return;
        }

        if (target != lastTarget) {
            lastTarget = target;
            Echo.particleManager.addParticle(new MarkerParticle(target, colorSetting));
        }
    }
}
