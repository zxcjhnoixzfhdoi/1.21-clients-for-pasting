package hack.echo.client.render3.impl.gl.api;

import hack.echo.client.render2.api.CrossTexture;
import hack.echo.client.render2.impl.opengl.api.GlInstancedShader;
import hack.echo.client.render2.impl.opengl.api.GlState;
import hack.echo.client.render3.api.FramebufferTarget;
import hack.echo.client.render2.impl.opengl.utils.RenderUtil;
import hack.echo.client.particle.ParticleManager;
import hack.echo.client.render3.impl.gl.GlBox;
import hack.echo.client.render3.impl.gl.GlLine;
import hack.echo.client.render3.impl.gl.GlParticle;
import hack.echo.client.render3.impl.gl.GlTracers;
import net.minecraft.client.Minecraft;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.joml.Matrix4f;

import java.util.List;

import static org.lwjgl.opengl.GL33.*;

public class GlMinecraftTarget extends FramebufferTarget {

    private final List<GlInstancedShader> shaders = new ObjectArrayList<>();

    private final GlBox box;
    private final GlTracers glTracers;
    private final GlLine glLine;
    private final GlParticle glParticle;
    public GlMinecraftTarget() {
        box = new GlBox();
        glTracers = new GlTracers();
        glLine = new GlLine();
        glParticle = new GlParticle();
        shaders.add(box);
        shaders.add(glTracers);
        shaders.add(glLine);
        shaders.add(glParticle);
    }


    @Override
    public void beginFrame() {

    }

    @Override
    public void endFrame() {
        var glState = new GlState();
        RenderUtil.bindMainFramebuffer();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);

        shaders.forEach(GlInstancedShader::flush);

        glState.restore();
    }

    @Override
    public void cleanup() {
        shaders.forEach(GlInstancedShader::cleanup);
        shaders.clear();
    }

    @Override
    public void box(double x, double y, double z, double sx, double sy, double sz, int color) {
        box.add(x, y, z, sx, sy, sz, color);
    }


    @Override
    public void tracer(double x, double y, double z, int color) {
        glTracers.addLine(x, y, z, color);
    }

    @Override
    public void line(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, int color) {
        glLine.addLine(fromX, fromY, fromZ, toX, toY, toZ, color);
    }

    @Override
    public void particle(double x, double y, double z, float size, int color, ParticleManager.UV uv, float rotation) {
        glParticle.add(x, y, z, size, color, uv, rotation);
    }

}
