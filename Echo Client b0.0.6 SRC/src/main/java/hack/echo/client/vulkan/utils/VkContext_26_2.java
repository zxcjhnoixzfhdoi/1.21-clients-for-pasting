package hack.echo.client.vulkan.utils;

//? if >26.1.2 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vulkan.VulkanCommandEncoder;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import hack.echo.client.mixin.accessors.GpuDeviceAccessor;
import hack.echo.client.mixin.accessors.VulkanCommandEncoderAccessor;
import hack.echo.client.vulkan.memory.TransferQueue;
import org.joml.Vector4i;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_GREATER_OR_EQUAL;

public class VkContext_26_2 implements VkContext {

    private final VkDevice vkDevice;
    private final VulkanCommandEncoder commandEncoder;
    private final TransferQueue transferQueue;
    private final long vma;
    public VkContext_26_2() {
        var vulkanDevice = (VulkanDevice) ((GpuDeviceAccessor) RenderSystem.getDevice()).getBackend();

        this.vma = vulkanDevice.vma();
        this.commandEncoder = vulkanDevice.createCommandEncoder();
        this.vkDevice = vulkanDevice.vkDevice();
        this.transferQueue = new TransferQueue(vkDevice);
    }

    @Override
    public TransferQueue getTransferQueue() {
        return transferQueue;
    }

    @Override
    public VkDevice getDevice() {
        return vkDevice;
    }

    @Override
    public VkCommandBuffer getCommandBuffer() {
        return ((VulkanCommandEncoderAccessor) commandEncoder).getCurrentCommandBuffer();
    }

    @Override
    public Vector4i getScissorRect(float x, float y, float w, float h) {
        var window = mc.getWindow();
        float scale = 2f;
        int windowHeight = window.getHeight();

        return new Vector4i(
                (int) (x * scale),
                windowHeight - (int)((y + h) * scale),
                (int) (w * scale),
                (int) (h *  scale)
        );
    }

    @Override
    public long getVma() {
        return this.vma;
    }

    @Override
    public int getCurrentFrame() {
        long index = ((VulkanCommandEncoderAccessor) commandEncoder).getCurrentSubmitIndex();
        return (int) (index % getFramesNum());
    }

    @Override
    public int getFramesNum() {
        return VulkanCommandEncoder.MAX_SUBMITS_IN_FLIGHT;
    }

    @Override
    public boolean isDynamicRendering() {
        return true;
    }

    @Override
    public float depthClearValue() {
        return 0f;
    }

    @Override
    public int depthCompareOp() {
        return VK_COMPARE_OP_GREATER_OR_EQUAL;
    }
}
*///?} else {
public final class VkContext_26_2 {

}
//?}