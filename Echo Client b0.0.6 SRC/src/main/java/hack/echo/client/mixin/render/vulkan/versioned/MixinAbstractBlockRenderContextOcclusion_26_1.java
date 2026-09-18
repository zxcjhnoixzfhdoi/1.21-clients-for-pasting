package hack.echo.client.mixin.render.vulkan.versioned;

import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.XrayModule;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * VulkanMod 0.6.5 moved the block render context out of the FRAPI package.
 */
@Mixin(targets = "net.vulkanmod.render.chunk.build.renderer.AbstractBlockRenderContext", remap = false)
public class MixinAbstractBlockRenderContextOcclusion_26_1 {

    @Inject(method = "faceNotOccluded", at = @At("HEAD"), cancellable = true)
    private void overrideOcclusionForXrayBlocks(BlockState selfBlockState, Direction facing, CallbackInfoReturnable<Boolean> cir) {
        if (Echo.isDestroyed) return;

        XrayModule xray = XrayModule.getActive();
        if (xray != null && xray.shouldRenderBlock(selfBlockState.getBlock())) {
            cir.setReturnValue(true);
        }
    }
}
