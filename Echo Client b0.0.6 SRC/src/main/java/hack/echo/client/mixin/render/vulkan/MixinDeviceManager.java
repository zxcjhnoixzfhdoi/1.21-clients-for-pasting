package hack.echo.client.mixin.render.vulkan;

import net.vulkanmod.vulkan.device.DeviceManager;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static org.lwjgl.vulkan.VK10.vkCreateDevice;

@Mixin(value = DeviceManager.class, remap = false)
public class MixinDeviceManager {

    @Redirect(
        method = "createLogicalDevice",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VK10;vkCreateDevice(Lorg/lwjgl/vulkan/VkPhysicalDevice;Lorg/lwjgl/vulkan/VkDeviceCreateInfo;Lorg/lwjgl/vulkan/VkAllocationCallbacks;Lorg/lwjgl/PointerBuffer;)I",
            remap = false
        )
    )
    private static int injectVulkan12Features(VkPhysicalDevice physicalDevice, VkDeviceCreateInfo createInfo,
                                               VkAllocationCallbacks allocator, PointerBuffer pDevice) {
        MemoryStack stack = MemoryStack.stackGet();

        VkPhysicalDeviceVulkan12Features vk12Features = VkPhysicalDeviceVulkan12Features.calloc(stack);
        vk12Features.sType$Default();
        vk12Features.descriptorBindingPartiallyBound(true);
        vk12Features.shaderSampledImageArrayNonUniformIndexing(true);
        vk12Features.pNext(createInfo.pNext());
        createInfo.pNext(vk12Features.address());

        PointerBuffer existing = createInfo.ppEnabledExtensionNames();
        int existingCount = existing != null ? existing.remaining() : 0;
        PointerBuffer extended = stack.mallocPointer(existingCount + 1);
        for (int i = 0; i < existingCount; i++) {
            extended.put(existing.get(existing.position() + i));
        }
        extended.put(stack.UTF8("VK_KHR_push_descriptor"));
        extended.flip();
        createInfo.ppEnabledExtensionNames(extended);

        return vkCreateDevice(physicalDevice, createInfo, allocator, pDevice);
    }
}
