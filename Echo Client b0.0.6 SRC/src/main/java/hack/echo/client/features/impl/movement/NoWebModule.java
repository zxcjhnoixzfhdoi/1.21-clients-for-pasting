package hack.echo.client.features.impl.movement;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.FloatSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.utils.player.MoveUtils;
import hack.echo.client.utils.player.PlayerIntersectionUtil;
import hack.echo.client.utils.strings.Concat;

public class NoWebModule extends Feature {
    public NoWebModule() {
        super(new FeatureInfo(
                Concat.of("No Web"),
                Concat.of("No Web Slowdown"),
                Category.MOVEMENT
        ));
    }

    private final ModeSetting modeSetting = new ModeSetting(Concat.of("Mode"), Concat.of("Grim"), Concat.of("Grim"));
    private final FloatSetting speedSetting = new FloatSetting(Concat.of("Speed"), 0.35f, 0.35f, 0.65f, 0.1f);

    @EventSubscribe
    private void onTick(EventTick e) {
        if (isNull() || !isEnabled() || !modeSetting.is(Concat.of("Grim"))) {
            return;
        }

        if (!PlayerIntersectionUtil.isPlayerInWeb()) {
            return;
        }

        final double horizontalSpeed = speedSetting.getValue();
        final double yaw = MoveUtils.getDirection();
        final double x = -Math.sin(yaw) * horizontalSpeed;
        final double z = Math.cos(yaw) * horizontalSpeed;
        final double y = mc.options.keyJump.isDown() ? 0.65D : (mc.options.keyShift.isDown() ? -0.65D : 0.0D);

        mc.player.setDeltaMovement(
                mc.player.getDeltaMovement().x + x,
                y,
                mc.player.getDeltaMovement().z + z
        );
    }
}
