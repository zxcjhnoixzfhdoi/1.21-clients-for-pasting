package hack.echo.client.vulkan.descriptor;

import hack.echo.client.vulkan.memory.MemUtil;
import lombok.Getter;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import static org.lwjgl.vulkan.VK10.nvkCmdPushConstants;

@Getter
public class PushConstants {
    private final int size;
    private final int stages;
    public final long pointer;

    public PushConstants(int size, int stages) {
        this.size = size;
        this.stages = stages;
        this.pointer = MemoryUtil.nmemAlloc(size);
    }

    public void push(VkCommandBuffer commandBuffer, long pipelineLayout) {
        nvkCmdPushConstants(commandBuffer, pipelineLayout, stages, 0, size, pointer);
    }

    public void cleanup() {
        MemoryUtil.nmemFree(pointer);
    }
}
