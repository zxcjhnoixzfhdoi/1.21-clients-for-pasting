package hack.echo.client.vulkan.graphics;

//? if >26.1.2 {
/*import com.mojang.blaze3d.vulkan.VulkanConst;
*///?}
import hack.echo.client.Echo;
import hack.echo.client.render2.impl.opengl.utils.ShaderUtil;
import hack.echo.client.utils.Imports;
import hack.echo.client.vulkan.descriptor.Descriptor;
import hack.echo.client.vulkan.descriptor.manager.DescriptorManager;
import hack.echo.client.vulkan.descriptor.PushConstants;
import hack.echo.client.vulkan.utils.VkUtils;
import lombok.Getter;
//? if <=26.1.2 {
import net.vulkanmod.vulkan.Renderer;
//?}
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRPushDescriptor.VK_DESCRIPTOR_SET_LAYOUT_CREATE_PUSH_DESCRIPTOR_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;

@SuppressWarnings("ALL")
public class VkGraphicsPipeline implements Imports {
    private static VkDevice vkDevice;


    private final long vertShaderModule;
    private final long fragShaderModule;
    @Getter
    private final long pipelineLayout;
    private final long descriptorSetLayout;
    private final DescriptorManager descriptorManager;

    private final List<AttributeInfo> attributes;
    private final int stride;
    private final int inputRate;
    @Getter
    private final List<Descriptor> descriptors;

    private final int topology;
    private final int cullMode;
    private final int polygonMode;
    private final boolean depthTest;
    private final boolean depthWrite;
    private final int depthCompareOp;
    private final boolean blendEnable;
    private final int srcColorBlendFactor;
    private final int dstColorBlendFactor;
    private final int srcAlphaBlendFactor;
    private final int dstAlphaBlendFactor;
    private final int blendOp;
    private final int colorWriteMask;
    private long pipelineHandle = 0;

    private final PushConstants pc;

    private VkGraphicsPipeline(Builder builder) {
        vkDevice = Echo.vkContext.getDevice();
        this.topology = builder.topology;
        this.cullMode = builder.cullMode;
        this.polygonMode = builder.polygonMode;
        this.depthTest = builder.depthTest;
        this.depthWrite = builder.depthWrite;
        this.depthCompareOp = builder.depthCompareOp;
        this.blendEnable = builder.blendEnable;
        this.srcColorBlendFactor = builder.srcColorBlendFactor;
        this.dstColorBlendFactor = builder.dstColorBlendFactor;
        this.srcAlphaBlendFactor = builder.srcAlphaBlendFactor;
        this.dstAlphaBlendFactor = builder.dstAlphaBlendFactor;
        this.blendOp = builder.blendOp;
        this.colorWriteMask = builder.colorWriteMask;
        this.attributes = List.copyOf(builder.attributes);
        this.stride = builder.stride;
        this.inputRate = builder.inputRate;
        this.descriptors = List.copyOf(builder.descriptors);
        this.pc = builder.pc;
        this.vertShaderModule = createShaderModule(builder.vertBytes);
        MemoryUtil.memFree(builder.vertBytes);
        this.fragShaderModule = createShaderModule(builder.fragBytes);
        MemoryUtil.memFree(builder.fragBytes);
        this.descriptorSetLayout = createDescriptorSetLayout();
        this.pipelineLayout = createPipelineLayout();
        this.pipelineHandle = createGraphicsPipeline();
        this.descriptorManager = descriptors.isEmpty()
                ? null
                : new DescriptorManager(pipelineLayout, descriptors);
    }

