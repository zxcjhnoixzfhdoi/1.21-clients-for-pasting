package hack.echo.client.vulkan.descriptor;

import hack.echo.client.Echo;
import hack.echo.client.vulkan.api.BufferUsage;
import hack.echo.client.vulkan.api.VulkanBuffer;
import hack.echo.client.vulkan.utils.VkUtils;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static hack.echo.client.Echo.vulkanDevice;
import static org.lwjgl.vulkan.VK10.*;

public class SimpleTexelBuffer implements Descriptor {
    private final int binding;
    private final int stages;
    private final int format;
    @Getter
    public VulkanBuffer buffer;
    private long bufferView;

    public SimpleTexelBuffer(int binding, int stages, int format, long bufferSize) {
        this.binding = binding;
        this.stages = stages;
        this.format = format;
        this.buffer = vulkanDevice.newBuffer(BufferUsage.TEXEL, bufferSize, false);
        createBufferView();
    }

    private void createBufferView() {
        if (bufferView != 0) {
            vkDestroyBufferView(Echo.vkContext.getDevice(), bufferView, null);
        }

        try (var stack = MemoryStack.stackPush()) {
            var viewInfo = VkBufferViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_VIEW_CREATE_INFO)
                    .buffer(buffer.id)
                    .format(format)
                    .offset(0)
                    .range(VK_WHOLE_SIZE);

            var pView = stack.mallocLong(1);
            VkUtils.check(vkCreateBufferView(Echo.vkContext.getDevice(), viewInfo, null, pView),
                    "Failed to create buffer view");
            bufferView = pView.get(0);
        }
    }

    public void cleanup() {
        if (bufferView != 0) {
            vkDestroyBufferView(Echo.vkContext.getDevice(), bufferView, null);
            bufferView = 0;
        }
    }

    @Override
    public int binding() {
        return binding;
    }

    @Override
    public int getType() {
        return VK_DESCRIPTOR_TYPE_UNIFORM_TEXEL_BUFFER;
    }

    @Override
    public int stages() {
        return stages;
    }

    @Override
    public void write(VkWriteDescriptorSet write, MemoryStack stack) {
        if (buffer == null)
            throw new IllegalStateException("Buffer not set for texel buffer binding " + binding);

        LongBuffer pView = stack.mallocLong(1);
        pView.put(0, bufferView);
        write.pTexelBufferView(pView);
    }
}
