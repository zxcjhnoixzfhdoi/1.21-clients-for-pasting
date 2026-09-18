package hack.echo.client.vulkan.descriptor.manager;

import hack.echo.client.vulkan.descriptor.Descriptor;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRPushDescriptor.vkCmdPushDescriptorSetKHR;
import static org.lwjgl.vulkan.VK10.*;

public record DescriptorManager(long pipelineLayout, List<Descriptor> descriptors) {

    public void bind(VkCommandBuffer commandBuffer, int bindPoint) {
        if (descriptors.isEmpty()) return;

        try (MemoryStack stack = stackPush()) {
            var writes = VkWriteDescriptorSet.calloc(descriptors.size(), stack);
            int writeIndex = 0;

            for (var desc : descriptors) {
                int writeCount = desc.writeDescriptorCount();
                if (writeCount <= 0) {
                    continue;
                }

                var write = writes.get(writeIndex++)
                        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstBinding(desc.binding())
                        .dstArrayElement(0)
                        .descriptorType(desc.getType())
                        .descriptorCount(writeCount);
                desc.write(write, stack);
            }

            if (writeIndex == 0) {
                return;
            }

            writes.limit(writeIndex);
            vkCmdPushDescriptorSetKHR(commandBuffer, bindPoint, pipelineLayout, 0, writes);
        }
    }
}
