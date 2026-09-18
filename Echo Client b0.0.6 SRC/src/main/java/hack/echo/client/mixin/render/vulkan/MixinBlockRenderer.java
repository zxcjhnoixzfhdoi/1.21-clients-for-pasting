package hack.echo.client.mixin.render.vulkan;

import hack.echo.client.Echo;
import hack.echo.client.api.LightmapCompat;
import hack.echo.client.features.impl.render.XrayModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.vulkanmod.render.chunk.build.light.data.QuadLightData;
import net.vulkanmod.render.chunk.build.renderer.BlockRenderer;
import net.vulkanmod.render.model.quad.ModelQuadView;
import net.vulkanmod.render.vertex.TerrainBuilder;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

/**
 * Vulkan block renderer override hook for {@link hack.echo.client.features.impl.render.XrayModule}.
 */
@Mixin(value = BlockRenderer.class, remap = false)
public abstract class MixinBlockRenderer {

    @Unique
    private BlockState echo$currentBlockState;

    @Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true)
    private void filterBlockRender(BlockState state, BlockPos pos, Vector3f origin, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        this.echo$currentBlockState = state;

        XrayModule xray = XrayModule.getActive();
        if (xray != null && !xray.shouldRenderBlock(state.getBlock())) {
            ci.cancel();
        }
    }

    @ModifyArg(
            method = "renderBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;emitQuads(Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/QuadEmitter;Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;Ljava/util/function/Predicate;)V",
                    remap = false
            ),
            index = 5,
            require = 0
    )
    private Predicate<?> echo$showAllSelectedBlockFaces_26_1(Predicate<?> cullTest) {
        return this.echo$showAllSelectedBlockFaces(cullTest);
    }

    @ModifyArg(
            method = "renderBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/class_1087;emitQuads(Lnet/fabricmc/fabric/api/renderer/v1/mesh/QuadEmitter;Lnet/minecraft/class_1920;Lnet/minecraft/class_2338;Lnet/minecraft/class_2680;Lnet/minecraft/class_5819;Ljava/util/function/Predicate;)V",
                    remap = false
            ),
            index = 5,
            require = 0
    )
    private Predicate<?> echo$showAllSelectedBlockFaces_1_21_11(Predicate<?> cullTest) {
        return this.echo$showAllSelectedBlockFaces(cullTest);
    }

    @Unique
    private Predicate<?> echo$showAllSelectedBlockFaces(Predicate<?> cullTest) {
        XrayModule xray = this.echo$getActiveXray();
        if (xray == null || this.echo$currentBlockState == null) {
            return cullTest;
        }

        if (!xray.shouldRenderBlock(this.echo$currentBlockState.getBlock())) {
            return cullTest;
        }

        return face -> face == null && cullTest.test(null);
    }

    @Redirect(
            method = "endRenderQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/vulkanmod/render/chunk/build/renderer/BlockRenderer;bufferQuad(Lnet/vulkanmod/render/vertex/TerrainBuilder;Lorg/joml/Vector3f;Lnet/vulkanmod/render/model/quad/ModelQuadView;Lnet/vulkanmod/render/chunk/build/light/data/QuadLightData;)V"
            )
    )
    private void echo$forceFullbrightForXray(
            BlockRenderer instance,
            TerrainBuilder terrainBuilder,
            Vector3f pos,
            ModelQuadView quad,
            QuadLightData quadLightData
    ) {
        XrayModule xray = this.echo$getActiveXray();
        boolean selectedXrayBlock = xray != null
                && this.echo$currentBlockState != null
                && xray.shouldRenderBlock(this.echo$currentBlockState.getBlock());

        if (selectedXrayBlock) {
            for (int i = 0; i < 4; i++) {
                quadLightData.lm[i] = LightmapCompat.FULL_BRIGHT;
                quadLightData.br[i] = 1.0f;
            }
        }

        instance.bufferQuad(terrainBuilder, pos, quad, quadLightData);
    }

    @Unique
    private XrayModule echo$getActiveXray() {
        if (Echo.isDestroyed) {
            return null;
        }

        if (Echo.featureManager == null) {
            return null;
        }

        return XrayModule.getActive();
    }

}
