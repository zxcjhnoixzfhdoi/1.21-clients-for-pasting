package hack.echo.client.screens.clickgui.glass.settings;

import hack.echo.client.features.settings.impl.StringSetting;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.screens.clickgui.TooltipManager;
import hack.echo.client.utils.ClientUtil;
import hack.echo.client.screens.ScreenManager;
import hack.echo.client.utils.audio.SoundUtil;
import hack.echo.client.utils.animation.Animation;
import hack.echo.client.utils.animation.Easing;
import lombok.Getter;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static hack.echo.client.screens.clickgui.glass.GlassUIConstants.*;

public class GlassStringElement implements GlassSettingElement {

    @Getter
    private final StringSetting setting;

    private float x, y;
    private int cursorPos = 0;
    private final Animation cursorBlinkAnim = new Animation(Easing.EASE_IN_OUT_QUART, 400);
    private boolean wasHovered = false;
    private boolean open = false;
    private final Animation expandAnimation = new Animation(Easing.EASE_OUT_CUBIC, 200);
    private int scrollOffset = 0;
    private final int maxVisible = 5;

    public GlassStringElement(StringSetting setting) {
        this.setting = setting;
        this.cursorPos = setting.getValue().length();
        expandAnimation.start(0, 0);
    }

    private float getBaseHeight() {
        return SETTING_HEIGHT + 10 + (open ? 0 : 5);
    }

    private float getDropdownHeight() {
        if (!setting.isAllowMultiple() || setting.getEntries().isEmpty()) {
            return 0;
        }
        int visibleCount = Math.min(maxVisible, setting.getEntries().size());
        float entryHeight = 14f;
        return visibleCount * entryHeight + Math.max(0, visibleCount - 1) * SETTING_SPACING + PADDING * 2;
    }

    @Override
    public void render(Draw2D draw, float x, float y, Matrix4f mat, int mouseX, int mouseY, float delta) {
        this.x = x;
        this.y = y;
        float target = open ? 1f : 0f;
        expandAnimation.updateTo(target);
        float anim = (float) expandAnimation.getDelta();

        float renderX = x + SETTING_INDENT;
        float renderW = PANEL_WIDTH - SETTING_INDENT - 4;
        float dropdownH = getDropdownHeight() * anim;
        float totalHeight = getBaseHeight() + dropdownH;

        boolean hovered = isInside(renderX, y, renderW, totalHeight, mouseX, mouseY);
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

        CharSequence name = ClientUtil.truncateName(setting.getNameSequence(), 16);
        draw.text(Fonts.interSemiBold, mat, name, renderX + PADDING, y + 3, 6, argbMul(TEXT_ON_SURFACE_SECONDARY, delta));

        float inputY = y + SETTING_HEIGHT - 2;
        float inputX = renderX + PADDING;

        String buttonText = setting.isAllowMultiple() ? "Add" : "+";
        float buttonW = Fonts.interSemiBold.getWidth(buttonText, 6) + 8;
        float buttonX = renderX + renderW - PADDING - buttonW;
        float buttonY = inputY;
        float buttonH = 12f;

        float inputW = buttonX - inputX - 4;

        draw.rect(mat, inputX, inputY, inputW, 12, 6f, wa(SURFACE_INTERACTIVE_MUTED.getRGB(), delta));

        String value = setting.getValue();
        String displayText = value.isEmpty() ? setting.getPlaceholder() : value;
        int textColor = value.isEmpty() ? argb(TEXT_ON_SURFACE_MUTED) : argb(TEXT_ON_SURFACE_PRIMARY);

        String clippedText = displayText;
        while (Fonts.interSemiBold.getWidth(clippedText, 6) > inputW - 8 && clippedText.length() > 0) {
            clippedText = clippedText.substring(1);
        }

        draw.text(Fonts.interSemiBold, mat, clippedText, inputX + 4, inputY + 6 - 4, 6, wa(textColor, delta));

        if (ScreenManager.focusedSetting == setting) {
            if (cursorBlinkAnim.isFinished()) {
                cursorBlinkAnim.start(cursorBlinkAnim.getTo(), 1.0 - cursorBlinkAnim.getTo());
            }
            int cursorIndex = Math.min(cursorPos, value.length());
            float cursorX = inputX + 4 + Fonts.interSemiBold.getWidth(value.substring(0, cursorIndex), 6);
            int cursorAlpha = (int) (cursorBlinkAnim.getDelta() * 255);
            draw.rect(mat, cursorX, inputY + 2, 1, 8, 0, wa(withAlpha(SURFACE_CONTROL_ELEVATED, cursorAlpha).getRGB(), delta));
        }

        draw.rect(mat, buttonX, buttonY, buttonW, buttonH, 6f, wa(ACCENT_PRIMARY.getRGB(), delta));
        draw.text(Fonts.interSemiBold, mat, buttonText, buttonX + 4, buttonY + 6 - 4, 6, argbMul(TEXT_ON_ACCENT, delta));

        if (setting.isAllowMultiple() && anim > EXPAND_THRESHOLD && !setting.getEntries().isEmpty()) {
            List<String> entries = new ArrayList<>(setting.getEntries());
            int startIdx = Math.max(0, scrollOffset);
            int endIdx = Math.min(entries.size(), startIdx + maxVisible);

            float entryY = y + getBaseHeight() + PADDING;
            float entryH = 14f;
            for (int i = startIdx; i < endIdx; i++) {
                String entry = entries.get(i);
                boolean entryHover = isInside(renderX + PADDING, entryY, renderW - PADDING * 2, entryH, mouseX, mouseY);

                if (entryHover) {
                    draw.rect(mat, renderX + PADDING, entryY, renderW - PADDING * 2, entryH, RADIUS_SM,
                            wa(SURFACE_SETTING_HOVER.getRGB(), delta));
                }

                float removeW = 14f;
                float removeX = renderX + renderW - PADDING - removeW - 2f;
                float textXStart = renderX + PADDING + 2f;
                float textClipRight = removeX - 4f;
                float availableEntryWidth = Math.max(0f, textClipRight - textXStart);
                String entryDisplay = trimToWidth(entry, availableEntryWidth, 6);
                draw.text(Fonts.interSemiBold, mat, entryDisplay, textXStart, entryY + (entryH - 6) / 2f, 6,
                        argbMul(TEXT_ON_SURFACE_PRIMARY, delta));

                if (Fonts.lucide != null) {
                    String removeText = "\uE1B2";
                    boolean removeHover = isInside(removeX, entryY, removeW, entryH, mouseX, mouseY);

                    if (removeHover) {
                        draw.rect(mat, removeX, entryY + 1, removeW, entryH - 2, RADIUS_SM,
                                wa(SURFACE_INTERACTIVE_MUTED.getRGB(), delta));
                    }

                    draw.text(Fonts.lucide, mat, removeText,
                            removeX + (removeW - Fonts.lucide.getWidth(removeText, 10)) / 2f,
                            entryY + (entryH - 10) / 2f, 10, argbMul(TEXT_ON_SURFACE_PRIMARY, delta));
                }

                entryY += entryH + SETTING_SPACING;
            }
        }

        if (setting.isAllowMultiple() && !setting.getEntries().isEmpty() && Fonts.lucide != null) {
            float arrowX = renderX + renderW - PADDING - 14;
            float arrowY = y + 2;
            String arrow = open ? "\uE06D" : "\uE070";
            draw.text(Fonts.lucide, mat, arrow, arrowX, arrowY, 10, argbMul(TEXT_ON_SURFACE_SECONDARY, delta));
        }
    }

