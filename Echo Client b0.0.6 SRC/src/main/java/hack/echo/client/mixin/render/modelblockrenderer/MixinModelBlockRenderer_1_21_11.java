package hack.echo.client.mixin.render.modelblockrenderer;

//? if <26.1 {
import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.XrayModule;
import hack.echo.client.utils.SodiumCompat;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.block.ModelBlockRenderer$Cache")
public class MixinModelBlockRenderer_1_21_11 {

    @Inject(method = "getLightColor", at = @At("RETURN"), cancellable = true)
    private void modifyLightColor(BlockState blockState, BlockAndTintGetter blockAndTintGetter, BlockPos blockPos, CallbackInfoReturnable<Integer> cir) {
        if (Echo.isDestroyed) return;

        if (SodiumCompat.isSodiumLoaded()) {
            return;
        }

        if (Echo.featureManager == null) return;

        XrayModule xray = XrayModule.getActive();
        if (xray != null && xray.shouldRenderBlock(blockState.getBlock())) {
            cir.setReturnValue(LightTexture.FULL_BRIGHT);
        }
    }
}
//?} else {
/*public final class MixinModelBlockRenderer_1_21_11 {
}
*///?}
