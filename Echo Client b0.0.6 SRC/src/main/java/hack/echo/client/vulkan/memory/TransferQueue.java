package hack.echo.client.vulkan.memory;


//? if >26.1.2 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import hack.echo.client.mixin.accessors.GpuDeviceAccessor;
*///?}

import hack.echo.client.Echo;
import hack.echo.client.vulkan.utils.VkUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class TransferQueue {

    private final VkQueue handle;
    private final VkDevice vkDevice;
    private final long commandPool;

    //? if > 26.1.2 {
    /*public TransferQueue(VkDevice vkDevice) {
        var vulkanDevice = ((VulkanDevice) ((GpuDeviceAccessor) RenderSystem.getDevice()).getBackend());
        var queue = vulkanDevice.transferQueue();

        this.vkDevice = vkDevice;
        this.handle = queue.vkQueue();

        try (var stack = stackPush()) {
            var poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .queueFamilyIndex(queue.queueFamilyIndex())
                    .flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);

            var pPool = stack.mallocLong(1);
            VkUtils.check(vkCreateCommandPool(vkDevice, poolInfo, null, pPool),
                    "Failed to create transfer command pool");
            commandPool = pPool.get(0);
        }
    }
    *///?} else {

    public TransferQueue(VkDevice vkDevice) {
        this.vkDevice = vkDevice;
        try (var stack = stackPush()) {
            int family = findTransferFamily(stack);

            var pQueue = stack.mallocPointer(1);
            vkGetDeviceQueue(vkDevice, family, 0, pQueue);
            this.handle = new VkQueue(pQueue.get(0), vkDevice);

            var poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .queueFamilyIndex(family)
                    .flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);

            var pPool = stack.mallocLong(1);
            VkUtils.check(vkCreateCommandPool(vkDevice, poolInfo, null, pPool),
                    "Failed to create transfer command pool");
            commandPool = pPool.get(0);
        }
    }

    private int findTransferFamily(MemoryStack stack) {
        var count = stack.mallocInt(1);
        vkGetPhysicalDeviceQueueFamilyProperties(vkDevice.getPhysicalDevice(), count, null);

        var props = VkQueueFamilyProperties.malloc(count.get(0), stack);
        vkGetPhysicalDeviceQueueFamilyProperties(vkDevice.getPhysicalDevice(), count, props);

        for (int i = 0; i < props.capacity(); i++) {
            int flags = props.get(i).queueFlags();
            if ((flags & VK_QUEUE_TRANSFER_BIT) != 0 && (flags & VK_QUEUE_GRAPHICS_BIT) == 0) {
                return i;
            }
        }

        for (int i = 0; i < props.capacity(); i++) {
            if ((props.get(i).queueFlags() & VK_QUEUE_TRANSFER_BIT) != 0) {
                return i;
            }
        }

        throw new RuntimeException("No transfer queue family found");
    }

    //?}

    public VkCommandBuffer beginCommands() {
        try (var stack = stackPush()) {
            var allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPool)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1);

            var pCmd = stack.mallocPointer(1);
            VkUtils.check(vkAllocateCommandBuffers(vkDevice, allocInfo, pCmd),
                    "Failed to allocate transfer command buffer");

            var cmd = new VkCommandBuffer(pCmd.get(0), vkDevice);

            var beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            VkUtils.check(vkBeginCommandBuffer(cmd, beginInfo),
                    "Failed to begin transfer command buffer");

            return cmd;
        }
    }

    public void submitCommands(VkCommandBuffer cmd) {
        try (var stack = stackPush()) {
            VkUtils.check(vkEndCommandBuffer(cmd), "Failed to end transfer command buffer");

            var submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(cmd));

            VkUtils.check(vkQueueSubmit(handle, submitInfo, VK_NULL_HANDLE),
                    "Failed to submit transfer command buffer");
        }
    }

    public void waitIdle() {
        vkQueueWaitIdle(handle);
    }

    public void cleanUp() {
        vkDestroyCommandPool(vkDevice, commandPool, null);
    }
}