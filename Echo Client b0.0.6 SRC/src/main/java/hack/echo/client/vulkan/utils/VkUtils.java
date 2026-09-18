package hack.echo.client.vulkan.utils;


import hack.echo.client.vulkan.api.VulkanBuffer;
import hack.echo.client.vulkan.api.VulkanDevice;
import hack.echo.client.vulkan.memory.MemUtil;
import lombok.experimental.UtilityClass;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkBufferMemoryBarrier;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkMemoryBarrier;

import java.nio.ByteBuffer;

import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.VK10.*;

@UtilityClass
public final class VkUtils {

    public void check(int result, String message) {
        if (result != 0) {
            throw new RuntimeException(String.format("%s: %s", message, VkResult.decode(result)));
        }
    }

    public enum ShaderKind {
        VERTEX(shaderc_glsl_vertex_shader),
        FRAGMENT(shaderc_glsl_fragment_shader),
        COMPUTE(shaderc_glsl_compute_shader);

        final int shadercKind;
        ShaderKind(int kind) { this.shadercKind = kind; }
    }

    public static VulkanBuffer growBuffer(VulkanDevice device, VulkanBuffer src, long size) {
        var dst = device.newBuffer(src.usage, size, src.perFrame);
        MemUtil.memcpy(src.contents(), dst.contents(), src.size);
        device.releaseResource(src);
        return dst;
    }

    public static VkBufferMemoryBarrier.Buffer bufferBarrier(MemoryStack stack, long buffer, int srcAccess, int dstAccess) {
        return VkBufferMemoryBarrier.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                .srcAccessMask(srcAccess)
                .dstAccessMask(dstAccess)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(buffer)
                .offset(0)
                .size(VK_WHOLE_SIZE);
    }


    public static void fillImageBarrier(VkImageMemoryBarrier b, long image, int oldLayout, int newLayout, int srcAccess, int dstAccess, int baseMip, int levelCount, int aspectMask) {
        b.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .oldLayout(oldLayout)
                .newLayout(newLayout)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .image(image)
                .srcAccessMask(srcAccess)
                .dstAccessMask(dstAccess);
        b.subresourceRange()
                .aspectMask(aspectMask)
                .baseMipLevel(baseMip).levelCount(levelCount)
                .baseArrayLayer(0).layerCount(1);
    }

    public static ByteBuffer compile(String name, String source, ShaderKind kind) {
        if (source == null) throw new RuntimeException("Shader source not found: " + name);
        long compiler = shaderc_compiler_initialize();
        long options  = shaderc_compile_options_initialize();
        shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_performance);

        long result = shaderc_compile_into_spv(compiler, source, kind.shadercKind, name, "main", options);

        shaderc_compile_options_release(options);
        shaderc_compiler_release(compiler);

        if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
            String error = shaderc_result_get_error_message(result);
            shaderc_result_release(result);
            throw new RuntimeException("Shader compilation failed for : " + name + "\n" + error);
        }

        ByteBuffer spirv = shaderc_result_get_bytes(result);
        ByteBuffer copy = MemoryUtil.memAlloc(spirv.remaining());
        copy.put(spirv).flip();
        shaderc_result_release(result);
        return copy;
    }

}