    private static int wordStart(String text, int pos) {
        int i = pos;
        while (i > 0 && text.charAt(i - 1) == ' ') i--;
        while (i > 0 && text.charAt(i - 1) != ' ') i--;
        return i;
    }

    private String trimToWidth(String text, float maxWidth, float fontSize) {
        if (maxWidth <= 0f || text == null || text.isEmpty()) {
            return "";
        }
        if (Fonts.interSemiBold == null) {
            return text.length() > 10 ? text.substring(0, 10) + "…" : text;
        }

        if (Fonts.interSemiBold.getWidth(text, fontSize) <= maxWidth) {
            return text;
        }

        String ellipsis = "…";
        float ellipsisWidth = Fonts.interSemiBold.getWidth(ellipsis, fontSize);
        if (ellipsisWidth > maxWidth) {
            return "";
        }

        int end = text.length();
        while (end > 0) {
            String candidate = text.substring(0, end) + ellipsis;
            if (Fonts.interSemiBold.getWidth(candidate, fontSize) <= maxWidth) {
                return candidate;
            }
            end--;
        }
        return ellipsis;
    }

    @Override
    public float getHeight() {
        float anim = (float) expandAnimation.getDelta();
        float dropdownH = getDropdownHeight() * anim;
        return getBaseHeight() + dropdownH;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0)
            return false;

        float renderX = x + SETTING_INDENT;
        float renderW = PANEL_WIDTH - SETTING_INDENT - 4;
        float inputY = y + SETTING_HEIGHT - 2;
        float inputX = renderX + PADDING;

        String buttonText = setting.isAllowMultiple() ? "Add" : "+";
        float buttonW = Fonts.interSemiBold.getWidth(buttonText, 6) + 8;
        float buttonX = renderX + renderW - PADDING - buttonW;
        float buttonY = inputY;
        float buttonH = 12f;

