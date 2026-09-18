package we.devs.opium.api.utilities;

import com.mojang.blaze3d.systems.RenderSystem;
import me.x150.renderer.font.FontRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import we.devs.opium.api.utilities.ColorUtils;
import we.devs.opium.api.utilities.font.FontRenderers;
import we.devs.opium.api.utilities.font.ShadowFontRenderer;
import we.devs.opium.api.utilities.font.fxFontRenderer;
import we.devs.opium.client.modules.client.ModuleFont;

import java.awt.*;
import java.util.Objects;

public class RenderUtils implements IMinecraft {
    public static Frustum camera = new Frustum(new Matrix4f(), new Matrix4f());
    public static DrawContext drawContext;

    public static void setDrawContext(DrawContext drawContext) {
        RenderUtils.drawContext = drawContext;
    }

    public static void prepare() {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(1.0f);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void release() {
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void drawRoundedGradientQuad(MatrixStack matrices, Color startColor, Color endColor, float x, float y, float width, float height, float radius, int samples) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Center of the rounded rectangle
        float centerX = x + radius;
        float centerY = y + radius;

        // Draw the rounded rectangle with gradient
        bufferBuilder.vertex(matrix, centerX, centerY, 0.0f).color(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), startColor.getAlpha());
        for (int i = 0; i <= 90; i++) {
            double angle = Math.toRadians(i);
            float dx = (float) (radius * Math.cos(angle));
            float dy = (float) (radius * Math.sin(angle));
            Color color = interpolateColor(startColor, endColor, (float) i / 90);
            bufferBuilder.vertex(matrix, centerX + dx, centerY + dy, 0.0f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }
        for (int i = 90; i <= 180; i++) {
            double angle = Math.toRadians(i);
            float dx = (float) (radius * Math.cos(angle));
            float dy = (float) (radius * Math.sin(angle));
            Color color = interpolateColor(startColor, endColor, (float) i / 180);
            bufferBuilder.vertex(matrix, centerX + dx, centerY + dy, 0.0f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }
        for (int i = 180; i <= 270; i++) {
            double angle = Math.toRadians(i);
            float dx = (float) (radius * Math.cos(angle));
            float dy = (float) (radius * Math.sin(angle));
            Color color = interpolateColor(startColor, endColor, (float) i / 270);
            bufferBuilder.vertex(matrix, centerX + dx, centerY + dy, 0.0f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }
        for (int i = 270; i <= 360; i++) {
            double angle = Math.toRadians(i);
            float dx = (float) (radius * Math.cos(angle));
            float dy = (float) (radius * Math.sin(angle));
            Color color = interpolateColor(startColor, endColor, (float) i / 360);
            bufferBuilder.vertex(matrix, centerX + dx, centerY + dy, 0.0f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    private static Color interpolateColor(Color start, Color end, float progress) {
        int red = (int) (start.getRed() + (end.getRed() - start.getRed()) * progress);
        int green = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * progress);
        int blue = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * progress);
        int alpha = (int) (start.getAlpha() + (end.getAlpha() - start.getAlpha()) * progress);
        return new Color(red, green, blue, alpha);
    }

    public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, Color color) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        bufferBuilder.vertex(x, height, 0.0f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        bufferBuilder.vertex(width, height, 0.0f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        bufferBuilder.vertex(width, y, 0.0f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        bufferBuilder.vertex(x, y, 0.0f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, float radius, Color color) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Center of the rounded rectangle
        float centerX = x + radius;
        float centerY = y + radius;

        // Draw the rounded rectangle
        bufferBuilder.vertex(matrix, centerX, centerY, 0.0f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        for (int i = 0; i <= 90; i++) {
            double angle = Math.toRadians(i);
            float dx = (float) (radius * Math.cos(angle));
            float dy = (float) (radius * Math.sin(angle));
            bufferBuilder.vertex(matrix, centerX + dx, centerY + dy, 0.0f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }
        for (int i = 90; i <= 180; i++) {
            double angle = Math.toRadians(i);
            float dx = (float) (radius * Math.cos(angle));
            float dy = (float) (radius * Math.sin(angle));
            bufferBuilder.vertex(matrix, centerX + dx, centerY + dy, 0.0f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }
        for (int i = 180; i <= 270; i++) {
            double angle = Math.toRadians(i);
            float dx = (float) (radius * Math.cos(angle));
            float dy = (float) (radius * Math.sin(angle));
            bufferBuilder.vertex(matrix, centerX + dx, centerY + dy, 0.0f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }
        for (int i = 270; i <= 360; i++) {
            double angle = Math.toRadians(i);
            float dx = (float) (radius * Math.cos(angle));
            float dy = (float) (radius * Math.sin(angle));
            bufferBuilder.vertex(matrix, centerX + dx, centerY + dy, 0.0f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    public static void drawTriangle(MatrixStack matrices, float x, float y, float size, float rotation, Color color) {
        prepare();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Calculate the vertices of the triangle
        float angle = (float) Math.toRadians(rotation);
        float x1 = x + (float) (size * Math.cos(angle));
        float y1 = y + (float) (size * Math.sin(angle));
        float x2 = x + (float) (size * Math.cos(angle + Math.toRadians(120)));
        float y2 = y + (float) (size * Math.sin(angle + Math.toRadians(120)));
        float x3 = x + (float) (size * Math.cos(angle + Math.toRadians(240)));
        float y3 = y + (float) (size * Math.sin(angle + Math.toRadians(240)));

        // Draw the triangle
        bufferBuilder.vertex(matrix, x1, y1, 0.0f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        bufferBuilder.vertex(matrix, x2, y2, 0.0f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        bufferBuilder.vertex(matrix, x3, y3, 0.0f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        release();
    }

    public static void drawOutline(MatrixStack matrices, float x, float y, float width, float height, float lineWidth, Color color) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Set line width and color
        RenderSystem.lineWidth(lineWidth);
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        // Define outline coordinates with proper alignment

        // Draw rectangle outline
        bufferBuilder.vertex(matrix, x, y, 0).color(r, g, b, a);
        bufferBuilder.vertex(matrix, width, y, 0).color(r, g, b, a);
        bufferBuilder.vertex(matrix, width, height, 0).color(r, g, b, a);
        bufferBuilder.vertex(matrix, x, height, 0).color(r, g, b, a);
        bufferBuilder.vertex(matrix, x, y, 0).color(r, g, b, a); // Close the loop

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    public static void drawOutline(MatrixStack matrices, float x, float y, float width, float height, float lineWidth, float radius, Color color) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.lineWidth(lineWidth);
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        // Calculate rounded corners
        float right = x + width;
        float bottom = y + height;
        int segments = 10;

        // Top right corner
        for(int i = 0; i <= segments; i++) {
            float angle = (float) (Math.PI * 1.5 + (Math.PI * i) / (segments * 2));
            bufferBuilder.vertex(matrix, x + width - radius + (float) Math.cos(angle) * radius,
                            y + radius + (float) Math.sin(angle) * radius, 0)
                    .color(r, g, b, a);
        }

        // Bottom right corner
        for(int i = 0; i <= segments; i++) {
            float angle = (float) (Math.PI * 0.0 + (Math.PI * i) / (segments * 2));
            bufferBuilder.vertex(matrix, x + width - radius + (float) Math.cos(angle) * radius,
                            y + height - radius + (float) Math.sin(angle) * radius, 0)
                    .color(r, g, b, a);
        }

        // Bottom left corner
        for(int i = 0; i <= segments; i++) {
            float angle = (float) (Math.PI * 0.5 + (Math.PI * i) / (segments * 2));
            bufferBuilder.vertex(matrix, x + radius + (float) Math.cos(angle) * radius,
                            y + height - radius + (float) Math.sin(angle) * radius, 0)
                    .color(r, g, b, a);
        }

        // Top left corner
        for(int i = 0; i <= segments; i++) {
            float angle = (float) (Math.PI * 1.0 + (Math.PI * i) / (segments * 2));
            bufferBuilder.vertex(matrix, x + radius + (float) Math.cos(angle) * radius,
                            y + radius + (float) Math.sin(angle) * radius, 0)
                    .color(r, g, b, a);
        }

        // Close the loop
        bufferBuilder.vertex(matrix, x + width - radius, y, 0).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    public static void drawSidewaysGradient(MatrixStack matrices, float x, float y, float width, float height, Color startColor, Color endColor) {
        prepare();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        drawSidewaysPart(bufferBuilder, matrix, x, y, width, height, startColor, endColor);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        release();
    }

    private static void drawSidewaysPart(BufferBuilder bufferBuilder, Matrix4f matrix, float x, float y, float width, float height, Color startColor, Color endColor) {
        bufferBuilder.vertex(matrix, x, y, 0.0f).color(startColor.getRed() / 255.0f, startColor.getGreen() / 255.0f, startColor.getBlue() / 255.0f, startColor.getAlpha() / 255.0f);
        bufferBuilder.vertex(matrix, x + width, y, 0.0f).color(endColor.getRed() / 255.0f, endColor.getGreen() / 255.0f, endColor.getBlue() / 255.0f, endColor.getAlpha() / 255.0f);
        bufferBuilder.vertex(matrix, x + width, y + height, 0.0f).color(endColor.getRed() / 255.0f, endColor.getGreen() / 255.0f, endColor.getBlue() / 255.0f, endColor.getAlpha() / 255.0f);
        bufferBuilder.vertex(matrix, x, y + height, 0.0f).color(startColor.getRed() / 255.0f, startColor.getGreen() / 255.0f, startColor.getBlue() / 255.0f, startColor.getAlpha() / 255.0f);
    }

    public static void drawCircle(float x, float y, float radius, Color color) {
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        RenderSystem.setShader(GameRenderer::getPositionProgram);
        RenderSystem.setShaderColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 255f);
        double degree = Math.PI / 180;
        for (double i = 0; i <= 90; i += 1) {
            double v = Math.sin(i * degree) * radius;
            double v1 = Math.cos(i * degree) * radius;
            bufferBuilder.vertex((float) (x + v), (float) (y + v1), 0.0F);
            bufferBuilder.vertex((float) (x + v), (float) (y - v1), 0.0F);
            bufferBuilder.vertex((float) (x - v), (float) (y - v1), 0.0F);
            bufferBuilder.vertex((float) (x - v), (float) (y + v1), 0.0F);
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.setShaderColor(1, 1, 1, 1);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void drawCenteredString(MatrixStack matrixStack, String text, float centerX, float centerY, int color) {
        FontRenderer fontRenderer = getFontRenderer();
        if (fontRenderer == null) return;

        // Calculate the width and height of the text
        int textWidth = (int) fontRenderer.getStringWidth(text);
        int textHeight = (int) fontRenderer.getStringHeight(text);

        // Calculate top-left coordinates to center the text
        float x = centerX - (textWidth / 2.0f);
        float y = centerY - (textHeight / 2.0f);

        // Draw the text at the calculated position
        drawString(matrixStack, text, x, y, color);
    }

    public static void drawBlock(BlockPos position, Color color) {
        RenderUtils.drawBlock(RenderUtils.getRenderBB(position), color);
    }

    public static void drawBlock(Box bb, Color color) {
        camera.setPosition(Objects.requireNonNull(mc.getCameraEntity()).getX(), mc.getCameraEntity().getY(), mc.getCameraEntity().getZ());
        if (camera.isVisible(new Box(bb.minX + mc.getEntityRenderDispatcher().camera.getPos().x, bb.minY + mc.getEntityRenderDispatcher().camera.getPos().y, bb.minZ + mc.getEntityRenderDispatcher().camera.getPos().z, bb.maxX + mc.getEntityRenderDispatcher().camera.getPos().x, bb.maxY + mc.getEntityRenderDispatcher().camera.getPos().y, bb.maxZ + mc.getEntityRenderDispatcher().camera.getPos().z))) {
            prepare();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            bufferBuilder.vertex((float) bb.minX, (float) bb.minY, (float) bb.minZ).color(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
            bufferBuilder.vertex((float) bb.maxX, (float) bb.minY, (float) bb.minZ).color(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
            bufferBuilder.vertex((float) bb.maxX, (float) bb.maxY, (float) bb.minZ).color(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
            bufferBuilder.vertex((float) bb.minX, (float) bb.maxY, (float) bb.minZ).color(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
            bufferBuilder.vertex((float) bb.minX, (float) bb.minY, (float) bb.maxZ).color(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
            bufferBuilder.vertex((float) bb.maxX, (float) bb.minY, (float) bb.maxZ).color(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
            bufferBuilder.vertex((float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).color(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
            bufferBuilder.vertex((float) bb.minX, (float) bb.maxY, (float) bb.maxZ).color(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            release();
        }
    }

    public static void drawBlockOutline(MatrixStack matrices, BlockPos position, Color color) {
        drawBlockOutline(matrices, new Box(position), color);
    }

    public static void drawBlockOutline(MatrixStack matrices, Box bb, Color color) {
        Renderer3d.renderOutline(matrices, color, bb.getMinPos(), bb.getMaxPos().subtract(bb.getMinPos()));
    }

    public static void scaleAndPosition(MatrixStack matrices, float x, float y, float scale) {
        matrices.push(); // Save the current transformation state

        // Translate the origin back to the desired position
        matrices.translate(x, y, 0);

        // Scale the matrix
        matrices.scale(scale, scale, 1.0F);

        matrices.translate(-x, -y, 0);
    }

    public static void stopScaling(MatrixStack matrices) {
        matrices.pop();
    }

    public static Box getRenderBB(Object position) {
        if (position instanceof BlockPos) {
            return new Box((double)((BlockPos)position).getX() - mc.getEntityRenderDispatcher().camera.getPos().x, (double)((BlockPos)position).getY() - mc.getEntityRenderDispatcher().camera.getPos().y, (double)((BlockPos)position).getZ() - mc.getEntityRenderDispatcher().camera.getPos().z, (double)(((BlockPos)position).getX() + 1) - mc.getEntityRenderDispatcher().camera.getPos().x, (double)(((BlockPos)position).getY() + 1) - mc.getEntityRenderDispatcher().camera.getPos().y, (double)(((BlockPos)position).getZ() + 1) - mc.getEntityRenderDispatcher().camera.getPos().z);
        }
        if (position instanceof Box) {
            return new Box(((Box)position).minX - mc.getEntityRenderDispatcher().camera.getPos().x, ((Box)position).minY - mc.getEntityRenderDispatcher().camera.getPos().y, ((Box)position).minZ - mc.getEntityRenderDispatcher().camera.getPos().z, ((Box)position).maxX - mc.getEntityRenderDispatcher().camera.getPos().x, ((Box)position).maxY - mc.getEntityRenderDispatcher().camera.getPos().y, ((Box)position).maxZ - mc.getEntityRenderDispatcher().camera.getPos().z);
        }
        return null;
    }

    public static FontRenderer getFontRenderer() {
        return FontRenderers.fontRenderer;
    }
    public static fxFontRenderer getFxFontRenderer() {
        return FontRenderers.fxfontRenderer;
    }

    public static void drawString(MatrixStack matrixStack, String text, float x, float y, int color) {
        if (!ModuleFont.INSTANCE.customFonts.getValue()) {
            color = fixColorValue(color);
            if(!ModuleFont.INSTANCE.textShadows.getValue()) {
                drawContext.drawText(MinecraftClient.getInstance().textRenderer, text, (int) x, (int) y, color, false);
            } else if(ModuleFont.INSTANCE.textShadows.getValue()) {
                drawContext.drawText(MinecraftClient.getInstance().textRenderer, text, (int) x, (int) y, color, true);
            }
        } else if (ModuleFont.INSTANCE.customFonts.getValue()) {
            if(getFontRenderer() == null) return;
            if(!ModuleFont.INSTANCE.textShadows.getValue()) {
                getFontRenderer().drawString(matrixStack, text, x, y, 256 - ColorUtils.getRed(color), 256 - ColorUtils.getGreen(color), 256 - ColorUtils.getBlue(color), 256 - ColorUtils.getAlpha(color));
            } else if(ModuleFont.INSTANCE.textShadows.getValue()) {
                ShadowFontRenderer shadowFont = new ShadowFontRenderer(getFontRenderer());
                shadowFont.drawStringWithShadow(matrixStack, text, x, y, 256 - ColorUtils.getRed(color), 256 - ColorUtils.getGreen(color), 256 - ColorUtils.getBlue(color), 256 - ColorUtils.getAlpha(color));
            }
        }
    }

    private static int fixColorValue(int color){
        return ColorUtils.setAlpha(color,5f,5).getRGB();
    }
}