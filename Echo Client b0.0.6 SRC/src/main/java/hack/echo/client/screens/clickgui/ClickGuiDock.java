package hack.echo.client.screens.clickgui;

import hack.echo.client.render2.api.CrossTexture;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.utils.TextLuicideConstants;
import hack.echo.client.utils.audio.SoundUtil;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.awt.Color;

import static hack.echo.client.screens.clickgui.glass.GlassUIConstants.wa;

public class ClickGuiDock {

    public enum Tab {
        FEATURES(TextLuicideConstants.component, "Features"),
        CONFIGS(TextLuicideConstants.files, "Configs"),
        FRIENDS(TextLuicideConstants.user, "Friends"),
        HUD_EDITOR(TextLuicideConstants.laptopMinimal, "HUD Editor");

        public final String icon;
        public final String label;

        Tab(String icon, String label) {
            this.icon = icon;
            this.label = label;
        }
    }

    public record Theme(
            Color accent,
            Color dockBackground,
            Color buttonBackground,
            Color buttonHover,
            Color iconColor,
            Color activeIconColor
    ) {}

    private static final float DOCK_HEIGHT = 30f;
    private static final float ICON_BUTTON_SIZE = 24f;
    private static final float ICON_SIZE = 9f;
    private static final float BUTTON_SPACING = 3f;
    private static final float DOCK_PADDING = 3f;
    private static final float DOCK_RADIUS = 8f;
    private static final float BUTTON_RADIUS = 5f;
    private static final float DOCK_Y = 6f;

    private Tab selectedTab = Tab.FEATURES;
    private final DockHudEditor hudEditor = new DockHudEditor();

    private float dockX, dockWidth;
    private final float[] btnX = new float[Tab.values().length];
    private final float[] btnY = new float[Tab.values().length];

    public Tab getSelectedTab() {
        return selectedTab;
    }

    public boolean isTab(Tab tab) {
        return selectedTab == tab;
    }

    public float getBottomY() {
        return DOCK_Y + DOCK_HEIGHT + 6f;
    }

    public void render(Draw2D draw, Matrix4f mat, float screenWidth, int mouseX, int mouseY,
                       Theme theme, float alphaMul, @Nullable CrossTexture blurTexture) {
        Tab[] tabs = Tab.values();
        int count = tabs.length;

        dockWidth = DOCK_PADDING * 2 + count * ICON_BUTTON_SIZE + (count - 1) * BUTTON_SPACING;
        dockX = (screenWidth - dockWidth) / 2f;

        if (blurTexture != null) {
            draw.screenImage(mat, blurTexture, dockX, DOCK_Y, dockWidth, DOCK_HEIGHT, DOCK_RADIUS, alphaMul);
        }
        draw.rect(mat, dockX, DOCK_Y, dockWidth, DOCK_HEIGHT, DOCK_RADIUS,
                wa(theme.dockBackground.getRGB(), alphaMul));

        float bx = dockX + DOCK_PADDING;
        float by = DOCK_Y + (DOCK_HEIGHT - ICON_BUTTON_SIZE) / 2f;

        for (int i = 0; i < count; i++) {
            btnX[i] = bx;
            btnY[i] = by;

            boolean selected = tabs[i] == selectedTab;
            boolean hovered = mouseX >= bx && mouseX <= bx + ICON_BUTTON_SIZE
                    && mouseY >= by && mouseY <= by + ICON_BUTTON_SIZE;

            Color bg = selected ? theme.accent : hovered ? theme.buttonHover : theme.buttonBackground;
            draw.rect(mat, bx, by, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE, BUTTON_RADIUS,
                    wa(bg.getRGB(), alphaMul));

            if (Fonts.lucide != null) {
                Color ic = selected ? theme.activeIconColor : theme.iconColor;
                float iconW = Fonts.lucide.getWidth(tabs[i].icon, ICON_SIZE);
                float ix = bx + (ICON_BUTTON_SIZE - iconW) / 2f;
                float iy = by + ICON_BUTTON_SIZE / 2f - ICON_SIZE / 2f;
                draw.text(Fonts.lucide, mat, tabs[i].icon, ix, iy, ICON_SIZE,
                        wa(ic.getRGB(), alphaMul));
            }

            bx += ICON_BUTTON_SIZE + BUTTON_SPACING;
        }
    }

    public void renderHudEditor(Draw2D draw, Matrix4f mat, int screenWidth, int screenHeight,
                                int mouseX, int mouseY, float alphaMul) {
        if (selectedTab == Tab.HUD_EDITOR) {
            hudEditor.render(draw, mat, screenWidth, screenHeight, mouseX, mouseY, alphaMul);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Tab[] tabs = Tab.values();
            for (int i = 0; i < tabs.length; i++) {
                if (mouseX >= btnX[i] && mouseX <= btnX[i] + ICON_BUTTON_SIZE
                        && mouseY >= btnY[i] && mouseY <= btnY[i] + ICON_BUTTON_SIZE) {
                    if (tabs[i] != selectedTab) {
                        selectedTab = tabs[i];
                        hudEditor.cancelDrag();
                        SoundUtil.playClick();
                    }
                    return true;
                }
            }
        }

        if (selectedTab == Tab.HUD_EDITOR && button == 0) {
            return hudEditor.mouseClicked(mouseX, mouseY);
        }

        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                int screenWidth, int screenHeight) {
        if (selectedTab == Tab.HUD_EDITOR) {
            return hudEditor.mouseDragged(mouseX, mouseY, button, screenWidth, screenHeight);
        }
        return false;
    }

    public boolean mouseReleased(int button) {
        if (selectedTab == Tab.HUD_EDITOR) {
            return hudEditor.mouseReleased(button);
        }
        return false;
    }
}
