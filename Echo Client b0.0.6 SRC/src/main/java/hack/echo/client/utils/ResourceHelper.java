package hack.echo.client.utils;

import hack.echo.client.Echo;
import lombok.experimental.UtilityClass;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_MAX_LEVEL;

@UtilityClass
public class ResourceHelper {
    private final String assetsPath = "assets/" + Echo.MOD_ID + "/";
    private final Map<String, Integer> textureCache = new HashMap<>();

    public static byte[] getBytes(String path) {
        try (InputStream inputStream = getStream(path)) {
            return inputStream != null ? inputStream.readAllBytes() : null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getString(String path) {
        byte[] bytes = getBytes(path);
        return bytes != null ? new String(bytes, StandardCharsets.UTF_8) : null;
    }

    public InputStream getStream(String path) {
        return ResourceHelper.class.getClassLoader().getResourceAsStream(assetsPath + path);
    }

    public int getTexture(String path) {
        Integer cached = textureCache.get(path);
        if (cached != null) return cached;

        int textureId = createTexture(path);
        if (textureId != -1) {
            textureCache.put(path, textureId);
        }
        return textureId;
    }

    private int createTexture(String path) {
        byte[] bytes = getBytes(path);
        if (bytes == null) return -1;

        ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
        buffer.put(bytes).flip();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            STBImage.stbi_set_flip_vertically_on_load(false);
            ByteBuffer image = STBImage.stbi_load_from_memory(buffer, width, height, channels, 4);
            MemoryUtil.memFree(buffer);

            if (image == null) {
                System.err.println("Failed to load texture: " + path + " - " + STBImage.stbi_failure_reason());
                return -1;
            }

            int w = width.get(0);
            int h = height.get(0);

            int textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);

            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
            glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
            glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 4);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            glBindTexture(GL_TEXTURE_2D, 0);

            STBImage.stbi_image_free(image);
            return textureId;
        }
    }

    public void clearTextureCache() {
        for (int textureId : textureCache.values()) {
            glDeleteTextures(textureId);
        }
        textureCache.clear();
    }
}