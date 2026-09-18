package hack.echo.client.screens.clickgui.glass.settings;

import hack.echo.client.features.settings.impl.ColorSetting;
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

public class GlassColorElement implements GlassSettingElement {

    @Getter
    private final ColorSetting setting;

    private float x, y;
    private boolean expanded = false;
    private final Animation expandAnimation = new Animation(Easing.EASE_OUT_CUBIC, 200);

    private boolean draggingSB = false;
    private boolean draggingHue = false;
    private boolean draggingAlpha = false;
    private boolean wasHovered = false;

    public GlassColorElement(ColorSetting setting) {
        this.setting = setting;
        expandAnimation.start(0, 0);
    }

    @Override
    public void render(Draw2D draw, float x, float y, Matrix4f mat, int mouseX, int mouseY, float delta) {
        this.x = x;
        this.y = y;

        float target = expanded ? 1f : 0f;
        expandAnimation.updateTo(target);
        float anim = (float) expandAnimation.getDelta();

        if (draggingSB)
            updateSB(mouseX, mouseY);
        else if (draggingHue)
            updateHue(mouseX);
        else if (draggingAlpha)
            updateAlpha(mouseX);

        float renderX = x + SETTING_INDENT;
        float renderW = PANEL_WIDTH - SETTING_INDENT - 4;

        boolean hovered = isInside(renderX, y, renderW, getHeight(), mouseX, mouseY);
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

        draw.rect(mat, renderX, y, renderW, getHeight(), RADIUS_MD,
                wa((hovered ? SURFACE_SETTING_HOVER : SURFACE_SETTING).getRGB(), delta));
        draw.rect(mat, renderX, y + 2, 1.5f, getHeight() - 4, 0, wa(ACCENT_PRIMARY.getRGB(), delta));

        CharSequence name = ClientUtil.truncateName(setting.getNameSequence(), 14);
        draw.text(Fonts.interSemiBold, mat, name, renderX + PADDING, y + SETTING_HEIGHT / 2 - 4, 6, argbMul(TEXT_ON_SURFACE_SECONDARY, delta));

        float swatchX = renderX + renderW - TOGGLE_WIDTH - PADDING;
        float swatchY = y + (SETTING_HEIGHT - TOGGLE_HEIGHT) / 2;
        draw.rect(mat, swatchX, swatchY, TOGGLE_WIDTH, TOGGLE_HEIGHT, TOGGLE_HEIGHT / 2.3f,
                wa(setting.getARGB(), delta));

        if (anim > 0.01f) {
            float[] hsb = Color.RGBtoHSB(setting.getRed(), setting.getGreen(), setting.getBlue(), null);

            float pickerY = y + SETTING_HEIGHT;
            float pickerH = 60 * anim;
            float pickerX = renderX + PADDING;
            float pickerW = renderW - PADDING * 2;

            draw.sbPicker(mat, pickerX, pickerY, pickerW, pickerH, hsb[0]);

            float sbX = pickerX + pickerW * hsb[1];
            float sbY = pickerY + pickerH * (1 - hsb[2]);
            draw.rect(mat, sbX - 2, sbY - 2, 4, 4, 2f, wa(Color.WHITE.getRGB(), delta * anim));

            float hueY = pickerY + pickerH + 5 * anim;
            draw.hueSlider(mat, pickerX, hueY, pickerW, 5 * anim);
            draw.rect(mat, pickerX + pickerW * hsb[0] - 2, hueY - 2 * anim, 4, 9 * anim, 2f * anim,
                    wa(Color.WHITE.getRGB(), delta * anim));

            if (setting.isShowAlpha()) {
                float alphaY = hueY + 10 * anim;
                float r = setting.getRed() / 255f;
                float g = setting.getGreen() / 255f;
                float b = setting.getBlue() / 255f;
                draw.alphaSlider(mat, pickerX, alphaY, pickerW, 5 * anim, r, g, b);
                draw.rect(mat, pickerX + pickerW * setting.getAlphaNormalized() - 2, alphaY - 2 * anim, 4, 9 * anim,
                        2f * anim,
                        wa(Color.WHITE.getRGB(), delta * anim));
            }
        }
    }

