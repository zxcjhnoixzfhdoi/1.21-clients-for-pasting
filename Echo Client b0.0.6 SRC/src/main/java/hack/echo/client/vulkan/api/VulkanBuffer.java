package hack.echo.client.vulkan.api;

import hack.echo.client.vulkan.utils.VkUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.VkBufferCreateInfo;

import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanBuffer implements Resource {

    public final long id;
    private final long allocation;
    private final long ptr;
    private final VulkanDevice device;
    public final BufferUsage usage;
    public final long size;

    public final boolean perFrame;
    protected VulkanBuffer(VulkanDevice device, BufferUsage usage, long size, boolean perFrame) {
        this.usage = usage;
        this.size = size;
        this.device = device;
        this.perFrame = perFrame;
        try (var stack = MemoryStack.stackPush()) {
            var bufferInfo = VkBufferCreateInfo.calloc(stack)
                    .sType$Default()
                    .size(perFrame ? size * device.framesNum : size)
                    .usage(toVkUsage(usage))
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            var allocInfo = VmaAllocationCreateInfo.calloc(stack)
                    .usage(VMA_MEMORY_USAGE_AUTO)
                    .flags(VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT |
                            VMA_ALLOCATION_CREATE_MAPPED_BIT);

            var idBuffer = stack.mallocLong(1);
            var allocBuffer = stack.mallocPointer(1);

            VkUtils.check(
                    vmaCreateBuffer(device.vma, bufferInfo, allocInfo, idBuffer, allocBuffer, null),
                    "Failed to allocate vulkan buffer"
            );

            this.id = idBuffer.get(0);
            this.allocation = allocBuffer.get(0);

            var ptrBuffer = stack.mallocPointer(1);
            vmaMapMemory(device.vma, allocation, ptrBuffer);

            this.ptr = ptrBuffer.get(0);
        }
    }

    private static int toVkUsage(BufferUsage usage) {
        return switch (usage) {
            case VERTEX -> VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
            case UNIFORM -> VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
            case TEXEL -> VK_BUFFER_USAGE_UNIFORM_TEXEL_BUFFER_BIT;
            case STORAGE -> VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
            case INDIRECT -> VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT;
        };
    }

    public long offset() {
        return perFrame ? this.size * device.frameIndex() : 0;
    }

    public long contents() {
        return ptr + offset();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VulkanBuffer buffer) {
            return this.id == buffer.id;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public void release() {
        vmaUnmapMemory(device.vma, allocation);
        vmaDestroyBuffer(device.vma, id, allocation);
    }
}
