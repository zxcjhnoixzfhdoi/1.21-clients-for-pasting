package dev.ambience.mixin.client;

import dev.ambience.hooks.XRayHook;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FluidRenderer.class)
public class MixinLiquidBlockRenderer {
   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void ambience$xrayFluids(BlockRenderView var1, BlockPos var2, VertexConsumer var3, BlockState var4, FluidState var5, CallbackInfo var6) {
      if (XRayHook.hidesFluids()) {
         var6.cancel();
      }
   }
}
