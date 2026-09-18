package hack.echo.client.mixin.render.indigo.versioned;

//? if <26.1 {
import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.XrayModule;
import hack.echo.client.utils.SodiumCompat;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.TerrainRenderContext;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TerrainRenderContext.class, remap = false)
public class MixinTerrainRenderContext_1_21_11 {

    @Inject(method = "bufferModel", at = @At("HEAD"), cancellable = true)
    private void echo$hideNonSelectedBlocksForXray(BlockStateModel model, BlockState state, BlockPos pos, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        if (SodiumCompat.isSodiumLoaded() || Echo.featureManager == null) {
            return;
        }

        XrayModule xray = XrayModule.getActive();
        if (xray != null && !xray.shouldRenderBlock(state.getBlock())) {
            ci.cancel();
        }
    }
}
//?} else {
/*public final class MixinTerrainRenderContext_1_21_11 {
}
*///?}
