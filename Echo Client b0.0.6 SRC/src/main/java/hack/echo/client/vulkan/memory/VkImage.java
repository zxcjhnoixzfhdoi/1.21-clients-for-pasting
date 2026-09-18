package hack.echo.client.vulkan.memory;

import com.mojang.blaze3d.opengl.GlTexture;
//? if >26.1.2 {
/*import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.vulkan.VulkanConst;
import com.mojang.blaze3d.vulkan.VulkanGpuSampler;
import com.mojang.blaze3d.vulkan.VulkanGpuTexture;
import com.mojang.blaze3d.vulkan.VulkanGpuTextureView;
*///?}
import hack.echo.client.Echo;
import hack.echo.client.utils.ResourceHelper;
import hack.echo.client.vulkan.utils.VkContext_26_2;
import hack.echo.client.vulkan.utils.VkUtils;
import lombok.Getter;
import lombok.Setter;
//? if <=26.1.2 {
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.vulkan.texture.VulkanImage;

//?}
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

@Getter
public class VkImage {
    public final int width, height, format, aspect, mipLevels, arrayLayers, formatSize, usage;

    private long id;
    private long allocation;
    private final long imageView;
    private final long[] mipViews;
    private final long sampler;
    @Setter
    private int currentLayout = VK_IMAGE_LAYOUT_UNDEFINED;

    public static VkImage defaultTexture = null;
    private VkImage(long id, long imageView, long sampler, int currentLayout, int width, int height, int format,
            int aspect, int arrayLayers, int mipLevels, int formatSize, int usage) {
        this.id = id;
        this.imageView = imageView;
        this.sampler = sampler;
        this.currentLayout = currentLayout;
        this.allocation = 0;
        this.width = width;
        this.height = height;
        this.format = format;
        this.aspect = aspect;
        this.arrayLayers = arrayLayers;
        this.mipLevels = mipLevels;
        this.formatSize = formatSize;
        this.usage = usage;
        this.mipViews = null;
    }

    public static void createDefaultTexture() {
        defaultTexture = VkImage.builder(1, 1)
                .setUsage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT)
                .setLinearFiltering(true)
                .setClamp(true)
                .createVulkanImage();

        var buffer = MemoryUtil.memAlloc(4);
        buffer.putInt(-1);
        buffer.flip();
        defaultTexture.uploadSubTextureAsync(0, 1, 1, 0, 0, 0, 0, 1, buffer);

