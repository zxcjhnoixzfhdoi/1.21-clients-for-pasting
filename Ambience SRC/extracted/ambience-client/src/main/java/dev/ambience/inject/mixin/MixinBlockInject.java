package dev.ambience.inject.mixin;

import com.mixininject.api.Cancel;
import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.hooks.XRayHook;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;

@Mixin(Block.class)
public class MixinBlockInject {
   @Inject(method = "shouldDrawSide", at = "HEAD", cancellable = true, captureArgs = true)
   public static void xrayFaces(BlockState var0, BlockState var1, Direction var2) {
      if (XRayHook.active()) {
         if (XRayHook.hides(var0)) {
            Cancel.cancel(Boolean.FALSE);
         } else {
            Cancel.cancel(Boolean.TRUE);
         }
      }
   }
}
