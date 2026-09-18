package hack.echo.client.mixin.render.vulkan;

import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.XrayModule;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.vulkanmod.render.chunk.build.task.BuildTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vulkan chunk build filtering hook for {@link hack.echo.client.features.impl.render.XrayModule}.
 */
@Mixin(value = BuildTask.class, remap = false)
public class MixinBuildTask {

    @Redirect(
        method = "compile",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getRenderShape()Lnet/minecraft/world/level/block/RenderShape;",
            remap = true
        )
    )
    private RenderShape filterBlockRenderShape(BlockState blockState) {
        if (Echo.isDestroyed) return blockState.getRenderShape();

        if (Echo.featureManager != null) {
            XrayModule xray = XrayModule.getActive();
            if (xray != null && !xray.shouldRenderBlock(blockState.getBlock())) {
                return RenderShape.INVISIBLE;
            }
        }
        return blockState.getRenderShape();
    }
}
