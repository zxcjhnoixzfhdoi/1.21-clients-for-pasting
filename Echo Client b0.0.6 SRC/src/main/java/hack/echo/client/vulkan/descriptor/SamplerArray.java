package hack.echo.client.vulkan.descriptor;

import hack.echo.client.vulkan.memory.VkImage;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.Arrays;

import static org.lwjgl.vulkan.VK12.VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;

public class SamplerArray implements Descriptor {
    private final int binding;
    private final int stages;
    private final int maxCount;
    private final VkImage[] images;
    private int writeCount;

    public SamplerArray(int binding, int stages, int maxCount) {
        this.binding = binding;
        this.stages = stages;
        this.maxCount = maxCount;
        this.images = new VkImage[maxCount];
        for (int i = 0; i < maxCount; i++) {
            images[i] = VkImage.defaultTexture;
        }
    }

    public void setImage(int i, VkImage img) {
        if (i < 0 || i >= maxCount) {
            throw new IllegalArgumentException("Sampler array index out of range: " + i + " / " + maxCount);
        }

        images[i] = img;
        writeCount = Math.max(writeCount, i + 1);
    }

    public void clear() {
        Arrays.fill(images, VkImage.defaultTexture);
        writeCount = 0;
    }

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
    public int descriptorCount() {
        return maxCount;
    }

    @Override
    public int writeDescriptorCount() {
        return writeCount;
    }

    @Override
    public void write(VkWriteDescriptorSet write, MemoryStack stack) {
        var imageInfos = VkDescriptorImageInfo.calloc(writeCount, stack);
        for (int i = 0; i < writeCount; i++) {
            VkImage img = images[i];
            if (img == null) {
                throw new IllegalStateException("Sampler array slot " + i + " is missing for binding " + binding);
            }

            var info = imageInfos.get(i)
                            .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            info.imageView(img.getImageView()).sampler(img.getSampler());
        }
        write.pImageInfo(imageInfos);
    }
}
