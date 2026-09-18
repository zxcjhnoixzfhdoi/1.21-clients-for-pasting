package hack.echo.client.screens.clickgui.glassrewrite;

import hack.echo.client.features.Feature;
import hack.echo.client.features.settings.Setting;
import hack.echo.client.features.settings.impl.*;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.screens.clickgui.TooltipManager;
import hack.echo.client.screens.clickgui.glass.settings.*;
import hack.echo.client.utils.ClientUtil;
import hack.echo.client.utils.TextLuicideConstants;
import hack.echo.client.utils.animation.Animation;
import hack.echo.client.utils.animation.Easing;
import hack.echo.client.utils.audio.SoundUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import net.minecraft.client.input.MouseButtonEvent;
import org.joml.Matrix4f;

import java.util.List;

import static hack.echo.client.screens.clickgui.glass.GlassUIConstants.*;

public class GlassModuleElement {

    @Getter
    private float height = MODULE_HEIGHT;
    @Getter
    private float width;
    @Getter
    private float x, y;
    @Getter
    private final Feature feature;
    private final GlassScreen screen;
    private final List<GlassSettingElement> settingElements = new ObjectArrayList<>();
    private final Animation heightAnimation = new Animation(Easing.EASE_OUT_CUBIC, 300);

    public GlassModuleElement(GlassScreen screen, Feature feature) {
        this.feature = feature;
        this.screen = screen;

        for (Setting setting : feature.settings) {
            settingElements.add(switch (setting) {
                case BoolSetting bool -> new GlassBoolElement(bool);
                case IntSetting intSetting -> new GlassIntSliderElement(intSetting);
                case FloatSetting floatSetting -> new GlassFloatSliderElement(floatSetting);
                case ModeSetting mode -> new GlassModeElement(mode);
                case KeybindSetting keybindSetting -> new GlassKeybindElement(keybindSetting);
                case ColorSetting colorSetting -> new GlassColorElement(colorSetting);
                case MultiModeSetting multiModeSetting -> new GlassMultiModeElement(multiModeSetting);
                case RangeSetting range -> new GlassRangeElement(range);
                case StringSetting str -> new GlassStringElement(str);
                case RegistryPickerSetting<?> rps -> new GlassRegistryPickerElement<>(rps);
                case HotbarSelectionSetting hss -> new GlassHotbarSelectionElement(hss);
                default -> null;
            });
        }

        heightAnimation.start(MODULE_HEIGHT, MODULE_HEIGHT);
    }

    private float calculateHeight() {
        if (!expanded)
            return MODULE_HEIGHT;

        float h = MODULE_HEIGHT;
        for (var element : settingElements) {
            if (element == null || !element.getSetting().isVisible())
                continue;
            h += element.getHeight() + SETTING_SPACING;
        }
        return h;
    }

    public void updateHeight() {
        float target = calculateHeight();
        if (target != heightAnimation.getTo()) {
            heightAnimation.updateTo(target);
        }
    }

    public void updatePosition(float x, float y) {
        this.x = x;
        this.y = y;
        this.width = PANEL_WIDTH;
        updateHeight();
        this.height = (float) heightAnimation.getDelta();
    }

    public boolean wasHovered = false;
    public boolean expanded = false;
    private static final float VISIBILITY_ICON_SIZE = 9f;
    private static final float VISIBILITY_ICON_GAP = 4f;

