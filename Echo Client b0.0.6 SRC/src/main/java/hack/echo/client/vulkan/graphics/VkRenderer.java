package hack.echo.client.vulkan.graphics;

import hack.echo.client.vulkan.CommandBuffer;

public interface VkRenderer {
    void beginFrame();
    void flush(CommandBuffer commandBuffer);
    void cleanup();
}
