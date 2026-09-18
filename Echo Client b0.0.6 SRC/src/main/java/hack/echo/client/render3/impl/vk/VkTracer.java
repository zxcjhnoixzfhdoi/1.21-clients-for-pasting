package hack.echo.client.render3.impl.vk;

import hack.echo.client.Echo;
import hack.echo.client.vulkan.descriptor.SimpleUBO;
import hack.echo.client.vulkan.graphics.VkGraphicsPipeline;
import hack.echo.client.render3.api.Draw3D;
import hack.echo.client.vulkan.graphics.VkShader;
import hack.echo.client.vulkan.memory.MemUtil;
import net.minecraft.world.phys.Vec3;

import static org.lwjgl.vulkan.VK10.*;

public class VkTracer extends VkShader {
    private final SimpleUBO ubo;
    private static final int stride = 16;
    public VkTracer(SimpleUBO ubo) {
        super(stride, 2);
        this.ubo = ubo;
    }

    @Override
    protected VkGraphicsPipeline.Builder getPipelineBuilder() {
        return VkGraphicsPipeline.builder("3d/tracer.vert", "3d/basic.frag")
                .stride(stride)
                .attribute(0, VK_FORMAT_R32G32B32_SFLOAT, 0)
                .attribute(1, VK_FORMAT_R32_SINT, 12)
                .descriptor(ubo)
                .cull(VK_CULL_MODE_BACK_BIT)
                .topology(VK_PRIMITIVE_TOPOLOGY_LINE_LIST)
                .depthFunc(Echo.vkContext.depthCompareOp())
                .blend(true);
    }

    public void add(double x, double y, double z, int color) {
        Vec3 o = Draw3D.getInstance().origin;
        long p = ptr();
        MemUtil.putVec3(
                p,
                (float) (x - o.x),
                (float) (y - o.y),
                (float) (z - o.z)
        );
        MemUtil.putInt(p + 12, color);
        next();
    }

}
