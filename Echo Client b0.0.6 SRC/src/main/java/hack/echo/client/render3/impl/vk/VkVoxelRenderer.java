package hack.echo.client.render3.impl.vk;

import hack.echo.client.Echo;
import hack.echo.client.chunk.BuiltChunk;
import hack.echo.client.chunk.ChunkManager;
import hack.echo.client.features.impl.misc.VoxelEspConfig;
import hack.echo.client.render3.api.Draw3D;
import hack.echo.client.vulkan.CommandBuffer;
import hack.echo.client.vulkan.api.BufferUsage;
import hack.echo.client.vulkan.api.VulkanBuffer;
import hack.echo.client.vulkan.descriptor.PushConstants;
import hack.echo.client.vulkan.descriptor.SimpleSSBO;
import hack.echo.client.vulkan.descriptor.SimpleTexelBuffer;
import hack.echo.client.vulkan.descriptor.SimpleUBO;
import hack.echo.client.vulkan.graphics.VkGraphicsPipeline;
import hack.echo.client.vulkan.graphics.VkRenderer;
import hack.echo.client.vulkan.memory.MemUtil;
import hack.echo.client.vulkan.utils.VkUtils;
import net.minecraft.world.phys.Vec3;

import static hack.echo.client.Echo.vulkanDevice;
import static hack.echo.client.vulkan.memory.MemUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkVoxelRenderer implements ChunkManager.ChunkCallback, VkRenderer {
    private static final int vertexStride = 4;
    private static final int paletteStride = 4;
    private static final int chunkStride = 64;
    private static final int indirectStride = 16;
    private static final int verticesPerChunk = 4096 / 2 * 6;
    private int chunkCapacity = 100;


    private VulkanBuffer vbo;
    private VulkanBuffer chunkBuffer;
    private final VulkanBuffer indirectBuffer;


    private final SimpleUBO ubo;
    private final SimpleSSBO chunkDescriptor;
    private final SimpleTexelBuffer paletteDescriptor;
    private final PushConstants pc;

    private final ChunkManager cm;

    private VkGraphicsPipeline voxelPipeline;

    public VkVoxelRenderer(SimpleUBO ubo) {
        this.ubo = ubo;
        this.cm = Echo.chunkManager;
        this.cm.setCallback(this);

        this.vbo = vulkanDevice.newBuffer(BufferUsage.VERTEX, vertexStride * verticesPerChunk * chunkCapacity, false);
        this.chunkBuffer = vulkanDevice.newBuffer(BufferUsage.STORAGE, (long) chunkStride * chunkCapacity, false);
        this.indirectBuffer = vulkanDevice.newBuffer(BufferUsage.INDIRECT, (long) indirectStride * cm.getMaxChunks() * 6, false);

        this.chunkDescriptor = new SimpleSSBO(1, VK_SHADER_STAGE_VERTEX_BIT);
        this.pc = new PushConstants(36, VK_SHADER_STAGE_VERTEX_BIT);
        this.paletteDescriptor = new SimpleTexelBuffer(2, VK_SHADER_STAGE_VERTEX_BIT, VK_FORMAT_R32_UINT, 4096 * paletteStride);

        chunkDescriptor.buffer = chunkBuffer;
    }


    @Override
    public void onChunkSet(int index, BuiltChunk chunk) {
        updateChunk(index, chunk);
    }

    @Override
    public void onChunkUpdated(int index, BuiltChunk chunk) {
        updateChunk(index, chunk);
    }

    private void grow(int index) {
        if (index < chunkCapacity) return;
        while (index <= chunkCapacity) chunkCapacity <<= 1;

        vbo = VkUtils.growBuffer(vulkanDevice, vbo, (long) chunkCapacity * vertexStride * verticesPerChunk);
        chunkBuffer = VkUtils.growBuffer(vulkanDevice, chunkBuffer, (long) chunkStride * chunkCapacity);
    }

    private void updateChunk(int index, BuiltChunk chunk) {
        grow(index);
        var vertices = chunk.getVertices();

        long chunkPtr = chunkBuffer.contents() + (long) index * chunkStride;
        putIVec3(chunkPtr, chunk.cx, chunk.cy, chunk.cz);
        putInt(chunkPtr + 12, 0);

        long indirectPtr = indirectBuffer.contents() + (long) index * 6 * indirectStride;

        if (vertices != null) {
            long basePtr = vbo.contents() + (long) index * verticesPerChunk * vertexStride;
            int slotBase = index * verticesPerChunk;
            long faceOffset = 0;
            for (int i = 0; i < 6; i++) {
                int size = vertices[i].size();

                memcpy(vertices[i].elements(), basePtr + faceOffset, size);

                long meshDataPtr = chunkPtr + 16 + (long) i * 8;
                putInt(meshDataPtr, size);
                putInt(meshDataPtr + 4, slotBase);

                long cmdPtr = indirectPtr + (long) i * indirectStride;
                putInt(cmdPtr, 4);
                putInt(cmdPtr + 4, size);
                putInt(cmdPtr + 8, (index * 6 + i) * 4);
                putInt(cmdPtr + 12, slotBase);

                faceOffset += (long) size * vertexStride;
                slotBase += size;
            }
        } else {
            for (int i = 0; i < 6; i++) {
                putInt(chunkPtr + 16 + (long) i * 8, 0);

                long cmdPtr = indirectPtr + (long) i * indirectStride;
                putInt(cmdPtr, 0);
                putInt(cmdPtr + 4, 0);
                putInt(cmdPtr + 8, 0);
                putInt(cmdPtr + 12, 0);
            }
        }
    }

    @Override
    public void onChunkRemoved(int index) {
        putInt(chunkBuffer.contents() + (long) index * chunkStride + 12, 0);

        long indirectPtr = indirectBuffer.contents() + (long) index * 6 * indirectStride;
        for (int i = 0; i < 6; i++) {
            putInt(indirectPtr + (long) i * indirectStride + 4, 0);
        }
    }

    @Override
    public void onClear() {

    }

    @Override
    public void onPaletteChange(int[] palette) {
        long dst = paletteDescriptor.getBuffer().contents();
        memcpy(palette, dst, palette.length);
    }

    @Override
    public void beginFrame() {
        if (voxelPipeline == null) {
            voxelPipeline = VkGraphicsPipeline.builder("3d/voxel.vert", "3d/voxel.frag")
                    .attribute(0, VK_FORMAT_R32_UINT, 0)
                    .stride(4)
                    .descriptor(ubo)
                    .descriptor(chunkDescriptor)
                    .descriptor(paletteDescriptor)
                    .pushConstants(pc)
                    .blend(false)
                    .cull(VK_CULL_MODE_FRONT_BIT)
                    .depthFunc(Echo.vkContext.depthCompareOp())
                    .build();
        }
    }

    private void uploadPc() {
        Vec3 origin = Draw3D.getInstance().origin;
        int chunkCount = cm.getCount();
        int camChunkX = (int) Math.floor(origin.x / 16.0);
        int camChunkY = (int) Math.floor(origin.y / 16.0);
        int camChunkZ = (int) Math.floor(origin.z / 16.0);

        long ptr = pc.pointer;

        MemUtil.putInt(ptr, VoxelEspConfig.isFill() ? 1 : 0);
        MemUtil.putInt(ptr + 4, VoxelEspConfig.isOutline() ? 1 : 0);
        MemUtil.putIVec3(ptr + 8, camChunkX, camChunkY, camChunkZ);
        MemUtil.putVec3(ptr + 20,
                (float) (origin.x - camChunkX * 16.0),
                (float) (origin.y - camChunkY * 16.0),
                (float) (origin.z - camChunkZ * 16.0)
        );
        MemUtil.putInt(ptr + 32, chunkCount);
    }


    @Override
    public void flush(CommandBuffer metal) {
        int chunkCount = cm.getCount();
        if (chunkCount == 0) return;

        uploadPc();
        metal.drawIndirect(voxelPipeline, vbo, indirectBuffer, chunkCount * 6, indirectStride);
    }

    @Override
    public void cleanup() {

    }
}
