package hack.echo.client.render2.impl.vulkan.impl;

import hack.echo.client.vulkan.descriptor.SimpleUBO;
import hack.echo.client.vulkan.graphics.VkGraphicsPipeline;
import hack.echo.client.vulkan.graphics.VkShader;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.vulkan.memory.MemUtil;
import org.joml.Matrix4f;

import static org.lwjgl.vulkan.VK10.*;

public class VkAlphaSlider extends VkShader {
    private static final int STRIDE = 96;
    private final SimpleUBO ubo;

    public VkAlphaSlider(SimpleUBO ubo) {
        super(STRIDE, 4);
        this.ubo = ubo;
    }

    @Override
    protected VkGraphicsPipeline.Builder getPipelineBuilder() {
        return VkGraphicsPipeline.builder("2d/color/alpha_slider.vert", "2d/color/alpha_slider.frag")
                .stride(STRIDE)
                .attribute(0, VK_FORMAT_R32G32B32A32_SFLOAT, 0)
                .attribute(1, VK_FORMAT_R32G32B32A32_SFLOAT, 16)
                .attribute(2, VK_FORMAT_R32G32B32A32_SFLOAT, 32)
                .attribute(3, VK_FORMAT_R32G32B32A32_SFLOAT, 48)
                .attribute(4, VK_FORMAT_R32G32B32A32_SFLOAT, 64)
                .attribute(5, VK_FORMAT_R32G32B32_SFLOAT, 80)
                .attribute(6, VK_FORMAT_R32_SFLOAT, 92)
                .depthTest(false).depthWrite(false)
                .descriptor(ubo);
    }

    public void add(Matrix4f model, float x, float y, float w, float h, float r, float g, float b) {
        long p = ptr();
        MemUtil.putMat4(p, model);
        MemUtil.putVec4(p + 64, x, y, w, h);
        MemUtil.putFloat(p + 80, r);
        MemUtil.putFloat(p + 84, g);
        MemUtil.putFloat(p + 88, b);
        MemUtil.putFloat(p + 92, Draw2D.nextZ());
        next();
    }
}
