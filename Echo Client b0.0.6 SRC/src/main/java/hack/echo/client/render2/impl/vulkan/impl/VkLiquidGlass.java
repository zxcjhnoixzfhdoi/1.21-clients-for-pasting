package hack.echo.client.render2.impl.vulkan.impl;

import hack.echo.client.Echo;
import hack.echo.client.vulkan.CommandBuffer;
import hack.echo.client.vulkan.descriptor.SimpleSampler;
import hack.echo.client.vulkan.descriptor.SimpleUBO;
import hack.echo.client.vulkan.graphics.VkGraphicsPipeline;
import hack.echo.client.vulkan.graphics.VkShader;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.vulkan.memory.MemUtil;
import org.joml.Matrix4f;

import static org.lwjgl.vulkan.VK10.*;

public class VkLiquidGlass extends VkShader {
    private static final int STRIDE = 108;
    private final SimpleUBO ubo;
    private final SimpleSampler sampler;

    public VkLiquidGlass(SimpleUBO ubo) {
        super(STRIDE, 4);
        this.ubo = ubo;
        this.sampler = new SimpleSampler(1, VK_SHADER_STAGE_FRAGMENT_BIT);
    }

    @Override
    protected VkGraphicsPipeline.Builder getPipelineBuilder() {
        return VkGraphicsPipeline.builder("2d/glass/liquid_glass.vert", "2d/glass/liquid_glass.frag")
                .stride(STRIDE)
                .attribute(0, VK_FORMAT_R32G32B32A32_SFLOAT, 0)
                .attribute(1, VK_FORMAT_R32G32B32A32_SFLOAT, 16)
                .attribute(2, VK_FORMAT_R32G32B32A32_SFLOAT, 32)
                .attribute(3, VK_FORMAT_R32G32B32A32_SFLOAT, 48)
                .attribute(4, VK_FORMAT_R32G32B32A32_SFLOAT, 64)
                .attribute(5, VK_FORMAT_R32G32B32A32_SFLOAT, 80)
                .attribute(6, VK_FORMAT_R32_SINT, 96)
                .attribute(7, VK_FORMAT_R32_SFLOAT, 100)
                .attribute(8, VK_FORMAT_R32_SFLOAT, 104)
                .blend(true)
                .depthTest(false).depthWrite(false)
                .descriptor(ubo)
                .descriptor(sampler);
    }

    public void add(Matrix4f model, float x, float y, float w, float h,
                    float r1, float r2, float r3, float r4,
                    int tint, float refractionStrength) {
        long p = ptr();
        MemUtil.putMat4(p, model);
        MemUtil.putVec4(p + 64, x, y, w, h);
        MemUtil.putVec4(p + 80, r1, r2, r3, r4);
        MemUtil.putInt(p + 96, tint);
        MemUtil.putFloat(p + 100, refractionStrength);
        MemUtil.putFloat(p + 104, Draw2D.nextZ());
        next();
    }

    @Override
    public void flush(CommandBuffer cmd) {
        var blur = Echo.draw2D.getBlurResult();
        if (count == 0 || blur == null) return;
        sampler.setImage(blur.vkImage);
        super.flush(cmd);
    }

}
