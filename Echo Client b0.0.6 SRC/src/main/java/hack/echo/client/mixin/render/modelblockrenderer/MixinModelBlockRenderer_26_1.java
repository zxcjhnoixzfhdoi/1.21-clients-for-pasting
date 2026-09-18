package hack.echo.client.mixin.render.modelblockrenderer;

//? if >=26.1 {
/*import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.XrayModule;
import hack.echo.client.utils.SodiumCompat;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public class MixinModelBlockRenderer_26_1 {

    @Shadow
    private boolean shouldRenderFace(BlockAndTintGetter level, BlockState state, Direction direction, BlockPos neighborPos) {
        throw new AssertionError();
    }

    @Inject(method = "tesselateBlock", at = @At("HEAD"), cancellable = true)
    private void echo$skipHiddenBlocks(
            BlockQuadOutput output,
            float x,
            float y,
            float z,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState blockState,
            BlockStateModel model,
            long seed,
            CallbackInfo ci
    ) {
        if (Echo.isDestroyed) return;

        if (SodiumCompat.isSodiumLoaded()) {
            return;
        }

        XrayModule xray = this.echo$getActiveXray();
        if (xray == null) {
            return;
        }

        if (xray.shouldRenderBlock(blockState.getBlock())) {
            return;
        }

        ci.cancel();
    }

    @Redirect(
            method = "tesselateAmbientOcclusion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;shouldRenderFace(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private boolean echo$alwaysRenderSelectedBlockFacesAmbient(
            ModelBlockRenderer instance,
            BlockAndTintGetter level,
            BlockState blockState,
            Direction direction,
            BlockPos neighborPos
    ) {
        if (Echo.isDestroyed) return this.shouldRenderFace(level, blockState, direction, neighborPos);

        return this.echo$shouldRenderFace(level, blockState, direction, neighborPos);
    }

    @Redirect(
            method = "tesselateFlat",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;shouldRenderFace(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private boolean echo$alwaysRenderSelectedBlockFacesFlat(
            ModelBlockRenderer instance,
            BlockAndTintGetter level,
            BlockState blockState,
            Direction direction,
            BlockPos neighborPos
    ) {
        if (Echo.isDestroyed) return this.shouldRenderFace(level, blockState, direction, neighborPos);

        return this.echo$shouldRenderFace(level, blockState, direction, neighborPos);
    }

    private boolean echo$shouldRenderFace(
            BlockAndTintGetter level,
            BlockState blockState,
            Direction direction,
            BlockPos neighborPos
    ) {
        if (Echo.isDestroyed) return this.shouldRenderFace(level, blockState, direction, neighborPos);

        XrayModule xray = this.echo$getActiveXray();
        if (xray != null && xray.shouldRenderBlock(blockState.getBlock())) {
            return true;
        }

        return this.shouldRenderFace(level, blockState, direction, neighborPos);
    }

    private XrayModule echo$getActiveXray() {
        if (Echo.isDestroyed) return null;

        if (Echo.featureManager == null) {
            return null;
        }

        return XrayModule.getActive();
    }
}
*///?} else {
public final class MixinModelBlockRenderer_26_1 {
}
//?}
