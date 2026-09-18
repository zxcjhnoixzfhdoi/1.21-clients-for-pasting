package dev.ambience.mixin.client;

import dev.ambience.hooks.GuiRuntime;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameOptions.class)
public class MixinOptions {
   @Inject(method = "getMenuBackgroundBlurrinessValue", at = @At("RETURN"), cancellable = true)
   private void ambience$frostedGui(CallbackInfoReturnable<Integer> var1) {
      if (GuiRuntime.modernBlur()) {
         var1.setReturnValue(0);
      }
   }
}
