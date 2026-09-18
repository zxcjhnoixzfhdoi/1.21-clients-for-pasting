package hack.echo.client.render2.impl.vulkan.api;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import hack.echo.client.Echo;
import hack.echo.client.render3.impl.vk.post.VkBlur;
import hack.echo.client.vulkan.CommandBuffer;
import hack.echo.client.vulkan.api.BufferUsage;
import hack.echo.client.vulkan.api.VulkanBuffer;
import hack.echo.client.vulkan.descriptor.SimpleUBO;
import hack.echo.client.render2.api.CrossTexture;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Font;
import hack.echo.client.render2.impl.vulkan.impl.*;
import hack.echo.client.vulkan.graphics.VkShader;
import hack.echo.client.vulkan.memory.MemUtil;
import lombok.Getter;
import net.minecraft.util.ARGB;

//? if <= 26.1.2 {
import net.vulkanmod.vulkan.Renderer;
//?}
import org.joml.Matrix4f;

import java.util.*;

import static hack.echo.client.Echo.vulkanDevice;
import static hack.echo.client.utils.Imports.mc;
import static org.lwjgl.vulkan.VK10.*;

public class VkDraw2D extends Draw2D {

    private final VulkanBuffer projBuffer;

    private final SimpleUBO ubo;

    private final VkRoundedRect vkRoundedRect;
    private final VkImage vkImage;
    private final VkScreenImage vkScreenImage;
    private final VkLiquidGlass vkLiquidGlass;
    private final VkSBPicker vkSBPicker;
    private final VkHueSlider vkHueSlider;
    private final VkAlphaSlider vkAlphaSlider;
    private final VkText vkText;
    private final VkBlur vkBlur;
    @Getter
    private final List<VkShader> shaders = new ArrayList<>();


    private RenderPass renderPass;
    private CommandBuffer metal;

    private int lastFrameIndex = -1;
    private boolean inFrame = false;

    private final Queue<Runnable> postQueue = new ArrayDeque<>();

    private void addCall(Runnable runnable) {
        if (inFrame) runnable.run();
        else postQueue.add(runnable);
    }


    public VkDraw2D() {
        super();

        projBuffer = vulkanDevice.newBuffer(BufferUsage.UNIFORM, 64);
        ubo = new SimpleUBO(0, VK_SHADER_STAGE_VERTEX_BIT);
        ubo.buffer = projBuffer;

        vkRoundedRect = new VkRoundedRect(ubo);
        vkImage = new VkImage(ubo);
        vkScreenImage = new VkScreenImage(ubo);
        vkLiquidGlass = new VkLiquidGlass(ubo);
        vkSBPicker = new VkSBPicker(ubo);
        vkHueSlider = new VkHueSlider(ubo);
        vkAlphaSlider = new VkAlphaSlider(ubo);
        vkText = new VkText(ubo);
        vkBlur = new VkBlur();


        shaders.add(vkImage);
        shaders.add(vkScreenImage);
        shaders.add(vkSBPicker);
        shaders.add(vkHueSlider);
        shaders.add(vkAlphaSlider);
        shaders.add(vkRoundedRect);
        shaders.add(vkLiquidGlass);
        shaders.add(vkText);
    }

    @Override
    public void cleanup() {
        blurResult.cleanup();
        vkBlur.cleanup();

        shaders.forEach(VkShader::cleanup);
        shaders.clear();
    }


    @Override
    public void beginFrame(Matrix4f proj) {
        int currentFrame = Echo.vkContext.getCurrentFrame();
        long p = projBuffer.contents();

        MemUtil.putMat4(p, proj);
        metal = new CommandBuffer(Echo.vkContext.getCommandBuffer());

        var mainTarget = mc.getMainRenderTarget();

        //? if <= 26.1.2 {
        Renderer.getInstance().endRenderPass();
        //?}
        blurResult.vkImage = vkBlur.executeCompute(metal, CrossTexture.from(mainTarget), 3, 1);

        assert mainTarget.getColorTextureView() != null;
        renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Draw2D Begin",
                mainTarget.getColorTextureView(),
                OptionalInt.empty(),
                mainTarget.getDepthTextureView(),
                OptionalDouble.of(1.0)
        );

