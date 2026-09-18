package hack.echo.client.render2.impl.opengl.instanced;

import hack.echo.client.Echo;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.api.GlAttributeBuilder;
import hack.echo.client.render2.impl.opengl.api.GlInstancedShader;
import hack.echo.client.render2.impl.opengl.utils.GLShaderUniforms;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL33.*;

public class GlLiquidGlass extends GlInstancedShader {

    private static final int STRIDE = 108;

    public GlLiquidGlass() {
        super("2d/glass/liquid_glass.frag", "2d/glass/liquid_glass.vert", STRIDE);

        new GlAttributeBuilder()
                .stride(STRIDE)
                .mat4Attrib(0)
                .floatAttrib(4, 64)
                .floatAttrib(4, 80)
                .intAttrib(1, 96)
                .floatAttrib(1, 100)
                .floatAttrib(1, 104);
    }

    public void add(Matrix4f model, float x, float y, float w, float h,
                    float r1, float r2, float r3, float r4,
                    int tint, float refractionStrength) {
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
        buffer.putInt(pos + 96, tint);
        buffer.putFloat(pos + 100, refractionStrength);
        buffer.putFloat(pos + 104, Draw2D.nextZ());
        buffer.position(pos + STRIDE);
        count++;
    }

    @Override
    public void flush() {
        var blur = Echo.draw2D.getBlurResult();
        if (count == 0 || blur == null) return;

        glUseProgram(program);
        GLShaderUniforms.uniformTexture(program, "blurTex", blur.glId);
        glBindVertexArray(vao);
        glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 4, flushBuffer());
    }
}
