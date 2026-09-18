package hack.echo.client.render2.impl.opengl.api;

import com.mojang.blaze3d.platform.Window;
import hack.echo.client.Echo;
import hack.echo.client.mixin.accessors.MinecraftAccessor;
import hack.echo.client.render2.api.CrossTexture;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Font;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.render2.impl.opengl.immediate.GlRoundedImage;
import hack.echo.client.render2.impl.opengl.immediate.GlRoundedScreenImage;
import hack.echo.client.render2.impl.opengl.instanced.GlAlphaSlider;
import hack.echo.client.render2.impl.opengl.instanced.GlHueSlider;
import hack.echo.client.render2.impl.opengl.instanced.GlLiquidGlass;
import hack.echo.client.render2.impl.opengl.instanced.GlRoundedRect;
import hack.echo.client.render2.impl.opengl.instanced.GlSBPicker;
import hack.echo.client.render2.impl.opengl.post.GlBlur;
import hack.echo.client.render2.impl.opengl.utils.RenderUtil;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;
import org.joml.Vector4i;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static hack.echo.client.utils.Imports.mc;
import static org.lwjgl.opengl.GL33.*;

public class GlDraw2D extends Draw2D {

    private final int ubo;

    // Instanced
    private final GlRoundedRect roundedRect;
    private final GlSBPicker sbPicker;
    private final GlHueSlider hueSlider;
    private final GlAlphaSlider alphaSlider;
    private final GlLiquidGlass liquidGlass;
    private final List<GlInstancedShader> shaders = new ArrayList<>();

    // Immediate
    private final GlRoundedScreenImage roundedScreenImage;
    private final GlRoundedImage roundedImage;

    private final GlBlur glBlur;

    private GlState state;

    private final Queue<Runnable> postQueue = new ArrayDeque<>();
    private boolean inFrame = false;

    void addCall(Runnable runnable) {
        if (inFrame) runnable.run();
        else postQueue.add(runnable);
    }

