package hack.echo.client.mixin.render.vulkan;

import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.XrayModule;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.vulkanmod.render.chunk.build.frapi.render.AbstractBlockRenderContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vulkan face occlusion override hook for {@link hack.echo.client.features.impl.render.XrayModule}.
 */
@Mixin(value = AbstractBlockRenderContext.class, remap = false)
public class MixinAbstractBlockRenderContextOcclusion {

    @Inject(method = "faceNotOccluded", at = @At("HEAD"), cancellable = true)
    private void overrideOcclusionForXrayBlocks(BlockState selfBlockState, Direction facing, CallbackInfoReturnable<Boolean> cir) {
        if (Echo.isDestroyed) return;

        XrayModule xray = XrayModule.getActive();
        if (xray != null && xray.shouldRenderBlock(selfBlockState.getBlock())) {
            cir.setReturnValue(true);
        }
    }
}
