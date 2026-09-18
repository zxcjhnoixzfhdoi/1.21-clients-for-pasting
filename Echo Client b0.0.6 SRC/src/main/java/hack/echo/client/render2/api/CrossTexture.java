package hack.echo.client.render2.api;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

//? if >26.1.2 {
/*import com.mojang.blaze3d.vulkan.VulkanGpuSampler;
import com.mojang.blaze3d.vulkan.VulkanGpuTexture;
import com.mojang.blaze3d.vulkan.VulkanGpuTextureView;
*///?}
import hack.echo.client.Echo;
import hack.echo.client.render3.impl.gl.api.GlDraw3D;
import hack.echo.client.utils.ResourceHelper;
import hack.echo.client.utils.VulkanUtil;
import hack.echo.client.vulkan.memory.VkImage;
import net.minecraft.resources.Identifier;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static hack.echo.client.utils.Imports.mc;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_MAX_LEVEL;
import static org.lwjgl.vulkan.VK10.*;

public class CrossTexture {
    public VkImage vkImage = null;
    public int glId = -1;

    public static CrossTexture from(String path) {
        CrossTexture texture = new CrossTexture();
        if (VulkanUtil.isVulkanLoaded()) {
            texture.vkImage = VkImage.of(path);
        } else {
            byte[] bytes = ResourceHelper.getBytes(path);
            if (bytes == null) {
                return texture;
            }

            if (Echo.draw3d instanceof GlDraw3D glDraw3D) {
                glDraw3D.queueRunnable(() -> texture.glId = createGlTexture(bytes));
            } else {
                texture.glId = createGlTexture(bytes);
            }
        }
        return texture;
    }

    public static CrossTexture from(Identifier identifier) {
        var manager = mc.getTextureManager();
        var abstractTexture = manager.getTexture(identifier);

        var texture = abstractTexture.getTexture();
        var view = abstractTexture.getTextureView();
        var sampler = abstractTexture.getSampler();

        return from(texture, view, sampler);
    }

    public static CrossTexture from(RenderTarget renderTarget) {
        var texture = renderTarget.getColorTexture();
        var view = renderTarget.getColorTextureView();
        var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        return from(texture, view, sampler);
    }

    public static CrossTexture from(GpuTexture texture, GpuTextureView view, GpuSampler sampler) {
        var crossTexture = new CrossTexture();


        if (VulkanUtil.isVulkanLoaded()) {
            //? if >26.1.2 {
            /*crossTexture.vkImage = VkImage.of((VulkanGpuTexture) texture, (VulkanGpuTextureView) view, (VulkanGpuSampler) sampler);
            *///?} else {
            crossTexture.vkImage = VkImage.of((GlTexture) texture);
            //?}
        } else {
            crossTexture.glId = ((GlTexture) texture).glId();
        }
        return crossTexture;
    }

    public static CrossTexture from(byte[] bytes) {
        CrossTexture texture = new CrossTexture();
        if (VulkanUtil.isVulkanLoaded()) {
            texture.vkImage = VkImage.of(bytes);
        } else {
            if (Echo.draw3d instanceof GlDraw3D glDraw3D) {
                glDraw3D.queueRunnable(() -> texture.glId = createGlTexture(bytes));
            } else {
                texture.glId = createGlTexture(bytes);
            }
        }
        return texture;
    }

    public static CrossTexture fromPixels(ByteBuffer rgba, int w, int h) {
        CrossTexture texture = new CrossTexture();
        if (VulkanUtil.isVulkanLoaded()) {
            VkImage image = VkImage.builder(w, h)
                    .setUsage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT)
                    .setLinearFiltering(true)
                    .setClamp(true)
                    .createVulkanImage();
            image.uploadSubTextureAsync(0, w, h, 0, 0, 0, 0, w, rgba);
            texture.vkImage = image;
        } else {
            ByteBuffer copy = MemoryUtil.memAlloc(rgba.remaining());
            copy.put(rgba.duplicate()).flip();
            ((GlDraw3D) Echo.draw3d).queueRunnable(() -> {
                int id = glGenTextures();
                glBindTexture(GL_TEXTURE_2D, id);
                glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
                glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
                glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
                glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, copy);
                glBindTexture(GL_TEXTURE_2D, 0);
                MemoryUtil.memFree(copy);
                texture.glId = id;
            });
        }
        return texture;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CrossTexture other)) return false;
        if (VulkanUtil.isVulkanLoaded()) {
            return vkImage != null && other.vkImage != null
                    && vkImage.getImageView() == other.vkImage.getImageView()
                    && vkImage.getSampler() == other.vkImage.getSampler();
        }
        return glId != -1 && glId == other.glId;
    }

    private static int createGlTexture(byte[] bytes) {
        ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
        buffer.put(bytes).flip();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer ch = stack.mallocInt(1);
            STBImage.stbi_set_flip_vertically_on_load(false);
            ByteBuffer image = STBImage.stbi_load_from_memory(buffer, w, h, ch, 4);
            MemoryUtil.memFree(buffer);
            if (image == null) return -1;
            int id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
            glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
            glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w.get(0), h.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            glBindTexture(GL_TEXTURE_2D, 0);
            STBImage.stbi_image_free(image);
            return id;
        }
    }

    @Override
    public int hashCode() {
        if (VulkanUtil.isVulkanLoaded()) {
            return vkImage != null ? Long.hashCode(vkImage.getImageView() * 31 + vkImage.getSampler()) : 0;
        }
        return Integer.hashCode(glId);
    }

    public void cleanup() {
        if (glId != -1) {
            glDeleteTextures(glId);
            glId = -1;
        }
        if (vkImage != null) {
            vkImage.free();
            vkImage = null;
        }
    }

}
