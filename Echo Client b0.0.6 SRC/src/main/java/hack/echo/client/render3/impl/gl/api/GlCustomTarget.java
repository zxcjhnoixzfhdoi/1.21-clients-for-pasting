package hack.echo.client.render3.impl.gl.api;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import hack.echo.client.api.GlDeviceCompat;
import hack.echo.client.event.EventManager;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventRender2DGui;
import hack.echo.client.render2.api.CrossTexture;
import hack.echo.client.render2.impl.opengl.api.GlInstancedShader;
import hack.echo.client.render2.impl.opengl.api.GlState;
import hack.echo.client.render2.impl.opengl.utils.RenderUtil;
import hack.echo.client.render3.api.FramebufferTarget;
import hack.echo.client.particle.ParticleManager;
import hack.echo.client.render3.impl.gl.*;
import hack.echo.client.utils.Imports;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.joml.Matrix4f;

import java.util.List;

import static org.lwjgl.opengl.GL33.*;

public class GlCustomTarget extends FramebufferTarget implements Imports {

    private final List<GlInstancedShader> shaders = new ObjectArrayList<>();

    private final GlBox box;
    private final GlTracers glTracers;
    private final GlVoxelRenderer glVoxelRenderer;
    private final GlLine glLine;
    private final GlParticle glParticle;

    private int width;
    private int height;

    private final RenderTarget renderTarget;
    private CrossTexture colorAttachment;

    public GlCustomTarget() {
        box = new GlBox();
        glTracers = new GlTracers();
        glVoxelRenderer = new GlVoxelRenderer();
        glLine = new GlLine();
        glParticle = new GlParticle();
        shaders.add(box);
        shaders.add(glTracers);
        shaders.add(glLine);
        shaders.add(glParticle);

        this.width = mc.getWindow().getWidth();
        this.height = mc.getWindow().getHeight();
        this.renderTarget = new RenderTargetDescriptor(width, height, true, 0).allocate();
        this.colorAttachment = CrossTexture.from(renderTarget);

        EventManager.register(this);
    }

    @EventSubscribe(priority = EventSubscribe.Priority.HIGHEST)
    public void onRender2D(EventRender2DGui event) {
        var window = mc.getWindow();
        event.getDraw2D().screenImage(
                new Matrix4f(),
                colorAttachment,
                0, 0, RenderUtil.getScaledWidth(), RenderUtil.getScaledHeight(),
                0, 1f
        );
    }

    private void resize() {
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        if (this.width != w || this.height != h) {
            this.width = w;
            this.height = h;
            renderTarget.resize(w, h);
            colorAttachment = CrossTexture.from(renderTarget);
        }
    }

    @Override
    public void beginFrame() {
        resize();
        var state = new GlState();
        GlDeviceCompat.bindFramebuffer(renderTarget);
        glDisable(GL_SCISSOR_TEST);
        glColorMask(true, true, true, true);
        glDepthMask(true);
        glClearColor(0, 0, 0, 0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        state.restore();
    }

    @Override
    public void endFrame() {
        var state = new GlState();
        GlDeviceCompat.bindFramebuffer(renderTarget);
        glDisable(GL_SCISSOR_TEST);
        glColorMask(true, true, true, true);
        glDepthMask(true);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
        shaders.forEach(GlInstancedShader::flush);
        glVoxelRenderer.flush();

        state.restore();
    }

    @Override
    public void cleanup() {
        EventManager.unregister(this);
        shaders.forEach(GlInstancedShader::cleanup);
        colorAttachment.cleanup();
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
