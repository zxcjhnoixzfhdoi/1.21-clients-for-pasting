package hack.echo.client.render3.impl.vk.post;

import hack.echo.client.render2.api.CrossTexture;
import hack.echo.client.vulkan.CommandBuffer;
import hack.echo.client.vulkan.compute.VkComputePipeline;
import hack.echo.client.vulkan.descriptor.SimpleSampler;
import hack.echo.client.vulkan.descriptor.PushConstants;
import hack.echo.client.vulkan.descriptor.SimpleStorageImage;
import hack.echo.client.vulkan.memory.MemUtil;
import hack.echo.client.vulkan.memory.VkImage;

import static org.lwjgl.vulkan.VK10.*;

public class VkBlur {

    private static final int MAX = 8;

    private final SimpleSampler sampler;
    private final SimpleStorageImage storage;
    private final PushConstants downPc;
    private final PushConstants upPc;
    private final VkComputePipeline downPipeline;
    private final VkComputePipeline upPipeline;

    public VkBlur() {
        this.sampler = new SimpleSampler(0, VK_SHADER_STAGE_COMPUTE_BIT);
        this.storage = new SimpleStorageImage(1, VK_SHADER_STAGE_COMPUTE_BIT);

        this.downPc = new PushConstants(8, VK_SHADER_STAGE_COMPUTE_BIT);
        this.upPc = new PushConstants(4, VK_SHADER_STAGE_COMPUTE_BIT);

        this.downPipeline = VkComputePipeline.builder("compute/kawase_down.comp")
                .descriptor(sampler)
                .descriptor(storage)
                .pushConstants(downPc)
                .build();

        this.upPipeline = VkComputePipeline.builder("compute/kawase_up.comp")
                .descriptor(sampler)
                .descriptor(storage)
                .pushConstants(upPc)
                .build();
    }

    private VkImage workImage;
    private final int[] widths  = new int[MAX];
    private final int[] heights = new int[MAX];
    private int lastWidth = -1, lastHeight = -1, lastLevels = -1;


    private void resize(int w, int h, int lvl) {
        if (lastWidth == w && lastHeight == h && lastLevels == lvl) return;
        lastWidth = w;
        lastHeight = h;
        lastLevels = lvl;

        if (workImage != null) {
            workImage.free();
            workImage = null;
        }

        for (int i = 0; i < MAX; i++) {
            widths[i]  = Math.max(1, w >> (i + 1));
            heights[i] = Math.max(1, h >> (i + 1));
        }

        workImage = VkImage.builder(widths[0], heights[0])
                .addUsage(VK_IMAGE_USAGE_STORAGE_BIT | VK_IMAGE_USAGE_SAMPLED_BIT)
                .setMipLevels(lvl)
                .setLinearFiltering(true)
                .setClamp(true)
                .createVulkanImage();

    }

    public VkImage executeCompute(CommandBuffer metal, CrossTexture input, int levels, float offsetMul) {
        var inputImage = input.vkImage;
        levels = Math.min(levels, MAX);
        resize(inputImage.width, inputImage.height, levels);

        long upPtr = upPc.pointer;
        long downPtr = downPc.pointer;
        for (int i = 0; i < levels; i++) {
            if (i == 0) {
                sampler.setImage(inputImage);
            } else {
                sampler.setMipView(workImage, i - 1);
            }
            storage.setMipView(workImage, i);
            MemUtil.putFloat(downPtr, (i + 0.5f) * offsetMul);
            //? if > 26.1.2
            //MemUtil.putInt(downPtr + 4, i == 0 ? 1 : 0);
            metal.dispatch(downPipeline, (widths[i] + 15) / 16, (heights[i] + 15) / 16, 1);
        }

        for (int i = levels - 2; i >= 0; i--) {
            sampler.setMipView(workImage, i + 1);
            storage.setMipView(workImage, i);
            MemUtil.putFloat(upPtr, (i + 0.5f) * offsetMul);
            metal.dispatch(upPipeline, (widths[i] + 15) / 16, (heights[i] + 15) / 16, 1);
        }

        metal.transitionImage(workImage, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                VK_ACCESS_SHADER_READ_BIT,
                VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT | VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT);
        metal.transitionImage(inputImage, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                VK_ACCESS_SHADER_READ_BIT,
                VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT | VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT);

        return this.workImage;
    }

    public void cleanup() {
        downPipeline.cleanUp();
        upPipeline.cleanUp();
        downPc.cleanup();
        upPc.cleanup();
        workImage.free();
    }
}
