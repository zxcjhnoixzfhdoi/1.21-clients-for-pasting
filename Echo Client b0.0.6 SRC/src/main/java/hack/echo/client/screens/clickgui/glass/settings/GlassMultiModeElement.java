package hack.echo.client.screens.clickgui.glass.settings;

import hack.echo.client.features.settings.impl.MultiModeSetting;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.screens.clickgui.TooltipManager;
import hack.echo.client.utils.ClientUtil;
import hack.echo.client.utils.audio.SoundUtil;
import hack.echo.client.utils.animation.Animation;
import hack.echo.client.utils.animation.Easing;
import hack.echo.client.utils.strings.Concat;
import lombok.Getter;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static hack.echo.client.screens.clickgui.glass.GlassUIConstants.*;

public class GlassMultiModeElement implements GlassSettingElement {

    @Getter
    private final MultiModeSetting setting;

    private float x, y;
    private boolean expanded = false;
    private final Animation expandAnimation = new Animation(Easing.EASE_OUT_CUBIC, 200);
    private boolean wasHovered = false;
    private static final int maxVisible = 8;
    private int scrollOffset = 0;

    public GlassMultiModeElement(MultiModeSetting setting) {
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

        int count = 0;
        for (Boolean v : setting.getOptionsView().values())
            if (Boolean.TRUE.equals(v))
                count++;
        CharSequence summary = count == 0 ? Concat.of("None") : Concat.of(Concat.ofInt(count), Concat.of(" sel."));
        float summaryW = Fonts.interSemiBold.getWidth(summary, 6);
        draw.text(Fonts.interSemiBold, mat, summary, renderX + renderW - PADDING - summaryW, y + SETTING_HEIGHT / 2 - 4, 6,
                argbMul(TEXT_ON_SURFACE_MUTED, delta));

        if (anim > ANIMATION_EPSILON) {
            List<Map.Entry<String, Boolean>> options = new ArrayList<>(setting.getOptionsView().entrySet());
            int totalOptions = options.size();
            boolean needsScrolling = totalOptions > maxVisible;
            float optionY = y + SETTING_HEIGHT;
            int alpha = (int) (anim * 255);

            int startIdx = needsScrolling ? scrollOffset : 0;
            int endIdx = needsScrolling ? Math.min(scrollOffset + maxVisible, totalOptions) : totalOptions;
            float currentY = optionY;

            for (int i = startIdx; i < endIdx; i++) {
                Map.Entry<String, Boolean> entry = options.get(i);
                boolean selected = Boolean.TRUE.equals(entry.getValue());

                int textColor = selected ? argbMul(withAlpha(TEXT_ON_SURFACE_PRIMARY, alpha), delta)
                        : argbMul(withAlpha(TEXT_ON_SURFACE_MUTED, alpha), delta);
                draw.text(Fonts.interSemiBold, mat, entry.getKey(), renderX + PADDING + 4,
                        currentY + (SETTING_HEIGHT - 2) / 2 - 4, 6, textColor);

                currentY += SETTING_HEIGHT - 2;
            }
        }
    }

    @Override
    public float getHeight() {
        float anim = (float) expandAnimation.getDelta();
        float baseHeight = SETTING_HEIGHT;
        if (anim > ANIMATION_EPSILON) {
            int totalOptions = setting.getOptionsView().size();
            float optionsH = Math.min(totalOptions * (SETTING_HEIGHT - 2), maxVisible * (SETTING_HEIGHT - 2));
            baseHeight += optionsH * anim;
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
            List<String> modes = new ArrayList<>(setting.getOptionsView().keySet());
            int totalOptions = modes.size();
            boolean needsScrolling = totalOptions > maxVisible;
            float maxContentH = maxVisible * (SETTING_HEIGHT - 2);
            float contentH = Math.min(totalOptions * (SETTING_HEIGHT - 2), maxContentH);
            float optionY = y + SETTING_HEIGHT;

            int startIdx = needsScrolling ? scrollOffset : 0;
            int endIdx = needsScrolling ? Math.min(scrollOffset + maxVisible, totalOptions) : totalOptions;
            float currentY = optionY;

            if (mouseY >= optionY && mouseY <= optionY + contentH) {
                for (int i = startIdx; i < endIdx; i++) {
                    String mode = modes.get(i);
                    if (isInside(renderX + PADDING, currentY, renderW - PADDING * 2, SETTING_HEIGHT - 2, (float) mouseX,
                            (float) mouseY)) {
                        setting.toggle(mode);
                        SoundUtil.playToggle(true);
                        return true;
                    }
                    currentY += SETTING_HEIGHT - 2;
                }
            }
        }
        return false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        float anim = (float) expandAnimation.getDelta();
        if (expanded && anim > EXPAND_THRESHOLD) {
            float renderX = x + SETTING_INDENT;
            float renderW = PANEL_WIDTH - SETTING_INDENT - 4;
            int totalOptions = setting.getOptionsView().size();

            if (totalOptions > maxVisible
                    && isInside(renderX, y, renderW, getHeight(), (float) mouseX, (float) mouseY)) {
                float optionY = y + SETTING_HEIGHT;
                float maxContentH = maxVisible * (SETTING_HEIGHT - 2);
                float contentH = Math.min(totalOptions * (SETTING_HEIGHT - 2), maxContentH);

                if (mouseY >= optionY && mouseY <= optionY + contentH) {
                    if (amount > 0) {
                        scrollOffset = Math.max(0, scrollOffset - 1);
                    } else if (amount < 0) {
                        scrollOffset = Math.min(totalOptions - maxVisible, scrollOffset + 1);
                    }
                    SoundUtil.playScroll();
                    return true;
                }
            }
        }
        return false;
    }
}
