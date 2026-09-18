package hack.echo.client.vulkan.api;


import hack.echo.client.event.EventManager;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventEarlyBeginFrame;
import org.lwjgl.vulkan.VkDevice;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.IntSupplier;

public class VulkanDevice {

    public final long vma;
    public final VkDevice vkDevice;
    private final IntSupplier frameIndex;
    public final int framesNum;

    private final Queue<Resource>[] deletionQueue;
    public VulkanDevice(long vma, VkDevice vkDevice, IntSupplier frameIndex, int framesNum) {
        this.vma = vma;
        this.vkDevice = vkDevice;
        this.frameIndex = frameIndex;
        this.framesNum = framesNum;

        deletionQueue = new Queue[framesNum];
        for (int i = 0; i < framesNum; i++) {
            deletionQueue[i] = new ArrayDeque<>();
        }
        EventManager.register(this);
    }

    public VulkanBuffer newBuffer(BufferUsage usage, long size) {
        return newBuffer(usage, size, true);
    }

    public VulkanBuffer newBuffer(BufferUsage usage, long size, boolean perFrame) {
        return new VulkanBuffer(this, usage, size, perFrame);
    }

    @EventSubscribe
    public void event(EventEarlyBeginFrame event) {
        var queue = deletionQueue[frameIndex()];
        while (!queue.isEmpty()) {
            queue.poll().release();
        }
    }

    public void releaseResource(Resource resource) {
        var queue = deletionQueue[frameIndex()];
        queue.add(resource);
    }

    public int frameIndex() {
        return frameIndex.getAsInt();
    }
}
