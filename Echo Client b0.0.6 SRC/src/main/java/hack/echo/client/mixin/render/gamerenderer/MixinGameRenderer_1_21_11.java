package hack.echo.client.mixin.render.gamerenderer;

//? if <26.1 {
import com.llamalad7.mixinextras.sugar.Local;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import hack.echo.client.Echo;
import hack.echo.client.event.impl.EventRender3D;
import hack.echo.client.mixininterface.IGameRenderer;
import hack.echo.client.api.GuiGraphicsCompat;
import hack.echo.client.render2.api.CrossTexture;
import hack.echo.client.render2.impl.opengl.utils.RenderUtil;
import hack.echo.client.utils.VulkanUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.vulkanmod.vulkan.Renderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static hack.echo.client.utils.Imports.mc;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer_1_21_11 implements IGameRenderer {

    @Shadow
    @Final
    private FogRenderer fogRenderer;

    @Shadow
    @Final
    private GuiRenderer guiRenderer;

    @Shadow
    protected abstract void bobView(PoseStack matrices, float tickProgress);

    @Shadow
    protected abstract void bobHurt(PoseStack matrices, float tickProgress);

    @Inject(
            method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                    ordinal = 2
            )
    )
    private void onWorldRender(
            DeltaTracker tickCounter,
            CallbackInfo ci,
            @Local(ordinal = 0) float tickProgress,
            @Local(ordinal = 0) Matrix4f projectionMatrix,
            @Local(ordinal = 1) Matrix4f viewMatrix,
            @Local PoseStack matrices
    ) {
        if (Echo.isDestroyed || Echo.draw3d == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = ((GameRenderer) (Object) this).getMainCamera();

        RenderSystem.getModelViewStack().pushMatrix().mul(viewMatrix);

        PoseStack cancelBobbingStack = new PoseStack();
        this.echo$undoCameraBobbing(cancelBobbingStack, tickProgress);
        RenderSystem.getModelViewStack().mul(cancelBobbingStack.last().pose().invert());

        var draw3D = Echo.draw3d;
        draw3D.updateCamera(viewMatrix, projectionMatrix);
        draw3D.beginFrame();

        EventRender3D event = new EventRender3D(
                matrices,
                tickProgress,
                tickCounter.getGameTimeDeltaTicks(),
                camera,
                (GameRenderer) (Object) this,
                projectionMatrix,
                viewMatrix,
                minecraft.levelRenderer,
                draw3D
        );
        event.call();

        draw3D.endFrame();
        RenderSystem.getModelViewStack().popMatrix();
    }

    @Inject(method = "resize", at = @At("HEAD"), cancellable = true)
    private void onResize(int width, int height, CallbackInfo ci) {
        if (mc == null) {
            return;
        }

        if (hack.echo.client.api.MinecraftCompat.getOverlay() == null) {
            return;
        }

        ci.cancel();
    }

    @Override
    public void echo$flushGuiState() {
        if (VulkanUtil.isVulkanLoaded()) {
            return;
        }

        guiRenderer.render(fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
    }

    @Inject(
            method = "renderItemInHand(FZLorg/joml/Matrix4f;)V",
            at = @At("HEAD")
    )
    private void echo$clearDepthBeforeHand(float tickDelta, boolean bl, Matrix4f modelViewMatrix, CallbackInfo ci) {
        if (!VulkanUtil.isVulkanLoaded()) return;

        Renderer.getInstance().endRenderPass();

        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "echo_clear_depth",
                Minecraft.getInstance().getMainRenderTarget().getColorTextureView(),
                OptionalInt.empty(),
                Minecraft.getInstance().getMainRenderTarget().getDepthTextureView(),
                OptionalDouble.of(1.0)
        ).close();
    }

    private void echo$undoCameraBobbing(PoseStack cancelBobbingStack, float tickProgress) {
        this.bobHurt(cancelBobbingStack, tickProgress);

        if (!mc.options.bobView().get()) {
            return;
        }

        this.bobView(cancelBobbingStack, tickProgress);
    }
}
//?} else {
/*public final class MixinGameRenderer_1_21_11 {
}
*///?}
