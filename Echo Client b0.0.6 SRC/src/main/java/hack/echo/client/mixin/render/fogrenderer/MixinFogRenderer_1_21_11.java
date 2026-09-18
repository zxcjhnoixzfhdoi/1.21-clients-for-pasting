package hack.echo.client.mixin.render.fogrenderer;

//? if <26.1 {
import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.AmbienceMod;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static hack.echo.client.utils.Imports.mc;

@Mixin(FogRenderer.class)
public class MixinFogRenderer_1_21_11 {

    @Inject(method = "setupFog", at = @At("RETURN"), cancellable = true)
    private void echo$modifyFogColor(Camera camera, int viewDistance, DeltaTracker tickCounter, float skyDarkness, ClientLevel world, CallbackInfoReturnable<Vector4f> cir) {
        if (Echo.isDestroyed) return;
        if (mc.player == null || mc.level == null) return;
        if (Echo.featureManager == null) return;

        AmbienceMod ambience = Echo.featureManager.getFeatureByClass(AmbienceMod.class);
        if (ambience == null || !ambience.isEnabled()) return;
        if (!AmbienceMod.isChangeFogColorEnabled()) return;

        float r = AmbienceMod.getFogColorR() / 255.0f;
        float g = AmbienceMod.getFogColorG() / 255.0f;
        float b = AmbienceMod.getFogColorB() / 255.0f;
        cir.setReturnValue(new Vector4f(r, g, b, 1.0f));
    }

    @ModifyArgs(
            method = "setupFog",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"
            )
    )
    private void echo$modifyFogDistances(Args args) {
        if (Echo.isDestroyed) return;
        if (mc.player == null || mc.level == null) return;
        if (Echo.featureManager == null) return;

        AmbienceMod ambience = Echo.featureManager.getFeatureByClass(AmbienceMod.class);
        if (ambience == null || !ambience.isEnabled()) return;
        if (!AmbienceMod.isChangeFogColorEnabled()) return;

        float r = AmbienceMod.getFogColorR() / 255.0f;
        float g = AmbienceMod.getFogColorG() / 255.0f;
        float b = AmbienceMod.getFogColorB() / 255.0f;
        float fogEnd = AmbienceMod.getFogRenderDistance() * 16.0f;

        args.set(2, new Vector4f(r, g, b, 1.0f));
        args.set(4, fogEnd);
        args.set(7, fogEnd);
        args.set(8, fogEnd);
    }
}
//?} else {
/*public final class MixinFogRenderer_1_21_11 {
}
*///?}
