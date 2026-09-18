package hack.echo.client.render2.impl.vulkan.impl;

import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.vulkan.descriptor.PushConstants;
import hack.echo.client.vulkan.descriptor.SamplerArray;
import hack.echo.client.vulkan.descriptor.SimpleSampler;
import hack.echo.client.vulkan.descriptor.SimpleUBO;
import hack.echo.client.vulkan.graphics.VkGraphicsPipeline;
import hack.echo.client.render2.impl.opengl.font.Font;
import hack.echo.client.render2.impl.opengl.font.Glyph;
import hack.echo.client.vulkan.graphics.VkShader;
import hack.echo.client.vulkan.memory.MemUtil;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;

import static org.lwjgl.vulkan.VK10.*;

public class VkText extends VkShader {

    private static final int STRIDE = 120;
    private final SimpleUBO ubo;
    private final SamplerArray samplerArray;
    private final PushConstants pc;

    private final Object2IntMap<Font> indices = new Object2IntArrayMap<>();
    public VkText(SimpleUBO ubo) {
        super(STRIDE, 4);
        this.ubo = ubo;
        this.samplerArray = new SamplerArray(1, VK_SHADER_STAGE_FRAGMENT_BIT, 16);
        this.pc = new PushConstants(4, VK_SHADER_STAGE_FRAGMENT_BIT);

        long ptr = pc.pointer;
        MemUtil.putInt(ptr, 8);

        put(0, Fonts.arial);
        put(1, Fonts.inter);
        put(2, Fonts.interSemiBold);
        put(3, Fonts.lucide);
    }

    private void put(int i, Font font) {
        indices.put(font, i);
        samplerArray.setImage(i, font.getTexture().vkImage);
    }


    @Override
    protected VkGraphicsPipeline.Builder getPipelineBuilder() {
        return VkGraphicsPipeline.builder("2d/font/text.vert", "2d/font/text.frag")
                .stride(STRIDE)
                .attribute(0, VK_FORMAT_R32G32B32A32_SFLOAT, 0)
                .attribute(1, VK_FORMAT_R32G32B32A32_SFLOAT, 16)
                .attribute(2, VK_FORMAT_R32G32B32A32_SFLOAT, 32)
                .attribute(3, VK_FORMAT_R32G32B32A32_SFLOAT, 48)
                .attribute(4, VK_FORMAT_R32G32B32A32_SFLOAT, 64)
                .attribute(5, VK_FORMAT_R32G32B32A32_SFLOAT, 80)
                .attribute(6, VK_FORMAT_R32G32B32A32_SINT, 96)
                .attribute(7, VK_FORMAT_R32_SINT, 112)
                .attribute(8, VK_FORMAT_R32_SFLOAT, 116)
                .descriptor(ubo)
                .descriptor(samplerArray)
                .pushConstants(pc);
    }

    private void addGlyph(Matrix4f model, float x0, float y0, float w, float h,
                          float u0, float v0, float uW, float vH, int c1, int c2, int c3, int c4, int texIndex, float z) {
        long p = ptr();
        MemUtil.putMat4(p, model);
        MemUtil.putVec4(p + 64, x0, y0, w, h);
        MemUtil.putVec4(p + 80, u0, v0, uW, vH);
        MemUtil.putInt(p + 96, c1);
        MemUtil.putInt(p + 100, c2);
        MemUtil.putInt(p + 104, c3);
        MemUtil.putInt(p + 108, c4);
        MemUtil.putInt(p + 112, texIndex);
        MemUtil.putFloat(p + 116, z);
        next();
    }

