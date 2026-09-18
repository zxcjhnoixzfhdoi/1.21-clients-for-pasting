package hack.echo.client.utils.player;

import com.mojang.blaze3d.platform.NativeImage;
import hack.echo.client.render2.api.CrossTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class PlayerHeadTextures {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final Map<String, CrossTexture> headCache = new HashMap<>();

    private PlayerHeadTextures() {
    }

    public static CrossTexture getHead(PlayerInfo info) {
        if (info == null) {
            return null;
        }

        Identifier texturePath = info.getSkin().body().texturePath();
        String cacheKey = texturePath.toString();

        CrossTexture cached = headCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        CrossTexture built = buildHeadTexture(texturePath);
        if (built == null) {
            return null;
        }

        headCache.put(cacheKey, built);
        return built;
    }

    private static CrossTexture buildHeadTexture(Identifier texturePath) {
        NativeImage skin = readSkin(texturePath);
        if (skin == null) {
            return null;
        }

        try {
            ByteBuffer pixels = buildHeadPixels(skin);
            if (pixels == null) {
                return null;
            }

            return CrossTexture.fromPixels(pixels, 8, 8);
        } finally {
            skin.close();
        }
    }

    private static NativeImage readSkin(Identifier texturePath) {
        AbstractTexture texture = mc.getTextureManager().getTexture(texturePath);
        if (texture instanceof DynamicTexture dynamicTexture && dynamicTexture.getPixels() != null) {
            return copyImage(dynamicTexture.getPixels());
        }

        try {
            var optional = mc.getResourceManager().getResource(texturePath);
            if (optional.isEmpty()) {
                return null;
            }

            try (InputStream stream = optional.get().open()) {
                return NativeImage.read(stream);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static NativeImage copyImage(NativeImage source) {
        NativeImage copy = new NativeImage(source.getWidth(), source.getHeight(), true);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                copy.setPixel(x, y, source.getPixel(x, y));
            }
        }
        return copy;
    }

    private static ByteBuffer buildHeadPixels(NativeImage skin) {
        if (skin.getWidth() < 64 || skin.getHeight() < 32) {
            return null;
        }

        ByteBuffer buffer = MemoryUtil.memAlloc(8 * 8 * 4);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int pixel = skin.getPixel(8 + x, 8 + y);
                int hat = skin.getPixel(40 + x, 8 + y);
                int merged = blend(pixel, hat);
                writeRgba(buffer, merged);
            }
        }

        buffer.flip();
        return buffer;
    }

    private static int blend(int base, int overlay) {
        int overlayAlpha = (overlay >>> 24) & 0xFF;
        if (overlayAlpha == 0) {
            return base;
        }

        float alpha = overlayAlpha / 255.0f;

        int baseA = (base >>> 24) & 0xFF;
        int baseR = (base >>> 16) & 0xFF;
        int baseG = (base >>> 8) & 0xFF;
        int baseB = base & 0xFF;

        int overR = (overlay >>> 16) & 0xFF;
        int overG = (overlay >>> 8) & 0xFF;
        int overB = overlay & 0xFF;

        int outA = Math.min(255, Math.round(baseA + (255 - baseA) * alpha));
        int outR = Math.round(baseR * (1.0f - alpha) + overR * alpha);
        int outG = Math.round(baseG * (1.0f - alpha) + overG * alpha);
        int outB = Math.round(baseB * (1.0f - alpha) + overB * alpha);

        return (outA << 24) | (outR << 16) | (outG << 8) | outB;
    }

    private static void writeRgba(ByteBuffer buffer, int argb) {
        buffer.put((byte) ((argb >> 16) & 0xFF));
        buffer.put((byte) ((argb >> 8) & 0xFF));
        buffer.put((byte) (argb & 0xFF));
        buffer.put((byte) ((argb >> 24) & 0xFF));
    }
}
