package hack.echo.client.mixin.render;

import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.XrayModule;
import hack.echo.client.utils.SodiumCompat;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla section occlusion data hook for {@link hack.echo.client.features.impl.render.XrayModule}.
 */
@Mixin(SectionCompiler.class)
public class MixinSectionCompiler {

    @Redirect(
        method = "compile",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;isSolidRender()Z"
        )
    )
    private boolean echo$disableHiddenBlockOcclusion(BlockState blockState) {
        if (Echo.isDestroyed) return blockState.isSolidRender();

        if (SodiumCompat.isSodiumLoaded() || Echo.featureManager == null) {
            return blockState.isSolidRender();
        }

        XrayModule xray = XrayModule.getActive();
        if (xray != null && !xray.shouldRenderBlock(blockState.getBlock())) {
            return false;
        }

        return blockState.isSolidRender();
    }
}
