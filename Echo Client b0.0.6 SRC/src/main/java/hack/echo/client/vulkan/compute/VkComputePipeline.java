package hack.echo.client.vulkan.compute;

import hack.echo.client.Echo;
import hack.echo.client.render2.impl.opengl.utils.ShaderUtil;
import hack.echo.client.vulkan.descriptor.Descriptor;
import hack.echo.client.vulkan.descriptor.manager.DescriptorManager;
import hack.echo.client.vulkan.descriptor.PushConstants;
import hack.echo.client.vulkan.graphics.VkGraphicsPipeline;
import hack.echo.client.vulkan.utils.VkUtils;
import hack.echo.client.vulkan.utils.VkUtils.*;

import lombok.Getter;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRPushDescriptor.VK_DESCRIPTOR_SET_LAYOUT_CREATE_PUSH_DESCRIPTOR_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class VkComputePipeline {
    private final long shaderModule;
    private final long descriptorSetLayout;
    private final long pipelineLayout;
    private final long computePipeline;
    private final DescriptorManager descriptorManager;
    @Getter
    private final List<Descriptor> descriptors;
    private final PushConstants pc;
    private final VkDevice vkDevice;
    private VkComputePipeline(Builder builder) {
        this.vkDevice = Echo.vkContext.getDevice();
        this.descriptors = List.copyOf(builder.descriptors);
        this.pc = builder.pc;
        this.shaderModule = createShaderModule(builder.bytes);
        this.descriptorSetLayout = createDescriptorSetLayout();
        this.pipelineLayout = createPipelineLayout();
        this.computePipeline = createComputePipeline();

        this.descriptorManager = descriptors.isEmpty()
                ? null
                : new DescriptorManager(pipelineLayout, descriptors);
    }

    public void recordDispatch(VkCommandBuffer cmd, int x, int y, int z) {
        vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline);

        if (descriptorManager != null) {
            descriptorManager.bind(cmd, VK_PIPELINE_BIND_POINT_COMPUTE);
        }

        if (pc != null) {
            pc.push(cmd, pipelineLayout);
        }

        vkCmdDispatch(cmd, x, y, z);
    }


private long createShaderModule(ByteBuffer spirvCode) {
        try (var stack = stackPush()) {
            var createInfo = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(spirvCode);

            var pModule = stack.mallocLong(1);
            VkUtils.check(vkCreateShaderModule(vkDevice, createInfo, null, pModule),
                    "Failed to create shader module");
            return pModule.get(0);
        }
    }

    private long createDescriptorSetLayout() {
        try (var stack = stackPush()) {
            var layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .flags(VK_DESCRIPTOR_SET_LAYOUT_CREATE_PUSH_DESCRIPTOR_BIT_KHR);

            if (!descriptors.isEmpty()) {
                var bindings = VkDescriptorSetLayoutBinding.calloc(descriptors.size(), stack);
                for (int i = 0; i < descriptors.size(); i++) {
                    Descriptor desc = descriptors.get(i);
                    bindings.get(i)
                            .binding(desc.binding())
                            .descriptorType(desc.getType())
                            .descriptorCount(1)
                            .stageFlags(desc.stages());
                }
                layoutInfo.pBindings(bindings);
            }

            var pLayout = stack.mallocLong(1);
            VkUtils.check(vkCreateDescriptorSetLayout(vkDevice, layoutInfo, null, pLayout),
                    "Failed to create descriptor set layout");
            return pLayout.get(0);
        }
    }

    private long createPipelineLayout() {
        try (var stack = stackPush()) {
            var info = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .pSetLayouts(stack.longs(descriptorSetLayout));

            return VkGraphicsPipeline.setPushConstants(stack, info, pc, vkDevice);
        }
    }

    private long createComputePipeline() {
        try (var stack = stackPush()) {
            var stageInfo = VkPipelineShaderStageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_COMPUTE_BIT)
                    .module(shaderModule)
                    .pName(stack.UTF8("main"));

            var pipelineInfo = VkComputePipelineCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO)
                    .stage(stageInfo)
                    .layout(pipelineLayout);

            var pPipeline = stack.mallocLong(1);
            VkUtils.check(vkCreateComputePipelines(vkDevice, VK_NULL_HANDLE, pipelineInfo, null, pPipeline),
                    "Failed to create compute pipeline");
            return pPipeline.get(0);
        }
    }

    public void cleanUp() {
        vkDestroyShaderModule(vkDevice, shaderModule, null);
        vkDestroyPipeline(vkDevice, computePipeline, null);
        vkDestroyPipelineLayout(vkDevice, pipelineLayout, null);
        vkDestroyDescriptorSetLayout(vkDevice, descriptorSetLayout, null);
    }

    public static Builder builder(String shaderSource) {
        return new Builder(shaderSource);
    }

    public static class Builder {
        ByteBuffer bytes;
        List<Descriptor> descriptors = new ArrayList<>();
        PushConstants pc;

        Builder(String shaderPath) {
            String shaderSource = ShaderUtil.getShaderResource("vk/" + shaderPath);
            this.bytes = VkUtils.compile("Compute Shader", shaderSource, ShaderKind.COMPUTE);
        }

        public Builder descriptor(Descriptor descriptor) {
            this.descriptors.add(descriptor);
            return this;
        }

        public Builder pushConstants(PushConstants pc) {
            this.pc = pc;
            return this;
        }

        public VkComputePipeline build() {
            return new VkComputePipeline(this);
        }
    }
}
