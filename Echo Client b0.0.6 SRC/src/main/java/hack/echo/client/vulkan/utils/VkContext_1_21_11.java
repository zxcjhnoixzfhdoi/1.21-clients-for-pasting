package hack.echo.client.vulkan.utils;


//? if <= 26.1.2 {
import hack.echo.client.vulkan.memory.TransferQueue;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import org.joml.Vector4i;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;

import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_LESS_OR_EQUAL;

public class VkContext_1_21_11 implements VkContext {

    private final VkDevice vkDevice;
    private final boolean isDynamicRendering;
    private final TransferQueue transferQueue;
    public VkContext_1_21_11() {
        this.vkDevice = DeviceManager.vkDevice;
        this.isDynamicRendering = Vulkan.DYNAMIC_RENDERING || Renderer.getInstance()
                .getBoundRenderPass().getId() == 0;
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
        return Renderer.getCommandBuffer();
    }

    @Override
    public Vector4i getScissorRect(float x, float y, float w, float h) {
        float scale = 2;
        return new Vector4i(
                (int) (x * scale),
                (int) (y * scale),
                (int) (w * scale),
                (int) (h * scale)
        );
    }

    @Override
    public long getVma() {
        return Vulkan.getAllocator();
    }

    @Override
    public int getCurrentFrame() {
        return Renderer.getCurrentFrame();
    }

    @Override
    public int getFramesNum() {
        return Renderer.getFramesNum();
    }

    @Override
    public boolean isDynamicRendering() {
        return isDynamicRendering;
    }

    @Override
    public float depthClearValue() {
        return 1f;
    }

    @Override
    public int depthCompareOp() {
        return VK_COMPARE_OP_LESS_OR_EQUAL;
    }
}
//?} else {
/*public class VkContext_1_21_11 {}
*///?}
