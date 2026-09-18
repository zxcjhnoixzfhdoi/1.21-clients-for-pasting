package hack.echo.client.render3.impl.gl;

import hack.echo.client.Echo;
import hack.echo.client.particle.ParticleManager;
import hack.echo.client.render2.impl.opengl.api.GlAttributeBuilder;
import hack.echo.client.render2.impl.opengl.api.GlInstancedShader;
import hack.echo.client.render3.api.Draw3D;

import static org.lwjgl.opengl.GL33.*;

public class GlParticle extends GlInstancedShader {

    public GlParticle() {
        super("3d/particle.frag", "3d/particle.vert", 40);
        new GlAttributeBuilder()
                .stride(40)
                .floatAttrib(3, 0)
                .floatAttrib(1, 12)
                .intAttrib(1, 16)
                .floatAttrib(4, 20)
                .floatAttrib(1, 36);
    }

    public void add(double x, double y, double z, float size, int color, ParticleManager.UV uv, float rotation) {
        checkFlush();
        int pos = buffer.position();
        var origin = Draw3D.getInstance().origin;
        buffer.putFloat(pos,      (float) (x - origin.x));
        buffer.putFloat(pos + 4,  (float) (y - origin.y));
        buffer.putFloat(pos + 8,  (float) (z - origin.z));
        buffer.putFloat(pos + 12, size);
        buffer.putInt(pos + 16,   color);
        buffer.putFloat(pos + 20, uv.minX());
        buffer.putFloat(pos + 24, uv.minY());
        buffer.putFloat(pos + 28, uv.maxX());
        buffer.putFloat(pos + 32, uv.maxY());
        buffer.putFloat(pos + 36, rotation);
        buffer.position(pos + 40);
        count++;
    }

    @Override
    public void flush() {
        var atlas = Echo.particleManager.getAtlas();
        if (atlas == null || atlas.glId == -1 || count == 0) return;

        glDisable(GL_CULL_FACE);
        glBlendFuncSeparate(GL_ONE, GL_ONE, GL_ONE, GL_ONE);
        glDepthMask(false);

        glUseProgram(program);
        glBindVertexArray(vao);
        uniformTexture("Sampler0", atlas.glId);
        glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 4, flushBuffer());

        glDepthMask(true);
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_CULL_FACE);
    }
}
