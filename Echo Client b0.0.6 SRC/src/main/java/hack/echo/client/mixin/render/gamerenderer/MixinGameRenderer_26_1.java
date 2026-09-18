package hack.echo.client.mixin.render.gamerenderer;

//? if ~26.1 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import hack.echo.client.Echo;
import hack.echo.client.event.impl.EventRender3D;
import hack.echo.client.mixininterface.IGameRenderer;
import hack.echo.client.api.ScreenRenderCompat;
import hack.echo.client.render2.api.CrossTexture;
import hack.echo.client.render2.impl.opengl.utils.RenderUtil;
import hack.echo.client.utils.VulkanUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.OptionalDouble;
import java.util.OptionalInt;

import static hack.echo.client.utils.Imports.mc;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer_26_1 implements IGameRenderer {

    @Shadow
    @Final
    private GameRenderState gameRenderState;

    @Shadow
    protected abstract void bobView(CameraRenderState cameraState, PoseStack poseStack);

    @Shadow
    protected abstract void bobHurt(CameraRenderState cameraState, PoseStack poseStack);

    @Inject(
            method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                    ordinal = 2
            )
    )
    private void onWorldRender(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        float worldPartialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = ((GameRenderer) (Object) this).getMainCamera();
        CameraRenderState cameraState = gameRenderState.levelRenderState.cameraRenderState;

        Matrix4f projectionMatrix = new Matrix4f(cameraState.projectionMatrix);
        PoseStack bobbingStack = new PoseStack();
        this.bobHurt(cameraState, bobbingStack);

        if (gameRenderState.optionsRenderState.bobView) {
            this.bobView(cameraState, bobbingStack);
        }

        projectionMatrix.mul(bobbingStack.last().pose());

        Matrix4f viewMatrix = new Matrix4f(cameraState.viewRotationMatrix);

        var draw3D = Echo.draw3d;
        draw3D.updateCamera(viewMatrix, projectionMatrix);
        draw3D.beginFrame();

        EventRender3D event = new EventRender3D(
                new PoseStack(),
                worldPartialTicks,
                deltaTracker.getGameTimeDeltaTicks(),
                camera,
                (GameRenderer) (Object) this,
                projectionMatrix,
                viewMatrix,
                minecraft.levelRenderer,
                draw3D
        );
        event.call();

        draw3D.endFrame();
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
        if (VulkanUtil.isVulkanLoaded()) return;
    }

    @Inject(
        method = "renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V",
        at = @At("HEAD")
    )
    private void echo$clearDepthBeforeHand(CameraRenderState cameraState, float deltaPartialTick, Matrix4fc modelViewMatrix, CallbackInfo ci) {
        if (!VulkanUtil.isVulkanLoaded()) return;

        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            () -> "echo_clear_depth",
            Minecraft.getInstance().getMainRenderTarget().getColorTextureView(),
            OptionalInt.empty(),
            Minecraft.getInstance().getMainRenderTarget().getDepthTextureView(),
            OptionalDouble.of(1.0)
        ).close();
    }
}
*///?} else {
public final class MixinGameRenderer_26_1 {
}
//?}
