package hack.echo.client.features.impl.movement;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.SprintEvent;
import hack.echo.client.features.Feature;
import hack.echo.client.features.Category;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.utils.strings.Concat;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.handlers.impl.SprintController;
import hack.echo.client.utils.player.PlayerUtils;

public class Sprint extends Feature {
    private static final CharSequence EMPTY = Concat.of();
    private static final CharSequence OMNI = Concat.of("Omni");

	public Sprint() {
		super(new FeatureInfo(
			Concat.of("Sprint"),
			Concat.of("Auto sprint"),
			Category.MOVEMENT
		));
	}

    private final BoolSetting omniSprint = new BoolSetting(Concat.of("Omni"), false);

    public boolean isOmniSprintEnabled() {
        return isEnabled() && omniSprint.getValue();
    }

    @Override
    public CharSequence concat() {
        if (!isOmniSprintEnabled()) {
            return EMPTY;
        }

        return OMNI;
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            SprintController.reset();
            mc.player.setSprinting(false);
        }
        super.onDisable();
    }

    @EventSubscribe
    public void onSprint(SprintEvent event) {
        if (!isEnabled()) {
            return;
        }
        
        if (SprintController.isForceStopSprint()) {
            event.setShouldSprint(false);
            return;
        }
        
        if (PlayerUtils.isMoving()) {
            event.setShouldSprint(true);
        }
    }
}
