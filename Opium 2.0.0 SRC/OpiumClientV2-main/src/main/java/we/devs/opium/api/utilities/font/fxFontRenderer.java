package we.devs.opium.api.utilities.font;

import me.x150.renderer.font.FontRenderer;
import net.minecraft.client.util.math.MatrixStack;
import we.devs.opium.api.utilities.ColorUtils;
import we.devs.opium.api.utilities.RenderUtils;

import java.awt.*;

/**
 * fx means Fixed, originally was made for rendering with fixed sizes.
 */
public class fxFontRenderer extends FontRenderer {

    public fxFontRenderer(Font[] fonts, float sizePx) {
        super(fonts, sizePx);
    }

    public String trimToWidth(String text, float width) {
        float textWidth = this.getStringWidth(text);
        if (textWidth <= width) {
            return text;
        } else {
            String trimmedText = text;
            while (textWidth > width && !trimmedText.isEmpty()) {
                trimmedText = trimmedText.substring(0, trimmedText.length() - 1);
                textWidth = this.getStringWidth(trimmedText);
            }
            return trimmedText;
        }
    }

    public void drawString(MatrixStack matrixStack, String text, float x, float y, float scale, int color) {
        RenderUtils.scaleAndPosition(matrixStack,x,y,scale);
        this.drawString(matrixStack,text,x,y,color);
        RenderUtils.stopScaling(matrixStack);
    }

    public void drawString(MatrixStack matrixStack, String text, float x, float y, int color) {
        int r = 256 - ColorUtils.getRed(color);
        int g = 256 - ColorUtils.getGreen(color);
        int b = 256 - ColorUtils.getBlue(color);
        int a = 256 - ColorUtils.getAlpha(color);

        // Draw the text at the specified position with the specified color
        //this.drawString(matrixStack, text, x / scaleFactor, y / scaleFactor, r, g, b, a);
        try {
            super.drawString(matrixStack, text, x, y, r, g, b, a);
        } catch (NullPointerException ignored) {
        }
    }

    public void drawCenteredString(MatrixStack stack, String s, float x, float y, int color) {
        int r = 256 - ColorUtils.getRed(color);
        int g = 256 - ColorUtils.getGreen(color);
        int b = 256 - ColorUtils.getBlue(color);
        int a = 256 - ColorUtils.getAlpha(color);

        try {
            super.drawCenteredString(stack, s, x, y, r, g, b, a);
        } catch (NullPointerException ignored) {
        }
    }

}