    private int lastFrameIndex = -1;
    public GlDraw2D() {
        roundedImage = new GlRoundedImage();
        roundedRect = new GlRoundedRect();
        roundedScreenImage = new GlRoundedScreenImage();
        sbPicker = new GlSBPicker();
        alphaSlider = new GlAlphaSlider();
        hueSlider = new GlHueSlider();
        liquidGlass = new GlLiquidGlass();
        glBlur = new GlBlur();


        shaders.add(roundedRect);
        shaders.add(hueSlider);
        shaders.add(alphaSlider);
        shaders.add(sbPicker);
        shaders.add(liquidGlass);

        ubo = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, ubo);
        glBufferData(GL_UNIFORM_BUFFER, 64L, GL_DYNAMIC_DRAW);
        glBindBufferBase(GL_UNIFORM_BUFFER, cameraUboBinding, ubo);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);

        shaders.add(Fonts.lucide.getGlText());
        shaders.add(Fonts.arial.getGlText());
        shaders.add(Fonts.interSemiBold.getGlText());
        shaders.add(Fonts.inter.getGlText());
    }

    @Override
    public void cleanup() {
        shaders.forEach(GlInstancedShader::cleanup);
        roundedImage.cleanup();
        roundedScreenImage.cleanup();
        glDeleteBuffers(ubo);
    }

    public void computeBlur() {
        var target = mc.getMainRenderTarget();
        if (target == null || target.width <= 0 || target.height <= 0) return;
        var src = CrossTexture.from(target);
        this.blurResult.glId = glBlur.blur(src, target.width, target.height, 3, 1f);
    }

    @Override
    public void beginFrame(Matrix4f proj) {
        RenderUtil.bindMainFramebuffer();
        this.state = new GlState();
        this.updateProjection(proj);

        int currentFrameIndex = ((MinecraftAccessor) mc).getFrames();

        if (currentFrameIndex != lastFrameIndex) {
            lastFrameIndex = currentFrameIndex;
            z = 0f;
        }

        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glEnable(GL_DEPTH_TEST);
        //? if > 26.1.2 {
        /*glDepthFunc(GL_GEQUAL);
        *///?} else {
        glDepthFunc(GL_LEQUAL);
        //?}
        inFrame = true;
    }

    @Override
    public void endFrame() {
        while (!postQueue.isEmpty()) {
            postQueue.poll().run();
        }
        shaders.forEach(GlInstancedShader::flush);
        glEnable(GL_CULL_FACE);
        state.restore();
        inFrame = false;
    }

    private void updateProjection(Matrix4f projection) {
        glBindBuffer(GL_UNIFORM_BUFFER, ubo);
        ByteBuffer mapped = glMapBufferRange(
                GL_UNIFORM_BUFFER,
                0,
                64L,
                GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
        if (mapped == null) {
            return;
        }

        projection.get(0, mapped);
        glUnmapBuffer(GL_UNIFORM_BUFFER);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
        glBindBufferBase(GL_UNIFORM_BUFFER, cameraUboBinding, ubo);
    }

    @Override
    public void rect(Matrix4f model, float x, float y, float w, float h, float radius, int color) {
        addCall(() -> roundedRect.addRect(model, x, y, w, h, radius, color));
    }

    @Override
    public void rect(Matrix4f model, float x, float y, float w, float h, float r1, float r2, float r3, float r4,
            int color) {
        addCall(() -> roundedRect.addRect(model, x, y, w, h, r1, r2, r3, r4, color));
    }

    @Override
    public void rect(Matrix4f model, float x, float y, float w, float h, float r1, float r2, float r3, float r4,
            int c1, int c2, int c3, int c4) {
        addCall(() -> roundedRect.addRect(model, x, y, w, h, r1, r2, r3, r4, c1, c2, c3, c4));
    }

    @Override
    public void text(Font font, Matrix4f model, CharSequence text, float x, float y, float size, int color) {
        addCall(() -> font.getGlText().text(model, text, x, y, size, color));
    }

    @Override
    public void text(Font font, Matrix4f model, CharSequence text, float x, float y, float size, int c1, int c2, int c3, int c4) {
        addCall(() -> font.getGlText().text(model, text, x, y, size, c1, c2, c3, c4));
    }

    @Override
    public void textWithShadow(Font font, Matrix4f model, CharSequence text, float x, float y, float size, int color) {
        int shadowColor = shadow(color);
        addCall(() -> {
            font.getGlText().text(model, text, x + 0.75f, y + 0.75f, size, shadowColor);
            font.getGlText().text(model, text, x, y, size, color);
        });
    }

    @Override
    public void textWithShadow(Font font, Matrix4f model, CharSequence text, float x, float y, float size, int c1, int c2, int c3, int c4) {
        int sc1 = shadow(c1), sc2 = shadow(c2), sc3 = shadow(c3), sc4 = shadow(c4);
        addCall(() -> {
            font.getGlText().text(model, text, x + 0.75f, y + 0.75f, size, sc1, sc2, sc3, sc4);
            font.getGlText().text(model, text, x, y, size, c1, c2, c3, c4);
        });
    }

    private static int shadow(int color) {
        int alpha = ARGB.alpha(color);
        int red = Math.round(ARGB.red(color) * 0.25f);
        int green = Math.round(ARGB.green(color) * 0.25f);
        int blue = Math.round(ARGB.blue(color) * 0.25f);
        return ARGB.color(alpha, red, green, blue);
    }

    @Override
    public void image(Matrix4f model, CrossTexture texture, float x, float y, float w, float h, float radius,
            float alpha) {
        addCall(() -> roundedImage.draw(model, x, y, w, h, radius, radius, radius, radius, texture.glId, alpha));
    }

    @Override
    public void screenImage(Matrix4f model, CrossTexture texture, float x, float y, float w, float h, float radius, float alpha) {
        addCall(() -> roundedScreenImage.draw(model, x, y, w, h, radius, radius, radius, radius, texture.glId,  alpha));
    }

    @Override
    public void liquidGlass(Matrix4f model, float x, float y, float w, float h, float radius, int tint, float refractionStrength) {
        addCall(() -> liquidGlass.add(model, x, y, w, h, radius, radius, radius, radius, tint, refractionStrength));
    }

    @Override
    public void sbPicker(Matrix4f model, float x, float y, float w, float h, float hue) {
        addCall(() -> sbPicker.picker(model, x, y, w, h, hue));
    }

    @Override
    public void hueSlider(Matrix4f model, float x, float y, float w, float h) {
        addCall(() -> hueSlider.slider(model, x, y, w, h));
    }

    @Override
    public void alphaSlider(Matrix4f model, float x, float y, float w, float h, float r, float g, float b) {
        addCall(() -> alphaSlider.slider(model, x, y, w, h, r, g, b));
    }

    @Override
    public void flush() {
        addCall(() -> {
            for (GlInstancedShader shader : this.shaders) {
                shader.flush();
            }
        });
    }

    @Override
    public void pushScissor(float x, float y, float width, float height) {
        addCall(() -> {
            this.flush();
            float scale = 2f;
            float sx = x * scale;
            float sy = (RenderUtil.getScaledHeight() - y - height) * scale;
            float swidth = width * scale;
            float sheight = height * scale;
            Vector4i rect = new Vector4i((int) sx, (int) sy, (int) swidth, (int) sheight);
            if (!scissorStack.isEmpty()) {
                rect = intersect(scissorStack.peek(), rect);
            }

            scissorStack.push(rect);
            glEnable(GL_SCISSOR_TEST);
            glScissor(rect.x, rect.y, rect.z, rect.w);
        });
    }

    @Override
    public void popScissor() {
        addCall(() -> {
            this.flush();

            if (!scissorStack.isEmpty()) {
                scissorStack.pop();
            }

            if (!scissorStack.isEmpty()) {
                var rect = scissorStack.peek();
                glScissor(rect.x, rect.y, rect.z, rect.w);
            } else {
                glDisable(GL_SCISSOR_TEST);
            }
        });
    }
}