    public void text(Font font, Matrix4f model, CharSequence text, float x, float y, float size, int color) {
        float ascender = font.getAscender();
        float lineHeight = font.getLineHeight();
        int width = font.getWidth();
        int height = font.getHeight();
        float startX = x;
        int texIndex = indices.getInt(font);
        float z = Draw2D.nextZ();

        for (int i = 0; i < text.length(); i++) {
            int unicode = Character.codePointAt(text, i);
            if (unicode == '\n') { x = startX; y += lineHeight * size; continue; }
            Glyph glyph = font.getGlyph(unicode);
            if (glyph == null) continue;
            if (glyph.getPlaneRight() - glyph.getPlaneLeft() != 0) {
                float x0 = x + glyph.getPlaneLeft() * size;
                float y0 = y + ascender * size - glyph.getPlaneTop() * size;
                float w = (glyph.getPlaneRight() - glyph.getPlaneLeft()) * size;
                float h = (glyph.getPlaneTop() - glyph.getPlaneBottom()) * size;
                float u0 = glyph.getAtlasLeft() / width;
                float v0 = (1.0f - glyph.getAtlasTop() / height);
                float uW = (glyph.getAtlasRight() - glyph.getAtlasLeft()) / width;
                float vH = (glyph.getAtlasTop() - glyph.getAtlasBottom()) / height;
                addGlyph(model, x0, y0, w, h, u0, v0, uW, vH, color, color, color, color, texIndex, z);
            }
            x += size * glyph.getAdvance();
        }
    }

    public void text(Font font, Matrix4f model, CharSequence text, float x, float y, float size, int c1, int c2, int c3, int c4) {
        float lineWidth = 0, totalWidth = 0;
        int numLines = 1;
        for (int i = 0; i < text.length(); i++) {
            int unicode = Character.codePointAt(text, i);
            if (unicode == '\n') {
                totalWidth = Math.max(totalWidth, lineWidth);
                lineWidth = 0;
                numLines++;
                continue;
            }
            Glyph glyph = font.getGlyph(unicode);
            if (glyph != null) lineWidth += size * glyph.getAdvance();
        }
        totalWidth = Math.max(totalWidth, lineWidth);
        if (totalWidth == 0) return;

        float ascender = font.getAscender();
        float lineHeight = font.getLineHeight();
        int width = font.getWidth();
        int height = font.getHeight();
        float startX = x;
        int currentLine = 0;
        int texIndex = indices.getInt(font);
        float z = Draw2D.nextZ();

        for (int i = 0; i < text.length(); i++) {
            int unicode = Character.codePointAt(text, i);
            if (unicode == '\n') {
                x = startX;
                y += lineHeight * size;
                currentLine++;
                continue;
            }
            Glyph glyph = font.getGlyph(unicode);
            if (glyph == null) continue;
            if (glyph.getPlaneRight() - glyph.getPlaneLeft() != 0) {
                float x0 = x + glyph.getPlaneLeft() * size;
                float y0 = y + ascender * size - glyph.getPlaneTop() * size;
                float w = (glyph.getPlaneRight() - glyph.getPlaneLeft()) * size;
                float h = (glyph.getPlaneTop() - glyph.getPlaneBottom()) * size;
                float uL = (x0 - startX) / totalWidth;
                float uR = (x0 + w - startX) / totalWidth;
                float vT = (float) currentLine / numLines;
                float vB = (float) (currentLine + 1) / numLines;
                float u0 = glyph.getAtlasLeft() / width;
                float v0 = (1.0f - glyph.getAtlasTop() / height);
                float uW = (glyph.getAtlasRight() - glyph.getAtlasLeft()) / width;
                float vH = (glyph.getAtlasTop() - glyph.getAtlasBottom()) / height;
                addGlyph(model, x0, y0, w, h, u0, v0, uW, vH,
                    interpolate(c1, c2, c3, c4, uL, vT),
                    interpolate(c1, c2, c3, c4, uR, vT),
                    interpolate(c1, c2, c3, c4, uL, vB),
                    interpolate(c1, c2, c3, c4, uR, vB),
                    texIndex, z
                );
            }
            x += size * glyph.getAdvance();
        }
    }

    private static int interpolate(int c1, int c2, int c3, int c4, float u, float v) {
        return ARGB.srgbLerp(v, ARGB.srgbLerp(u, c1, c2), ARGB.srgbLerp(u, c3, c4));
    }
}
