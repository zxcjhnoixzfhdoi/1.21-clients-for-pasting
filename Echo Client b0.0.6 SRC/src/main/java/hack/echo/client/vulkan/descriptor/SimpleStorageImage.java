package hack.echo.client.vulkan.descriptor;

import hack.echo.client.vulkan.memory.VkImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import static org.lwjgl.vulkan.VK10.*;

public class SimpleStorageImage implements Descriptor {
    private final int binding;
    private final int stages;
    private long view = VK_NULL_HANDLE;
    private VkImage image;

    public SimpleStorageImage(int binding, int stages) {
        this.binding = binding;
        this.stages = stages;
    }

    public void setMipView(VkImage image, int mip) {
        this.image = image;
        this.view = image.getMipView(mip);
    }

    public void setImage(VkImage img) {
        this.image = img;
        this.view = img.getImageView();
    }

    @Override
    public VkImage getImage() { return image; }

    @Override
    public int binding() {
        return binding;
    }

    @Override
    public int getType() {
        return VK_DESCRIPTOR_TYPE_STORAGE_IMAGE;
    }

    @Override
    public int stages() {
        return stages;
    }

    @Override
    public void write(VkWriteDescriptorSet write, MemoryStack stack) {
        if (view == VK_NULL_HANDLE)
            throw new IllegalStateException("Image not set for storage image binding " + binding);

        var imageInfo = VkDescriptorImageInfo.calloc(1, stack)
                .imageLayout(VK_IMAGE_LAYOUT_GENERAL)
                .imageView(view);

        write.pImageInfo(imageInfo);
    }
}
