package hack.echo.client.features.impl.movement;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.features.Feature;
import hack.echo.client.mixin.accessors.LivingEntityAccessor;
import hack.echo.client.utils.strings.Concat;
import hack.echo.client.features.Category;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.utils.strings.Concat;

public class NoJumpDelay extends Feature {

	public NoJumpDelay() {
		super(new FeatureInfo(
			Concat.of("No Jump Delay"),
			Concat.of("Removes jump cooldown"),
			Category.MOVEMENT
		));
	}

    @EventSubscribe
    public void onTick(EventHandleInput.Early event) {
        if (mc.player == null || mc.level == null) return;

        ((LivingEntityAccessor) mc.player).setNoJumpDelay(0);
    }
}
