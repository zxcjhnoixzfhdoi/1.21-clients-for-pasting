package hack.echo.client.features.impl.movement;

import hack.echo.client.event.EventManager;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventMovementInput;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.handlers.RotationHandler;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.util.Mth;


/**
 *  @see hack.echo.client.handlers.RotationHandler
 *  @see hack.echo.client.mixin.entity.MixinEntity#redirectMovementYaw
 *  @see hack.echo.client.mixin.input.MixinKeyboardInput
 *  @see hack.echo.client.mixin.entity.MixinLivingEntity
*
 * @see hack.echo.client.event.impl.EventMovementInput
 */
public class MoveFix extends Feature {

    private static final CharSequence NONE_MODE = Concat.of("None");
    private static final CharSequence SILENT_MODE = Concat.of("Silent");
    private static final CharSequence STRICT_MODE = Concat.of("Strict");

    private final ModeSetting modeSetting = new ModeSetting(
        Concat.of("Mode"),
        SILENT_MODE,
        NONE_MODE,
        SILENT_MODE,
        STRICT_MODE
    );

    public MoveFix() {
        super(new FeatureInfo(
            Concat.of("Move Fix"),
            Concat.of("Silent movement correction for rotations"),
            Category.MOVEMENT
        ));

        EventManager.register(this);
    }

    @Override
    public void toggle() {

    }

    @Override
    public void setEnabled(boolean enabled) {

    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public boolean shouldApplyRotationFix() {
        return !modeSetting.is(NONE_MODE);
    }

    public boolean shouldCorrectMovementInput() {
        if (!shouldApplyRotationFix()) return false;
        return modeSetting.is(SILENT_MODE);
    }

    @EventSubscribe(priority = EventSubscribe.Priority.LOWEST)
    public void onMovementInput(EventMovementInput event) {
        if (mc.player == null) return;
        if (!shouldCorrectMovementInput()) return;
        if (!RotationHandler.isHasSilentRotation()) return;

        event.syncFloatsFromBooleans();

        float forward = event.forwardAmount;
        float sideways = event.sidewaysAmount;

        if (forward == 0 && sideways == 0) return;

        float serverYaw = RotationHandler.getServerYaw();
        float clientYaw = mc.player.getYRot();
        float intendedAngle = (float) Math.toDegrees(getDirection(clientYaw, forward, sideways));

        float closestForward = 0;
        float closestSideways = 0;
        float closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1f; predictedForward <= 1f; predictedForward += 1f) {
            for (float predictedSideways = -1f; predictedSideways <= 1f; predictedSideways += 1f) {
                if (predictedForward == 0 && predictedSideways == 0) continue;

                float predictedAngle = (float) Math.toDegrees(getDirection(serverYaw, predictedForward, predictedSideways));
                float difference = Math.abs(Mth.wrapDegrees(intendedAngle - predictedAngle));

                if (difference >= closestDifference) continue;

                closestDifference = difference;
                closestForward = predictedForward;
                closestSideways = predictedSideways;
            }
        }

        event.forwardAmount = closestForward;
        event.sidewaysAmount = closestSideways;
        event.syncBooleansFromFloats();
    }

    private static double getDirection(float yaw, float forward, float sideways) {
        if (forward < 0) yaw += 180f;

        float multiplier = 1f;
        if (forward < 0) multiplier = -0.5f;
        else if (forward > 0) multiplier = 0.5f;

        if (sideways > 0) yaw -= 90f * multiplier;
        if (sideways < 0) yaw += 90f * multiplier;

        return Math.toRadians(yaw);
    }
}