        MemoryUtil.memFree(buffer);
    }

    public static VkImage of(long id, long imageView, long sampler, int currentLayout, int width, int height,
            int format, int aspect, int arrayLayers, int mipLevels, int formatSize, int usage) {
        return new VkImage(id, imageView, sampler, currentLayout, width, height, format, aspect, arrayLayers, mipLevels,
                formatSize, usage);
    }



    //? if <=26.1.2 {
    public static VkImage of(GlTexture glTexture) {
        VkGlTexture texture = VkGlTexture.getTexture(glTexture.glId());

        // You can remove if u no like
        if (texture == null || texture.getVulkanImage() == null) {
            return null;
        }

        return of(texture.getVulkanImage());
    }

    public static VkImage of(VulkanImage image) {
        return of(
                image.getId(), image.getImageView(), image.getSampler(), image.getCurrentLayout(), image.width, image.height, image.format, image.aspect, image.arrayLayers, image.mipLevels, image.formatSize, image.usage
        );
    }
    //?}

    //? if >26.1.2 {
    /*public static VkImage of(VulkanGpuTexture texture, VulkanGpuTextureView view, VulkanGpuSampler sampler) {
        GpuFormat gpuFormat = texture.getFormat();
        int vkFormat = VulkanConst.toVk(gpuFormat);
        int aspect = VulkanConst.formatAspectMask(gpuFormat);
        int mipLevels = view.mipLevels();
        int arrayLayers = texture.getDepthOrLayers();
        int width = view.getWidth(0);
        int height = view.getHeight(0);
        int formatSize = gpuFormat.pixelSize();
        int usage = VulkanConst.textureUsageToVk(texture.usage(), gpuFormat);
        return VkImage.of(texture.vkImage(), view.vkImageView(), sampler.vkSampler(),
                VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                width, height, vkFormat, aspect, arrayLayers, mipLevels, formatSize, usage);
    }
    *///?}


    public static VkImage of(String texturePath) {
        byte[] bytes = ResourceHelper.getBytes(texturePath);
        if (bytes == null) throw new IllegalStateException("Failed to load texture : " + texturePath);
        return of(bytes);
    }

    public static VkImage of(byte[] bytes) {
        var buffer = MemoryUtil.memAlloc(bytes.length);
        buffer.put(bytes).flip();

        try (var stack = MemoryStack.stackPush()) {
            var w = stack.mallocInt(1);
            var h = stack.mallocInt(1);
            var ch = stack.mallocInt(1);

            STBImage.stbi_set_flip_vertically_on_load(false);
            var pixels = STBImage.stbi_load_from_memory(buffer, w, h, ch, 4);
            MemoryUtil.memFree(buffer);

            if (pixels == null) {
                throw new IllegalStateException("Failed to decode image: " + STBImage.stbi_failure_reason());
            }

            VkImage image = VkImage.builder(w.get(0), h.get(0))
                    .setUsage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT)
                    .setLinearFiltering(true)
                    .setClamp(true)
                    .createVulkanImage();

            image.uploadSubTextureAsync(0, w.get(0), h.get(0), 0, 0, 0, 0, w.get(0), pixels);

            return image;
        }
    }


    private VkImage(Builder builder) {
        this.width = builder.width;
        this.height = builder.height;
        this.format = builder.format;
        this.mipLevels = builder.mipLevels;
        this.arrayLayers = builder.arrayLayers;
        this.formatSize = builder.formatSize;
        this.usage = builder.usage;
        this.aspect = getAspect(format);

        try (var stack = stackPush()) {
            var imageInfo = VkImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(VK_IMAGE_TYPE_2D)
                    .format(format)
                    .mipLevels(mipLevels)
                    .arrayLayers(arrayLayers)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .tiling(VK_IMAGE_TILING_OPTIMAL)
                    .usage(usage)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.extent().width(width).height(height).depth(1);

            var allocInfo = VmaAllocationCreateInfo.calloc(stack)
                    .usage(VMA_MEMORY_USAGE_AUTO);

            var pImage = stack.mallocLong(1);
            var pAlloc = stack.mallocPointer(1);
            VkUtils.check(vmaCreateImage(Echo.vkContext.getVma(), imageInfo, allocInfo, pImage, pAlloc, null),
                    "Failed to create image");
            this.id = pImage.get(0);
            this.allocation = pAlloc.get(0);

            this.imageView = createImageView(id, VK_IMAGE_VIEW_TYPE_2D, format, aspect, arrayLayers, 0, mipLevels,
                    stack);
            this.sampler = createSampler(builder.linearFiltering, builder.clamp, mipLevels, stack);

            if (mipLevels > 1) {
                mipViews = new long[mipLevels];
                for (int i = 0; i < mipLevels; i++) {
                    mipViews[i] = createImageView(id, VK_IMAGE_VIEW_TYPE_2D, format, aspect, arrayLayers, i, 1, stack);
                }
            } else {
                mipViews = null;
            }
        }
    }

    public static long createImageView(long image, int viewType, int format, int aspectFlags,
            int arrayLayers, int baseMip, int mipLevels, MemoryStack stack) {
        var viewInfo = VkImageViewCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .image(image)
                .viewType(viewType)
                .format(format);
        viewInfo.subresourceRange()
                .aspectMask(aspectFlags)
                .baseMipLevel(baseMip)
                .levelCount(mipLevels)
                .baseArrayLayer(0)
                .layerCount(arrayLayers);
        var pView = stack.mallocLong(1);
        VkUtils.check(vkCreateImageView(Echo.vkContext.getDevice(), viewInfo, null, pView), "Failed to create image view");
        return pView.get(0);
    }

    private static long createSampler(boolean linear, boolean clamp, int mipLevels, MemoryStack stack) {
        int filter = linear ? VK_FILTER_LINEAR : VK_FILTER_NEAREST;
        int mipmapMode = linear ? VK_SAMPLER_MIPMAP_MODE_LINEAR : VK_SAMPLER_MIPMAP_MODE_NEAREST;
        int addressMode = clamp ? VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE : VK_SAMPLER_ADDRESS_MODE_REPEAT;

        var samplerInfo = VkSamplerCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                .magFilter(filter)
                .minFilter(filter)
                .mipmapMode(mipmapMode)
                .addressModeU(addressMode)
                .addressModeV(addressMode)
                .addressModeW(addressMode)
                .maxLod(mipLevels - 1)
                .borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK);
        var pSampler = stack.mallocLong(1);
        VkUtils.check(vkCreateSampler(Echo.vkContext.getDevice(), samplerInfo, null, pSampler), "Failed to create sampler");
        return pSampler.get(0);
    }

    public void uploadSubTextureAsync(int mipLevel, int width, int height,
            int xOffset, int yOffset,
            int unpackSkipRows, int unpackSkipPixels, int unpackRowLength,
            ByteBuffer buffer) {
        try (var stack = stackPush()) {
            int uploadSize = (unpackRowLength * height - unpackSkipPixels) * formatSize;

            var stagingBufInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(uploadSize)
                    .usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            var stagingAllocInfo = VmaAllocationCreateInfo.calloc(stack)
                    .usage(VMA_MEMORY_USAGE_AUTO)
                    .flags(VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT | VMA_ALLOCATION_CREATE_MAPPED_BIT);

            var pStaging = stack.mallocLong(1);
            var pStagingAlloc = stack.mallocPointer(1);
            VkUtils.check(
                    vmaCreateBuffer(Echo.vkContext.getVma(), stagingBufInfo, stagingAllocInfo, pStaging, pStagingAlloc, null),
                    "Failed to create staging buffer for image upload");

            long stagingId = pStaging.get(0);
            long stagingAlloc = pStagingAlloc.get(0);

            var pData = stack.mallocPointer(1);
            vmaMapMemory(Echo.vkContext.getVma(), stagingAlloc, pData);
            long srcAddr = MemoryUtil.memAddress(buffer)
                    + ((long) unpackRowLength * unpackSkipRows + unpackSkipPixels) * formatSize;
            MemoryUtil.memCopy(srcAddr, pData.get(0), uploadSize);
            vmaUnmapMemory(Echo.vkContext.getVma(), stagingAlloc);

            var queue = Echo.vkContext.getTransferQueue();
            var cmd = queue.beginCommands();

            insertBarrier(stack, cmd, currentLayout, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            currentLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;

            var region = VkBufferImageCopy.calloc(1, stack)
                    .bufferOffset(0)
                    .bufferRowLength(unpackRowLength)
                    .bufferImageHeight(height);
            region.imageSubresource()
                    .aspectMask(aspect)
                    .mipLevel(mipLevel)
                    .baseArrayLayer(0)
                    .layerCount(1);
            region.imageOffset().x(xOffset).y(yOffset).z(0);
            region.imageExtent().width(width).height(height).depth(1);
            vkCmdCopyBufferToImage(cmd, stagingId, id, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);

            insertBarrier(stack, cmd, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            currentLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;

            queue.submitCommands(cmd);
            queue.waitIdle();

            vmaDestroyBuffer(Echo.vkContext.getVma(), stagingId, stagingAlloc);
        }
    }

    private void insertBarrier(MemoryStack stack, VkCommandBuffer cmd, int oldLayout, int newLayout) {
        int srcAccess, dstAccess, srcStage, dstStage;

        switch (oldLayout) {
            case VK_IMAGE_LAYOUT_UNDEFINED -> {
                srcAccess = 0;
                srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
            }
            case VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL -> {
                srcAccess = VK_ACCESS_TRANSFER_WRITE_BIT;
                srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            }
            case VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL -> {
                srcAccess = VK_ACCESS_SHADER_READ_BIT;
                srcStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            }
            case VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL -> {
                srcAccess = VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
                srcStage = VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT;
            }
            default -> throw new RuntimeException("Unsupported src layout: " + oldLayout);
        }

        switch (newLayout) {
            case VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL -> {
                dstAccess = VK_ACCESS_TRANSFER_WRITE_BIT;
                dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            }
            case VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL -> {
                dstAccess = VK_ACCESS_SHADER_READ_BIT;
                dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            }
            case VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL -> {
                dstAccess = VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
                dstStage = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
            }
            default -> throw new RuntimeException("Unsupported dst layout: " + newLayout);
        }

        var barrier = VkImageMemoryBarrier.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .oldLayout(oldLayout)
                .newLayout(newLayout)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .image(id)
                .srcAccessMask(srcAccess)
                .dstAccessMask(dstAccess);
        barrier.subresourceRange()
                .aspectMask(aspect)
                .baseMipLevel(0)
                .levelCount(VK_REMAINING_MIP_LEVELS)
                .baseArrayLayer(0)
                .layerCount(VK_REMAINING_ARRAY_LAYERS);

        vkCmdPipelineBarrier(cmd, srcStage, dstStage, 0, null, null, barrier);
    }

    public long getMipView(int mip) {
        if (mipViews == null) return imageView;
        return mipViews[Math.min(mip, mipLevels - 1)];
    }

    public void free() {
        if (allocation == 0)
            return;
        var device = Echo.vkContext.getDevice();
        if (mipViews != null) {
            for (long view : mipViews) {
                if (view != VK_NULL_HANDLE) vkDestroyImageView(device, view, null);
            }
        }
        vmaDestroyImage(Echo.vkContext.getVma(), id, allocation);
        vkDestroyImageView(device, imageView, null);
        vkDestroySampler(device, sampler, null);
        id = 0;
        allocation = 0;
    }

    public static int getAspect(int format) {
        return switch (format) {
            case 124, 125, 126 -> VK_IMAGE_ASPECT_DEPTH_BIT;
            case 129, 130 -> VK_IMAGE_ASPECT_DEPTH_BIT | VK_IMAGE_ASPECT_STENCIL_BIT;
            default -> VK_IMAGE_ASPECT_COLOR_BIT;
        };
    }

    public static Builder builder(int width, int height) {
        return new Builder(width, height);
    }

    public static class Builder {
        final int width, height;
        int format = VK_FORMAT_R8G8B8A8_UNORM;
        int formatSize;
        int mipLevels = 1;
        int arrayLayers = 1;
        int usage = VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT;
        boolean linearFiltering = false;
        boolean clamp = false;

        Builder(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public Builder setFormat(int format) {
            this.format = format;
            return this;
        }

        public Builder setMipLevels(int n) {
            this.mipLevels = n;
            return this;
        }

        public Builder setArrayLayers(int n) {
            this.arrayLayers = n;
            return this;
        }

        public Builder setUsage(int usage) {
            this.usage = usage;
            return this;
        }

        public Builder addUsage(int usage) {
            this.usage |= usage;
            return this;
        }

        public Builder setLinearFiltering(boolean b) {
            this.linearFiltering = b;
            return this;
        }

        public Builder setClamp(boolean b) {
            this.clamp = b;
            return this;
        }

        public VkImage createVulkanImage() {
            this.formatSize = formatSize(this.format);
            return new VkImage(this);
        }

        private static int formatSize(int format) {
            return switch (format) {
                case VK_FORMAT_R8_UNORM -> 1;
                case VK_FORMAT_D16_UNORM -> 2;
                case VK_FORMAT_R8G8B8A8_UNORM, VK_FORMAT_R8G8B8A8_UINT, VK_FORMAT_R8G8B8A8_SINT,
                        VK_FORMAT_R8G8B8A8_SRGB, VK_FORMAT_X8_D24_UNORM_PACK32,
                        VK_FORMAT_D32_SFLOAT, VK_FORMAT_D24_UNORM_S8_UINT,
                        VK_FORMAT_R32_SFLOAT -> 4;
                case VK_FORMAT_D16_UNORM_S8_UINT -> 3;
                case VK_FORMAT_D32_SFLOAT_S8_UINT -> 5;
                default -> throw new IllegalArgumentException("Unexpected format: " + format);
            };
        }
    }
}

