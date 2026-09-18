package hack.echo.client.mixin.render.blockrendererdispatcher;

//? if <26.1 {
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.XrayModule;
import hack.echo.client.utils.SodiumCompat;
import java.util.List;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderDispatcher.class)
public class MixinBlockRenderDispatcher_1_21_11 {

    @Inject(method = "renderBatched", at = @At("HEAD"), cancellable = true)
    public void renderBlock(BlockState state, BlockPos pos, BlockAndTintGetter world, PoseStack matrices, VertexConsumer vertexConsumer, boolean cull, List<BlockModelPart> parts, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        if (SodiumCompat.isSodiumLoaded()) {
            return;
        }

        if (Echo.featureManager == null) return;
        XrayModule xray = XrayModule.getActive();
        if (xray != null && !xray.shouldRenderBlock(state.getBlock())) {
            ci.cancel();
        }
    }

    @Inject(method = "renderLiquid", at = @At("HEAD"), cancellable = true)
    public void renderLiquid(BlockPos blockPos, BlockAndTintGetter blockAndTintGetter, VertexConsumer vertexConsumer, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        if (SodiumCompat.isSodiumLoaded()) {
            return;
        }

        if (Echo.featureManager == null) return;
        XrayModule xray = XrayModule.getActive();
        if (xray != null && xray.seeThroughLiquids.getValue()) {
            ci.cancel();
        }
    }
}
//?} else {
/*public final class MixinBlockRenderDispatcher_1_21_11 {
}
*///?}
