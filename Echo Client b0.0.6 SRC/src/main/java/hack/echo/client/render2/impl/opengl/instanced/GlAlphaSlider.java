package hack.echo.client.render2.impl.opengl.instanced;

import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.api.GlAttributeBuilder;
import hack.echo.client.render2.impl.opengl.api.GlInstancedShader;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL33.*;

public class GlAlphaSlider extends GlInstancedShader {

    public GlAlphaSlider() {
        super("2d/color/alpha_slider.frag", "2d/color/alpha_slider.vert", 96);
        new GlAttributeBuilder()
                .stride(96)
                .mat4Attrib(0)
                .floatAttrib(4, 64)
                .floatAttrib(3, 80)
                .floatAttrib(1, 92);
    }

    public void slider(Matrix4f model, float x, float y, float w, float h, float r, float g, float b) {
        checkFlush();
        int pos = buffer.position();
        model.get(pos, buffer);
        buffer.putFloat(pos + 64, x);
        buffer.putFloat(pos + 68, y);
        buffer.putFloat(pos + 72, w);
        buffer.putFloat(pos + 76, h);
        buffer.putFloat(pos + 80, r);
        buffer.putFloat(pos + 84, g);
        buffer.putFloat(pos + 88, b);
        buffer.putFloat(pos + 92, Draw2D.nextZ());
        buffer.position(pos + 96);
        count++;
    }

    @Override
    public void flush() {
        if (count == 0) return;
        glUseProgram(program);
        glBindVertexArray(vao);
        glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 4, flushBuffer());
    }
}