    public void render(Draw2D draw, Matrix4f mat, int mouseX, int mouseY, float delta, boolean editingVisibility) {
        boolean isHovered = isInside(this.x, this.y, this.width, MODULE_HEIGHT, mouseX, mouseY);
        if (isHovered && !wasHovered) {
            SoundUtil.playHover();
        }
        wasHovered = isHovered;

        if (isHovered && !editingVisibility) {
            CharSequence description = feature.getDescription();
            if (description != null && description.length() > 0) {
                TooltipManager.request(feature, feature.getName(), description, mouseX, mouseY);
            }
        }

        if (editingVisibility) {
            renderVisibilityEditor(draw, mat, delta, isHovered);
            return;
        }

        if (feature.isEnabled()) {
            draw.rect(mat, x + 2, y, width - 4, MODULE_HEIGHT, RADIUS_MD,
                    wa(ACCENT_PRIMARY.getRGB(), delta));
        }

        if (isHovered) {
            draw.rect(mat, this.x + 2, y, width - 4, MODULE_HEIGHT, RADIUS_MD,
                    wa(SURFACE_MODULE_HOVER.getRGB(), delta));
        }

        int text = wa(
                feature.isEnabled() ? argb(TEXT_ON_ACCENT) : argb(TEXT_ON_SURFACE_SECONDARY),
                delta);

        draw.text(Fonts.interSemiBold, mat, feature.getName(), x + 2 + PADDING, y + MODULE_HEIGHT / 2 - 5, 8, text);
        boolean binding = screen.binding == this;
        String bind = binding ? "..." : ClientUtil.keyLabel(feature.getKey());
        if (binding || !bind.equalsIgnoreCase("none")) {
            float w = Fonts.interSemiBold.getWidth(bind, 7);
            int bindColor = binding ? argbMul(TEXT_ON_SURFACE_PRIMARY, delta) : text;
            draw.text(Fonts.interSemiBold, mat, bind,
                    this.x + width - w - 10,
                    y + MODULE_HEIGHT / 2 - 4,
                    7,
                    bindColor);
        }

        if (!feature.settings.isEmpty()) {
            String arrow = expanded ? "\u25BC" : "\u25B6";
            float w = Fonts.interSemiBold.getWidth(arrow, 8);
            draw.text(Fonts.interSemiBold, mat, arrow,
                    x + width - w - 4,
                    y + MODULE_HEIGHT / 2 - 5,
                    8, text);
        }

        if (height > MODULE_HEIGHT) {
            float setY = y + MODULE_HEIGHT + SETTING_SPACING;
            draw.pushScissor(x, setY, PANEL_WIDTH, height - MODULE_HEIGHT);
            for (var element : settingElements) {
                if (element == null || !element.getSetting().isVisible())
                    continue;

                element.render(draw, x, setY, mat, mouseX, mouseY, delta);
                setY += element.getHeight() + SETTING_SPACING;
            }
            draw.popScissor();
        }

        this.x -= 2;
        this.width -= 4;
    }

    private void renderVisibilityEditor(Draw2D draw, Matrix4f mat, float delta, boolean isHovered) {
        if (isHovered) {
            draw.rect(mat, this.x + 2, y, width - 4, MODULE_HEIGHT, RADIUS_MD,
                    wa(SURFACE_MODULE_HOVER.getRGB(), delta));
        }

        boolean visible = feature.isVisible();
        int textColor = visible
                ? argbMul(TEXT_ON_SURFACE_PRIMARY, delta)
                : argbMul(TEXT_ON_SURFACE_SECONDARY, delta);

        float textX = x + 2 + PADDING;
        if (Fonts.lucide != null) {
            String icon = visible ? TextLuicideConstants.minus : TextLuicideConstants.plus;
            float iconWidth = Fonts.lucide.getWidth(icon, VISIBILITY_ICON_SIZE);
            float iconY = y + MODULE_HEIGHT / 2f - 4.5f;

            draw.text(Fonts.lucide, mat, icon, textX, iconY, VISIBILITY_ICON_SIZE, textColor);
            textX += iconWidth + VISIBILITY_ICON_GAP;
        }

        draw.text(Fonts.interSemiBold, mat, feature.getName(), textX, y + MODULE_HEIGHT / 2 - 5, 8, textColor);
    }

    public boolean onClicked(MouseButtonEvent event) {
        if (!expanded)
            return false;

        float settingY = y + MODULE_HEIGHT + SETTING_SPACING;
        for (var element : settingElements) {
            if (element == null || !element.getSetting().isVisible())
                continue;

            float settingHeight = element.getHeight();
            if (event.y() >= settingY && event.y() <= settingY + settingHeight) {
                if (element.mouseClicked(event.x(), event.y(), event.button())) {
                    return true;
                }
            }
            settingY += settingHeight + SETTING_SPACING;
        }
        return false;
    }

    public boolean mouseReleased(double x, double y, int button) {
        if (!expanded)
            return false;

        for (var element : settingElements) {
            if (element == null)
                continue;
            element.mouseReleased(x, y, button);
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!expanded)
            return false;

        for (var element : settingElements) {
            if (element == null || !element.getSetting().isVisible())
                continue;
            if (element.keyPressed(keyCode, scanCode, modifiers))
                return true;
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!expanded)
            return false;

        for (var element : settingElements) {
            if (element == null || !element.getSetting().isVisible())
                continue;
            element.charTyped(chr, modifiers);
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!expanded)
            return false;

        float settingY = y + MODULE_HEIGHT + SETTING_SPACING;
        for (var element : settingElements) {
            if (element == null || !element.getSetting().isVisible())
                continue;

            float settingHeight = element.getHeight();
            if (mouseY >= settingY && mouseY <= settingY + settingHeight &&
                    mouseX >= x && mouseX <= x + PANEL_WIDTH) {
                if (element.mouseScrolled(mouseX, mouseY, amount)) {
                    return true;
                }
            }
            settingY += settingHeight + SETTING_SPACING;
        }
        return false;
    }
}
