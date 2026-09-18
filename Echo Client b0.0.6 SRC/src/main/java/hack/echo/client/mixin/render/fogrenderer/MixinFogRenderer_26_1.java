package hack.echo.client.mixin.render.fogrenderer;

//? if >=26.1 {
/*import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.AmbienceMod;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static hack.echo.client.utils.Imports.mc;

@Mixin(FogRenderer.class)
public class MixinFogRenderer_26_1 {

    @Inject(method = "setupFog", at = @At("RETURN"), cancellable = true)
    private void echo$modifyFogState(Camera camera, int viewDistance, DeltaTracker tickCounter, float skyDarkness, ClientLevel world, CallbackInfoReturnable<FogData> cir) {
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

        FogData fog = cir.getReturnValue();
        if (fog == null) return;

        fog.color.set(r, g, b, 1.0f);
        fog.environmentalEnd = fogEnd;
        fog.skyEnd = fogEnd;
        fog.cloudEnd = fogEnd;
    }
}
*///?} else {
public final class MixinFogRenderer_26_1 {
}
//?}
