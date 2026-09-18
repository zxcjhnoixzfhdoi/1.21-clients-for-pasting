package hack.echo.client.features.impl.movement;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventMovementInput;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.utils.strings.Concat;

/**
 * @See hack.echo.client.mixin.input.MixinKeyboardInput
 */
public class SnapTap extends Feature {

    private boolean wasLeftPressed = false;
    private boolean wasRightPressed = false;
    private boolean wasForwardPressed = false;
    private boolean wasBackPressed = false;

    private boolean lastHorizontal = false;
    private boolean lastVertical = false;

    public SnapTap() {
        super(new FeatureInfo(
            Concat.of("Null Move"),
            Concat.of("Instant direction changes"),
            Category.MOVEMENT,
            false
        ));
    }

    @EventSubscribe
    public void onMove(EventMovementInput e) {
		if (isNull()) return;
		if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;
        boolean leftJustPressed = e.left && !wasLeftPressed;
        boolean rightJustPressed = e.right && !wasRightPressed;

        if (leftJustPressed && !rightJustPressed) {
            lastHorizontal = false;
        } else if (rightJustPressed && !leftJustPressed) {
            lastHorizontal = true;
        }

        if (e.left && e.right) {
            if (lastHorizontal) {
                e.left = false;
            } else {
                e.right = false;
            }
        }

        boolean forwardJustPressed = e.forward && !wasForwardPressed;
        boolean backJustPressed = e.back && !wasBackPressed;

        if (forwardJustPressed && !backJustPressed) {
            lastVertical = true;
        } else if (backJustPressed && !forwardJustPressed) {
            lastVertical = false;
        }

        if (e.forward && e.back) {
            if (lastVertical) {
                e.back = false;
            } else {
                e.forward = false;
            }
        }

        wasLeftPressed = e.originalLeft;
        wasRightPressed = e.originalRight;
        wasForwardPressed = e.originalForward;
        wasBackPressed = e.originalBack;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        wasLeftPressed = false;
        wasRightPressed = false;
        wasForwardPressed = false;
        wasBackPressed = false;
    }
}
