package hack.echo.client.mixin.render.sodium.versioned;

//? if <26.1 {
import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.XrayModule;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockRenderer.class, remap = false)
public class MixinBlockRenderer_1_21_11 {

    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true)
    private void filterBlockRender(BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
        if (Echo.isDestroyed) return;
        if (Echo.featureManager == null) return;

        XrayModule xray = XrayModule.getActive();
        if (xray != null && !xray.shouldRenderBlock(state.getBlock())) {
            ci.cancel();
        }
    }
}
//?} else {
/*public final class MixinBlockRenderer_1_21_11 {
}
*///?}
