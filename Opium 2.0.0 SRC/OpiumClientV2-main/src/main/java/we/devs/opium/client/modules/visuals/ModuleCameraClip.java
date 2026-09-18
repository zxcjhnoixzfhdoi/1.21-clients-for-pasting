package we.devs.opium.client.modules.visuals;

import net.minecraft.client.option.Perspective;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.FadeUtils;
import we.devs.opium.client.events.EventRender3D;
import we.devs.opium.client.values.impl.ValueBoolean;
import we.devs.opium.client.values.impl.ValueNumber;

@RegisterModule(name = "CameraClip", description = "CameraClip", tag = "CameraClip", category = Module.Category.VISUALS)
public class ModuleCameraClip extends Module {
    public static ModuleCameraClip INSTANCE = new ModuleCameraClip();

    private final FadeUtils animation = new FadeUtils(300);
    public final ValueNumber distance = new ValueNumber("Distance", "Distance", "Distance", 4, 1, 20);
    public final ValueNumber animateTime = new ValueNumber("AnimationTime", "AnimationTime", "AnimationTime", 200, 0, 1000);
    private final ValueBoolean noFront = new ValueBoolean("NoFront", "NoFront", "NoFront", false);


    boolean first = false;
    @Override
    public void onRender3D(EventRender3D event) {
        if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT && noFront.getValue())
            mc.options.setPerspective(Perspective.FIRST_PERSON);
        animation.setLength(animateTime.getValue().intValue());
        if (mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            if (!first) {
                first = true;
                animation.reset();
            }
        } else {
            if (first) {
                first = false;
                animation.reset();
            }
        }
    }

    public float getDistance() {
        double quad = mc.options.getPerspective() == Perspective.FIRST_PERSON ? 1 - animation.easeOutQuad() : animation.easeOutQuad();
        return (float) (1f + ((distance.getValue().floatValue() - 1f) * quad));
    }
}
