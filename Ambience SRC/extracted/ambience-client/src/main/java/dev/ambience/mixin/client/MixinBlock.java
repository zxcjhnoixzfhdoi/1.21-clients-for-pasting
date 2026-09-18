package dev.ambience.mixin.client;

import dev.ambience.hooks.XRayHook;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class MixinBlock {
   @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
   private static void ambience$xrayFaces(BlockState var0, BlockState var1, Direction var2, CallbackInfoReturnable<Boolean> var3) {
      if (XRayHook.active()) {
         if (XRayHook.hides(var0)) {
            var3.setReturnValue(false);
         } else {
            var3.setReturnValue(true);
         }
      }
   }
}
