package hack.echo.client.render3.impl.vk.api;

import com.mojang.blaze3d.systems.RenderSystem;
import hack.echo.client.Echo;
import hack.echo.client.particle.ParticleManager;
import hack.echo.client.render3.impl.vk.*;
import hack.echo.client.vulkan.CommandBuffer;
import hack.echo.client.vulkan.descriptor.SimpleUBO;
import hack.echo.client.render3.api.FramebufferTarget;
import hack.echo.client.vulkan.graphics.VkRenderer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import static hack.echo.client.utils.Imports.mc;

public class VkMinecraftTarget extends FramebufferTarget {

    private final List<VkRenderer> shaders = new ObjectArrayList<>();

    private final VkBox box;
    private final VkTracer vkTracer;
    private final VkLine vkLine;
    private final VkParticle vkParticle;
    public VkMinecraftTarget(SimpleUBO ubo) {
        box = new VkBox(ubo);
        vkTracer = new VkTracer(ubo);
        vkLine = new VkLine(ubo);
        vkParticle = new VkParticle(ubo);
        shaders.add(box);
        shaders.add(vkTracer);
        shaders.add(vkLine);
        shaders.add(vkParticle);
    }

    @Override
    public void beginFrame() {
        shaders.forEach(VkRenderer::beginFrame);
    }

    @Override
    public void endFrame() {
        var target = mc.getMainRenderTarget();

        var encoder = RenderSystem.getDevice().createCommandEncoder();
        var pass = encoder.createRenderPass(
                () -> "VkMinecraftTarget",
                target.getColorTextureView(),
                OptionalInt.empty(),
                target.getDepthTextureView(),
                OptionalDouble.empty()
        );

        var cmd = Echo.vkContext.getCommandBuffer();
        var metal = new CommandBuffer(cmd);

        shaders.forEach(e -> e.flush(metal));
        pass.close();
    }


    @Override
    public void cleanup() {
        shaders.forEach(VkRenderer::cleanup);
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
