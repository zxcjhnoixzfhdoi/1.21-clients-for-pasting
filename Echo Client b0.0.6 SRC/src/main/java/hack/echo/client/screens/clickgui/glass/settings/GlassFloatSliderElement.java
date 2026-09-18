package hack.echo.client.screens.clickgui.glass.settings;

import hack.echo.client.features.settings.impl.FloatSetting;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.screens.clickgui.TooltipManager;
import hack.echo.client.utils.ClientUtil;
import hack.echo.client.utils.audio.SoundUtil;
import hack.echo.client.utils.animation.Animation;
import hack.echo.client.utils.animation.Easing;
import lombok.Getter;
import org.joml.Matrix4f;

import static hack.echo.client.screens.clickgui.glass.GlassUIConstants.*;

public class GlassFloatSliderElement implements GlassSettingElement {

    @Getter
    private final FloatSetting setting;

    private float x, y;
    private boolean dragging = false;
    private final Animation animation = new Animation(Easing.EASE_OUT_CUBIC, 150);
    private float lastSoundValue = Float.MIN_VALUE;
    private boolean wasHovered = false;

    public GlassFloatSliderElement(FloatSetting setting) {
        this.setting = setting;
        float initial = getProgress();
        animation.start(initial, initial);
    }

    @Override
    public void render(Draw2D draw, float x, float y, Matrix4f mat, int mouseX, int mouseY, float delta) {
        this.x = x;
        this.y = y;
        if (dragging) {
            updateValue(mouseX);
        }

        float target = getProgress();
        animation.updateTo(target);
        float anim = (float) animation.getDelta();

        float renderX = x + SETTING_INDENT;
        float renderW = PANEL_WIDTH - SETTING_INDENT - 4;

        boolean hovered = isInside(renderX, y, renderW, SLIDER_HEIGHT, mouseX, mouseY);
        if (hovered && !wasHovered) {
            SoundUtil.playHover();
        }
        wasHovered = hovered;

        if (hovered) {
            CharSequence description = setting.getDescription();
            if (description != null && description.length() > 0) {
                TooltipManager.request(setting, setting.getNameSequence(), description, mouseX, mouseY);
            }
        }

        draw.rect(mat, renderX, y, renderW, SLIDER_HEIGHT, RADIUS_MD,
                wa((hovered ? SURFACE_SETTING_HOVER : SURFACE_SETTING).getRGB(), delta));
        draw.rect(mat, renderX, y + 2, 1.5f, SLIDER_HEIGHT - 4, 0, wa(ACCENT_PRIMARY.getRGB(), delta));

        CharSequence name = ClientUtil.truncateName(setting.getNameSequence(), 16);
        draw.text(Fonts.interSemiBold, mat, name, renderX + PADDING, y + 3, 6, argbMul(TEXT_ON_SURFACE_SECONDARY, delta));

        CharSequence value = ClientUtil.floatSequence(setting.getValue(), 2);
        float valueW = Fonts.interSemiBold.getWidth(value, 6);
        draw.text(Fonts.interSemiBold, mat, value, renderX + renderW - PADDING - valueW, y + 3, 6, argbMul(TEXT_ON_SURFACE_MUTED, delta));

        float sliderY = y + SETTING_HEIGHT;
        float trackStart = renderX + PADDING;
        float trackEnd = renderX + renderW - PADDING;
        float trackWidth = trackEnd - trackStart;

        draw.rect(mat, trackStart, sliderY, trackWidth, 3, 1.5f, wa(SURFACE_INTERACTIVE_MUTED.getRGB(), delta));
        draw.rect(mat, trackStart, sliderY, trackWidth * anim, 3, 1.5f, wa(SURFACE_CONTROL_ELEVATED.getRGB(), delta));
        draw.rect(mat, trackStart + trackWidth * anim - 4, sliderY - 2, 8, 7, 4f, wa(CONTROL_KNOB.getRGB(), delta));
    }

    @Override
    public float getHeight() {
        return SLIDER_HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0)
            return false;

        float renderX = x + SETTING_INDENT;
        float renderW = PANEL_WIDTH - SETTING_INDENT - 4;
        float sliderY = y + SETTING_HEIGHT;
        float trackStart = renderX + PADDING;
        float trackEnd = renderX + renderW - PADDING;

        if (isInside(trackStart - 6, sliderY - 5, (trackEnd - trackStart) + 12, 12, (float) mouseX, (float) mouseY)) {
            dragging = true;
            lastSoundValue = setting.getValue();
            updateValue(mouseX);
            SoundUtil.playClick();
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            setting.notifyChange();
        }
        dragging = false;
    }

    private void updateValue(double mouseX) {
        float renderX = x + SETTING_INDENT;
        float renderW = PANEL_WIDTH - SETTING_INDENT - 4;
        float trackStart = renderX + PADDING;
        float trackWidth = renderW - PADDING * 2;
        float percent = Math.clamp((float) (mouseX - trackStart) / trackWidth, 0, 1);
        float newVal = setting.getMinValue() + (setting.getMaxValue() - setting.getMinValue()) * percent;
        setting.setValueNoNotify(newVal);

        float range = setting.getMaxValue() - setting.getMinValue();
        float threshold = range * 0.03f;
        if (lastSoundValue == Float.MIN_VALUE || Math.abs(newVal - lastSoundValue) >= threshold) {
            lastSoundValue = newVal;
            SoundUtil.playSliderChange();
        }
    }

    private float getProgress() {
        return (setting.getValue() - setting.getMinValue()) / (setting.getMaxValue() - setting.getMinValue());
    }
}
