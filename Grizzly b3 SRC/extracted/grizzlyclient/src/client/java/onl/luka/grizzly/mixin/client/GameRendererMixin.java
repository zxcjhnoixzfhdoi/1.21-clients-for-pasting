package onl.luka.grizzly.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import onl.luka.grizzly.MedvedClient;
import onl.luka.grizzly.config.ConfigManager;
import onl.luka.grizzly.module.modules.combat.Misplace;
import onl.luka.grizzly.module.modules.render.ESP3D;
import onl.luka.grizzly.util.CameraOverriddenEntity;
import onl.luka.grizzly.util.RotationManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class)
public class GameRendererMixin {

    @Shadow
    private void bobView(CameraRenderState cameraState, PoseStack poseStack) {
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void medved$pollFrameKeybinds(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        ConfigManager.pollKeybinds();
        MedvedClient.pollFrameInput();
        Misplace.applyForFrame();
    }

    @Redirect(method = "renderItemInHand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"
    ))
    private void medved$skipPerspectiveHandBob(GameRenderer instance, CameraRenderState cameraState, PoseStack poseStack) {
        if (!RotationManager.perspective) {
            this.bobView(cameraState, poseStack);
        }
    }

    @Inject(method = "renderLevel", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V"
    ))
    private void medved$blitGlowBeforeHand(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ESP3D.usesGlowOutline()) {
            Minecraft.getInstance().levelRenderer.doEntityOutline();
        }
    }

    @Redirect(method = "render", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V"
    ))
    private void medved$skipLateGlowBlit(LevelRenderer levelRenderer) {
        if (!ESP3D.usesGlowOutline()) {
            levelRenderer.doEntityOutline();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        Misplace.restoreAfterFrame();
        Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();

        if (RotationManager.perspective && camera.entity() instanceof LocalPlayer) {
            CameraOverriddenEntity e = (CameraOverriddenEntity) camera.entity();

            ((CameraAccessor)camera).medved$setRotation(
                    e.medved$getCameraYaw(),
                    e.medved$getCameraPitch()
            );
        }
    }
}
