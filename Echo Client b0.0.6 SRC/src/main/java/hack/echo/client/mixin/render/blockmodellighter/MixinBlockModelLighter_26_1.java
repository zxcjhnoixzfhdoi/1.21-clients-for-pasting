package hack.echo.client.mixin.render.blockmodellighter;

//? if >=26.1 {
/*import com.mojang.blaze3d.vertex.QuadInstance;
import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.XrayModule;
import hack.echo.client.api.LightmapCompat;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockModelLighter.class)
public class MixinBlockModelLighter_26_1 {

    @Inject(method = "prepareQuadAmbientOcclusion", at = @At("TAIL"))
    private void echo$forceWhiteAmbient(
            BlockAndTintGetter level,
            BlockState state,
            BlockPos centerPosition,
            BakedQuad quad,
            QuadInstance outputInstance,
            CallbackInfo ci
    ) {
        this.echo$forceWhiteForXray(state, outputInstance);
    }

    @Inject(method = "prepareQuadFlat", at = @At("TAIL"))
    private void echo$forceWhiteFlat(
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            int lightCoords,
            BakedQuad quad,
            QuadInstance outputInstance,
            CallbackInfo ci
    ) {
        this.echo$forceWhiteForXray(state, outputInstance);
    }

    private void echo$forceWhiteForXray(BlockState state, QuadInstance outputInstance) {
        if (Echo.isDestroyed) return;

        XrayModule xray = XrayModule.getActive();
        boolean isXrayBlock = xray != null
                && xray.shouldRenderBlock(state.getBlock());

        if (!isXrayBlock) {
            return;
        }

        outputInstance.setLightCoords(LightmapCompat.FULL_BRIGHT);
        outputInstance.setColor(-1);
    }
}
*///?} else {
public final class MixinBlockModelLighter_26_1 {
}
//?}
