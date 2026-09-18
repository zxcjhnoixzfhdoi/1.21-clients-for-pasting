package dev.ambience.inject.mixin;

import com.mixininject.api.Cancel;
import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.hooks.NoInvisHook;
import dev.ambience.util.traits.Util;
import net.minecraft.entity.Entity;

@Mixin(Entity.class)
public class MixinEntityNoInvisInject {
   @Inject(method = "isInvisible", at = "HEAD", cancellable = true, captureArgs = true)
   public static void isInvisible(Entity var0) {
      if (NoInvisHook.enabled() && Util.mc.player != null) {
         if (var0 != Util.mc.player) {
            Cancel.cancel(Boolean.FALSE);
         }
      }
   }
}
