package hack.echo.client.render3.impl.gl.api;

import hack.echo.client.Echo;
import hack.echo.client.render2.impl.opengl.api.GlDraw2D;
import hack.echo.client.render3.api.Draw3D;
import hack.echo.client.render3.api.FramebufferTarget;
import hack.echo.client.render2.impl.opengl.api.GlState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

import static hack.echo.client.render2.api.Draw2D.cameraUboBinding;
import static org.lwjgl.opengl.GL33.*;

public class GlDraw3D extends Draw3D {

    private final GlMinecraftTarget minecraftTarget;
    private final GlCustomTarget customTarget;
    private final int ubo;

    private final Queue<Runnable> queue = new ArrayDeque<>();
    public GlDraw3D() {
        super();
        this.minecraftTarget = new GlMinecraftTarget();
        this.customTarget = new GlCustomTarget();
        ubo = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, ubo);
        glBufferData(GL_UNIFORM_BUFFER, 140L, GL_DYNAMIC_DRAW);
        glBindBufferBase(GL_UNIFORM_BUFFER, cameraUboBinding, ubo);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }

    public void queueRunnable(Runnable runnable) {
        queue.add(runnable);
    }

    @Override
    public void updateCamera(Matrix4f view, Matrix4f proj) {
        Minecraft minecraft = Minecraft.getInstance();

        this.view = view;
        this.proj = proj;
        this.mvp = new Matrix4f(proj).mul(view);
        Camera camera = minecraft.gameRenderer.getMainCamera();
        this.origin = camera.position();

        glBindBuffer(GL_UNIFORM_BUFFER, this.ubo);
        ByteBuffer mapped = glMapBufferRange(
                GL_UNIFORM_BUFFER,
                0,
                140L,
                GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
        if (mapped == null)
            return;

        this.view.get(0, mapped);
        this.proj.get(64, mapped);
        mapped.putFloat(128, (float) origin.x);
        mapped.putFloat(132, (float) origin.y);
        mapped.putFloat(136, (float) origin.z);
        glUnmapBuffer(GL_UNIFORM_BUFFER);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
        glBindBufferBase(GL_UNIFORM_BUFFER, cameraUboBinding, this.ubo);
    }

    @Override
    public FramebufferTarget getMinecraftTarget() {
        return minecraftTarget;
    }

    @Override
    public FramebufferTarget getCustomTarget() {
        return customTarget;
    }

    @Override
    public void beginFrame() {
        if (!queue.isEmpty()) {
            GlState state = new GlState();
            try {
                while (!queue.isEmpty()) {
                    queue.poll().run();
                }
            } finally {
                state.restore();
            }
        }
        if (Echo.draw2D instanceof GlDraw2D glDraw2D) {
            glDraw2D.computeBlur();
        }
        customTarget.beginFrame();
        minecraftTarget.beginFrame();
    }

    @Override
    public void endFrame() {
        minecraftTarget.endFrame();
        customTarget.endFrame();
    }

    @Override
    public void cleanup() {
        customTarget.cleanup();
        minecraftTarget.cleanup();
    }
}

