package dev.ambience.inject.mixin;

import com.mixininject.api.Cancel;
import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.hooks.XRayHook;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

@Mixin(FluidRenderer.class)
public class MixinLiquidBlockRendererInject {
   @Inject(method = "render", at = "HEAD", cancellable = true, captureArgs = true)
   public static void xrayFluids(FluidRenderer var0, BlockRenderView var1, BlockPos var2, VertexConsumer var3, BlockState var4, FluidState var5) {
      if (XRayHook.hidesFluids()) {
         Cancel.cancel();
      }
   }
}
