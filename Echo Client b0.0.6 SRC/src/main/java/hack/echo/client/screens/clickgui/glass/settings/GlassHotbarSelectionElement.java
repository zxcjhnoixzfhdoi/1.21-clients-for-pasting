package hack.echo.client.screens.clickgui.glass.settings;

import hack.echo.client.features.settings.Setting;
import hack.echo.client.features.settings.impl.HotbarSelectionSetting;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.screens.clickgui.TooltipManager;
import hack.echo.client.utils.animation.Animation;
import hack.echo.client.utils.animation.Easing;
import hack.echo.client.utils.audio.SoundUtil;
import hack.echo.client.utils.strings.Concat;
import org.joml.Matrix4f;

import java.awt.*;

import static hack.echo.client.screens.clickgui.glass.GlassUIConstants.*;

public class GlassHotbarSelectionElement implements GlassSettingElement {

    private static final float SLOT_GAP = 1.5f;
    private static final float SLOT_ROW_PAD = 1.5f;

    private final HotbarSelectionSetting setting;
    private float x, y;
    private int wasHoveredSlot = -1;
    private int lastClickedSlot = -1;

    private final Animation clickAnim = new Animation(Easing.EASE_OUT_CUBIC, 150);

    public GlassHotbarSelectionElement(HotbarSelectionSetting setting) {
        this.setting = setting;
        clickAnim.start(0f, 0f);
    }

    private float rowW(float renderW) {
        return renderW - 2 * PADDING;
    }

    private float slotSize(float renderW) {
        return (rowW(renderW) - 8 * SLOT_GAP) / 9f;
    }

    @Override
    public void render(Draw2D draw, float x, float y, Matrix4f mat, int mouseX, int mouseY, float delta) {
        this.x = x;
        this.y = y;

        float renderX = x + SETTING_INDENT;
        float renderW = PANEL_WIDTH - SETTING_INDENT - 4;
        float totalH = getHeight();
        float slotSize = slotSize(renderW);
        float slotY = y + SETTING_HEIGHT + SLOT_ROW_PAD;
        float slotRowLeft = renderX + PADDING;
        float flash = (float) clickAnim.getDelta();

        boolean rowHovered = isInside(renderX, y, renderW, totalH, mouseX, mouseY);

        if (rowHovered) {
            CharSequence description = setting.getDescription();
            if (description != null && description.length() > 0) {
                TooltipManager.request(setting, setting.getNameSequence(), description, mouseX, mouseY);
            }
        }

        draw.rect(mat, renderX, y, renderW, totalH, RADIUS_MD,
                wa((rowHovered ? SURFACE_SETTING_HOVER : SURFACE_SETTING).getRGB(), delta));
        draw.rect(mat, renderX, y + 2, 1.5f, totalH - 4, 0, wa(ACCENT_PRIMARY.getRGB(), delta));

        draw.text(Fonts.interSemiBold, mat, setting.getNameSequence(),
                renderX + PADDING, y + SETTING_HEIGHT / 2f - 4f, 6f,
                argbMul(TEXT_ON_SURFACE_SECONDARY, delta));

        int count = 0;
        for (int i = 0; i < 9; i++) if (setting.isSelected(i)) count++;
        CharSequence res = Concat.of(Concat.ofInt(count), Concat.of("/9"));
        float resW = Fonts.interSemiBold.getWidth(res, 6);
        draw.text(Fonts.interSemiBold, mat, res,
                renderX + renderW - PADDING - resW, y + SETTING_HEIGHT / 2f - 4f, 6f,
                wa(CONTROL_TOGGLE_ON.getRGB(), delta)
        );


        int hoveredSlot = -1;
        for (int i = 0; i < 9; i++) {
            float slotX = slotRowLeft + i * (slotSize + SLOT_GAP);
            boolean slotHovered = isInside(slotX, slotY, slotSize, slotSize, mouseX, mouseY);
            if (slotHovered) hoveredSlot = i;

            boolean selected = setting.isSelected(i);
            Color bg = selected ? CONTROL_TOGGLE_ON : SURFACE_INTERACTIVE_MUTED;
            if (slotHovered) bg = lerpColor(bg, SURFACE_SETTING_HOVER, 0.3f);
            if (i == lastClickedSlot) bg = lerpColor(bg, CONTROL_KNOB, flash * 0.25f);
            draw.rect(mat, slotX, slotY, slotSize, slotSize, RADIUS_SM, wa(bg.getRGB(), delta));
        }

        if (hoveredSlot != wasHoveredSlot) {
            if (hoveredSlot != -1) SoundUtil.playHover();
            wasHoveredSlot = hoveredSlot;
        }
    }

    @Override
    public float getHeight() {
        float renderW = PANEL_WIDTH - SETTING_INDENT - 4;
        return SETTING_HEIGHT + SLOT_ROW_PAD + slotSize(renderW) + SLOT_ROW_PAD;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        float renderW = PANEL_WIDTH - SETTING_INDENT - 4;
        float renderX = x + SETTING_INDENT;
        float slotSize = slotSize(renderW);
        float slotY = y + SETTING_HEIGHT + SLOT_ROW_PAD;
        float slotRowLeft = renderX + PADDING;

        for (int i = 0; i < 9; i++) {
            float slotX = slotRowLeft + i * (slotSize + SLOT_GAP);
            if (isInside(slotX, slotY, slotSize, slotSize, (float) mouseX, (float) mouseY)) {
                setting.toggleSelected(i);
                SoundUtil.playToggle(setting.isSelected(i));
                lastClickedSlot = i;
                clickAnim.start(1f, 0f);
                return true;
            }
        }
        return false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {}

    @Override
    public Setting getSetting() {
        return setting;
    }
}
