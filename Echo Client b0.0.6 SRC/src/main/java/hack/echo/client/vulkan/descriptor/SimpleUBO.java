package hack.echo.client.vulkan.descriptor;

import hack.echo.client.vulkan.api.VulkanBuffer;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import static org.lwjgl.vulkan.VK10.*;

public class SimpleUBO implements Descriptor {
    private final int binding;
    private final int stages;
    @Getter
    public VulkanBuffer buffer;

    public SimpleUBO(int binding, int stages) {
        this.binding = binding;
        this.stages = stages;
    }

    @Override
    public int binding() { return binding; }

    @Override
    public int getType() { return VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER; }

    @Override
    public int stages() { return stages; }

    @Override
    public void write(VkWriteDescriptorSet write, MemoryStack stack) {
        if (buffer == null)
            throw new IllegalStateException("Buffer not set for UBO binding " + binding);

        var bufferInfo = VkDescriptorBufferInfo.calloc(1, stack)
                .buffer(buffer.id)
                .offset(0)
                .range(VK_WHOLE_SIZE);

        write.pBufferInfo(bufferInfo);
    }
}
