package hack.echo.client.features.impl.render;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventChunkOcclusion;
import hack.echo.client.event.impl.EventRender3D;
import hack.echo.client.event.impl.EventStartAttack;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.FloatSetting;
import hack.echo.client.utils.strings.Concat;
import lombok.Getter;
import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class Freecam extends Feature {
    public Freecam() {
        super(new FeatureInfo(
                Concat.of("Freecam"),
                Concat.of("Free movement of camera"),
                Category.RENDER
        ));
    }

    @Getter
    private Vec3 position;
    @Getter
    private float yaw, pitch;
    private CameraType cameraType = CameraType.FIRST_PERSON;
    private final FloatSetting speedSetting = new
            FloatSetting(Concat.of("Speed"), 1.0f, 1.0f, 5.0f, 0.1f);

    @Override
    public void onEnable() {
        super.onEnable();
        if (isNull()) return;
        if (mc.player == null) {
            this.setEnabled(false);
            return;
        }
        position = mc.player.getEyePosition();
        CameraType currentType = mc.options.getCameraType();
        cameraType = currentType != null ? currentType : CameraType.FIRST_PERSON;
        yaw = mc.player.getYRot();
        pitch = mc.player.getXRot();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        position = null;
        if (mc == null || mc.options == null) return;
        if (cameraType != null && mc.options.getCameraType() != cameraType) {
            try {
                mc.options.setCameraType(cameraType);
            } catch (Exception e) {
            }
        }
    }

    public void rotate(float yawDelta, float pitchDelta) {
        this.yaw += yawDelta * 0.15f;
        this.pitch = Mth.clamp(this.pitch + pitchDelta * 0.15f, -90f, 90f);
    }

    @EventSubscribe
    private void onChunkOcclusion(EventChunkOcclusion e) {
        e.cancel();
    }

    @EventSubscribe
    private void onMove(EventRender3D event) {
        if (isNull()) return;
        if (position == null) return;

        float speed = speedSetting.getValue() * event.getDeltaTicks();
        Vec3 look = Vec3.directionFromRotation(pitch, yaw);
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();

        if (mc.options.keyUp.isDown()) position = position.add(look.scale(speed));
        if (mc.options.keyDown.isDown()) position = position.subtract(look.scale(speed));
        if (mc.options.keyLeft.isDown()) position = position.subtract(right.scale(speed));
        if (mc.options.keyRight.isDown()) position = position.add(right.scale(speed));
        if (mc.options.keyJump.isDown()) position = position.add(0, speed, 0);
        if (mc.options.keyShift.isDown()) position = position.subtract(0, speed, 0);
    }

    @EventSubscribe
    private void onStartAttack(EventStartAttack eventStartAttack) {
        eventStartAttack.cancel();
    }

    @EventSubscribe
    private void onTick(EventTick eventTick) {
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);

        }
    }

//    @EventSubscribe
//    private void onSetPerspective(EventSetPerspective event) {
//        event.cancel();
//    }
}
