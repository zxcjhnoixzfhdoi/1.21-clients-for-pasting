package we.devs.opium.api.utilities.font;

import me.x150.renderer.font.FontRenderer;
import net.minecraft.client.util.math.MatrixStack;

public class ShadowFontRenderer {
    private FontRenderer fontRenderer;
    private float shadowOffsetX = 0.3f;
    private float shadowOffsetY = 0.3f;
    private float shadowOpacity = 0.9f;

    public ShadowFontRenderer(FontRenderer fontRenderer) {
        this.fontRenderer = fontRenderer;
    }

    public void drawStringWithShadow(MatrixStack stack, String text, float x, float y, float r, float g, float b, float a) {

        // Draw shadow
        fontRenderer.drawString(stack, text, x + shadowOffsetX, y + shadowOffsetY, 0 * shadowOpacity, 0 * shadowOpacity, 0 * shadowOpacity, 255 * shadowOpacity);

        // Draw main text
        fontRenderer.drawString(stack, text, x, y, r, g, b, a);
    }
}

