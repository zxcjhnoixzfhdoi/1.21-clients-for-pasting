package hack.echo.client.vulkan;

import hack.echo.client.vulkan.api.BufferUsage;
import hack.echo.client.vulkan.api.VulkanBuffer;
import hack.echo.client.vulkan.compute.VkComputePipeline;
import hack.echo.client.vulkan.descriptor.Descriptor;
import hack.echo.client.vulkan.graphics.VkGraphicsPipeline;
import hack.echo.client.vulkan.memory.VkImage;
import hack.echo.client.vulkan.utils.VkUtils;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class CommandBuffer {
    private final Long2IntMap imageLayouts = new Long2IntOpenHashMap();
    private final Long2IntMap imageAccess = new Long2IntOpenHashMap();
    private final Long2IntMap bufferAccess = new Long2IntOpenHashMap();

    private final VkCommandBuffer cmd;
    public CommandBuffer(VkCommandBuffer cmd) {
        this.cmd = cmd;
    }


    public void dispatch(VkComputePipeline pipeline, int x, int y, int z) {
        syncDescriptors(pipeline.getDescriptors());
        pipeline.recordDispatch(cmd, x, y, z);
        markDescriptorAccess(pipeline.getDescriptors());
    }

    public void draw(VkGraphicsPipeline pipeline, VulkanBuffer vbo, int vertexCount, int instanceCount, int firstInstance) {
        syncDescriptors(pipeline.getDescriptors());
        pipeline.bindPipeline(cmd);
        pipeline.bindDescriptors(cmd);
        pipeline.bindPushConstants(cmd);
        pipeline.bindVertexBuffer(cmd, 0, vbo.id, vbo.offset());
        vkCmdDraw(cmd, vertexCount, instanceCount, 0, firstInstance);
        markDescriptorAccess(pipeline.getDescriptors());
    }

    public void drawIndirect(VkGraphicsPipeline pipeline, VulkanBuffer vbo, VulkanBuffer indirect, int count, int stride) {
        syncDescriptors(pipeline.getDescriptors());
        syncVertexBuffer(vbo);
        syncIndirectBuffer(indirect);

        pipeline.bindPipeline(cmd);
        pipeline.bindDescriptors(cmd);
        pipeline.bindPushConstants(cmd);
        pipeline.bindVertexBuffer(cmd, vbo.id);
        vkCmdDrawIndirect(cmd, indirect.id, 0, count, stride);

        markDescriptorAccess(pipeline.getDescriptors());
        bufferAccess.put(vbo.id, VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT);
        bufferAccess.put(indirect.id, VK_ACCESS_INDIRECT_COMMAND_READ_BIT);
    }

    public void transitionImage(VkImage image, int newLayout, int dstAccess, int dstStage) {
        int curLayout  = imageLayouts.getOrDefault(image.getId(), image.getCurrentLayout());
        int lastAccess = imageAccess.getOrDefault(image.getId(), srcAccessFor(curLayout));
        int srcStage = lastAccess != 0 ? accessToPipelineStage(lastAccess) : srcStageFor(curLayout);
        insertImageBarrier(image, curLayout, newLayout, lastAccess, dstAccess, srcStage, dstStage);
        imageLayouts.put(image.getId(), newLayout);
        imageAccess.put(image.getId(), dstAccess);
        image.setCurrentLayout(newLayout);
    }

    public void scissor(int x, int y, int w, int h) {
        try (var stack = MemoryStack.stackPush()) {
            var rect = VkRect2D.calloc(1, stack);
            rect.offset().set(x, y);
            rect.extent().set(w, h);

            vkCmdSetScissor(cmd, 0, rect);
        }
    }

    public void viewport(int x, int y, int w, int h) {
        try (var stack = MemoryStack.stackPush()) {
            var viewport = VkViewport.calloc(1, stack)
                    .x(x).y(y).width(w).height(h)
                    .minDepth(0f).maxDepth(1f);
            vkCmdSetViewport(cmd, 0, viewport);
        }
    }

    private void syncDescriptors(List<Descriptor> descriptors) {
        for (Descriptor desc : descriptors) {
            switch (desc.getType()) {
                case VK_DESCRIPTOR_TYPE_STORAGE_IMAGE -> {
                    VkImage img = desc.getImage();
                    if (img == null) continue;
                    syncImage(img,
                        VK_IMAGE_LAYOUT_GENERAL,
                        VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_SHADER_WRITE_BIT,
                        shaderStageToPipelineStage(desc.stages()));
                }

                case VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER -> {
                    int dstStage = shaderStageToPipelineStage(desc.stages());
                    for (VkImage img : desc.getImages()) {
                        int reqLayout = (img.usage & VK_IMAGE_USAGE_STORAGE_BIT) != 0
                            ? VK_IMAGE_LAYOUT_GENERAL
                            : VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
                        syncImage(img, reqLayout, VK_ACCESS_SHADER_READ_BIT, dstStage);
                    }
                }

                case VK_DESCRIPTOR_TYPE_STORAGE_BUFFER -> {
                    VulkanBuffer buf = desc.getBuffer();
                    if (buf == null || (buf.usage != BufferUsage.STORAGE)) continue;
                    int lastAccess = bufferAccess.getOrDefault(buf.id, 0);
                    if (!isWriteAccess(lastAccess)) continue;
                    int dstAccess = VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_SHADER_WRITE_BIT;
                    int dstStage  = shaderStageToPipelineStage(desc.stages());
                    try (var stack = stackPush()) {
                        vkCmdPipelineBarrier(cmd,
                            accessToPipelineStage(lastAccess), dstStage, 0,
                            null,
                            VkUtils.bufferBarrier(stack, buf.id, lastAccess, dstAccess),
                            null);
                    }
                    bufferAccess.put(buf.id, dstAccess);
                }
            }
        }
    }

    private void syncImage(VkImage img, int reqLayout, int reqAccess, int dstStage) {
        int curLayout  = imageLayouts.getOrDefault(img.getId(), img.getCurrentLayout());
        int lastAccess = imageAccess.getOrDefault(img.getId(), srcAccessFor(curLayout));

        if (curLayout != reqLayout) {
            int srcStage = lastAccess != 0 ? accessToPipelineStage(lastAccess) : srcStageFor(curLayout);
            insertImageBarrier(img, curLayout, reqLayout, lastAccess, reqAccess, srcStage, dstStage);
            imageLayouts.put(img.getId(), reqLayout);
            img.setCurrentLayout(reqLayout);
        } else if (isWriteAccess(lastAccess)) {
            insertImageBarrier(img, curLayout, curLayout,
                lastAccess, reqAccess,
                accessToPipelineStage(lastAccess), dstStage);
        }

        imageAccess.put(img.getId(), reqAccess);
    }

    private void syncVertexBuffer(VulkanBuffer vbo) {
        int lastAccess = bufferAccess.getOrDefault(vbo.id, 0);
        if (!isWriteAccess(lastAccess)) return;
        try (var stack = stackPush()) {
            vkCmdPipelineBarrier(cmd,
                accessToPipelineStage(lastAccess), VK_PIPELINE_STAGE_VERTEX_INPUT_BIT, 0,
                null,
                VkUtils.bufferBarrier(stack, vbo.id, lastAccess, VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT),
                null);
        }
    }

    private void syncIndirectBuffer(VulkanBuffer indirect) {
        int lastAccess = bufferAccess.getOrDefault(indirect.id, 0);
        if (!isWriteAccess(lastAccess)) return;
        try (var stack = stackPush()) {
            vkCmdPipelineBarrier(cmd,
                accessToPipelineStage(lastAccess), VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT, 0,
                null,
                VkUtils.bufferBarrier(stack, indirect.id, lastAccess, VK_ACCESS_INDIRECT_COMMAND_READ_BIT),
                null);
        }
    }

    private void markDescriptorAccess(List<Descriptor> descriptors) {
        for (Descriptor desc : descriptors) {
            switch (desc.getType()) {
                case VK_DESCRIPTOR_TYPE_STORAGE_BUFFER -> {
                    VulkanBuffer buf = desc.getBuffer();
                    if (buf != null && (buf.usage == BufferUsage.STORAGE))
                        bufferAccess.put(buf.id, VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_SHADER_WRITE_BIT);
                }
                case VK_DESCRIPTOR_TYPE_STORAGE_IMAGE -> {
                    VkImage img = desc.getImage();
                    if (img != null) imageAccess.put(img.getId(), VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_SHADER_WRITE_BIT);
                }
                case VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER -> {
                    for (VkImage img : desc.getImages()) {
                        imageAccess.put(img.getId(), VK_ACCESS_SHADER_READ_BIT);
                    }
                }
            }
        }
    }

    private void insertImageBarrier(VkImage img, int oldLayout, int newLayout,
                                    int srcAccess, int dstAccess,
                                    int srcStage,  int dstStage) {
        try (var stack = stackPush()) {
            var barrier = VkImageMemoryBarrier.calloc(1, stack);
            VkUtils.fillImageBarrier(barrier.get(0), img.getId(),
                oldLayout, newLayout, srcAccess, dstAccess,
                0, VK_REMAINING_MIP_LEVELS, img.getAspect());
            vkCmdPipelineBarrier(cmd, srcStage, dstStage, 0, null, null, barrier);
        }
    }


    private static int srcAccessFor(int layout) {
        return switch (layout) {
            case VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL             -> VK_ACCESS_TRANSFER_WRITE_BIT;
            case VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL             -> VK_ACCESS_TRANSFER_READ_BIT;
            case VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                 VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL  -> VK_ACCESS_SHADER_READ_BIT;
            case VK_IMAGE_LAYOUT_GENERAL                          -> VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_SHADER_WRITE_BIT;
            case VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL         -> VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
            case VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL -> VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
            default -> 0;
        };
    }

    private static int srcStageFor(int layout) {
        return switch (layout) {
            case VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                 VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL             -> VK_PIPELINE_STAGE_TRANSFER_BIT;
            case VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                 VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL  -> VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT | VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT;
            case VK_IMAGE_LAYOUT_GENERAL                          -> VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT | VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            case VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL         -> VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
            case VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL -> VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT;
            default -> VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
        };
    }

    private static int shaderStageToPipelineStage(int shaderStages) {
        int result = 0;
        if ((shaderStages & VK_SHADER_STAGE_VERTEX_BIT)   != 0) result |= VK_PIPELINE_STAGE_VERTEX_SHADER_BIT;
        if ((shaderStages & VK_SHADER_STAGE_FRAGMENT_BIT) != 0) result |= VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
        if ((shaderStages & VK_SHADER_STAGE_COMPUTE_BIT)  != 0) result |= VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT;
        return result != 0 ? result : VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;
    }

    private static int accessToPipelineStage(int access) {
        if ((access & (VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_SHADER_WRITE_BIT)) != 0)
            return VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT | VK_PIPELINE_STAGE_VERTEX_SHADER_BIT | VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
        if ((access & (VK_ACCESS_TRANSFER_WRITE_BIT | VK_ACCESS_TRANSFER_READ_BIT)) != 0)
            return VK_PIPELINE_STAGE_TRANSFER_BIT;
        if ((access & VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT) != 0)
            return VK_PIPELINE_STAGE_VERTEX_INPUT_BIT;
        if ((access & VK_ACCESS_INDIRECT_COMMAND_READ_BIT) != 0)
            return VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT;
        if ((access & (VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_COLOR_ATTACHMENT_READ_BIT)) != 0)
            return VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
        if ((access & (VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT)) != 0)
            return VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT;
        return VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
    }

    private static boolean isWriteAccess(int access) {
        return (access & (VK_ACCESS_SHADER_WRITE_BIT
                        | VK_ACCESS_TRANSFER_WRITE_BIT
                        | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
                        | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT)) != 0;
    }

}
