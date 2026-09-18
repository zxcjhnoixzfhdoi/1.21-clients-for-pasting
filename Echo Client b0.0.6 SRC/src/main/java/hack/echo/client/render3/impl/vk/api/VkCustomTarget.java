package hack.echo.client.render3.impl.vk.api;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.systems.RenderSystem;
import hack.echo.client.Echo;
import hack.echo.client.event.EventManager;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventRender2DGui;
import hack.echo.client.particle.ParticleManager;
import hack.echo.client.render2.api.CrossTexture;
import hack.echo.client.render2.impl.opengl.utils.RenderUtil;
import hack.echo.client.render3.api.FramebufferTarget;
import hack.echo.client.render3.impl.vk.*;
import hack.echo.client.utils.Imports;
import hack.echo.client.vulkan.CommandBuffer;
import hack.echo.client.vulkan.descriptor.SimpleUBO;
import hack.echo.client.vulkan.graphics.VkRenderer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.joml.Matrix4f;

import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class VkCustomTarget extends FramebufferTarget implements Imports {

    private final List<VkRenderer> shaders = new ObjectArrayList<>();


    private final VkBox box;
    private final VkTracer vkTracer;
    private final VkLine vkLine;
    private final VkParticle vkParticle;
    private final VkVoxelRenderer vkVoxelRenderer;

    private int width;
    private int height;

    private final RenderTarget renderTarget;
    private CrossTexture colorAttachment;
    public VkCustomTarget(SimpleUBO ubo) {
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();

        this.width = width;
        this.height = height;
        box = new VkBox(ubo);
        vkTracer = new VkTracer(ubo);
        vkVoxelRenderer = new VkVoxelRenderer(ubo);
        vkLine = new VkLine(ubo);
        vkParticle = new VkParticle(ubo);

        shaders.add(box);
        shaders.add(vkTracer);
        shaders.add(vkLine);
        shaders.add(vkVoxelRenderer);
        shaders.add(vkParticle);

        this.renderTarget = new RenderTargetDescriptor(width, height, true, 0).allocate();
        this.colorAttachment = CrossTexture.from(renderTarget);

        EventManager.register(this);
    }

    @EventSubscribe(priority = EventSubscribe.Priority.HIGHEST)
    public void event(EventRender2DGui event) {
        var window = mc.getWindow();

        event.getDraw2D().screenImage(
                new Matrix4f(),
                colorAttachment,
                0, 0, RenderUtil.getScaledWidth(), RenderUtil.getScaledHeight(),
                0, 1f
        );
    }

    private void resize() {
        int windowWidth = mc.getWindow().getWidth();
        int windowHeight = mc.getWindow().getHeight();
        if (this.width != windowWidth || this.height != windowHeight) {
            this.width = windowWidth;
            this.height = windowHeight;

            renderTarget.resize(windowWidth, windowHeight);
            colorAttachment = CrossTexture.from(renderTarget);
        }
    }

    @Override
    public void beginFrame() {
        resize();
        shaders.forEach(VkRenderer::beginFrame);
    }

    @Override
    public void endFrame() {
        if (renderTarget == null) return;

        var cmd = Echo.vkContext.getCommandBuffer();
        var metal = new CommandBuffer(cmd);
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        var pass = encoder.createRenderPass(
                () -> "Draw3D",
                renderTarget.getColorTextureView(),
                OptionalInt.of(0x00000000),
                renderTarget.getDepthTextureView(),
                OptionalDouble.of(Echo.vkContext.depthClearValue())
        );

        metal.viewport(0, height, width, -height);
        metal.scissor(0, 0, width, height);

        shaders.forEach(e -> e.flush(metal));
        pass.close();
    }

    @Override
    public void cleanup() {
        shaders.forEach(VkRenderer::cleanup);
        colorAttachment.cleanup();
        shaders.clear();
    }


    @Override
    public void box(double x, double y, double z, double sx, double sy, double sz, int color) {
        box.add(x, y, z, sx, sy, sz, color);
    }


    @Override
    public void tracer(double x, double y, double z, int color) {
        vkTracer.add(x, y, z, color);
    }

    @Override
    public void line(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, int color) {
        vkLine.add(fromX, fromY, fromZ, toX, toY, toZ, color);
    }

    @Override
    public void particle(double x, double y, double z, float size, int color, ParticleManager.UV uv, float rotation) {
        vkParticle.add(x, y, z, size, color, uv, rotation);
    }

}
