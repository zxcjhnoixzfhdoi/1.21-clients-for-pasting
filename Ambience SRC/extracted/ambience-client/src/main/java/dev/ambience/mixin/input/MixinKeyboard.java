package dev.ambience.mixin.input;

import dev.ambience.event.impl.input.KeyInputEvent;
import dev.ambience.util.traits.Util;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class MixinKeyboard {
   @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
   private void onKey(long var1, int var3, KeyInput var4, CallbackInfo var5) {
      if (var3 == 0 || var3 == 1) {
         if (var3 == 1 && Util.EVENT_BUS.post(new KeyInputEvent(var4.key(), var3))) {
            var5.cancel();
         } else if (var3 == 0) {
            Util.EVENT_BUS.post(new KeyInputEvent(var4.key(), var3));
         }
      }
   }
}
