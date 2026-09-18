package dev.ambience.inject.mixin;

import com.mixininject.api.Cancel;
import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.hooks.FreecamHook;
import dev.ambience.hooks.FreelookHook;
import dev.ambience.util.SilentAim;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;

@Mixin(Entity.class)
public class MixinEntityTurnInject {
   @Inject(method = "changeLookDirection", at = "HEAD", cancellable = true, captureArgs = true)
   public static void onTurn(Entity var0, double var1, double var3) {
      if (var0 instanceof ClientPlayerEntity) {
         if (FreecamHook.isActive()) {
            FreecamHook.onTurn(var1, var3);
            Cancel.cancel();
         } else if (SilentAim.a()) {
            SilentAim.a(var1, var3);
            Cancel.cancel();
         } else {
            if (FreelookHook.isActive()) {
               FreelookHook.onTurn(var1, var3);
               Cancel.cancel();
            }
         }
      }
   }
}
