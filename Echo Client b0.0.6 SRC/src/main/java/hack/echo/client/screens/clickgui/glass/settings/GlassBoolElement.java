package hack.echo.client.screens.clickgui.glass.settings;

import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.screens.clickgui.TooltipManager;
import hack.echo.client.utils.ClientUtil;
import hack.echo.client.utils.audio.SoundUtil;
import hack.echo.client.utils.animation.Animation;
import hack.echo.client.utils.animation.Easing;
import lombok.Getter;
import org.joml.Matrix4f;

import java.awt.*;

import static hack.echo.client.screens.clickgui.glass.GlassUIConstants.*;

public class GlassBoolElement implements GlassSettingElement {

    @Getter
    private final BoolSetting setting;

    private float x, y;
    private final Animation animation = new Animation(Easing.EASE_OUT_CUBIC, 150);
    private boolean wasHovered = false;

    public GlassBoolElement(BoolSetting setting) {
        this.setting = setting;
        float initial = setting.getValue() ? 1f : 0f;
        animation.start(initial, initial);
    }

    @Override
    public void render(Draw2D draw, float x, float y, Matrix4f mat, int mouseX, int mouseY, float delta) {
        this.x = x;
        this.y = y;

        float target = setting.getValue() ? 1f : 0f;
        animation.updateTo(target);
        float anim = (float) animation.getDelta();

        float renderX = x + SETTING_INDENT;
        float renderW = PANEL_WIDTH - SETTING_INDENT - 4;

        boolean hovered = isInside(renderX, y, renderW, SETTING_HEIGHT, mouseX, mouseY);
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

        draw.rect(mat, renderX, y, renderW, SETTING_HEIGHT, RADIUS_MD,
                wa((hovered ? SURFACE_SETTING_HOVER : SURFACE_SETTING).getRGB(), delta));
        draw.rect(mat, renderX, y + 2, 1.5f, SETTING_HEIGHT - 4, 0, wa(ACCENT_PRIMARY.getRGB(), delta));

        CharSequence name = ClientUtil.truncateName(setting.getNameSequence(), 18);
        draw.text(Fonts.interSemiBold, mat, name, renderX + PADDING, y + SETTING_HEIGHT / 2 - 4, 6, argbMul(TEXT_ON_SURFACE_SECONDARY, delta));

        float toggleX = renderX + renderW - TOGGLE_WIDTH - PADDING;
        float toggleY = y + (SETTING_HEIGHT - TOGGLE_HEIGHT) / 2;

        Color toggleColor = lerpColor(CONTROL_TOGGLE_OFF, CONTROL_TOGGLE_ON, anim);
        draw.rect(mat, toggleX, toggleY, TOGGLE_WIDTH, TOGGLE_HEIGHT, TOGGLE_HEIGHT / 2.3f,
                wa(toggleColor.getRGB(), delta));

        float knobX = toggleX + 1.5f + (TOGGLE_WIDTH - KNOB_SIZE - 3) * anim;
        float knobY = toggleY + 1.5f;
        draw.rect(mat, knobX + 0.5f, knobY + 0.5f, KNOB_SIZE, KNOB_SIZE, KNOB_SIZE / 2,
                wa(SHADOW_KNOB.getRGB(), delta));
        draw.rect(mat, knobX, knobY, KNOB_SIZE, KNOB_SIZE, KNOB_SIZE / 2, wa(CONTROL_KNOB.getRGB(), delta));
    }

    @Override
    public float getHeight() {
        return SETTING_HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0)
            return false;

        float toggleX = x + PANEL_WIDTH - TOGGLE_WIDTH - PADDING - 2;
        float toggleY = y + (SETTING_HEIGHT - TOGGLE_HEIGHT) / 2;

        if (isInside(toggleX, toggleY, TOGGLE_WIDTH, TOGGLE_HEIGHT, (float) mouseX, (float) mouseY)) {
            setting.setValue(!setting.getValue());
            SoundUtil.playToggle(setting.getValue());
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

}
