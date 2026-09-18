package hack.echo.client.mixin.accessors;

//? if > 26.1.2 {
/*import com.mojang.blaze3d.vulkan.VulkanRenderPass;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(VulkanRenderPass.class)
public interface VulkanRenderPassAccessor {
    @Invoker("secondaryCommandBuffer")
    VkCommandBuffer getSecondaryCommandBuffer();
}
*///?} else {
public interface VulkanRenderPassAccessor{}
//?}