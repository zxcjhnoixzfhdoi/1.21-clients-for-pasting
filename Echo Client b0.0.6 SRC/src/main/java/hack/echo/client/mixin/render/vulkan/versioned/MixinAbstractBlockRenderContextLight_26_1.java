package hack.echo.client.mixin.render.vulkan.versioned;

import hack.echo.client.Echo;
import hack.echo.client.api.LightmapCompat;
import hack.echo.client.features.impl.render.XrayModule;
import net.vulkanmod.render.chunk.build.light.LightPipeline;
import net.vulkanmod.render.chunk.build.light.data.QuadLightData;
import net.vulkanmod.render.chunk.build.frapi.mesh.MutableQuadViewImpl;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * VulkanMod 0.6.5 lighting hook for x-ray selected blocks.
 */
@Mixin(targets = "net.vulkanmod.render.chunk.build.renderer.AbstractBlockRenderContext", remap = false)
public class MixinAbstractBlockRenderContextLight_26_1 {

    @Shadow
    protected BlockState blockState;

    @Shadow
    @Final
    protected QuadLightData quadLightData;

    @Inject(
            method = "shadeQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/vulkanmod/render/chunk/build/light/LightPipeline;calculate(Lnet/vulkanmod/render/model/quad/ModelQuadView;Lnet/minecraft/core/BlockPos;Lnet/vulkanmod/render/chunk/build/light/data/QuadLightData;Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;Z)V",
                    shift = At.Shift.AFTER
            )
    )
    private void forceFullbrightForXray(MutableQuadViewImpl quad, LightPipeline lightPipeline, boolean emissive, boolean vanillaShade, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        XrayModule xray = XrayModule.getActive();
        if (xray == null || !xray.shouldRenderBlock(this.blockState.getBlock())) {
            return;
        }

        for (int i = 0; i < 4; i++) {
            this.quadLightData.lm[i] = LightmapCompat.FULL_BRIGHT;
            this.quadLightData.br[i] = 1.0f;
        }
    }
}
