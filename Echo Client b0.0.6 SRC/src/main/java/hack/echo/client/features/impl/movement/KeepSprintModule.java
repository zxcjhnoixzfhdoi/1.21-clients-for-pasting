package hack.echo.client.features.impl.movement;

import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.FloatSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.utils.ChatUtils;
import hack.echo.client.utils.strings.Concat;

// @see MixinPlayer.java
public class KeepSprintModule extends Feature {

    public KeepSprintModule() {
        super(new FeatureInfo(
                Concat.of("Keep Sprint"),
                Concat.of("Keeps you sprinting"),
                Category.MOVEMENT
        ));
    }

    public final FloatSetting speedKeptVelocity = new FloatSetting(Concat.of("Speed Kept Velocity"), 0.6f, 0.6f, 1.0f, 0.1f);
    private final BoolSetting shouldSprint = new BoolSetting(Concat.of("Should Sprint"), true);
    // In what state sprint should be modified
    private final ModeSetting sprintCondition = new ModeSetting(Concat.of("Sprint Condition"), Concat.of("Both"), Concat.of("Air"), Concat.of("Ground"));

    public boolean dontModifySprint() {
        if (!shouldSprint.getValue()) return false;
        final boolean onGround = mc.player.onGround();
        if (sprintCondition.is(Concat.of("Air"))) return !onGround;
        else if (sprintCondition.is(Concat.of("Ground"))) return onGround;
        else return sprintCondition.is(Concat.of("Both"));
    }
}
