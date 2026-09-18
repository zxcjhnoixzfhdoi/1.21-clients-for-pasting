package hack.echo.client.render2.impl.vulkan.impl;

import hack.echo.client.vulkan.descriptor.SamplerArray;
import hack.echo.client.vulkan.descriptor.SimpleUBO;
import hack.echo.client.vulkan.graphics.VkGraphicsPipeline;
import hack.echo.client.render2.api.CrossTexture;
import hack.echo.client.vulkan.graphics.VkShader;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.vulkan.memory.MemUtil;
import org.joml.Matrix4f;

import java.util.HashMap;

import static org.lwjgl.vulkan.VK10.*;

public class VkScreenImage extends VkShader {
    private static final int STRIDE = 108;
    private final SimpleUBO ubo;
    private final SamplerArray samplerArray;
    private final HashMap<CrossTexture, Integer> textureIndices = new HashMap<>();
    private int imageCount = 0;

    public VkScreenImage(SimpleUBO ubo) {
        super(STRIDE, 4);
        this.ubo = ubo;
        this.samplerArray = new SamplerArray(1, VK_SHADER_STAGE_FRAGMENT_BIT, 16);
    }

    @Override
    protected VkGraphicsPipeline.Builder getPipelineBuilder() {
        return VkGraphicsPipeline.builder("2d/image/rounded_screen_image.vert", "2d/image/rounded_image.frag")
                .stride(STRIDE)
                .attribute(0, VK_FORMAT_R32G32B32A32_SFLOAT, 0)
                .attribute(1, VK_FORMAT_R32G32B32A32_SFLOAT, 16)
                .attribute(2, VK_FORMAT_R32G32B32A32_SFLOAT, 32)
                .attribute(3, VK_FORMAT_R32G32B32A32_SFLOAT, 48)
                .attribute(4, VK_FORMAT_R32G32B32A32_SFLOAT, 64)
                .attribute(5, VK_FORMAT_R32G32B32A32_SFLOAT, 80)
                .attribute(6, VK_FORMAT_R32_SFLOAT, 96)
                .attribute(7, VK_FORMAT_R32_SINT, 100)
                .attribute(8, VK_FORMAT_R32_SFLOAT, 104)
                .blend(true)
                .depthTest(false).depthWrite(false)
                .descriptor(ubo)
                .descriptor(samplerArray);
    }

    private int getIndex(CrossTexture texture) {
        Integer existing = textureIndices.get(texture);
        if (existing != null) return existing;

        int index = imageCount++;
        textureIndices.put(texture, index);
        samplerArray.setImage(index, texture.vkImage);
        return index;
    }

    public void add(Matrix4f model, float x, float y, float w, float h, float r1, float r2, float r3, float r4,
                    CrossTexture texture, float alpha) {
        if (texture.vkImage == null) return;
        int texIndex = getIndex(texture);
        long p = ptr();
        MemUtil.putMat4(p, model);
        MemUtil.putVec4(p + 64, x, y, w, h);
        MemUtil.putVec4(p + 80, r1, r2, r3, r4);
        MemUtil.putFloat(p + 96, alpha);
        MemUtil.putInt(p + 100, texIndex);
        MemUtil.putFloat(p + 104, Draw2D.nextZ());
        next();
    }

    @Override
    public void beginFrame() {
        super.beginFrame();
        textureIndices.clear();
        imageCount = 0;
        samplerArray.clear();
    }
}