        if (lastFrameIndex != currentFrame) {
            lastFrameIndex = currentFrame;
            z = 0;
            shaders.forEach(VkShader::beginFrame);
        }
        inFrame = true;
    }

    @Override
    public void endFrame() {
        while (!postQueue.isEmpty()) {
            postQueue.poll().run();
        }
        shaders.forEach(e -> e.flush(metal));
        renderPass.close();
        inFrame = false;
    }

    @Override
    public void rect(Matrix4f mat, float x, float y, float w, float h, float radius, int color) {
        addCall(() -> vkRoundedRect.add(mat, x, y, w, h, radius, color));
    }

    @Override
    public void rect(Matrix4f mat, float x, float y, float w, float h, float r1, float r2, float r3, float r4,
            int color) {
        addCall(() -> vkRoundedRect.add(mat, x, y, w, h, r1, r2, r3, r4, color));
    }

    @Override
    public void rect(Matrix4f mat, float x, float y, float w, float h, float r1, float r2, float r3, float r4,
            int c1, int c2, int c3, int c4) {
        addCall(() -> vkRoundedRect.add(mat, x, y, w, h, r1, r2, r3, r4, c1, c2, c3, c4));
    }

    @Override
    public void text(Font font, Matrix4f model, CharSequence text, float x, float y, float size, int color) {
        addCall(() -> vkText.text(font, model, text, x, y, size, color));
    }

    @Override
    public void textWithShadow(Font font, Matrix4f model, CharSequence text, float x, float y, float size, int color) {
        int shadowColor = shadow(color);
        addCall(() -> vkText.text(font, model, text, x + 0.75f, y + 0.75f, size, shadowColor));
        addCall(() -> vkText.text(font, model, text, x, y, size, color));
    }

    @Override
    public void text(Font font, Matrix4f model, CharSequence text, float x, float y, float size, int c1, int c2, int c3, int c4) {
        addCall(() -> vkText.text(font, model, text, x, y, size, c1, c2, c3, c4));
    }

    @Override
    public void textWithShadow(Font font, Matrix4f model, CharSequence text, float x, float y, float size, int c1, int c2, int c3, int c4) {
        int sc1 = shadow(c1), sc2 = shadow(c2), sc3 = shadow(c3), sc4 = shadow(c4);
        addCall(() -> vkText.text(font, model, text, x + 0.75f, y + 0.75f, size, sc1, sc2, sc3, sc4));
        addCall(() -> vkText.text(font, model, text, x, y, size, c1, c2, c3, c4));
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
        addCall(() -> vkImage.add(model, x, y, w, h, radius, radius, radius, radius, texture, alpha));
    }

    @Override
    public void screenImage(Matrix4f model, CrossTexture texture, float x, float y, float w, float h, float radius,
            float alpha) {
        addCall(() -> vkScreenImage.add(model, x, y, w, h, radius, radius, radius, radius, texture, alpha));
    }

    @Override
    public void liquidGlass(Matrix4f model, float x, float y, float w, float h, float radius, int tint, float refractionStrength) {
        addCall(() -> vkLiquidGlass.add(model, x, y, w, h, radius, radius, radius, radius, tint, refractionStrength));
    }

    @Override
    public void sbPicker(Matrix4f model, float x, float y, float w, float h, float hue) {
        addCall(() -> vkSBPicker.add(model, x, y, w, h, hue));
    }

    @Override
    public void hueSlider(Matrix4f model, float x, float y, float w, float h) {
        addCall(() -> vkHueSlider.add(model, x, y, w, h));
    }

    @Override
    public void alphaSlider(Matrix4f model, float x, float y, float w, float h, float r, float g, float b) {
        addCall(() -> vkAlphaSlider.add(model, x, y, w, h, r, g, b));
    }

    @Override
    public void flush() {
        addCall(() -> shaders.forEach(e -> e.flush(metal)));
    }

    @Override
    public void pushScissor(float x, float y, float width, float height) {
        addCall(() -> {
            this.flush();

            var rect = Echo.vkContext.getScissorRect(x, y, width, height);
            if (!scissorStack.isEmpty()) {
                rect = intersect(scissorStack.peek(), rect);
            }

            scissorStack.push(rect);
            metal.scissor(rect.x, rect.y, rect.z, rect.w);
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
                metal.scissor(rect.x, rect.y, rect.z, rect.w);
            } else {
                var window = mc.getWindow();
                metal.scissor(0, 0, window.getWidth(), window.getHeight());
            }
        });
    }

}
