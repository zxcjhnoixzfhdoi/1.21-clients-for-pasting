package hack.echo.client.screens.clickgui.glass;

import hack.echo.client.features.FeatureManager;
import hack.echo.client.features.impl.misc.ClickGUI;

import java.awt.*;

public class GlassUIConstants {
    
    public static float PANEL_WIDTH = 120f;
    public static float PANEL_HEADER_HEIGHT = 18f;
    public static float PANEL_SPACING = 8f;
    public static float INITIAL_Y = 15f;

    public static float PANEL_WIDTH_HUD_ELEMENT = 100f;
    public static float INDIVIDUAL_PILL_GAP = 3f;
    public static float VALUE_GAP = 6f;
    public static Color ACCENT_FEATURE_ACTIVE = new Color(0, 150, 255, 255);
    public static float HUD_ELEMENT_HEIGHT = 12f;

    public static float MODULE_HEIGHT = 16f;
    public static float MODULE_SPACING = 2f;
    
    public static float SETTING_HEIGHT = 16f;
    public static float SLIDER_HEIGHT = 24f;
    public static float SETTING_SPACING = 2f;
    
    public static float PADDING = 5f;
    public static float RADIUS_SM = 2f;
    public static float RADIUS_MD = 4f;
    public static float RADIUS_LG = 6f;
    
    public static float TOGGLE_WIDTH = 18f;
    public static float TOGGLE_HEIGHT = 9f;
    public static float KNOB_SIZE = 6f;
    
    public static float COLOR_PICKER_HEIGHT = 80f;
    public static float HUE_SLIDER_HEIGHT = 5f;
    
    public static float ANIMATION_SPEED = 0.15f;
    public static float FAST_ANIMATION_SPEED = 0.2f;
    public static Color SHADOW_HUD_SLOT = new Color(0, 0, 0, 20);

    private static final Color LIGHT_SURFACE_PANEL = new Color(248, 248, 248, 170);
    private static final Color LIGHT_BORDER_PANEL = new Color(255, 255, 255, 120);
    private static final Color LIGHT_BORDER_PANEL_HOVER = new Color(255, 255, 255, 180);
    private static final Color LIGHT_BORDER_PANEL_ACTIVE = new Color(255, 255, 255, 220);
    private static final Color LIGHT_BORDER_TOOLTIP = new Color(180, 180, 190, 200);
    private static final Color LIGHT_SURFACE_TOOLTIP = new Color(248, 248, 248, 140);
    private static final Color LIGHT_SURFACE_CONTROL_ELEVATED = new Color(255, 255, 255, 200);
    private static final Color LIGHT_SURFACE_INTERACTIVE_MUTED = new Color(174, 174, 178, 100);
    private static final Color LIGHT_CONTROL_KNOB = new Color(255, 255, 255, 250);
    private static final Color LIGHT_CONTROL_FILL_ACTIVE = new Color(255, 255, 255, 100);
    private static final Color LIGHT_CONTROL_TOGGLE_OFF = new Color(174, 174, 178, 160);
    private static final Color LIGHT_SURFACE_SETTING = new Color(255, 255, 255, 40);
    private static final Color LIGHT_SURFACE_SETTING_HOVER = new Color(255, 255, 255, 70);
    private static final Color LIGHT_SURFACE_MODULE_HOVER = new Color(255, 255, 255, 30);
    private static final Color LIGHT_TEXT_ON_SURFACE_PRIMARY = new Color(32, 32, 48, 240);
    private static final Color LIGHT_TEXT_ON_SURFACE_SECONDARY = new Color(48, 48, 64, 176);
    private static final Color LIGHT_TEXT_ON_SURFACE_MUTED = new Color(80, 80, 96, 112);

    private static final Color DARK_SURFACE_PANEL = new Color(26, 26, 26, 170);
    private static final Color DARK_BORDER_PANEL = new Color(255, 255, 255, 60);
    private static final Color DARK_BORDER_PANEL_HOVER = new Color(255, 255, 255, 100);
    private static final Color DARK_BORDER_PANEL_ACTIVE = new Color(255, 255, 255, 140);
    private static final Color DARK_SURFACE_TOOLTIP = new Color(34, 34, 34, 171);
    private static final Color DARK_BORDER_TOOLTIP = new Color(26, 26, 26, 200);
    private static final Color DARK_SURFACE_CONTROL_ELEVATED = new Color(255, 255, 255, 220);
    private static final Color DARK_SURFACE_INTERACTIVE_MUTED = new Color(85, 85, 95, 150);
    private static final Color DARK_CONTROL_KNOB = new Color(240, 240, 245, 245);
    private static final Color DARK_CONTROL_FILL_ACTIVE = new Color(255, 255, 255, 70);
    private static final Color DARK_CONTROL_TOGGLE_OFF = new Color(95, 95, 105, 170);
    private static final Color DARK_SURFACE_SETTING = new Color(255, 255, 255, 18);
    private static final Color DARK_SURFACE_SETTING_HOVER = new Color(255, 255, 255, 36);
    private static final Color DARK_SURFACE_MODULE_HOVER = new Color(255, 255, 255, 24);

