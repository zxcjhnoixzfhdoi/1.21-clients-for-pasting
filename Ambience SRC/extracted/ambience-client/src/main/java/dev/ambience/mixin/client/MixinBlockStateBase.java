package dev.ambience.mixin.client;

import dev.ambience.hooks.XRayHook;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlockState.class)
public abstract class MixinBlockStateBase {
   @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
   private void ambience$xrayRenderShape(CallbackInfoReturnable<BlockRenderType> var1) {
      if (XRayHook.hides(this.asState())) {
         var1.setReturnValue(BlockRenderType.INVISIBLE);
      }
   }

   @Inject(method = "isOpaque", at = @At("HEAD"), cancellable = true)
   private void ambience$xrayOcclude(CallbackInfoReturnable<Boolean> var1) {
      if (XRayHook.hides(this.asState())) {
         var1.setReturnValue(false);
      }
   }

   @Inject(method = "getCullingFace", at = @At("HEAD"), cancellable = true)
   private void ambience$xrayFaceOcclusion(Direction var1, CallbackInfoReturnable<VoxelShape> var2) {
      if (XRayHook.hides(this.asState())) {
         var2.setReturnValue(VoxelShapes.empty());
      }
   }

   @Inject(method = "getCullingShape", at = @At("HEAD"), cancellable = true)
   private void ambience$xrayOcclusionShape(CallbackInfoReturnable<VoxelShape> var1) {
      if (XRayHook.hides(this.asState())) {
         var1.setReturnValue(VoxelShapes.empty());
      }
   }

   @Inject(method = "isOpaqueFullCube", at = @At("HEAD"), cancellable = true)
   private void ambience$xraySolid(CallbackInfoReturnable<Boolean> var1) {
      if (XRayHook.hides(this.asState())) {
         var1.setReturnValue(false);
      }
   }

   @Inject(method = "getOpacity", at = @At("HEAD"), cancellable = true)
   private void ambience$xrayLight(CallbackInfoReturnable<Integer> var1) {
      if (XRayHook.hides(this.asState())) {
         var1.setReturnValue(0);
      }
   }

   @Inject(method = "isTransparent", at = @At("HEAD"), cancellable = true)
   private void ambience$xraySky(CallbackInfoReturnable<Boolean> var1) {
      if (XRayHook.hides(this.asState())) {
         var1.setReturnValue(true);
      }
   }

   @Inject(method = "getAmbientOcclusionLightLevel", at = @At("HEAD"), cancellable = true)
   private void ambience$xrayShade(BlockView var1, BlockPos var2, CallbackInfoReturnable<Float> var3) {
      if (XRayHook.active()) {
         var3.setReturnValue(1.0F);
      }
   }

   private BlockState asState() {
      return ((BlockState)(Object)this);
   }
}
