package hack.echo.client.mixin.render.guirenderer;

//? if ~26.1 {
/*import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.vertex.PoseStack;
import hack.echo.client.Echo;
import hack.echo.client.event.impl.EventRender2DGui;
import hack.echo.client.api.GuiGraphicsCompat;
import hack.echo.client.api.ScreenRenderCompat;
import hack.echo.client.render2.api.CrossTexture;
import hack.echo.client.render2.impl.opengl.utils.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public class MixinGuiRenderer_26_1 {

    @Shadow
    @Final
    private GuiRenderState renderState;

    private GuiRenderState echoRenderState;
    private GuiGraphicsExtractor echoGraphics;

    @Inject(
            method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;prepare()V"
            )
    )
    private void echo$renderGui2D(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        if (hack.echo.client.api.MinecraftCompat.getScreen() instanceof ScreenRenderCompat) {
            int mouseX = (int) minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
            int mouseY = (int) minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
            GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(minecraft, renderState, mouseX, mouseY);

            var draw = Echo.draw2D;
            draw.beginFrame(RenderUtil.getProjectionMatrix());
            CrossTexture blurTexture = draw.getBlurResult();

            EventRender2DGui event = new EventRender2DGui(
                    new PoseStack(),
                    minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true),
                    GuiGraphicsCompat.of(graphics),
                    draw,
                    blurTexture
            );
            event.call();
            draw.endFrame();
            return;
        }

        echoRenderState = new GuiRenderState();
        int mouseX = (int) minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
        int mouseY = (int) minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
        echoGraphics = new GuiGraphicsExtractor(minecraft, echoRenderState, mouseX, mouseY);

        var draw = Echo.draw2D;
        draw.beginFrame(RenderUtil.getProjectionMatrix());
        CrossTexture blurTexture = draw.getBlurResult();

        EventRender2DGui event = new EventRender2DGui(
                new PoseStack(),
                minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true),
                GuiGraphicsCompat.of(echoGraphics),
                draw,
                blurTexture
        );
        event.call();
        draw.endFrame();
    }

    @Inject(
            method = "prepare()V",
            at = @At("HEAD")
    )
    private void echo$injectStratum(CallbackInfo ci) {
        if (Echo.isDestroyed) {
            echoRenderState = null;
            echoGraphics = null;
            return;
        }

        if (echoRenderState == null || echoRenderState.strata.isEmpty()) {
            return;
        }

        var echoNode = echoRenderState.strata.get(0);
        renderState.strata.add(1, echoNode);
        renderState.itemModelIdentities.addAll(echoRenderState.itemModelIdentities);

        echoRenderState = null;
        echoGraphics = null;
    }
}
*///?} else {
public final class MixinGuiRenderer_26_1 {
}
//?}
