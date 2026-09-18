package hack.echo.client.features;

import hack.echo.client.features.settings.impl.FloatSetting;
import hack.echo.client.render2.impl.opengl.utils.RenderUtil;
import hack.echo.client.utils.strings.Concat;
import lombok.Getter;
import net.minecraft.util.Mth;

public abstract class HudFeature extends Feature {

    @Getter
    private final FloatSetting xPosition = new FloatSetting(Concat.of("Hud Module X Position"), 0.01f, 0f, 1f, 0.01f, s -> false);
    @Getter
    private final FloatSetting yPosition = new FloatSetting(Concat.of("Hud Module Y Position"), 0.01f, 0f, 1f, 0.01f, s -> false);

    public HudFeature(FeatureInfo info) {
        super(info);
    }

    public abstract float getWidth();
    public abstract float getHeight();

    public float getX() {
        float screenWidth = RenderUtil.getScaledWidth();
        float width = getWidth();

        if (screenWidth <= width) {
            return 0f;
        }

        float x = xPosition.getValue() * screenWidth;
        return Mth.clamp(x, 0f, screenWidth - width);
    }

    public float getY() {
        float screenHeight = RenderUtil.getScaledHeight();
        float height = getHeight();

        if (screenHeight <= height) {
            return 0f;
        }

        float y = yPosition.getValue() * screenHeight;
        return Mth.clamp(y, 0f, screenHeight - height);
    }

}
