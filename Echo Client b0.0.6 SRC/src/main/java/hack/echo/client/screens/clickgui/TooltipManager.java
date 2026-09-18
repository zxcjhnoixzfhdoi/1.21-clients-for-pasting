package hack.echo.client.screens.clickgui;

import hack.echo.client.render2.api.CrossTexture;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Font;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.screens.clickgui.glass.GlassUIConstants;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class TooltipManager {
    private static final long SHOW_DELAY_MS = 250L;
    private static final float BODY_SIZE = 7f;
    private static final float MAX_BODY_WIDTH = 220f;
    private static final float PAD_X = 5f;
    private static final float PAD_Y = 4f;
    private static final float CURSOR_OFFSET = 0f;
    private static final float SCREEN_MARGIN = 4f;
    private static final float BODY_LINE_H = BODY_SIZE + 2f;

    private static Object lastHoverKey;
    private static long hoverStartMs;

    private static Object pendingKey;
    private static CharSequence pendingBody;
    private static int pendingMouseX;
    private static int pendingMouseY;

    private TooltipManager() {
    }

    public static void request(Object key, CharSequence title, CharSequence body, int mouseX, int mouseY) {
        if (key == null || body == null || body.length() == 0) return;
        pendingKey = key;
        pendingBody = body;
        pendingMouseX = mouseX;
        pendingMouseY = mouseY;
    }

    public static void renderAndClear(Draw2D draw, Matrix4f mat, int sw, int sh, CrossTexture blurTexture) {
        Object key = pendingKey;
        CharSequence body = pendingBody;
        int mx = pendingMouseX;
        int my = pendingMouseY;

        pendingKey = null;
        pendingBody = null;

        if (key == null) {
            lastHoverKey = null;
            return;
        }

        long now = System.currentTimeMillis();
        if (key != lastHoverKey) {
            lastHoverKey = key;
            hoverStartMs = now;
            return;
        }
        if (now - hoverStartMs < SHOW_DELAY_MS) return;

        Font font = Fonts.interSemiBold;
        if (font == null) return;

        List<String> bodyLines = wrap(body.toString(), font, BODY_SIZE, MAX_BODY_WIDTH);
        float bodyW = 0f;
        for (String line : bodyLines) {
            bodyW = Math.max(bodyW, font.getWidth(line, BODY_SIZE));
        }

        if (bodyW <= 0f) return;

        float boxW = bodyW + PAD_X * 2f;
        float bodyH = bodyLines.size() * BODY_LINE_H;
        float boxH = PAD_Y * 2f + bodyH;

        float bx = mx + CURSOR_OFFSET;
        float by = my - boxH - CURSOR_OFFSET;
        if (bx + boxW > sw - SCREEN_MARGIN) bx = mx - boxW - CURSOR_OFFSET;
        if (by < SCREEN_MARGIN) by = my + CURSOR_OFFSET;
        if (bx < SCREEN_MARGIN) bx = SCREEN_MARGIN;
        if (by + boxH > sh - SCREEN_MARGIN) by = sh - boxH - SCREEN_MARGIN;

        int borderColor = GlassUIConstants.BORDER_TOOLTIP.getRGB();
        int fillColor = GlassUIConstants.SURFACE_TOOLTIP.getRGB();
        int textColor = GlassUIConstants.TEXT_ON_SURFACE_PRIMARY.getRGB();
        float radius = GlassUIConstants.RADIUS_SM;

        if (blurTexture != null) {
            draw.screenImage(mat, blurTexture, bx, by, boxW, boxH, radius, 1f);
        }

        draw.rect(mat, bx, by, boxW, boxH, radius, borderColor);
        draw.rect(mat, bx + 1f, by + 1f, boxW - 2f, boxH - 2f, Math.max(0f, radius - 1f), fillColor);

        float textX = bx + PAD_X;
        float textY = by + PAD_Y;
        for (String line : bodyLines) {
            draw.text(font, mat, line, textX, textY, BODY_SIZE, textColor);
            textY += BODY_LINE_H;
        }
    }

    public static void clear() {
        lastHoverKey = null;
        hoverStartMs = 0L;
        pendingKey = null;
        pendingBody = null;
    }

    private static List<String> wrap(String text, Font font, float size, float maxWidth) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (font.getWidth(candidate, size) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
            } else {
                if (current.length() > 0) out.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (current.length() > 0) out.add(current.toString());
        return out;
    }
}
