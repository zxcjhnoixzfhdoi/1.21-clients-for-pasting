package dev.ambience.inject.mixin;

import com.mixininject.api.Cancel;
import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.hooks.GuiRuntime;
import net.minecraft.client.option.GameOptions;

@Mixin(GameOptions.class)
public class MixinOptionsInject {
   @Inject(method = "getMenuBackgroundBlurrinessValue", at = "RETURN", cancellable = true, captureArgs = true, captureReturn = true)
   public static void frostedGui(GameOptions var0, int var1) {
      if (GuiRuntime.modernBlur()) {
         Cancel.cancel(0);
      }
   }
}
