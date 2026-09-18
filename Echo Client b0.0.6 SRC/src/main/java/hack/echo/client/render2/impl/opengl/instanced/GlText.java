package hack.echo.client.render2.impl.opengl.instanced;

import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.api.GlAttributeBuilder;
import hack.echo.client.render2.impl.opengl.api.GlInstancedShader;
import hack.echo.client.render2.impl.opengl.font.Font;
import hack.echo.client.render2.impl.opengl.font.Glyph;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL33.*;

public class GlText extends GlInstancedShader {

    private final Font font;

    public GlText(Font font) {
        super("2d/font/font.frag", "2d/font/font.vert", 116);
        this.font = font;
        new GlAttributeBuilder()
                .stride(116)
                .mat4Attrib(0)
                .floatAttrib(4, 64)
                .floatAttrib(4, 80)
                .intAttrib(4, 96)
                .floatAttrib(1, 112);
    }

    private void addGlyph(Matrix4f model, float x0, float y0, float w, float h, float u0, float v0, float uW,
            float vH, int c1, int c2, int c3, int c4, float z) {
        checkFlush();
        int pos = buffer.position();
        model.get(pos, buffer);
        buffer.putFloat(pos + 64, x0);
        buffer.putFloat(pos + 68, y0);
        buffer.putFloat(pos + 72, w);
        buffer.putFloat(pos + 76, h);
        buffer.putFloat(pos + 80, u0);
        buffer.putFloat(pos + 84, v0);
        buffer.putFloat(pos + 88, uW);
        buffer.putFloat(pos + 92, vH);
        buffer.putInt(pos + 96, c1);
        buffer.putInt(pos + 100, c2);
        buffer.putInt(pos + 104, c3);
        buffer.putInt(pos + 108, c4);
        buffer.putFloat(pos + 112, z);
        buffer.position(pos + 116);
        count++;
    }

    public void text(Matrix4f model, CharSequence text, float x, float y, float size, int color) {
        float ascender = font.getAscender();
        float lineHeight = font.getLineHeight();
        int width = font.getWidth();
        int height = font.getHeight();
        float startX = x;
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
                addGlyph(model, x0, y0, w, h, u0, v0, uW, vH, color, color, color, color, z);
            }
            x += size * glyph.getAdvance();
        }
    }

    public void text(Matrix4f model, CharSequence text, float x, float y, float size, int c1, int c2, int c3, int c4) {
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
        float z = Draw2D.nextZ();

        for (int i = 0; i < text.length(); i++) {
            int unicode = Character.codePointAt(text, i);
            if (unicode == '\n') { x = startX; y += lineHeight * size; currentLine++; continue; }
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
                    interpolate(c1, c2, c3, c4, uR, vB), z);
            }
            x += size * glyph.getAdvance();
        }
    }

    private static int interpolate(int c1, int c2, int c3, int c4, float u, float v) {
        return ARGB.srgbLerp(v, ARGB.srgbLerp(u, c1, c2), ARGB.srgbLerp(u, c3, c4));
    }

    @Override
    public void flush() {
        if (count == 0 || font == null)
            return;

        glUseProgram(program);
        uniform1f("pxRange", 8.0f);
        uniform1i("tex", 0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, font.getTexture().glId);
        glBindVertexArray(vao);
        glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 4, flushBuffer());
    }
}
