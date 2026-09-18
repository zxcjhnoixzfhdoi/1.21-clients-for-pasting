package dev.ambience.inject.mixin;

import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.hooks.AutoTotemHook;
import dev.ambience.hooks.FreecamHook;
import dev.ambience.hooks.JumpResetHook;
import dev.ambience.hooks.STapHook;
import dev.ambience.hooks.WTapHook;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInputInject extends KeyboardInput {
   private MixinKeyboardInputInject(GameOptions var1) {
      super(var1);
   }

   @Inject(at = "RETURN")
   public void tick() {
      FreecamHook.freezeInput();
      WTapHook.freezeInput();
      STapHook.freezeInput();
      JumpResetHook.applyJump();
      AutoTotemHook.freezeInput();
   }
}
