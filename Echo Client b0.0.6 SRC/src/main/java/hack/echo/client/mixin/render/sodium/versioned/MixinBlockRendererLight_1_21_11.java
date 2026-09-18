package hack.echo.client.mixin.render.sodium.versioned;

//? if <26.1 {
import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.XrayModule;
import hack.echo.client.api.LightmapCompat;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.SodiumShadeMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractBlockRenderContext.class, remap = false)
public class MixinBlockRendererLight_1_21_11 {

    @Shadow
    private BlockState state;

    @Shadow
    protected QuadLightData quadLightData;

    @Inject(method = "shadeQuad", at = @At("TAIL"))
    private void echo$modifyLight(
            MutableQuadViewImpl quad,
            LightMode lightMode,
            boolean emissive,
            SodiumShadeMode shadeMode,
            CallbackInfo ci
    ) {
        if (Echo.isDestroyed) return;

        if (Echo.featureManager == null || this.state == null) {
            return;
        }

        XrayModule xray = XrayModule.getActive();
        if (xray == null) {
            return;
        }

        if (!xray.shouldRenderBlock(this.state.getBlock())) {
            return;
        }

        for (int i = 0; i < 4; i++) {
            quad.setLight(i, LightmapCompat.FULL_BRIGHT);
            this.quadLightData.lm[i] = LightmapCompat.FULL_BRIGHT;
            this.quadLightData.br[i] = 1.0f;
        }
    }
}
//?} else {
/*public final class MixinBlockRendererLight_1_21_11 {
}
*///?}
