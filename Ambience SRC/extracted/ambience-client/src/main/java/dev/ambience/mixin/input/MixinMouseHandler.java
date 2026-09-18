package dev.ambience.mixin.input;

import dev.ambience.event.impl.input.MouseInputEvent;
import dev.ambience.hooks.AimAssistHook;
import dev.ambience.hooks.GuiRuntime;
import dev.ambience.hooks.ProjectileAimHook;
import dev.ambience.util.traits.Util;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MixinMouseHandler {
   @Inject(method = "updateMouse", at = @At("TAIL"))
   private void ambience$aimAssist(double var1, CallbackInfo var3) {
      ProjectileAimHook.onMouseTurn(var1);
      AimAssistHook.onMouseTurn(var1);
   }

   @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
   private void ambience$onButton(long var1, MouseInput var3, int var4, CallbackInfo var5) {
      if (var4 == 1 || var4 == 0) {
         int var6 = var3.button();
         if (GuiRuntime.handleBindMouse(var6, var4) && var4 == 1) {
            var5.cancel();
         } else {
            if (Util.EVENT_BUS.post(new MouseInputEvent(var6, var4))) {
               var5.cancel();
            }
         }
      }
   }
}
