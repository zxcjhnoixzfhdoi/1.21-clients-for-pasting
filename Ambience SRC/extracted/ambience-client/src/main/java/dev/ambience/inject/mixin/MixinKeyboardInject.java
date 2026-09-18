package dev.ambience.inject.mixin;

import com.mixininject.api.Cancel;
import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.event.impl.input.KeyInputEvent;
import dev.ambience.util.traits.Util;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;

@Mixin(Keyboard.class)
public class MixinKeyboardInject {
   @Inject(method = "onKey", at = "HEAD", cancellable = true, captureArgs = true)
   public static void onKey(Keyboard var0, long var1, int var3, KeyInput var4) {
      if (var3 == 0 || var3 == 1) {
         if (var3 == 1 && Util.EVENT_BUS.post(new KeyInputEvent(var4.key(), var3))) {
            Cancel.cancel();
         } else if (var3 == 0) {
            Util.EVENT_BUS.post(new KeyInputEvent(var4.key(), var3));
         }
      }
   }
}
