package dev.ambience.inject.mixin;

import com.mixininject.api.Cancel;
import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.event.impl.input.MouseInputEvent;
import dev.ambience.hooks.AimAssistHook;
import dev.ambience.hooks.GuiRuntime;
import dev.ambience.hooks.ProjectileAimHook;
import dev.ambience.util.traits.Util;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;

@Mixin(Mouse.class)
public class MixinMouseInject {
   @Inject(method = "onMouseButton", at = "HEAD", cancellable = true, captureArgs = true)
   public static void onButton(Mouse var0, long var1, MouseInput var3, int var4) {
      if (var4 == 1 || var4 == 0) {
         int var5 = var3.button();
         if (GuiRuntime.handleBindMouse(var5, var4) && var4 == 1) {
            Cancel.cancel();
         } else {
            if (Util.EVENT_BUS.post(new MouseInputEvent(var5, var4))) {
               Cancel.cancel();
            }
         }
      }
   }

   @Inject(method = "updateMouse", at = "RETURN", captureArgs = true)
   public static void turnPlayer(Mouse var0, double var1) {
      ProjectileAimHook.onMouseTurn(var1);
      AimAssistHook.onMouseTurn(var1);
   }
}
