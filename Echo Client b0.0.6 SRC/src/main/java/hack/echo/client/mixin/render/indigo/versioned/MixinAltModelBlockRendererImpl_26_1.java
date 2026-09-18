package hack.echo.client.mixin.render.indigo.versioned;

//? if >=26.1 {
/*import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.XrayModule;
import hack.echo.client.utils.SodiumCompat;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AltModelBlockRendererImpl.class, remap = false)
public class MixinAltModelBlockRendererImpl_26_1 {

    @Shadow
    private BlockState blockState;

    @Inject(method = "tesselateBlock", at = @At("HEAD"), cancellable = true)
    private void echo$hideNonSelectedBlocksForXray(
            QuadEmitter output,
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
        XrayModule xray = this.echo$getActiveXray();
        if (xray == null) {
            return;
        }

        if (xray.shouldRenderBlock(blockState.getBlock())) {
            return;
        }

        ci.cancel();
    }

    @Inject(method = "shouldCullFace", at = @At("HEAD"), cancellable = true)
    private void echo$drawSelectedBlockFacesAgainstHiddenNeighbors(Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (this.echo$isSelectedXrayBlock()) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "shadeQuad", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean echo$disableAoForXray(boolean ao) {
        return ao && !this.echo$isSelectedXrayBlock();
    }

    @Inject(method = "shadeQuad", at = @At("TAIL"))
    private void echo$forceFullbrightForXray(
            MutableQuadViewImpl quad,
            boolean ao,
            boolean emissive,
            boolean vanillaShade,
            CallbackInfo ci
    ) {
        if (!this.echo$isSelectedXrayBlock()) {
            return;
        }

        quad.lightmap(
                LightCoordsUtil.FULL_BRIGHT,
                LightCoordsUtil.FULL_BRIGHT,
                LightCoordsUtil.FULL_BRIGHT,
                LightCoordsUtil.FULL_BRIGHT
        );

        for (int i = 0; i < 4; i++) {
            quad.color(i, -1);
        }
    }

    private boolean echo$isSelectedXrayBlock() {
        XrayModule xray = this.echo$getActiveXray();
        return xray != null
                && this.blockState != null
                && xray.shouldRenderBlock(this.blockState.getBlock());
    }

    private XrayModule echo$getActiveXray() {
        if (Echo.isDestroyed) {
            return null;
        }

        if (SodiumCompat.isSodiumLoaded() || Echo.featureManager == null) {
            return null;
        }

        return XrayModule.getActive();
    }
}
*///?} else {
public final class MixinAltModelBlockRendererImpl_26_1 {
}
//?}
