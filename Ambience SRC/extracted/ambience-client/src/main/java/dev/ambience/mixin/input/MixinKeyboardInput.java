package dev.ambience.mixin.input;

import dev.ambience.hooks.AutoTotemHook;
import dev.ambience.hooks.FreecamHook;
import dev.ambience.hooks.JumpResetHook;
import dev.ambience.hooks.STapHook;
import dev.ambience.hooks.WTapHook;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {
   @Inject(method = "tick", at = @At("RETURN"))
   private void ambience$freecamFreeze(CallbackInfo var1) {
      FreecamHook.freezeInput();
      WTapHook.freezeInput();
      STapHook.freezeInput();
      JumpResetHook.applyJump();
      AutoTotemHook.freezeInput();
   }
}