    @Override
    public float getHeight() {
        float anim = (float) expandAnimation.getDelta();
        float baseHeight = SETTING_HEIGHT;
        if (anim > ANIMATION_EPSILON) {
            float extra = setting.isShowAlpha() ? COLOR_PICKER_HEIGHT : COLOR_PICKER_HEIGHT - 10;
            baseHeight += extra * anim;
        }
        return baseHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0)
            return false;

        float renderX = x + SETTING_INDENT;
        float renderW = PANEL_WIDTH - SETTING_INDENT - 4;

        float swatchX = renderX + renderW - TOGGLE_WIDTH - PADDING;
        float swatchY = y + (SETTING_HEIGHT - TOGGLE_HEIGHT) / 2;
        if (isInside(swatchX, swatchY, TOGGLE_WIDTH, TOGGLE_HEIGHT, (float) mouseX, (float) mouseY)) {
            expanded = !expanded;
            SoundUtil.playExpand(expanded);
            return true;
        }

        float anim = (float) expandAnimation.getDelta();
        if (expanded && anim > EXPAND_THRESHOLD) {
            float pickerY = y + SETTING_HEIGHT;
            float pickerH = 60;
            float pickerX = renderX + PADDING;
            float pickerW = renderW - PADDING * 2;
            float hueY = pickerY + 65;
            float alphaY = hueY + 10;

            if (isInside(pickerX, pickerY, pickerW, pickerH, (float) mouseX, (float) mouseY)) {
                resetDragging();
                draggingSB = true;
                updateSB(mouseX, mouseY);
                return true;
            }

            if (isInside(pickerX, hueY, pickerW, 5, (float) mouseX, (float) mouseY)) {
                resetDragging();
                draggingHue = true;
                updateHue(mouseX);
                return true;
            }

            if (setting.isShowAlpha() && isInside(pickerX, alphaY, pickerW, 5, (float) mouseX, (float) mouseY)) {
                resetDragging();
                draggingAlpha = true;
                updateAlpha(mouseX);
                return true;
            }
        }

        return false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingSB || draggingHue || draggingAlpha) {
            setting.notifyChange();
        }
        resetDragging();
    }

    private void resetDragging() {
        draggingSB = false;
        draggingHue = false;
        draggingAlpha = false;
    }

    private void updateSB(double mouseX, double mouseY) {
        float renderX = x + SETTING_INDENT;
        float pickerX = renderX + PADDING;
        float pickerW = (PANEL_WIDTH - SETTING_INDENT - 4) - PADDING * 2;

        float[] hsb = Color.RGBtoHSB(setting.getRed(), setting.getGreen(), setting.getBlue(), null);
        float pickerY = y + SETTING_HEIGHT;

        float sat = Math.clamp((float) (mouseX - pickerX) / pickerW, 0, 1);
        float bri = Math.clamp(1 - (float) (mouseY - pickerY) / 60, 0, 1);

        Color newColor = Color.getHSBColor(hsb[0], sat, bri);
        setting.setRedNoNotify(newColor.getRed());
        setting.setGreenNoNotify(newColor.getGreen());
        setting.setBlueNoNotify(newColor.getBlue());
    }

    private void updateHue(double mouseX) {
        float renderX = x + SETTING_INDENT;
        float pickerX = renderX + PADDING;
        float pickerW = (PANEL_WIDTH - SETTING_INDENT - 4) - PADDING * 2;

        float[] hsb = Color.RGBtoHSB(setting.getRed(), setting.getGreen(), setting.getBlue(), null);
        float hue = Math.clamp((float) (mouseX - pickerX) / pickerW, 0, 1);

        Color newColor = Color.getHSBColor(hue, hsb[1], hsb[2]);
        setting.setRedNoNotify(newColor.getRed());
        setting.setGreenNoNotify(newColor.getGreen());
        setting.setBlueNoNotify(newColor.getBlue());
    }

    private void updateAlpha(double mouseX) {
        float renderX = x + SETTING_INDENT;
        float pickerX = renderX + PADDING;
        float pickerW = (PANEL_WIDTH - SETTING_INDENT - 4) - PADDING * 2;

        setting.setAlphaNoNotify((int) (Math.clamp((float) (mouseX - pickerX) / pickerW, 0, 1) * 255));
    }
}