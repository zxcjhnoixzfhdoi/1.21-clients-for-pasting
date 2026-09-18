package hack.echo.client.features.impl.movement;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventMovementInput;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.FloatSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.utils.Imports;
import hack.echo.client.utils.player.MoveUtils;
import hack.echo.client.utils.player.PlayerUtils;
import hack.echo.client.utils.strings.Concat;

public class Speed extends Feature {
    private final ModeSetting mode = new ModeSetting(
            Concat.of("Mode"),
            Concat.of("MinemenClub"),
            Concat.of("MinemenClub"),
            Concat.of("Legit"),
            Concat.of("Grim Entity"),
            Concat.of("Motion"),
            Concat.of("Strafe")
    );
    private final BoolSetting timerAbuse = new BoolSetting("1.004 Timer", false);

    // grim
    private final FloatSetting speed = new FloatSetting
            ("Boost", 1.0f, 0.01f, 0.1f, 0.01f,
                    obj -> mode.is("Grim Entity"));

    // motion
    private final FloatSetting speedMotion = new FloatSetting(
            Concat.of("Motion"), 1.0f, 0.01f, 10.00f, 0.01f,
                        obj -> mode.is(Concat.of("Motion")));

    // strafe
    private final FloatSetting speedStrafe = new FloatSetting(
            Concat.of("Strafe"), 1.0f, 0.01f, 10.00f, 0.01f,
            obj -> mode.is(Concat.of("Strafe")));
    public Speed() {
        super(new FeatureInfo(
                Concat.of("Speed"),
                Concat.of("Increases movement speed"),
                Category.MOVEMENT
        ));
    }

    @EventSubscribe
    public void onEventMove(EventMovementInput e) {
        if (isNull() || !this.isEnabled()) return;
        if (timerAbuse.getValue()) {
            Imports.setTimer(1.004F);
        }
        if (!PlayerUtils.isMoving()) {
            return;
        }
        if (mode.is(Concat.of("MinemenClub"))) {
            if (mc.player.onGround()) {
                MoveUtils.strafe(0.26F);
                e.jump = true;
            }
        } else if (mode.is(Concat.of("Legit"))) {
            if (mc.player.onGround()) {
                e.jump = true;
            }
        } else if (mode.is(Concat.of("Grim Entity"))) {
            if (PlayerUtils.isCollidingWithHitbox(mc.player)) {
                double boost = speed.getValue();
                double yaw = MoveUtils.getDirection();
                MoveUtils.motion((float) boost);
            }
        } else if (mode.is(Concat.of("Motion"))) {
            MoveUtils.motion(speedMotion.getValue());
        } else if (mode.is(Concat.of("Strafe"))) {
            MoveUtils.strafe(speedStrafe.getValue());
        }
    }

    @Override
    public void onDisable() {
        Imports.setTimer(1.0F);
    }
}
