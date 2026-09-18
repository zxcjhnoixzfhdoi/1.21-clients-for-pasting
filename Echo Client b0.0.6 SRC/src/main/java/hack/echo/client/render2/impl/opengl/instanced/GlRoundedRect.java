package hack.echo.client.render2.impl.opengl.instanced;

import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.api.GlAttributeBuilder;
import hack.echo.client.render2.impl.opengl.api.GlInstancedShader;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL33.*;

public class GlRoundedRect extends GlInstancedShader {

    public GlRoundedRect() {
        super("2d/shapes/rounded_rect.frag", "2d/shapes/rounded_rect.vert", 116);

        new GlAttributeBuilder()
                .stride(116)
                .mat4Attrib(0)
                .floatAttrib(4, 64)
                .floatAttrib(4, 80)
                .intAttrib(4, 96)
                .floatAttrib(1, 112);
    }

    public void addRect(Matrix4f model, float x, float y, float w, float h, float radius, int color) {
        addRect(model, x, y, w, h, radius, radius, radius, radius, color, color, color, color);
    }

    public void addRect(Matrix4f model, float x, float y, float w, float h, float r1, float r2, float r3, float r4,
            int color) {
        addRect(model, x, y, w, h, r1, r2, r3, r4, color, color, color, color);
    }

    public void addRect(Matrix4f model, float x, float y, float w, float h, float r1, float r2, float r3, float r4,
            int c1, int c2, int c3, int c4) {
        checkFlush();
        int pos = buffer.position();
        model.get(pos, buffer);
        buffer.putFloat(pos + 64, x);
        buffer.putFloat(pos + 68, y);
        buffer.putFloat(pos + 72, w);
        buffer.putFloat(pos + 76, h);
        buffer.putFloat(pos + 80, r1);
        buffer.putFloat(pos + 84, r2);
        buffer.putFloat(pos + 88, r3);
        buffer.putFloat(pos + 92, r4);
        buffer.putInt(pos + 96, c1);
        buffer.putInt(pos + 100, c2);
        buffer.putInt(pos + 104, c3);
        buffer.putInt(pos + 108, c4);
        buffer.putFloat(pos + 112, Draw2D.nextZ());
        buffer.position(pos + 116);
        count++;
    }

    @Override
    public void flush() {
        if (count == 0)
            return;

        glUseProgram(program);
        glBindVertexArray(vao);
        glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 4, flushBuffer());
    }
}
