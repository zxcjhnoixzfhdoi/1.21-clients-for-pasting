package we.devs.opium.asm.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import we.devs.opium.client.modules.visuals.ModuleNoRender;

@Mixin(InGameOverlayRenderer.class)
public class GameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void onRenderFireOverlay(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        if (ModuleNoRender.INSTANCE.noFire()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true)
    private static void cancelInWallOverlay(Sprite sprite, MatrixStack matrices, CallbackInfo ci) {
        if (ModuleNoRender.INSTANCE.noBlock()) {
            ci.cancel();
        }
    }
}
