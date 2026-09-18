package dev.ambience.inject.mixin;

import com.mixininject.api.Cancel;
import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.hooks.XRayHook;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

@Mixin(AbstractBlockState.class)
public class MixinBlockStateBaseInject {
   @Inject(method = "getRenderType", at = "HEAD", cancellable = true, captureArgs = true)
   public static void getRenderShape(AbstractBlockState var0) {
      if (XRayHook.hides((BlockState)var0)) {
         Cancel.cancel(BlockRenderType.INVISIBLE);
      }
   }

   @Inject(method = "isOpaque", at = "HEAD", cancellable = true, captureArgs = true)
   public static void canOcclude(AbstractBlockState var0) {
      if (XRayHook.hides((BlockState)var0)) {
         Cancel.cancel(Boolean.FALSE);
      }
   }

   @Inject(method = "getCullingFace", at = "HEAD", cancellable = true, captureArgs = true)
   public static void getFaceOcclusionShape(AbstractBlockState var0, Direction var1) {
      if (XRayHook.hides((BlockState)var0)) {
         Cancel.cancel(VoxelShapes.empty());
      }
   }

   @Inject(method = "getCullingShape", at = "HEAD", cancellable = true, captureArgs = true)
   public static void getOcclusionShape(AbstractBlockState var0) {
      if (XRayHook.hides((BlockState)var0)) {
         Cancel.cancel(VoxelShapes.empty());
      }
   }

   @Inject(method = "isOpaqueFullCube", at = "HEAD", cancellable = true, captureArgs = true)
   public static void isSolidRender(AbstractBlockState var0) {
      if (XRayHook.hides((BlockState)var0)) {
         Cancel.cancel(Boolean.FALSE);
      }
   }

   @Inject(method = "getOpacity", at = "HEAD", cancellable = true, captureArgs = true)
   public static void getLightBlock(AbstractBlockState var0) {
      if (XRayHook.hides((BlockState)var0)) {
         Cancel.cancel(0);
      }
   }

   @Inject(method = "isTransparent", at = "HEAD", cancellable = true, captureArgs = true)
   public static void propagatesSkylightDown(AbstractBlockState var0) {
      if (XRayHook.hides((BlockState)var0)) {
         Cancel.cancel(Boolean.TRUE);
      }
   }

   @Inject(method = "getAmbientOcclusionLightLevel", at = "HEAD", cancellable = true, captureArgs = true)
   public static void getShadeBrightness(AbstractBlockState var0, BlockView var1, BlockPos var2) {
      if (XRayHook.active()) {
         Cancel.cancel(1.0F);
      }
   }
}
