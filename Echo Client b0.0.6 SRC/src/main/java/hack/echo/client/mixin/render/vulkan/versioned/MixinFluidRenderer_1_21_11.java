package hack.echo.client.mixin.render.vulkan.versioned;

//? if <=26.1.2 {
import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.XrayModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.vulkanmod.render.chunk.build.renderer.FluidRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FluidRenderer.class, remap = false)
public class MixinFluidRenderer_1_21_11 {

    @Inject(method = "renderLiquid", at = @At("HEAD"), cancellable = true)
    private void cancelFluidRender(BlockState blockState, FluidState fluidState, BlockPos blockPos, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        XrayModule xray = XrayModule.getActive();
        if (xray != null && xray.seeThroughLiquids.getValue()) {
            ci.cancel();
        }
    }
}
//?} else {
/*public final class MixinFluidRenderer_1_21_11 {
}
*///?}
