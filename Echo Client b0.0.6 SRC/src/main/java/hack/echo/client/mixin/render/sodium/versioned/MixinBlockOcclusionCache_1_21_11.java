package hack.echo.client.mixin.render.sodium.versioned;

//? if <26.1 {
import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.XrayModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache", remap = false)
public class MixinBlockOcclusionCache_1_21_11 {

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private void overrideOcclusionForXrayBlocks(BlockState selfBlockState, BlockGetter view, BlockPos selfPos, Direction facing, CallbackInfoReturnable<Boolean> cir) {
        if (Echo.isDestroyed) return;

        XrayModule xray = XrayModule.getActive();
        if (xray != null && xray.shouldRenderBlock(selfBlockState.getBlock())) {
            cir.setReturnValue(true);
        }
    }
}
//?} else {
/*public final class MixinBlockOcclusionCache_1_21_11 {
}
*///?}
