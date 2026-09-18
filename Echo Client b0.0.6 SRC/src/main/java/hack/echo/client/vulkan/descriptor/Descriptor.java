package hack.echo.client.vulkan.descriptor;

import hack.echo.client.vulkan.api.VulkanBuffer;
import hack.echo.client.vulkan.memory.VkImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.List;

public interface Descriptor {
    int binding();
    int getType();
    int stages();
    void write(VkWriteDescriptorSet write, MemoryStack stack);

    default int descriptorCount() {
        return 1;
    }
    default int writeDescriptorCount() {
        return descriptorCount();
    }
    default VkImage getImage() {
        return null;
    }
    default List<VkImage> getImages() {
        VkImage img = getImage();
        return img != null ? List.of(img) : List.of();
    }
    default VulkanBuffer getBuffer() {
        return null;
    }
}
