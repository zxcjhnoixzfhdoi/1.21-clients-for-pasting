package hack.echo.client.mixin.render.skyrenderer;

//? if <26.1 {
import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.AmbienceMod;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static hack.echo.client.utils.Imports.mc;

@Mixin(SkyRenderer.class)
public class MixinSkyRenderer_1_21_11 {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void echo$applyAmbienceSkyColor(ClientLevel world, float tickProgress, Camera camera, SkyRenderState state, CallbackInfo ci) {
        if (Echo.isDestroyed) return;
        if (mc.player == null || mc.level == null) return;
        if (Echo.featureManager == null) return;

        AmbienceMod ambience = Echo.featureManager.getFeatureByClass(AmbienceMod.class);
        if (ambience == null || !ambience.isEnabled() || !AmbienceMod.isChangeFogColorEnabled()) return;

        state.skyColor = ARGB.color(
            AmbienceMod.getFogColorR(),
            AmbienceMod.getFogColorG(),
            AmbienceMod.getFogColorB()
        );

        if (AmbienceMod.isFogGradientEnabled()) {
            state.sunriseAndSunsetColor = ARGB.color(
                255,
                AmbienceMod.getFogGradientColorR(),
                AmbienceMod.getFogGradientColorG(),
                AmbienceMod.getFogGradientColorB()
            );
        } else {
            state.sunriseAndSunsetColor = 0;
        }

        state.shouldRenderDarkDisc = false;
    }
}
//?} else {
/*public final class MixinSkyRenderer_1_21_11 {
}
*///?}
