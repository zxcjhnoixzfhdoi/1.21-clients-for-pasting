package hack.echo.client.render3.impl.vk;

import hack.echo.client.Echo;
import hack.echo.client.vulkan.CommandBuffer;
import hack.echo.client.vulkan.descriptor.SimpleUBO;
import hack.echo.client.vulkan.graphics.VkGraphicsPipeline;
import hack.echo.client.render3.api.Draw3D;
import hack.echo.client.vulkan.graphics.VkShader;
import hack.echo.client.vulkan.memory.MemUtil;
import net.minecraft.world.phys.Vec3;

import static org.lwjgl.vulkan.VK10.*;

public class VkBox extends VkShader {

    private final SimpleUBO ubo;

    public VkBox(SimpleUBO ubo) {
        super(28, 36);
        this.ubo = ubo;
    }

    @Override
    protected VkGraphicsPipeline.Builder getPipelineBuilder() {
        return VkGraphicsPipeline.builder("3d/box.vert", "3d/box.frag")
                .stride(28)
                .attribute(0, VK_FORMAT_R32G32B32_SFLOAT, 0)
                .attribute(1, VK_FORMAT_R32G32B32_SFLOAT, 12)
                .attribute(2, VK_FORMAT_R32_SINT, 24)
                .descriptor(ubo)
                .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                .depthFunc(Echo.vkContext.depthCompareOp())
                .blend(false);
    }

    @Override
    public void flush(CommandBuffer commandBuffer) {
        super.flush(commandBuffer);
    }

    public void add(double x, double y, double z, double sx, double sy, double sz, int color ){
        Vec3 origin = Draw3D.getInstance().origin;
        long pos = ptr();
        MemUtil.putVec3(pos, (float) (x - origin.x), (float) (y - origin.y), (float) (z - origin.z));
        MemUtil.putVec3(pos + 12, (float) sx, (float) sy, (float) sz);
        MemUtil.putInt(pos + 24, color);
        next();
    }
}

