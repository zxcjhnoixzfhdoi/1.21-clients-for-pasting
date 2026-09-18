package hack.echo.client.screens.clickgui.glass.settings;

import hack.echo.client.features.settings.impl.KeybindSetting;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.screens.clickgui.TooltipManager;
import hack.echo.client.utils.ClientUtil;
import hack.echo.client.utils.audio.SoundUtil;
import lombok.Getter;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import static hack.echo.client.screens.clickgui.glass.GlassUIConstants.*;

public class GlassKeybindElement implements GlassSettingElement {

    @Getter
    private final KeybindSetting setting;

    private float x, y;
    private boolean listening = false;
    private boolean wasHovered = false;

    public GlassKeybindElement(KeybindSetting setting) {
        this.setting = setting;
    }

    @Override
    public void render(Draw2D draw, float x, float y, Matrix4f mat, int mouseX, int mouseY, float delta) {
        this.x = x;
        this.y = y;
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

        CharSequence name = ClientUtil.truncateName(setting.getNameSequence(), 14);
        draw.text(Fonts.interSemiBold, mat, name, renderX + PADDING, y + SETTING_HEIGHT / 2 - 4, 6, argbMul(TEXT_ON_SURFACE_SECONDARY, delta));

        String keyText = listening ? "..." : ClientUtil.keyLabel(setting.getKey());
        float pillW = Fonts.interSemiBold.getWidth(keyText, 6) + 8;
        float pillX = renderX + renderW - PADDING - pillW;
        float pillY = y + (SETTING_HEIGHT - 10) / 2;

        int pillColor = listening ? wa(ACCENT_PRIMARY.getRGB(), delta) : wa(SURFACE_INTERACTIVE_MUTED.getRGB(), delta);
        int keyColor = listening ? argbMul(TEXT_ON_ACCENT, delta) : argbMul(TEXT_ON_SURFACE_PRIMARY, delta);
        draw.rect(mat, pillX, pillY, pillW, 10, 5f, pillColor);
        draw.text(Fonts.interSemiBold, mat, keyText, pillX + 4, y + SETTING_HEIGHT / 2 - 4, 6, keyColor);
    }

    @Override
    public float getHeight() {
        return SETTING_HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float renderX = x + SETTING_INDENT;
        float renderW = PANEL_WIDTH - SETTING_INDENT - 4;

        if (listening) {
            int mouseBind = 0x80000000 | (button & 0xFF);
            setting.setKey(mouseBind);
            setting.setListening(false);
            listening = false;
            SoundUtil.playClick();
            return true;
        }

        if (button != 0)
            return false;

        String keyText = listening ? "..." : ClientUtil.keyLabel(setting.getKey());
        float pillW = Fonts.interSemiBold.getWidth(keyText, 6) + 8;
        float pillX = renderX + renderW - PADDING - pillW;
        float pillY = y + (SETTING_HEIGHT - 10) / 2;

        if (isInside(pillX, pillY, pillW, 10, (float) mouseX, (float) mouseY)) {
            listening = !listening;
            setting.setListening(listening);
            SoundUtil.playClick();
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                setting.setKey(-1);
            } else {
                setting.setKey(keyCode);
            }
            setting.setListening(false);
            listening = false;
            SoundUtil.playKeypress();
            return true;
        }
        return false;
    }
}