    private static final Color DARK_TAB_INACTIVE = new Color(26, 26, 26, 18);
    private static final Color DARK_TAB_INACTIVE_HOVER = new Color(26, 26, 26, 36);

    private static final Color DARK_TEXT_ON_SURFACE_PRIMARY = new Color(245, 247, 255, 240);
    private static final Color DARK_TEXT_ON_SURFACE_SECONDARY = new Color(197, 201, 216, 184);
    private static final Color DARK_TEXT_ON_SURFACE_MUTED = new Color(147, 154, 168, 128);

    public static Color SURFACE_PANEL = LIGHT_SURFACE_PANEL;
    public static Color SURFACE_TOOLTIP = LIGHT_SURFACE_TOOLTIP;
    public static Color BORDER_PANEL = LIGHT_BORDER_PANEL;
    public static Color BORDER_PANEL_HOVER = LIGHT_BORDER_PANEL_HOVER;
    public static Color BORDER_PANEL_ACTIVE = LIGHT_BORDER_PANEL_ACTIVE;
    public static Color BORDER_TOOLTIP = LIGHT_BORDER_TOOLTIP;
    public static Color SURFACE_CONTROL_ELEVATED = LIGHT_SURFACE_CONTROL_ELEVATED;
    public static Color SURFACE_INTERACTIVE_MUTED = LIGHT_SURFACE_INTERACTIVE_MUTED;
    public static Color CONTROL_KNOB = LIGHT_CONTROL_KNOB;
    public static Color CONTROL_FILL_ACTIVE = LIGHT_CONTROL_FILL_ACTIVE;

    public static Color CONTROL_TOGGLE_OFF = LIGHT_CONTROL_TOGGLE_OFF;
    public static Color CONTROL_TOGGLE_ON = new Color(0, 122, 255, 180);
    public static Color SHADOW_KNOB = new Color(0, 0, 0, 40);

    public static Color SURFACE_SETTING = LIGHT_SURFACE_SETTING;
    public static Color SURFACE_SETTING_HOVER = LIGHT_SURFACE_SETTING_HOVER;
    public static Color ACCENT_PRIMARY = new Color(0, 122, 255, 120);

    public static Color TAB_INACTIVE = LIGHT_SURFACE_MODULE_HOVER;
    public static Color TAB_INACTIVE_HOVER = LIGHT_SURFACE_MODULE_HOVER;

    public static Color SURFACE_MODULE_HOVER = LIGHT_SURFACE_MODULE_HOVER;
    
    public static float SETTING_INDENT = 4f;
    
    public static float MAX_PANEL_HEIGHT = 400f;
    public static float SCROLL_MULTIPLIER = 6.7f;
    public static float ANIMATION_THRESHOLD = 0.5f;
    public static float ANIMATION_EPSILON = 0.01f;
    public static float ANIMATION_EPSILON_TIGHT = 0.001f;
    public static float EXPAND_THRESHOLD = 0.9f;
    
    public static Color TEXT_ON_SURFACE_PRIMARY = LIGHT_TEXT_ON_SURFACE_PRIMARY;
    public static Color TEXT_ON_SURFACE_SECONDARY = LIGHT_TEXT_ON_SURFACE_SECONDARY;
    public static Color TEXT_ON_SURFACE_MUTED = LIGHT_TEXT_ON_SURFACE_MUTED;
    public static Color TEXT_ON_ACCENT = new Color(255, 255, 255, 240);

    public static Color ACCENT_FOR_RENDERING = new Color(0, 122, 255, 40);


