package hack.echo.client.render3.impl.vk;

import hack.echo.client.Echo;
import hack.echo.client.particle.ParticleManager;
import hack.echo.client.render3.api.Draw3D;
import hack.echo.client.vulkan.CommandBuffer;
import hack.echo.client.vulkan.descriptor.SimpleSampler;
import hack.echo.client.vulkan.descriptor.SimpleUBO;
import hack.echo.client.vulkan.graphics.VkGraphicsPipeline;
import hack.echo.client.vulkan.graphics.VkShader;
import hack.echo.client.vulkan.memory.MemUtil;

import static org.lwjgl.vulkan.VK10.*;

public class VkParticle extends VkShader {
    private static final int stride = 40;

    private final SimpleUBO ubo;
    private final SimpleSampler sampler;
    public VkParticle(SimpleUBO ubo) {
        super(stride, 4);
        this.ubo = ubo;
        sampler = new SimpleSampler(1, VK_SHADER_STAGE_FRAGMENT_BIT);
    }

    @Override
    protected VkGraphicsPipeline.Builder getPipelineBuilder() {
        return VkGraphicsPipeline.builder("3d/particle.vert", "3d/particle.frag")
                .stride(stride)
                .attribute(0, VK_FORMAT_R32G32B32_SFLOAT, 0)
                .attribute(1, VK_FORMAT_R32_SFLOAT, 12)
                .attribute(2, VK_FORMAT_R32_SINT, 16)
                .attribute(3, VK_FORMAT_R32G32B32A32_SFLOAT, 20)
                .attribute(4, VK_FORMAT_R32_SFLOAT, 36)
                .descriptor(ubo)
                .descriptor(sampler)
                .blend(true)
                .depthWrite(false)
                .depthTest(true)
                .cull(VK_CULL_MODE_NONE)
                .depthFunc(Echo.vkContext.depthCompareOp())
                .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP)
                .blendFunc(VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ONE);
    }

    public void add(double x, double y, double z, float size, int color, ParticleManager.UV uv, float rotation) {
        long p = ptr();
        var origin = Draw3D.getInstance().origin;
        MemUtil.putVec3(
                p,
                (float) (x - origin.x),
                (float) (y - origin.y),
                (float) (z - origin.z)
        );
        MemUtil.putFloat(p + 12, size);
        MemUtil.putInt(p + 16, color);
        MemUtil.putVec4(
                p + 20,
                uv.minX(),
                uv.minY(),
                uv.maxX(),
                uv.maxY()
        );
        MemUtil.putFloat(p + 36, rotation);
        next();
    }

    @Override
    public void flush(CommandBuffer metal) {
        var texture = Echo.particleManager.getAtlas();
        if (texture == null || count == 0) return;

        sampler.setImage(texture.vkImage);
        super.flush(metal);
    }
}
