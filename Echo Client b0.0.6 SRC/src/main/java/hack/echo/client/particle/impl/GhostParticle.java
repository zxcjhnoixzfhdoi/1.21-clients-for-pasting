package hack.echo.client.particle.impl;

import hack.echo.client.features.impl.render.TargetESP;
import hack.echo.client.features.settings.impl.ColorSetting;
import hack.echo.client.particle.Particle;
import hack.echo.client.utils.MathUtil;
import hack.echo.client.utils.combat.TargetUtils;
import net.minecraft.world.entity.LivingEntity;

import java.awt.Color;

public class GhostParticle extends Particle {
    private final LivingEntity target;
    private final int layer;
    private final int index;
    private final ColorSetting colorSetting;

    public GhostParticle(LivingEntity target, int layer, int index, ColorSetting colorSetting) {
        super(0);
        this.target = target;
        this.layer = layer;
        this.index = index;
        this.colorSetting = colorSetting;
    }

    @Override
    public float getSize() {
        return 0.6f;
    }

    @Override
    public boolean isDead() {
        return !TargetESP.isGhostActive() || TargetUtils.getLastAttackedTarget(400) != target;
    }

    @Override
    public void update(float tickDelta) {
        float iF = (float) index;
        float jOffset = layer * 120f;
        float jMult = layer + 1f;

        float iAge = MathUtil.interpolate(target.tickCount - 1, target.tickCount, tickDelta);
        float ageMultiplier = iAge * 2.5f;

        double radians = Math.toRadians(((iF / 1.5f + iAge) * 8f + jOffset) % 2880.0f);
        double sinQuad = Math.sin(Math.toRadians(ageMultiplier + index * jMult) * 3f) / 1.8f;

        x = MathUtil.interpolate(target.xo, target.getX(), tickDelta) + Math.cos(radians) * target.getBbWidth();
        y = MathUtil.interpolate(target.yo, target.getY(), tickDelta) + 1.0 + sinQuad;
        z = MathUtil.interpolate(target.zo, target.getZ(), tickDelta) + Math.sin(radians) * target.getBbWidth();

        float alphaMul = Math.max(0f, Math.min(1f, iF / 14f));
        Color base = new Color(colorSetting.getRed(), colorSetting.getGreen(), colorSetting.getBlue(), colorSetting.getAlpha());
        color = new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.round(base.getAlpha() * alphaMul)).getRGB();
    }
}
