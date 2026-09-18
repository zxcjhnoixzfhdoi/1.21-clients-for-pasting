package hack.echo.client.vulkan.graphics;

import hack.echo.client.vulkan.CommandBuffer;
import hack.echo.client.vulkan.api.BufferUsage;
import hack.echo.client.vulkan.api.VulkanBuffer;
import hack.echo.client.vulkan.utils.VkUtils;

import static hack.echo.client.Echo.vulkanDevice;

public abstract class VkShader implements VkRenderer {
    protected VkGraphicsPipeline pipeline;
    protected VulkanBuffer vbo;
    protected final int stride;
    protected final int vertexCount;
    protected int count;
    protected int offset;

    protected VkShader(int stride, int vertexCount) {
        this.vertexCount = vertexCount;
        this.stride = stride;
        this.vbo = vulkanDevice.newBuffer(BufferUsage.VERTEX, 1L << 12);
    }

    protected abstract VkGraphicsPipeline.Builder getPipelineBuilder();

    public long ptr() {
        return vbo.contents() + (long)(offset + count) * stride;
    }

    public void next() {
        count++;
        long required = (long)(offset + count + 1) * stride;
        if (required > vbo.size) {
            this.vbo = VkUtils.growBuffer(vulkanDevice, vbo, vbo.size << 1);
        }
    }

    public void beginFrame() {
        if (pipeline == null) {
            pipeline = getPipelineBuilder().build();
        }
        count = 0;
        offset = 0;
    }

    public void flush(CommandBuffer commandBuffer) {
        if (count == 0) return;
        commandBuffer.draw(pipeline, vbo, vertexCount, count, offset);
        offset += count;
        count = 0;
    }

    public void cleanup() {
        pipeline.cleanUp();
        vulkanDevice.releaseResource(vbo);
    }
}
