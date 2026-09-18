package hack.echo.client.render3.impl.vk.api;

import hack.echo.client.Echo;
import hack.echo.client.vulkan.api.BufferUsage;
import hack.echo.client.vulkan.api.VulkanBuffer;
import hack.echo.client.vulkan.descriptor.SimpleUBO;
import hack.echo.client.render3.api.Draw3D;
import hack.echo.client.render3.api.FramebufferTarget;
import hack.echo.client.vulkan.memory.MemUtil;
import net.minecraft.client.Camera;
import org.joml.Matrix4f;

import static hack.echo.client.Echo.vulkanDevice;
import static hack.echo.client.utils.Imports.mc;
import static org.lwjgl.vulkan.VK10.*;

public class VkDraw3D extends Draw3D {

    private final VkMinecraftTarget minecraftTarget;
    private final VkCustomTarget customTarget;

    private final VulkanBuffer uniformBuffer;
    private final SimpleUBO simpleUBO;

    public VkDraw3D() {
        super();

        int framesNum = Echo.vkContext.getFramesNum();

        uniformBuffer = vulkanDevice.newBuffer(BufferUsage.UNIFORM, 140);
        simpleUBO = new SimpleUBO(0, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_COMPUTE_BIT);
        simpleUBO.buffer = uniformBuffer;

        minecraftTarget = new VkMinecraftTarget(simpleUBO);
        customTarget = new VkCustomTarget(simpleUBO);
    }

    @Override
    public void updateCamera(Matrix4f view, Matrix4f proj) {
        Camera camera = mc.gameRenderer.getMainCamera();
        this.origin = camera.position();
        this.view = view;
        this.proj = proj;
        this.mvp = new Matrix4f(proj).mul(view);
        long p = uniformBuffer.contents();

        MemUtil.putMat4(p, view);
        MemUtil.putMat4(p + 64, proj);
        MemUtil.putVec3(p + 128, (float) origin.x, (float) origin.y, (float) origin.z);
    }
    @Override
    public FramebufferTarget getMinecraftTarget() {
        return minecraftTarget;
    }

    @Override
    public FramebufferTarget getCustomTarget() {
        return customTarget;
    }

    @Override
    public void beginFrame() {
        customTarget.beginFrame();
        minecraftTarget.beginFrame();
    }

    @Override
    public void endFrame() {
        minecraftTarget.endFrame();
        customTarget.endFrame();
    }

    @Override
    public void cleanup() {
        minecraftTarget.cleanup();
        customTarget.cleanup();
    }

}
