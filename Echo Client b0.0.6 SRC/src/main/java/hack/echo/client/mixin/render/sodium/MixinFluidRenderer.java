package hack.echo.client.mixin.render.sodium;

import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.XrayModule;
import net.caffeinemc.mods.sodium.fabric.render.FluidRendererImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sodium fluid visibility hook for {@link hack.echo.client.features.impl.render.XrayModule}.
 */
@Mixin(value = FluidRendererImpl.class, remap = false)
public class MixinFluidRenderer {

    @Inject(
        method = "render",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cancelFluidRender(LevelSlice level, BlockState blockState, FluidState fluidState, BlockPos blockPos, BlockPos offset, TranslucentGeometryCollector collector, ChunkBuildBuffers buffers, CallbackInfo ci) {
        if (Echo.isDestroyed) return;
        if (Echo.featureManager == null) return;
        
        XrayModule xray = XrayModule.getActive();
        if (xray != null && xray.seeThroughLiquids.getValue()) {
            ci.cancel();
        }
    }
}
