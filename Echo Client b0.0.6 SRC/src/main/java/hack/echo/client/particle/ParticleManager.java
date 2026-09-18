package hack.echo.client.particle;

import hack.echo.client.event.EventManager;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventRender3D;
import hack.echo.client.particle.impl.GhostParticle;
import hack.echo.client.particle.impl.MarkerParticle;
import hack.echo.client.render2.api.CrossTexture;
import hack.echo.client.utils.ResourceHelper;
import lombok.Getter;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ParticleManager {

    private final Map<Class<? extends Particle>, String> registry = new HashMap<>();
    private final Map<Class<? extends Particle>, UV> uvMap = new HashMap<>();
    @Getter
    private CrossTexture atlas;

    private Particle[] active = new Particle[64];
    private int count = 0;

    public ParticleManager() {
        EventManager.register(this);
        registerParticle(GhostParticle.class, "textures/fx/glow_circle.png");
        registerParticle(MarkerParticle.class, "textures/fx/marker.png");
    }

    public void addParticle(Particle particle) {
        if (count >= active.length) {
            active = Arrays.copyOf(active, active.length * 2);
        }
        active[count++] = particle;
    }

    @EventSubscribe
    public void onRender3D(EventRender3D e) {
        var minecraft = e.getDraw3D().getMinecraftTarget();
        var custom = e.getDraw3D().getCustomTarget();

        int i = 0;
        while (i < count) {
            Particle p = active[i];
            p.age++;
            p.update(e.getTickDelta());
            if (p.isDead()) {
                active[i] = active[--count];
                active[count] = null;
            } else {
                if (p.isOverlay()) {
                    custom.particle(p.x, p.y, p.z, p.getSize(), p.color, uvMap.get(p.getClass()), p.rotation);
                } else {
                    minecraft.particle(p.x, p.y, p.z, p.getSize(), p.color, uvMap.get(p.getClass()), p.rotation);
                }
                i++;
            }
        }
    }

    public void registerParticle(Class<? extends Particle> type, String path) {
        registry.put(type, path);
    }

    public void buildAtlas() {
        if (registry.isEmpty()) return;


        var decoded = new ArrayList<Decoded>();
        for (var entry : registry.entrySet()) {
            byte[] bytes = ResourceHelper.getBytes(entry.getValue());
            if (bytes == null) continue;

            var buf = MemoryUtil.memAlloc(bytes.length);
            buf.put(bytes).flip();

            try (var stack = MemoryStack.stackPush()) {
                var w = stack.mallocInt(1);
                var h = stack.mallocInt(1);
                var ch = stack.mallocInt(1);
                STBImage.stbi_set_flip_vertically_on_load(false);
                var pixels = STBImage.stbi_load_from_memory(buf, w, h, ch, 4);
                MemoryUtil.memFree(buf);
                if (pixels == null) continue;
                decoded.add(new Decoded(entry.getKey(), pixels, w.get(0), h.get(0)));
            }
        }

        if (decoded.isEmpty()) return;

        int atlasWidth = decoded.stream().mapToInt(Decoded::w).sum();
        int atlasHeight = decoded.stream().mapToInt(Decoded::h).max().orElse(0);

        ByteBuffer atlasBuffer = MemoryUtil.memCalloc(atlasWidth * atlasHeight * 4);

        int xCursor = 0;
        for (var d : decoded) {
            for (int row = 0; row < d.h(); row++) {
                long srcAddr = MemoryUtil.memAddress(d.pixels()) + (long) row * d.w() * 4;
                long dstAddr = MemoryUtil.memAddress(atlasBuffer) + ((long) row * atlasWidth + xCursor) * 4;
                MemoryUtil.memCopy(srcAddr, dstAddr, (long) d.w() * 4);
            }
            float u0 = (float) xCursor / atlasWidth;
            float u1 = (float) (xCursor + d.w()) / atlasWidth;
            float v1 = (float) d.h() / atlasHeight;
            uvMap.put(d.type(), new UV(u0, 0f, u1, v1));
            xCursor += d.w();
            STBImage.stbi_image_free(d.pixels());
        }

        atlas = CrossTexture.fromPixels(atlasBuffer, atlasWidth, atlasHeight);
        MemoryUtil.memFree(atlasBuffer);
    }

    private record Decoded(Class<? extends Particle> type, ByteBuffer pixels, int w, int h) {}
    public record UV(float minX, float minY, float maxX, float maxY) {}
}
