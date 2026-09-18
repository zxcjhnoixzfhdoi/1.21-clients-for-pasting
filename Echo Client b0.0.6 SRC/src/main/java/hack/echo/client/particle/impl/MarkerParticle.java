package hack.echo.client.particle.impl;

import hack.echo.client.features.impl.render.TargetESP;
import hack.echo.client.features.settings.impl.ColorSetting;
import hack.echo.client.particle.Particle;
import hack.echo.client.utils.MathUtil;
import hack.echo.client.utils.combat.TargetUtils;
import net.minecraft.world.entity.LivingEntity;

import java.awt.Color;

public class MarkerParticle extends Particle {
    private final LivingEntity target;
    private final ColorSetting colorSetting;
    private float rotationAngle = 0f;
    private float rotationSpeed = 0f;
    private boolean isReversing = false;
    private long lastUpdateTime = System.currentTimeMillis();

    private static final float MAX_ROTATION_SPEED = 5.0f;
    private static final float TIME_FACTOR = 0.2f;

    public MarkerParticle(LivingEntity target, ColorSetting colorSetting) {
        super(0);
        this.target = target;
        this.colorSetting = colorSetting;
    }

    @Override
    public boolean isOverlay() {
        return true;
    }

    @Override
    public boolean isDead() {
        return !TargetESP.isMarkerActive() || TargetUtils.getLastAttackedTarget(400) != target;
    }

    @Override
    public float getSize() {
        return 0.9f;
    }

    @Override
    public void update(float tickDelta) {
        var hitBox = target.getBoundingBox();
        x = MathUtil.interpolate(target.xo, target.getX(), tickDelta);
        y = MathUtil.interpolate(target.yo, target.getY(), tickDelta) + (hitBox.maxY - hitBox.minY) / 2.0;
        z = MathUtil.interpolate(target.zo, target.getZ(), tickDelta);

        long currentTime = System.currentTimeMillis();
        float deltaSeconds = (currentTime - lastUpdateTime) * TIME_FACTOR;
        lastUpdateTime = currentTime;

        if (!isReversing) {
            rotationSpeed += 0.02f * deltaSeconds;
            if (rotationSpeed > MAX_ROTATION_SPEED) {
                rotationSpeed = MAX_ROTATION_SPEED;
                isReversing = true;
            }
        } else {
            rotationSpeed -= 0.02f * deltaSeconds;
            if (rotationSpeed < -MAX_ROTATION_SPEED) {
                rotationSpeed = -MAX_ROTATION_SPEED;
                isReversing = false;
            }
        }

        rotationAngle = (rotationAngle + rotationSpeed * deltaSeconds + 360) % 360;
        rotation = (float) Math.toRadians(rotationAngle);
        color = new Color(colorSetting.getRed(), colorSetting.getGreen(), colorSetting.getBlue(), colorSetting.getAlpha()).getRGB();
    }
}