        float inputW = buttonX - inputX - 4;

        if (isInside(buttonX, buttonY, buttonW, buttonH, (float) mouseX, (float) mouseY)) {
            if (!setting.getValue().trim().isEmpty()) {
                setting.addEntry(setting.getValue());
                setting.setValue("");
                cursorPos = 0;
                SoundUtil.playClick();
            }
            return true;
        }

        if (isInside(inputX, inputY, inputW, 12, (float) mouseX, (float) mouseY)) {
            ScreenManager.focusedSetting = setting;
            cursorBlinkAnim.start(0, 1);

            String value = setting.getValue();
            float clickX = (float) mouseX - inputX - 4;
            cursorPos = 0;
            for (int i = 0; i <= value.length(); i++) {
                float textW = Fonts.interSemiBold.getWidth(value.substring(0, i), 6);
                if (clickX <= textW)
                    break;
                cursorPos = i;
            }
            cursorPos = Math.min(cursorPos, value.length());
            return true;
        }

        if (setting.isAllowMultiple() && !setting.getEntries().isEmpty()) {
            float arrowX = renderX + renderW - PADDING - 14;
            if (isInside(arrowX, y, 10, 16, (float) mouseX, (float) mouseY)) {
                open = !open;
                SoundUtil.playExpand(open);
                return true;
            }
        }

        float anim = (float) expandAnimation.getDelta();
        if (open && anim > EXPAND_THRESHOLD && !setting.getEntries().isEmpty()) {
            List<String> entries = new ArrayList<>(setting.getEntries());
            int startIdx = Math.max(0, scrollOffset);
            int endIdx = Math.min(entries.size(), startIdx + maxVisible);

            float entryY = y + SLIDER_HEIGHT + PADDING;
            for (int i = startIdx; i < endIdx; i++) {
                String entry = entries.get(i);
                float entryH = 14f;

                float removeW = 14f;
                float removeX = renderX + renderW - PADDING - removeW - 2f;
                if (isInside(removeX, entryY, removeW, entryH, (float) mouseX, (float) mouseY)) {
                    setting.removeEntry(entry);
                    SoundUtil.playClick();
                    return true;
                }

                entryY += entryH + SETTING_SPACING;
            }
        }

        ScreenManager.focusedSetting = null;
        return false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ScreenManager.focusedSetting != setting)
            return false;

        String value = setting.getValue();

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                int newPos = wordStart(value, cursorPos);
                setting.setValue(value.substring(0, newPos) + value.substring(cursorPos));
                cursorPos = newPos;
            } else if (cursorPos > 0) {
                int newPos = cursorPos - 1;
                setting.setValue(value.substring(0, newPos) + value.substring(cursorPos));
                cursorPos = newPos;
            }
            cursorBlinkAnim.start(0, 1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (cursorPos < value.length()) {
                setting.setValue(value.substring(0, cursorPos) + value.substring(cursorPos + 1));
            }
            cursorBlinkAnim.start(0, 1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_LEFT) {
            cursorPos = Math.max(0, cursorPos - 1);
            cursorBlinkAnim.start(0, 1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            cursorPos = Math.min(value.length(), cursorPos + 1);
            cursorBlinkAnim.start(0, 1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_HOME) {
            cursorPos = 0;
            cursorBlinkAnim.start(0, 1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_END) {
            cursorPos = value.length();
            cursorBlinkAnim.start(0, 1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ENTER) {
            if (setting.isAllowMultiple() && !value.trim().isEmpty()) {
                setting.addEntry(value);
                setting.setValue("");
                cursorPos = 0;
            } else {
                ScreenManager.focusedSetting = null;
            }
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            ScreenManager.focusedSetting = null;
            return true;
        }

        return false;
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        if (ScreenManager.focusedSetting != setting)
            return;

        if (chr >= 32 && chr != 127) {
            String value = setting.getValue();
            if (value.length() >= setting.getMaxLength())
                return;

            int insertionPoint = Math.min(cursorPos, value.length());
            String newValue = value.substring(0, insertionPoint) + chr + value.substring(insertionPoint);
            setting.setValue(newValue);
            cursorPos = insertionPoint + 1;
            cursorBlinkAnim.start(0, 1);
            SoundUtil.playKeypress();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (open && setting.getEntries().size() > maxVisible) {
            float dropY = y + getBaseHeight();
            float dropH = getDropdownHeight();

            if (mouseY >= dropY && mouseY <= dropY + dropH) {
                scrollOffset = Math.clamp(scrollOffset - (int) amount, 0, setting.getEntries().size() - maxVisible);
                SoundUtil.playScroll();
                return true;
            }
        }
        return false;
    }
}
