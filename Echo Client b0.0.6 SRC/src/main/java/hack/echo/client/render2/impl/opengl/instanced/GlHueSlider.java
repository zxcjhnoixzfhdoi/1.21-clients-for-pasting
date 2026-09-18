package hack.echo.client.render2.impl.opengl.instanced;

import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.api.GlAttributeBuilder;
import hack.echo.client.render2.impl.opengl.api.GlInstancedShader;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL33.*;

public class GlHueSlider extends GlInstancedShader {

    public GlHueSlider() {
        super("2d/color/hue_slider.frag", "2d/color/hue_slider.vert", 84);
        new GlAttributeBuilder()
                .stride(84)
                .mat4Attrib(0)
                .floatAttrib(4, 64)
                .floatAttrib(1, 80);
    }

    public void slider(Matrix4f model, float x, float y, float w, float h) {
        checkFlush();
        int pos = buffer.position();
        model.get(pos, buffer);
        buffer.putFloat(pos + 64, x);
        buffer.putFloat(pos + 68, y);
        buffer.putFloat(pos + 72, w);
        buffer.putFloat(pos + 76, h);
        buffer.putFloat(pos + 80, Draw2D.nextZ());
        buffer.position(pos + 84);
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
