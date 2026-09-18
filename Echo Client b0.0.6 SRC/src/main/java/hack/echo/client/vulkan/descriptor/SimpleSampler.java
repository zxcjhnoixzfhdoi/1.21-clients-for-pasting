package hack.echo.client.vulkan.descriptor;

import hack.echo.client.vulkan.memory.VkImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;

public class SimpleSampler implements Descriptor {
    private final int binding;
    private final int stages;
    private long imageView;
    private long sampler;
    private VkImage image;

    public SimpleSampler(int binding, int stages) {
        this.binding = binding;
        this.stages = stages;
    }

    public void setImage(VkImage img) {
        this.image = img;
        this.imageView = img.getImageView();
        this.sampler = img.getSampler();
    }

    public void setMipView(VkImage img, int mip) {
        this.image = img;
        this.imageView = img.getMipView(mip);
        this.sampler = img.getSampler();
    }

    @Override
    public VkImage getImage() { return image; }

    @Override
    public int binding() {
        return binding;
    }

    @Override
    public int getType() {
        return VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    }

    @Override
    public int stages() {
        return stages;
    }

    @Override
    public void write(VkWriteDescriptorSet write, MemoryStack stack) {
        if (imageView == 0)
            throw new IllegalStateException("Image not set for sampler binding " + binding);

        var imageInfo = VkDescriptorImageInfo.calloc(1, stack)
                .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                .imageView(imageView)
                .sampler(sampler);

        write.pImageInfo(imageInfo);
    }
}