    public static void syncTheme() {
        boolean dark = FeatureManager.safeGetFeature(ClickGUI.class)
                .map(gui -> gui.glassDarkMode.getValue())
                .orElse(false);

        if (dark) {
            SURFACE_PANEL = DARK_SURFACE_PANEL;
            SURFACE_TOOLTIP = DARK_SURFACE_TOOLTIP;
            BORDER_PANEL = DARK_BORDER_PANEL;
            BORDER_PANEL_HOVER = DARK_BORDER_PANEL_HOVER;
            BORDER_PANEL_ACTIVE = DARK_BORDER_PANEL_ACTIVE;
            BORDER_TOOLTIP = DARK_BORDER_TOOLTIP;
            SURFACE_CONTROL_ELEVATED = DARK_SURFACE_CONTROL_ELEVATED;
            SURFACE_INTERACTIVE_MUTED = DARK_SURFACE_INTERACTIVE_MUTED;
            CONTROL_KNOB = DARK_CONTROL_KNOB;
            CONTROL_FILL_ACTIVE = DARK_CONTROL_FILL_ACTIVE;
            CONTROL_TOGGLE_OFF = DARK_CONTROL_TOGGLE_OFF;
            SURFACE_SETTING = DARK_SURFACE_SETTING;
            SURFACE_SETTING_HOVER = DARK_SURFACE_SETTING_HOVER;
            SURFACE_MODULE_HOVER = DARK_SURFACE_MODULE_HOVER;
            TAB_INACTIVE = DARK_TAB_INACTIVE;
            TAB_INACTIVE_HOVER = DARK_TAB_INACTIVE_HOVER;
            TEXT_ON_SURFACE_PRIMARY = DARK_TEXT_ON_SURFACE_PRIMARY;
            TEXT_ON_SURFACE_SECONDARY = DARK_TEXT_ON_SURFACE_SECONDARY;
            TEXT_ON_SURFACE_MUTED = DARK_TEXT_ON_SURFACE_MUTED;
            return;
        }

        SURFACE_PANEL = LIGHT_SURFACE_PANEL;
        SURFACE_TOOLTIP = LIGHT_SURFACE_TOOLTIP;
        BORDER_PANEL = LIGHT_BORDER_PANEL;
        BORDER_PANEL_HOVER = LIGHT_BORDER_PANEL_HOVER;
        BORDER_PANEL_ACTIVE = LIGHT_BORDER_PANEL_ACTIVE;
        BORDER_TOOLTIP = LIGHT_BORDER_TOOLTIP;
        SURFACE_CONTROL_ELEVATED = LIGHT_SURFACE_CONTROL_ELEVATED;
        SURFACE_INTERACTIVE_MUTED = LIGHT_SURFACE_INTERACTIVE_MUTED;
        CONTROL_KNOB = LIGHT_CONTROL_KNOB;
        CONTROL_FILL_ACTIVE = LIGHT_CONTROL_FILL_ACTIVE;
        CONTROL_TOGGLE_OFF = LIGHT_CONTROL_TOGGLE_OFF;
        SURFACE_SETTING = LIGHT_SURFACE_SETTING;
        SURFACE_SETTING_HOVER = LIGHT_SURFACE_SETTING_HOVER;
        TAB_INACTIVE = LIGHT_SURFACE_MODULE_HOVER;
        TAB_INACTIVE_HOVER = LIGHT_SURFACE_MODULE_HOVER;
        SURFACE_MODULE_HOVER = LIGHT_SURFACE_MODULE_HOVER;
        TEXT_ON_SURFACE_PRIMARY = LIGHT_TEXT_ON_SURFACE_PRIMARY;
        TEXT_ON_SURFACE_SECONDARY = LIGHT_TEXT_ON_SURFACE_SECONDARY;
        TEXT_ON_SURFACE_MUTED = LIGHT_TEXT_ON_SURFACE_MUTED;
    }
    
    public static Color lerpColor(Color from, Color to, float progress) {
        return new Color(
            (int)(from.getRed() + (to.getRed() - from.getRed()) * progress),
            (int)(from.getGreen() + (to.getGreen() - from.getGreen()) * progress),
            (int)(from.getBlue() + (to.getBlue() - from.getBlue()) * progress),
            (int)(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * progress)
        );
    }
    
    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static Object hudDragOwner = null;

    public static boolean isInside(float x, float y, float width, float height, float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height;
    }

    public static int wa(int color, float mul) {
        return (int) ((color >>> 24) * mul) << 24 | color & 0x00FFFFFF;
    }

    public static int argb(Color color) {
        return color.getRGB();
    }

    public static int argbMul(Color color, float mul) {
        return wa(color.getRGB(), mul);
    }

}