    public void bindPipeline(VkCommandBuffer commandBuffer) {
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, this.pipelineHandle);
    }

    public void bindVertexBuffer(VkCommandBuffer commandBuffer, long bufferId) {
        bindVertexBuffer(commandBuffer, 0, bufferId, 0);
    }

    public void bindVertexBuffer(VkCommandBuffer commandBuffer, int binding, long bufferId, long offset) {
        try (var stack = stackPush()) {
            vkCmdBindVertexBuffers(commandBuffer, binding, stack.longs(bufferId), stack.longs(offset));
        }
    }

    public void bindDescriptors(VkCommandBuffer commandBuffer) {
        if (descriptorManager != null) {
            descriptorManager.bind(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS);
        }
    }

    public void bindPushConstants(VkCommandBuffer commandBuffer) {
        if (pc != null) {
            pc.push(commandBuffer, pipelineLayout);
        }
    }


    public static Builder builder(String vertSource, String fragSource) {
        return new Builder(vertSource, fragSource);
    }

    private long createGraphicsPipeline() {
        if (pipelineHandle != 0) return this.pipelineHandle;
        var target = mc.getMainRenderTarget();

        int colorFormat;
        int depthFormat;
        long renderPassHandle = 0L;

        //? if >26.1.2 {
        /*colorFormat = VulkanConst.toVk(target.getColorTexture().getFormat());
        depthFormat= target.useDepth ? VulkanConst.toVk(target.getDepthTexture().getFormat()) : VK_FORMAT_UNDEFINED;
        *///?} else {

        var boundRenderPass = Renderer.getInstance().getBoundRenderPass();
        var framebuffer = Renderer.getInstance().getBoundFramebuffer();

        colorFormat = framebuffer.getFormat();
        depthFormat = framebuffer.getDepthFormat();
        renderPassHandle = boundRenderPass.getId();

         //?}



        try (var stack = stackPush()) {
            var entryPoint = stack.UTF8("main");

            var shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);

            var vertShaderStageInfo = shaderStages.get(0);
            vertShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            vertShaderStageInfo.stage(VK_SHADER_STAGE_VERTEX_BIT);
            vertShaderStageInfo.module(vertShaderModule);
            vertShaderStageInfo.pName(entryPoint);

            var fragShaderStageInfo = shaderStages.get(1);
            fragShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            fragShaderStageInfo.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
            fragShaderStageInfo.module(fragShaderModule);
            fragShaderStageInfo.pName(entryPoint);

            var vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);

            if (!attributes.isEmpty()) {
                var bindingDescription = VkVertexInputBindingDescription.calloc(1, stack);
                bindingDescription.get(0)
                        .binding(0)
                        .stride(stride)
                        .inputRate(inputRate);

                var attributeDescriptions = VkVertexInputAttributeDescription.calloc(attributes.size(), stack);
                for (int i = 0; i < attributes.size(); i++) {
                    var attr = attributes.get(i);
                    attributeDescriptions.get(i)
                            .binding(attr.binding())
                            .location(attr.location())
                            .format(attr.format())
                            .offset(attr.offset());
                }

                vertexInputInfo.pVertexBindingDescriptions(bindingDescription);
                vertexInputInfo.pVertexAttributeDescriptions(attributeDescriptions);
            }

            var inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            inputAssembly.topology(topology);
            inputAssembly.primitiveRestartEnable(false);

            var viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
            viewportState.viewportCount(1);
            viewportState.scissorCount(1);

            var rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
            rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
            rasterizer.depthClampEnable(false);
            rasterizer.rasterizerDiscardEnable(false);
            rasterizer.polygonMode(polygonMode);
            rasterizer.lineWidth(1.0f);
            rasterizer.cullMode(cullMode);
            rasterizer.frontFace(VK_FRONT_FACE_CLOCKWISE);
            rasterizer.depthBiasEnable(true);

            var multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
            multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
            multisampling.sampleShadingEnable(false);
            multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            var depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack);
            depthStencil.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
            depthStencil.depthTestEnable(depthTest);
            depthStencil.depthWriteEnable(depthWrite);
            depthStencil.depthCompareOp(depthCompareOp);
            depthStencil.depthBoundsTestEnable(false);
            depthStencil.minDepthBounds(0.0f);
            depthStencil.maxDepthBounds(1.0f);
            depthStencil.stencilTestEnable(false);

            var colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack);
            colorBlendAttachment.colorWriteMask(colorWriteMask);
            colorBlendAttachment.blendEnable(blendEnable);
            if (blendEnable) {
                colorBlendAttachment.srcColorBlendFactor(srcColorBlendFactor);
                colorBlendAttachment.dstColorBlendFactor(dstColorBlendFactor);
                colorBlendAttachment.colorBlendOp(blendOp);
                colorBlendAttachment.srcAlphaBlendFactor(srcAlphaBlendFactor);
                colorBlendAttachment.dstAlphaBlendFactor(dstAlphaBlendFactor);
                colorBlendAttachment.alphaBlendOp(blendOp);
            }

            var colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlending.logicOpEnable(false);
            colorBlending.pAttachments(colorBlendAttachment);
            colorBlending.blendConstants(stack.floats(0.0f, 0.0f, 0.0f, 0.0f));

            var dynamicStates = VkPipelineDynamicStateCreateInfo.calloc(stack);
            dynamicStates.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
            dynamicStates.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_DEPTH_BIAS, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR));

            var pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            pipelineInfo.pStages(shaderStages);
            pipelineInfo.pVertexInputState(vertexInputInfo);
            pipelineInfo.pInputAssemblyState(inputAssembly);
            pipelineInfo.pViewportState(viewportState);
            pipelineInfo.pRasterizationState(rasterizer);
            pipelineInfo.pMultisampleState(multisampling);
            pipelineInfo.pDepthStencilState(depthStencil);
            pipelineInfo.pColorBlendState(colorBlending);
            pipelineInfo.pDynamicState(dynamicStates);
            pipelineInfo.layout(pipelineLayout);
            pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
            pipelineInfo.basePipelineIndex(-1);


            if (Echo.vkContext.isDynamicRendering()) {
                var renderingInfo = VkPipelineRenderingCreateInfoKHR.calloc(stack);
                renderingInfo.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO_KHR);
                renderingInfo.pColorAttachmentFormats(stack.ints(colorFormat));
                renderingInfo.depthAttachmentFormat(depthFormat);
                pipelineInfo.pNext(renderingInfo);
            } else {
                pipelineInfo.renderPass(renderPassHandle);
                pipelineInfo.subpass(0);
            }

            var pGraphicsPipeline = stack.mallocLong(1);

            VkUtils.check(vkCreateGraphicsPipelines(vkDevice, VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline),
                    "Failed to create graphics pipeline : " +  this.getClass().getSimpleName());

            this.pipelineHandle = pGraphicsPipeline.get(0);
            return this.pipelineHandle;
        }
    }

    private static long createShaderModule(ByteBuffer spirvCode) {
        try (var stack = stackPush()) {
            var createInfo = VkShaderModuleCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(spirvCode);

            var pShaderModule = stack.mallocLong(1);
            VkUtils.check(vkCreateShaderModule(vkDevice, createInfo, null, pShaderModule), "Failed to create shader module");

            return pShaderModule.get(0);
        }
    }

    private long createDescriptorSetLayout() {
        try (var stack = stackPush()) {
            var layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
            layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
            layoutInfo.flags(VK_DESCRIPTOR_SET_LAYOUT_CREATE_PUSH_DESCRIPTOR_BIT_KHR);

            if (!descriptors.isEmpty()) {
                var bindings = VkDescriptorSetLayoutBinding.calloc(descriptors.size(), stack);
                var flags = stack.callocInt(descriptors.size());
                for (int i = 0; i < descriptors.size(); i++) {
                    var desc = descriptors.get(i);
                    bindings.get(i)
                            .binding(desc.binding())
                            .descriptorType(desc.getType())
                            .descriptorCount(desc.descriptorCount())
                            .stageFlags(desc.stages());

                }

                layoutInfo.pBindings(bindings);
            }

            var pDescriptorSetLayout = stack.mallocLong(1);
            VkUtils.check(vkCreateDescriptorSetLayout(vkDevice, layoutInfo, null, pDescriptorSetLayout),
                    "Failed to create descriptor set layout");

            return pDescriptorSetLayout.get(0);
        }
    }

    private long createPipelineLayout() {
        try (var stack = stackPush()) {
            var pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            pipelineLayoutInfo.pSetLayouts(stack.longs(descriptorSetLayout));

            return setPushConstants(stack, pipelineLayoutInfo, pc, vkDevice);
        }
    }

    public static long setPushConstants(MemoryStack stack, VkPipelineLayoutCreateInfo pipelineLayoutInfo, PushConstants pc, VkDevice device) {
        if (pc != null) {
            var pushConstantRange = VkPushConstantRange.calloc(1, stack);
            pushConstantRange.get(0)
                    .stageFlags(pc.getStages())
                    .offset(0)
                    .size(pc.getSize());
            pipelineLayoutInfo.pPushConstantRanges(pushConstantRange);
        }

        var pPipelineLayout = stack.mallocLong(1);
        VkUtils.check(vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout),
                "Failed to create pipeline layout");

        return pPipelineLayout.get(0);
    }

    public void cleanUp() {
        vkDestroyShaderModule(vkDevice, vertShaderModule, null);
        vkDestroyShaderModule(vkDevice, fragShaderModule, null);

        vkDestroyPipeline(vkDevice, pipelineHandle, null);
        vkDestroyDescriptorSetLayout(vkDevice, descriptorSetLayout, null);
        vkDestroyPipelineLayout(vkDevice, pipelineLayout, null);
    }

    public static class Builder {
        ByteBuffer vertBytes;
        ByteBuffer fragBytes;

        List<AttributeInfo> attributes = new ArrayList<>();
        List<Descriptor> descriptors = new ArrayList<>();

        int stride = 0;
        int inputRate = VK_VERTEX_INPUT_RATE_INSTANCE;
        int topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP;
        int cullMode = VK_CULL_MODE_NONE;
        int polygonMode = VK_POLYGON_MODE_FILL;
        boolean depthTest = true;
        boolean depthWrite = true;
        int depthCompareOp = VK_COMPARE_OP_LESS_OR_EQUAL;
        boolean blendEnable = true;
        int srcColorBlendFactor = VK_BLEND_FACTOR_SRC_ALPHA;
        int dstColorBlendFactor = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
        int srcAlphaBlendFactor = VK_BLEND_FACTOR_SRC_ALPHA;
        int dstAlphaBlendFactor = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
        int blendOp = VK_BLEND_OP_ADD;
        int colorWriteMask = 0xF;


        PushConstants pc;

        protected Builder(String vertPath, String fragPath) {
            String vertSource = ShaderUtil.getShaderResource("vk/" + vertPath);
            String fragSource = ShaderUtil.getShaderResource("vk/" + fragPath);
            this.vertBytes = VkUtils.compile("Vertex Shader", vertSource, VkUtils.ShaderKind.VERTEX);
            this.fragBytes = VkUtils.compile("Fragment Shader", fragSource, VkUtils.ShaderKind.FRAGMENT);
        }

        public Builder attribute(int binding, int location, int format, int offset) {
            this.attributes.add(new AttributeInfo(binding, location, format, offset));
            return this;
        }

        public Builder attribute(int location, int format, int offset) {
            return attribute(0, location, format, offset);
        }

        public Builder stride(int stride) {
            this.stride = stride;
            return this;
        }

        public Builder inputRate(int inputRate) {
            this.inputRate = inputRate;
            return this;
        }

        public Builder descriptor(Descriptor descriptor) {
            this.descriptors.add(descriptor);
            return this;
        }

        public Builder topology(int topology) {
            this.topology = topology;
            return this;
        }

        public Builder cull(int cullMode) {
            this.cullMode = cullMode;
            return this;
        }

        public Builder polygonMode(int polygonMode) {
            this.polygonMode = polygonMode;
            return this;
        }

        public Builder depthTest(boolean enable) {
            this.depthTest = enable;
            return this;
        }

        public Builder depthWrite(boolean enable) {
            this.depthWrite = enable;
            return this;
        }

        public Builder depthFunc(int op) {
            this.depthCompareOp = op;
            return this;
        }

        public Builder blend(boolean enable) {
            this.blendEnable = enable;
            return this;
        }

        public Builder blendFunc(int srcColor, int dstColor, int srcAlpha, int dstAlpha) {
            this.srcColorBlendFactor = srcColor;
            this.dstColorBlendFactor = dstColor;
            this.srcAlphaBlendFactor = srcAlpha;
            this.dstAlphaBlendFactor = dstAlpha;
            return this;
        }

        public Builder colorWriteMask(int mask) {
            this.colorWriteMask = mask;
            return this;
        }

        public Builder pushConstants(PushConstants pc) {
            this.pc = pc;
            return this;
        }

        public VkGraphicsPipeline build() {
            return new VkGraphicsPipeline(this);
        }
    }
}
