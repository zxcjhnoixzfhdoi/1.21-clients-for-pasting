package hack.echo.client.mixin.render.indigo.versioned;

//? if <26.1 {
import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.XrayModule;
import hack.echo.client.utils.SodiumCompat;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockRenderInfo.class, remap = false)
public class MixinBlockRenderInfo_1_21_11 {

    @Shadow public BlockAndTintGetter blockView;
    @Shadow public BlockPos blockPos;
    @Shadow private BlockPos.MutableBlockPos searchPos;
    @Shadow public BlockState blockState;

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private void echo$drawXrayFacesAgainstHiddenNeighbors(Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (Echo.isDestroyed) return;

        if (side == null || SodiumCompat.isSodiumLoaded() || Echo.featureManager == null) {
            return;
        }

        XrayModule xray = XrayModule.getActive();
        if (xray == null || !xray.shouldRenderBlock(this.blockState.getBlock())) {
            return;
        }

        BlockState neighborState = this.blockView.getBlockState(this.searchPos.setWithOffset(this.blockPos, side));
        if (!xray.shouldRenderBlock(neighborState.getBlock())) {
            cir.setReturnValue(true);
        }
    }
}
//?} else {
/*public final class MixinBlockRenderInfo_1_21_11 {
}
*///?}
