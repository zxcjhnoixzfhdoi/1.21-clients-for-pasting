package hack.echo.client.render2.impl.vulkan.impl;

import hack.echo.client.vulkan.descriptor.SimpleUBO;
import hack.echo.client.vulkan.graphics.VkGraphicsPipeline;
import hack.echo.client.vulkan.graphics.VkShader;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.vulkan.memory.MemUtil;
import org.joml.Matrix4f;

import static org.lwjgl.vulkan.VK10.*;

public class VkRoundedRect extends VkShader {
    private static final int STRIDE = 116;
    private final SimpleUBO ubo;

    public VkRoundedRect(SimpleUBO ubo) {
        super(STRIDE, 4);
        this.ubo = ubo;
    }

    @Override
    protected VkGraphicsPipeline.Builder getPipelineBuilder() {
        return VkGraphicsPipeline.builder("2d/shapes/rounded_rect.vert", "2d/shapes/rounded_rect.frag")
                .stride(STRIDE)
                .attribute(0, VK_FORMAT_R32G32B32A32_SFLOAT, 0)
                .attribute(1, VK_FORMAT_R32G32B32A32_SFLOAT, 16)
                .attribute(2, VK_FORMAT_R32G32B32A32_SFLOAT, 32)
                .attribute(3, VK_FORMAT_R32G32B32A32_SFLOAT, 48)
                .attribute(4, VK_FORMAT_R32G32B32A32_SFLOAT, 64)
                .attribute(5, VK_FORMAT_R32G32B32A32_SFLOAT, 80)
                .attribute(6, VK_FORMAT_R32G32B32A32_SINT, 96)
                .attribute(7, VK_FORMAT_R32_SFLOAT, 112)
                .descriptor(ubo);
    }

    public void add(Matrix4f mat, float x, float y, float w, float h, float r, int color) {
        add(mat, x, y, w, h, r, r, r, r, color, color, color, color);
    }

    public void add(Matrix4f mat, float x, float y, float w, float h,
                    float r1, float r2, float r3, float r4, int color) {
        add(mat, x, y, w, h, r1, r2, r3, r4, color, color, color, color);
    }

    public void add(Matrix4f mat, float x, float y, float w, float h,
                    float r1, float r2, float r3, float r4, int c1, int c2, int c3, int c4) {
        long p = ptr();
        MemUtil.putMat4(p, mat);
        MemUtil.putVec4(p + 64, x, y, w, h);
        MemUtil.putVec4(p + 80, r1, r2, r3, r4);
        MemUtil.putInt(p + 96, c1);
        MemUtil.putInt(p + 100, c2);
        MemUtil.putInt(p + 104, c3);
        MemUtil.putInt(p + 108, c4);
        MemUtil.putFloat(p + 112, Draw2D.nextZ());
        next();
    }
}
