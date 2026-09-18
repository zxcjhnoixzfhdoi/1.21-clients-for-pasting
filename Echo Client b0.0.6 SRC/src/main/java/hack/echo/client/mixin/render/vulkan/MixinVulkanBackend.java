package hack.echo.client.mixin.render.vulkan;

//? if > 26.1.2 {
/*import com.mojang.blaze3d.vulkan.VulkanBackend;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static org.lwjgl.vulkan.VK10.vkCreateDevice;

@Mixin(value = VulkanBackend.class, remap = false)
public abstract class MixinVulkanBackend {

    @Redirect(
            method = "createDevice(Ljava/util/Collection;Lcom/mojang/blaze3d/vulkan/VulkanPhysicalDevice;)Lorg/lwjgl/vulkan/VkDevice;",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/vulkan/VK12;vkCreateDevice(Lorg/lwjgl/vulkan/VkPhysicalDevice;Lorg/lwjgl/vulkan/VkDeviceCreateInfo;Lorg/lwjgl/vulkan/VkAllocationCallbacks;Lorg/lwjgl/PointerBuffer;)I",
                    remap = false
            )
    )
    private int injectFeatures(VkPhysicalDevice vkPhysicalDevice, VkDeviceCreateInfo vkDeviceCreateInfo, VkAllocationCallbacks vkAllocationCallbacks, PointerBuffer pointerBuffer) {
        try (var stack = MemoryStack.stackPush()) {
            vkDeviceCreateInfo.pEnabledFeatures().multiDrawIndirect(true);

            var descriptorFeatures = VkPhysicalDeviceDescriptorIndexingFeatures.calloc(stack)
                    .sType$Default()
                    .descriptorBindingPartiallyBound(true)
                    .shaderSampledImageArrayNonUniformIndexing(true)
                    .pNext(vkDeviceCreateInfo.pNext());
            vkDeviceCreateInfo.pNext(descriptorFeatures.address());

            return vkCreateDevice(vkPhysicalDevice, vkDeviceCreateInfo, vkAllocationCallbacks, pointerBuffer);
        }
    }
}
*///?} else {
public abstract class MixinVulkanBackend {}
//?}