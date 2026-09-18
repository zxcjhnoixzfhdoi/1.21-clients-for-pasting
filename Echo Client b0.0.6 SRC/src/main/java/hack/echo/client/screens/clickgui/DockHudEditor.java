package hack.echo.client.screens.clickgui;

import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureManager;
import hack.echo.client.features.HudFeature;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static hack.echo.client.screens.clickgui.glass.GlassUIConstants.*;

public class DockHudEditor {

    private static final Color FILL_IDLE = new Color(255, 255, 255, 10);
    private static final Color FILL_ACTIVE = new Color(0, 128, 255, 25);
    private static final Color BORDER_IDLE = new Color(255, 255, 255, 80);
    private static final Color BORDER_ACTIVE = new Color(0, 128, 255, 200);

    private HudFeature dragging = null;
    private float dragOffsetX, dragOffsetY;

    public void render(Draw2D draw, Matrix4f mat, int screenWidth, int screenHeight,
                       int mouseX, int mouseY, float alphaMul) {
        if (Fonts.interSemiBold == null) return;

        draw.flush();
        List<HudFeature> hudFeatures = getEnabledHudFeatures();
        for (HudFeature hud : hudFeatures) {
            float x = hud.getX();
            float y = hud.getY();
            float w = hud.getWidth();
            float h = hud.getHeight();

            boolean active = dragging == hud
                    || (dragging == null && mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h);

            Color fill = active ? FILL_ACTIVE : FILL_IDLE;
            Color border = active ? BORDER_ACTIVE : BORDER_IDLE;

            draw.rect(mat, x, y, w, h, RADIUS_MD, wa(fill.getRGB(), alphaMul));

            draw.rect(mat, x, y, w, 1, 0, wa(border.getRGB(), alphaMul));
            draw.rect(mat, x, y + h - 1, w, 1, 0, wa(border.getRGB(), alphaMul));
            draw.rect(mat, x, y, 1, h, 0, wa(border.getRGB(), alphaMul));
            draw.rect(mat, x + w - 1, y, 1, h, 0, wa(border.getRGB(), alphaMul));

            String name = hud.getName().toString();
            float labelSize = 7f;
            float labelHeight = Fonts.interSemiBold.getLineHeight(labelSize);
            int nameColor = wa(active ? BORDER_ACTIVE.getRGB() : TEXT_ON_SURFACE_PRIMARY.getRGB(), alphaMul);
            draw.text(Fonts.interSemiBold, mat, name, x, y - labelHeight - 2f, labelSize, nameColor);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        for (HudFeature hud : getEnabledHudFeatures()) {
            float x = hud.getX();
            float y = hud.getY();
            float w = hud.getWidth();
            float h = hud.getHeight();

            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                dragging = hud;
                dragOffsetX = (float) mouseX - x;
                dragOffsetY = (float) mouseY - y;
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                int screenWidth, int screenHeight) {
        if (dragging == null || button != 0) return false;

        float newX = (float) mouseX - dragOffsetX;
        float newY = (float) mouseY - dragOffsetY;
        dragging.getXPosition().setValue(Math.clamp(newX / screenWidth, 0f, 1f));
        dragging.getYPosition().setValue(Math.clamp(newY / screenHeight, 0f, 1f));
        return true;
    }

    public boolean mouseReleased(int button) {
        if (button != 0 || dragging == null) return false;
        dragging = null;
        return true;
    }

    public void cancelDrag() {
        dragging = null;
    }

    private List<HudFeature> getEnabledHudFeatures() {
        List<HudFeature> hudFeatures = new ArrayList<>();

        for (Feature feature : FeatureManager.safeFeatures()) {
            if (!(feature instanceof HudFeature hud)) continue;
            if (!feature.isEnabled()) continue;

            hudFeatures.add(hud);
        }

        return hudFeatures;
    }
}
