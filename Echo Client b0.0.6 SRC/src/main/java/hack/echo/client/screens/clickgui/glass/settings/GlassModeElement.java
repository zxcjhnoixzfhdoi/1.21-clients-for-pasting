package hack.echo.client.screens.clickgui.glass.settings;

import hack.echo.client.features.settings.impl.ModeSetting;
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

public class GlassModeElement implements GlassSettingElement {

    @Getter
    private final ModeSetting setting;

    private float x, y;
    private boolean expanded = false;
    private final Animation expandAnimation = new Animation(Easing.EASE_OUT_CUBIC, 200);
    private boolean wasHovered = false;

    public GlassModeElement(ModeSetting setting) {
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

        float renderX = x + SETTING_INDENT;
        float renderW = PANEL_WIDTH - SETTING_INDENT - 4;
        float totalHeight = getHeight();

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

        draw.rect(mat, renderX, y, renderW, totalHeight, RADIUS_MD,
                wa((hovered ? SURFACE_SETTING_HOVER : SURFACE_SETTING).getRGB(), delta));
        draw.rect(mat, renderX, y + 2, 1.5f, totalHeight - 4, 0, wa(ACCENT_PRIMARY.getRGB(), delta));

        CharSequence name = ClientUtil.truncateName(setting.getNameSequence(), 14);
        draw.text(Fonts.interSemiBold, mat, name, renderX + PADDING, y + SETTING_HEIGHT / 2 - 4, 6, argbMul(TEXT_ON_SURFACE_SECONDARY, delta));

        CharSequence value = setting.getValue();
        float valueW = Fonts.interSemiBold.getWidth(value, 6);
        draw.text(Fonts.interSemiBold, mat, value, renderX + renderW - PADDING - valueW, y + SETTING_HEIGHT / 2 - 4, 6,
                argbMul(TEXT_ON_SURFACE_MUTED, delta));

        if (anim > ANIMATION_EPSILON) {
            float optionY = y + SETTING_HEIGHT;
            int alpha = (int) (anim * 255);

            for (CharSequence mode : setting.getModes()) {
                boolean selected = setting.is(mode);

                int textColor = selected ? argbMul(withAlpha(TEXT_ON_SURFACE_PRIMARY, alpha), delta)
                        : argbMul(withAlpha(TEXT_ON_SURFACE_MUTED, alpha), delta);
                draw.text(Fonts.interSemiBold, mat, mode, renderX + PADDING + 4, optionY + (SETTING_HEIGHT - 2) / 2 - 4, 6,
                        textColor);

                optionY += SETTING_HEIGHT - 2;
            }
        }
    }

    @Override
    public float getHeight() {
        float anim = (float) expandAnimation.getDelta();
        float baseHeight = SETTING_HEIGHT;
        if (anim > ANIMATION_EPSILON) {
            baseHeight += (SETTING_HEIGHT - 2) * setting.getModes().size() * anim;
        }
        return baseHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0)
            return false;

        float renderX = x + SETTING_INDENT;
        float renderW = PANEL_WIDTH - SETTING_INDENT - 4;

        if (isInside(renderX, y, renderW, SETTING_HEIGHT, (float) mouseX, (float) mouseY)) {
            expanded = !expanded;
            SoundUtil.playExpand(expanded);
            return true;
        }

        float anim = (float) expandAnimation.getDelta();
        if (expanded && anim > EXPAND_THRESHOLD) {
            float optionY = y + SETTING_HEIGHT;
            for (CharSequence mode : setting.getModes()) {
                if (isInside(renderX + PADDING, optionY, renderW - PADDING * 2, SETTING_HEIGHT - 2, (float) mouseX,
                        (float) mouseY)) {
                    setting.setValue(mode);
                    SoundUtil.playToggle(true);
                    return true;
                }
                optionY += SETTING_HEIGHT - 2;
            }
        }
        return false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }
}
