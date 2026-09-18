package hack.echo.client.features.impl.movement;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.FloatSetting;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class StepModule extends Feature {
    public StepModule() {
        super(new FeatureInfo(
                Concat.of("Step"),
                Concat.of("Changes the step height of the player"),
                Category.MOVEMENT,
                false
        ));
    }

    private final FloatSetting stepHeightSetting = new FloatSetting(Concat.of("Height"), 1.0f, 1.0f, 3.0f, 0.1f);
    private final BoolSetting disableOnSneak = new BoolSetting(Concat.of("Disable on sneak"), false);

    private void setStepHeight(float stepHeight) {
        mc.player.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(stepHeight);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        setStepHeight(0.6f);
    }


    // Source: CCBLUEX
    /**
     * A common jump looks like this:
     * PlayerMoveC2SPacket 207.30000001192093 62.0 149.86302076816213 0.0 0.0 true
     * PlayerMoveC2SPacket 207.30000001192093 62.41999998688698 149.86517219525172 0.0 0.0 false
     * PlayerMoveC2SPacket 207.30000001192093 62.7531999805212 149.8656024806536 0.0 0.0 false
     * PlayerMoveC2SPacket 207.28040473521648 63.00133597911215 149.86603276605547 0.0 0.0 false
     * PlayerMoveC2SPacket 207.23709917391574 63.166109260938214 149.8665921370619 0.0 0.0 false
     * PlayerMoveC2SPacket 207.17221725301053 63.24918707874468 149.86715150806833 0.0 0.0 false
     * PlayerMoveC2SPacket 207.0877008442994 63.25220334025373 149.86771087907476 0.0 0.0 false
     * PlayerMoveC2SPacket 206.98531705116994 63.17675927506424 149.8682702500812 0.0 0.0 false
     * PlayerMoveC2SPacket 206.86667393775122 63.024424088213685 149.86882962108763 0.0 0.0 false
     * PlayerMoveC2SPacket 206.73323484244284 63.0 149.86938899209406 0.0 0.0 true
     */

    @EventSubscribe
    public void onTick(EventTick event) {
        if (isNull()) return;

        if (mc.player.isShiftKeyDown() && disableOnSneak.getValue()) return;
        setStepHeight(stepHeightSetting.getValue());
    }


}
